package edu.mimuw.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.mimuw.sovaide.domain.graph.EdgeDirection;
import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.graph.GraphNode;
import edu.mimuw.sovaide.domain.plugin.DatabaseInterfaces;
import edu.mimuw.sovaide.domain.plugin.PluginResult;
import edu.mimuw.sovaide.domain.plugin.PluginSova;
import edu.mimuw.sovaide.domain.plugin.PluginType;
import edu.mimuw.sovaide.domain.plugin.UserInput;
import edu.mimuw.sovaide.domain.plugin.frontend.FrontendComponentType;
import edu.mimuw.sovaide.domain.plugin.frontend.GuiComponentData;

public class ClassImportsVisualizer implements PluginSova {

	@Override
	public String getName() {
		return "Class Imports Visualizer";
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

		List<GraphNode> entities = graphDBFacade.findNodes("Entity", Map.of("projectId", projectId));

		List<Map<String, Object>> nodes = entities.stream()
				.map(entity -> Map.of(
						"id", entity.getId(),
						"name", entity.getProperties().getOrDefault("fullClassName", "").toString(),
						"packageName", entity.getProperties().getOrDefault("packageName", "")
				))
				.toList();

		List<Map<String, Object>> links = new ArrayList<>();
		entities.forEach(entity -> {
			graphDBFacade.getEdges(entity, "IMPORTS", EdgeDirection.OUTGOING).forEach(edge -> {
				links.add(Map.of(
						"source", edge.getStartNode().getId(),
						"target", edge.getEndNode().getId(),
						"type", "imports"
				));
			});
		});

		Map<String, Object> graphData = Map.of(
				"nodes", nodes,
				"links", links
		);

		return new PluginResult(projectId, getName(), new GuiComponentData(FrontendComponentType.Graph, graphData, Map.of()));
	}
}
