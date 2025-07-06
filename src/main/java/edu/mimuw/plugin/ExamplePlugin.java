package edu.mimuw.plugin;

import java.util.List;
import java.util.Map;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.graph.GraphNode;
import edu.mimuw.sovaide.domain.plugin.PluginResult;
import edu.mimuw.sovaide.domain.plugin.PluginSova;
import edu.mimuw.sovaide.domain.model.repository.ProjectRepository;

public class ExamplePlugin implements PluginSova {

	@Override
	public String getName() {
		return "Imports relation creator";
	}

	@Override
	public String getType() {
		return "OUTPUT";
	}

	@Override
	public boolean isAcceptingFile() {
		return false;
	}

	@Override
	public PluginResult execute(String projectId, ProjectRepository repository, GraphDBFacade graphDBFacade, String fileUrl) {
		System.out.println("Hello from ExamplePlugin!");
		graphDBFacade.createNode("ExampleNode", Map.of("prop1", "val1", "projectId", projectId));

		long count = graphDBFacade.findNodes("Entity", Map.of("projectId", projectId)).size();
		System.out.println("Found " + count + " entities in project " + projectId);

		// find all entities in the project
		List<GraphNode> entities = graphDBFacade.findNodes("Entity", Map.of("projectId", projectId)).stream().toList();

		entities.forEach(entity -> {
			System.out.println("Processing entity: " + entity.getId());
			String content = entity.getProperties().getOrDefault("content", "").toString();

			CompilationUnit unit = StaticJavaParser.parse(content);

			String packageName = unit.getPackageDeclaration().isPresent() ?
					unit.getPackageDeclaration().get().getNameAsString() : "";

			String entityName = entity.getProperties().get("name").toString();
			String fullClassName = unit.getTypes().stream().filter(t -> t.getName().asString().equals(entityName))
					.map(decl -> decl.getFullyQualifiedName().orElse(""))
					.findFirst().orElse("");

			System.out.println("Updating entity " + entity.getId() + " with new fields: package name: " + packageName + " and full class name: " + fullClassName);
			graphDBFacade.updateNode(entity.getId(), Map.of("packageName", packageName, "fullClassName", fullClassName));
		});

		List<GraphNode> entitiesUpdated = graphDBFacade.findNodes("Entity", Map.of("projectId", projectId)).stream().toList();
		entitiesUpdated.forEach(entity -> {
			System.out.println("Processing entity: " + entity.getId());
			String content = entity.getProperties().getOrDefault("content", "").toString();

			CompilationUnit unit = StaticJavaParser.parse(content);

			unit.getImports().stream().filter(im -> !im.isAsterisk()).forEach(importDeclaration -> {
				String singleImport = importDeclaration.getNameAsString();
				List<GraphNode> nodes = graphDBFacade.findNodes("Entity",
						Map.of("projectId", projectId, "fullClassName", singleImport));
				if (nodes.isEmpty()) {
					return;
				}

				if (nodes.size() > 1) {
					System.out.println("Warning: Found " + nodes.size() + " entities for import: " + singleImport + ". Using the first one.");
				}
				GraphNode imported = nodes.getFirst();
				System.out.println("Creating edge from " + entity.getId() + " to " + imported.getId() + " for import: " + singleImport);
				graphDBFacade.createEdge(entity, imported, "IMPORTS", Map.of());
			});
		});
		return null;
	}
}
