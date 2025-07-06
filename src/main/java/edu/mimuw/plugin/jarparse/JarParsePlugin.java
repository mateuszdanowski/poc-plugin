package edu.mimuw.plugin.jarparse;

import java.util.Map;

import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.model.repository.ProjectRepository;
import edu.mimuw.sovaide.domain.plugin.GuiComponentData;
import edu.mimuw.sovaide.domain.plugin.PluginResult;
import edu.mimuw.sovaide.domain.plugin.PluginSova;

public class JarParsePlugin implements PluginSova {
	@Override
	public String getName() {
		return "JarParsePlugin";
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
	public PluginResult execute(String projectId, ProjectRepository repository, GraphDBFacade graphDBFacade, String fileUrl) {
		JarParseService jarParseService = new JarParseService(repository, graphDBFacade);

		int filesProcessed = 0;
		int entitiesCreated = 0;

		var projectOptional = repository.findById(projectId);
		if (projectOptional.isPresent()) {
			var project = projectOptional.get();
			jarParseService.parse(project, fileUrl);

			if (project.getFiles() != null) {
				filesProcessed = project.getFiles().size();
				entitiesCreated = project.getFiles().stream()
					.mapToInt(file -> file.getEntities() != null ? file.getEntities().size() : 0)
					.sum();
			}
		}

		StringBuilder summaryText = new StringBuilder();
		summaryText.append("JAR Parse Plugin - Execution Summary\n");
		summaryText.append("====================================\n\n");
		summaryText.append("Project ID: ").append(projectId).append("\n");
		summaryText.append("JAR File: ").append(fileUrl).append("\n");
		summaryText.append("Files processed: ").append(filesProcessed).append("\n");
		summaryText.append("Entities created: ").append(entitiesCreated).append("\n\n");
		summaryText.append("Executed steps:\n");
		summaryText.append("- Extracted and parsed JAR file contents\n");
		summaryText.append("- Identified Java source files and resources\n");
		summaryText.append("- Created entities for classes, interfaces, enums, etc.\n");
		summaryText.append("- Extracted members (fields, methods, constructors)\n");
		summaryText.append("- Saved project structure to repository\n\n");
		summaryText.append("The JAR file has been successfully parsed and integrated\n");
		summaryText.append("into the project structure for further analysis.");

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
