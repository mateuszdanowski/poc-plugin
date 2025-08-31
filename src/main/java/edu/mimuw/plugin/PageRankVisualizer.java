package edu.mimuw.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.mimuw.sovaide.domain.graph.EdgeDirection;
import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.graph.GraphNode;
import edu.mimuw.sovaide.domain.plugin.DatabaseInterfaces;
import edu.mimuw.sovaide.domain.plugin.GuiComponentData;
import edu.mimuw.sovaide.domain.plugin.PluginResult;
import edu.mimuw.sovaide.domain.plugin.PluginSova;
import edu.mimuw.sovaide.domain.plugin.UserInput;

public class PageRankVisualizer implements PluginSova {


	@Override
	public String getName() {
		return "PageRank Visualizer";
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
	public PluginResult execute(String projectId, DatabaseInterfaces dbInterfaces, UserInput userInput) {
		GraphDBFacade graphDBFacade = dbInterfaces.graphDBFacade();

		List<GraphNode> packages = graphDBFacade.findNodes("Package", Map.of("projectId", projectId));

		// Prepare nodes for D3.js
		List<Map<String, Object>> nodes = packages.stream()
				.map(pkg -> Map.of(
						"id", pkg.getId(),
						"name", pkg.getProperties().getOrDefault("name", "").toString(),
						"pageRank", pkg.getProperties().getOrDefault("pagerank", "0.0"),
						"quality", pkg.getProperties().getOrDefault("quality", "-1")
				)).toList();

		// Prepare edges for D3.js
		List<Map<String, Object>> links = new ArrayList<>();
		packages.forEach(pkg -> {
			graphDBFacade.getEdges(pkg, "PACKAGE_IMPORTS", EdgeDirection.OUTGOING).forEach(edge -> {
				links.add(Map.of(
						"source", edge.getStartNode().getId(),
						"target", edge.getEndNode().getId(),
						"type", "package-imports"
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
				"linkStrength", Math.sqrt(2),
				"pageRank", true
		);

		return new PluginResult(projectId, getName(), new GuiComponentData("D3Graph", graphData, config));
	}
}
