package edu.mimuw.plugin.logparse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.graph.GraphNode;
import edu.mimuw.sovaide.domain.plugin.DatabaseInterfaces;
import edu.mimuw.sovaide.domain.plugin.PluginResult;
import edu.mimuw.sovaide.domain.plugin.PluginSova;
import edu.mimuw.sovaide.domain.plugin.PluginType;
import edu.mimuw.sovaide.domain.plugin.UserInput;
import edu.mimuw.sovaide.domain.plugin.frontend.FrontendComponentType;
import edu.mimuw.sovaide.domain.plugin.frontend.GuiComponentData;

public class LogMethodTimeVisualizer implements PluginSova {
	@Override
	public String getName() {
		return "Log Method Time Visualizer";
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
		// Example multi-line log
		GraphDBFacade facade = dbInterfaces.graphDBFacade();

		List<GraphNode> logFileNodes = facade.findNodes("File", Map.of("projectId", projectId, "kind", "LOG_FILE"));

		if (logFileNodes.size() != 1) {
			throw new RuntimeException("Expected exactly one log file for project " + projectId);
		}

		GraphNode logFile = logFileNodes.getFirst();

		String logs = logFile.getProperties().get("content").toString();

		// Regex pattern
		String regex = "^(?<timestamp>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+Z) \\[INFO\\] (?<class>[\\w\\.]+)$";
		Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);

		Matcher matcher = pattern.matcher(logs);

		// Map to collect timestamps for each class
		Map<String, List<Long>> classTimestamps = new HashMap<>();

		while (matcher.find()) {
			String timestamp = matcher.group("timestamp");
			String className = matcher.group("class");

			long millis = Instant.parse(timestamp).toEpochMilli();
			classTimestamps.computeIfAbsent(className, k -> new ArrayList<>()).add(millis);
		}

		// Map to store duration for each class
		Map<String, Long> classDurations = new HashMap<>();
		for (Map.Entry<String, List<Long>> entry : classTimestamps.entrySet()) {
			List<Long> times = entry.getValue();
			if (!times.isEmpty()) {
				long min = times.stream().min(Long::compare).get();
				long max = times.stream().max(Long::compare).get();
				classDurations.put(entry.getKey(), max - min);
			}
		}

		// Prepare data for BarChart component: { "methodA": 10, ... }
		Map<String, Object> chartData = new HashMap<>();
		for (Map.Entry<String, Long> entry : classDurations.entrySet()) {
			chartData.put(entry.getKey(), entry.getValue().intValue());
		}
//
//		// Table config with style from LongClassFinder
//		Map<String, Object> config = new HashMap<>();
//		config.put("padding", "20px");
//		config.put("border", "1px solid #ddd");
//		config.put("borderRadius", "8px");
//		config.put("backgroundColor", "#ffffff");
//		config.put("minHeight", "200px");
//		config.put("overflow", "auto");

		return new PluginResult(projectId, getName(), new GuiComponentData(FrontendComponentType.BarChart, chartData, Map.of()));
	}
}
