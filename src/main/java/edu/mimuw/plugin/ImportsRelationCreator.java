package edu.mimuw.plugin;

import java.util.List;
import java.util.Map;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;

import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.graph.GraphNode;
import edu.mimuw.sovaide.domain.model.repository.ProjectRepository;
import edu.mimuw.sovaide.domain.plugin.DatabaseInterfaces;
import edu.mimuw.sovaide.domain.plugin.GuiComponentData;
import edu.mimuw.sovaide.domain.plugin.PluginResult;
import edu.mimuw.sovaide.domain.plugin.PluginSova;

public class ImportsRelationCreator implements PluginSova {

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
	public PluginResult execute(String projectId, DatabaseInterfaces dbInterfaces, String fileUrl) {
		GraphDBFacade graphDBFacade = dbInterfaces.graphDBFacade();

		// find all entities in the project
		List<GraphNode> entities = graphDBFacade.findNodes("Entity", Map.of("projectId", projectId)).stream().toList();

		int totalEntitiesCount = entities.size();
		int importsCreated = 0;

		for (GraphNode entity : entities) {
			String content = entity.getProperties().getOrDefault("content", "").toString();

			try {
				CompilationUnit unit = StaticJavaParser.parse(content);

				String packageName = unit.getPackageDeclaration().isPresent() ?
						unit.getPackageDeclaration().get().getNameAsString() : "";

				String entityName = entity.getProperties().get("name").toString();
				String fullClassName = unit.getTypes().stream().filter(t -> t.getName().asString().equals(entityName))
						.map(decl -> decl.getFullyQualifiedName().orElse(""))
						.findFirst().orElse("");

				graphDBFacade.updateNode(entity.getId(),
						Map.of("packageName", packageName, "fullClassName", fullClassName));
			} catch (Exception e) {
				System.err.println("Error parsing entity " + entity.getId() + ": " + e.getMessage());
			}
		}

		List<GraphNode> entitiesUpdated = graphDBFacade.findNodes("Entity", Map.of("projectId", projectId)).stream()
				.toList();

		for (GraphNode entity : entitiesUpdated) {
			String content = entity.getProperties().getOrDefault("content", "").toString();

			try {
				CompilationUnit unit = StaticJavaParser.parse(content);

				List<ImportDeclaration> imports = unit.getImports().stream().filter(im -> !im.isAsterisk()).toList();

				for (ImportDeclaration importDeclaration : imports) {
					String singleImport = importDeclaration.getNameAsString();
					List<GraphNode> nodes = graphDBFacade.findNodes("Entity",
							Map.of("projectId", projectId, "fullClassName", singleImport));
					if (nodes.isEmpty()) {
						continue;
					}
					if (nodes.size() > 1) {
						System.out.println("Warning: Found " + nodes.size() + " entities for import: " + singleImport
								+ ". Using the first one.");
					}
					GraphNode imported = nodes.getFirst();
					graphDBFacade.createEdge(entity, imported, "IMPORTS", Map.of());
					importsCreated++;
				}
			} catch (Exception e) {
				System.err.println("Error parsing entity " + entity.getId() + ": " + e.getMessage());
			}
		}

		// Create summary text for the custom component
		StringBuilder summaryText = new StringBuilder();
		summaryText.append("Import Relations Creator - Execution Summary\n");
		summaryText.append("============================================\n\n");
		summaryText.append("Total entities found: ").append(totalEntitiesCount).append("\n");
		summaryText.append("Imports created: ").append(importsCreated).append("\n\n");
		summaryText.append("Executed steps:\n");
		summaryText.append("- Updated all entities with package names and full class names\n");
		summaryText.append("- Created import relationship edges between entities\n");

		Map<String, Object> data = Map.of(
				"type", "text",
				"text", summaryText.toString()
		);

		Map<String, Object> config = Map.of(
				"padding", "20px",
				"border", "1px solid #ccc",
				"borderRadius", "5px",
				"backgroundColor", "#f9f9f9",
				"style", Map.of(
						"fontFamily", "monospace",
						"fontSize", "14px",
						"lineHeight", "1.5",
						"whiteSpace", "pre-wrap"
				)
		);

		return new PluginResult(projectId, getName(), new GuiComponentData("Custom", data, config));
	}
}
