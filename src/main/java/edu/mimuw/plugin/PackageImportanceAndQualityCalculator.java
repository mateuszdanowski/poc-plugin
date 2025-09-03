package edu.mimuw.plugin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mimuw.sovaide.domain.graph.EdgeDirection;
import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.graph.GraphEdge;
import edu.mimuw.sovaide.domain.graph.GraphNode;
import edu.mimuw.sovaide.domain.plugin.DatabaseInterfaces;
import edu.mimuw.sovaide.domain.plugin.PluginResult;
import edu.mimuw.sovaide.domain.plugin.PluginSova;
import edu.mimuw.sovaide.domain.plugin.PluginType;
import edu.mimuw.sovaide.domain.plugin.UserInput;

public class PackageImportanceAndQualityCalculator implements PluginSova {
	@Override
	public String getName() {
		return "Package Importance and Quality Calculator";
	}

	@Override
	public PluginType getType() {
		return PluginType.OUTPUT;
	}

	@Override
	public boolean isAcceptingFile() {
		return false;
	}

	@Override
	public PluginResult execute(String projectId, DatabaseInterfaces dbInterfaces, UserInput userInput) {
		GraphDBFacade graphDBFacade = dbInterfaces.graphDBFacade();

		List<GraphNode> classes = graphDBFacade.findNodes("Entity", Map.of("projectId", projectId));
		Set<String> packageNames = getPackageNames(projectId, graphDBFacade);
		System.out.print("PackageNames: " + packageNames);
		Map<String, GraphNode> packageByName = addPackageVertices(projectId, graphDBFacade, packageNames);
		addPackageEdges(projectId, graphDBFacade, packageByName);
		addClassPackageEdges(projectId, graphDBFacade, classes, packageByName);
		addPackageImports(projectId, graphDBFacade);
		addPageRank(projectId, graphDBFacade);
		calculateQuality(projectId, graphDBFacade);
		return null;
	}

	private void calculateQuality(String projectId, GraphDBFacade graphDBFacade) {
		calculateEntitesLinesCount(projectId, graphDBFacade);

		List<GraphNode> nodes = graphDBFacade.findNodes("Package", Map.of("projectId", projectId));

		nodes.forEach(node -> {
			List<GraphEdge> inPackage = graphDBFacade.getEdges(node, "IN_PACKAGE", EdgeDirection.INCOMING);
			int numberOfClasses = inPackage.size();

			if (numberOfClasses > 0) {
				Long classesLinesOfCodeAvg = inPackage.stream()
						.map(GraphEdge::getStartNode)
						.mapToLong(n -> ((Number) n.getProperties()
								.getOrDefault("linesOfCode", 0))
								.longValue())
						.sum() / numberOfClasses;

				graphDBFacade.updateNode(node.getId(), Map.of("quality", classesLinesOfCodeAvg));
			}
		});
	}

	private void calculateEntitesLinesCount(String projectId, GraphDBFacade graphDBFacade) {
		graphDBFacade.findNodes("Entity", Map.of("projectId", projectId)).forEach(entity -> {
			String content = entity.getProperties().getOrDefault("content", "").toString();
			int lineCount = content.split("\r\n|\r|\n").length;
			saveLinesCount(graphDBFacade, entity, lineCount);
		});
	}

	private void saveLinesCount(GraphDBFacade graphDBFacade, GraphNode entity, int lineCount) {
		graphDBFacade.updateNode(entity.getId(), Map.of("linesOfCode", lineCount));
	}

	private void addPageRank(String projectId, GraphDBFacade graphDBFacade) {
		// create an in-memory graph from stored nodes and relations
		String createProjectionQuery = """
				CALL gds.graph.project(
				  'pageRankProjection',
				  'Package',
				  'PACKAGE_IMPORTS'
				)
				""";

		graphDBFacade.executeCypher(createProjectionQuery, Map.of());

		// calculate PageRank and write results back to the nodes
		String pageRankQuery = """
				CALL gds.pageRank.write('pageRankProjection', {
				  writeProperty: 'pagerank'
				})
				YIELD nodePropertiesWritten, ranIterations
				""";

		graphDBFacade.executeCypher(pageRankQuery, Map.of());

		// clean up the projection
		String dropProjectionQuery = """
				CALL gds.graph.drop('pageRankProjection')
				""";

		graphDBFacade.executeCypher(dropProjectionQuery, Map.of());
	}

	private Set<String> getPackageNames(String projectId, GraphDBFacade graphDBFacade) {
		return graphDBFacade.findNodes("Entity", Map.of("projectId", projectId))
				.stream()
				.map(node -> node.getProperties().getOrDefault("packageName", "").toString())
				.collect(Collectors.toSet());
	}

	private Map<String, GraphNode> addPackageVertices(String projectId, GraphDBFacade graphDBFacade, Set<String> packageNames) {
		return packageNames.stream()
				.collect(Collectors.toMap(
						pkgName -> pkgName,
						pkgName -> {
							return graphDBFacade.createNode("Package",
											Map.of("projectId", projectId,
													"pluginId", getName(),
													"name", pkgName));
						}
				));
	}

	private void addPackageEdges(String projectId, GraphDBFacade graphDBFacade, Map<String, GraphNode> packageByName) {
		packageByName.forEach((pkgName, pkgVertex) -> {
			if (!pkgName.isEmpty()) {
				var outerPackage = packageByName.get(getPackageName(pkgName));
				if (outerPackage == null) {
					return;
				}
				graphDBFacade.createEdge(
						pkgVertex, outerPackage, "IN_PACKAGE",
						Map.of("projectId", projectId,
								"pluginId", getName(),
								"kind", "package"));
			}
		});
	}

	private void addClassPackageEdges(String projectId, GraphDBFacade g, List<GraphNode> classes, Map<String, GraphNode> packageByName) {
		classes.forEach(cls -> {
			GraphNode pkgVertex = packageByName.get(cls.getProperties().get("packageName").toString());
			g.createEdge(cls, pkgVertex, "IN_PACKAGE",
					Map.of("projectId", projectId,
									"pluginId", getName(),
									"kind", "class-package"));
		});
	}

	private void addPackageImports(String projectId, GraphDBFacade g) {
		// find all packages that contain classes and get their import relations
		String cypher = """
		        MATCH (pkg:Package {projectId: $projectId, pluginId: $pluginId})
		        <-[:IN_PACKAGE {kind: 'class-package'}]-(cls:Entity)
		        -[:IMPORTS]->(importedCls:Entity)
		        -[:IN_PACKAGE {kind: 'class-package'}]->(importedPkg:Package {projectId: $projectId, pluginId: $pluginId})
		        WHERE pkg <> importedPkg
		        WITH pkg, importedPkg
		        MERGE (pkg)-[:PACKAGE_IMPORTS {projectId: $projectId, pluginId: $pluginId, kind: 'package'}]->(importedPkg)
		        RETURN pkg.name as packageName, importedPkg.name as importsPackage
		        """;

		Map<String, Object> parameters = Map.of(
				"projectId", projectId,
				"pluginId", getName()
		);

		g.executeCypher(cypher, parameters);
	}

	private String getPackageName(String name) {
		if (name.contains(".")) {
			return name.substring(0, name.lastIndexOf('.'));
		} else {
			return "";
		}
	}
}
