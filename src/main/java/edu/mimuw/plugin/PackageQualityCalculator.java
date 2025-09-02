package edu.mimuw.plugin;

import java.util.List;
import java.util.Map;

import edu.mimuw.sovaide.domain.graph.EdgeDirection;
import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.graph.GraphEdge;
import edu.mimuw.sovaide.domain.graph.GraphNode;
import edu.mimuw.sovaide.domain.plugin.DatabaseInterfaces;
import edu.mimuw.sovaide.domain.plugin.PluginResult;
import edu.mimuw.sovaide.domain.plugin.PluginSova;
import edu.mimuw.sovaide.domain.plugin.PluginType;
import edu.mimuw.sovaide.domain.plugin.UserInput;

public class PackageQualityCalculator implements PluginSova {
	@Override
	public String getName() {
		return "Package Quality Calculator";
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
	public PluginResult execute(String projectId, DatabaseInterfaces databaseInterfaces, UserInput userInput) {
		GraphDBFacade graphDBFacade = databaseInterfaces.graphDBFacade();

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
			// todo add table to result
		});

		return null;
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

}
