package edu.mimuw.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.graph.GraphNode;
import edu.mimuw.sovaide.domain.model.Project;
import edu.mimuw.sovaide.domain.model.repository.ProjectRepository;
import edu.mimuw.sovaide.domain.plugin.DatabaseInterfaces;
import edu.mimuw.sovaide.domain.plugin.GuiComponentData;
import edu.mimuw.sovaide.domain.plugin.PluginResult;
import edu.mimuw.sovaide.domain.plugin.PluginSova;
import edu.mimuw.sovaide.domain.plugin.UserInput;

public class LogParsePlugin implements PluginSova {
	@Override
	public String getName() {
		return "Log Parse plugin";
	}

	@Override
	public String getType() {
		return "INPUT";
	}

	@Override
	public boolean isAcceptingFile() {
		return true;
	}

	@Override
	public PluginResult execute(String projectId, DatabaseInterfaces dbInterfaces, UserInput userInput) {
		String fileUrl = userInput.fileUrl();
		GraphDBFacade facade = dbInterfaces.graphDBFacade();
//		ProjectRepository repository = dbInterfaces.repository();

		if (fileUrl == null || fileUrl.isEmpty()) {
			return createErrorResult(projectId, "No file URL provided");
		}

		Optional<GraphNode> projectOptional = facade.getNodeById(projectId);

		if (projectOptional.isEmpty()) {
			return createErrorResult(projectId, "Expected one project for " + projectId);
		}

		GraphNode projectNode = projectOptional.get();

		try {
			// Read the log file content from the URL
			String logContent = readFileContent(fileUrl);

			GraphNode createdLogNode = facade.createNode("File", Map.of(
					"projectId", projectId,
					"kind", "LOG_FILE",
					"content", logContent
			));
			facade.createEdge(projectNode, createdLogNode, "HAS", Map.of("projectId", projectId));

			// Create the result with the log content as plain text
			Map<String, Object> data = Map.of(
				"type", "text",
				"text", logContent
			);

			Map<String, Object> config = Map.of(
				"padding", "20px",
				"border", "1px solid #ddd",
				"borderRadius", "8px",
				"backgroundColor", "#ffffff",
				"fontFamily", "monospace",
				"whiteSpace", "pre-wrap",
				"overflow", "auto",
				"maxHeight", "600px"
			);

			return new PluginResult(projectId, getName(), new GuiComponentData("Custom", data, config));
		} catch (IOException e) {
			return createErrorResult(projectId, "Error reading log file: " + e.getMessage());
		}
	}

	private String readFileContent(String filePath) throws IOException {
		StringBuilder content = new StringBuilder();

		// Try to parse as URL first
		try {
			// Check if it's a valid URL
			URL url = new URL(filePath);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					content.append(line).append("\n");
				}
			}
			return content.toString();
		} catch (MalformedURLException e) {
			// Not a valid URL, try as a file path
			try {
				Path path = Paths.get(filePath);
				return Files.readString(path);
			} catch (IOException ioe) {
				// If we got here, try one more approach with file:// protocol
				try {
					URL fileUrl = new File(filePath).toURI().toURL();
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(fileUrl.openStream()))) {
						String line;
						while ((line = reader.readLine()) != null) {
							content.append(line).append("\n");
						}
					}
					return content.toString();
				} catch (Exception finalEx) {
					throw new IOException("Failed to read file content from: " + filePath, finalEx);
				}
			}
		}
	}

	private PluginResult createErrorResult(String projectId, String errorMessage) {
		Map<String, Object> data = Map.of(
			"type", "text",
			"text", "ERROR: " + errorMessage
		);

		Map<String, Object> config = Map.of(
			"padding", "20px",
			"border", "1px solid #d32f2f",
			"borderRadius", "8px",
			"backgroundColor", "#ffebee",
			"color", "#d32f2f",
			"fontFamily", "Arial, sans-serif"
		);

		return new PluginResult(projectId, getName(), new GuiComponentData("Custom", data, config));
	}
}
