package edu.mimuw.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.mimuw.sovaide.domain.graph.EdgeDirection;
import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.graph.GraphNode;
import edu.mimuw.sovaide.domain.model.repository.ProjectRepository;
import edu.mimuw.sovaide.domain.plugin.GuiComponentData;
import edu.mimuw.sovaide.domain.plugin.PluginResult;
import edu.mimuw.sovaide.domain.plugin.PluginSova;

public class VisualizerPlugin implements PluginSova {

	@Override
	public String getName() {
		return "Visualizer";
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
	public PluginResult execute(String projectId, ProjectRepository projectRepository, GraphDBFacade graphDBFacade, String filePath) {
		List<GraphNode> entities = graphDBFacade.findNodes("Entity", Map.of("projectId", projectId));

		// Prepare nodes for D3.js
		List<Map<String, Object>> nodes = entities.stream()
				.map(entity -> Map.of(
						"id", entity.getId(),
						"name", entity.getProperties().get("name").toString(),
						"packageName", entity.getProperties().getOrDefault("packageName", "")
				))
				.toList();

		// Prepare edges for D3.js
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

		Map<String, Object> config = Map.of(
				"width", 800,
				"height", 600,
				"nodeRadius", 5,
				"linkStrength", Math.sqrt(2)
		);

		return new PluginResult(projectId, getName(), new GuiComponentData("D3Graph", graphData, config));
	}
}
