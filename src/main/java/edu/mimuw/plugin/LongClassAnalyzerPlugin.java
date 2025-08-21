package edu.mimuw.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;

import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.graph.GraphNode;
import edu.mimuw.sovaide.domain.model.repository.ProjectRepository;
import edu.mimuw.sovaide.domain.plugin.DatabaseInterfaces;
import edu.mimuw.sovaide.domain.plugin.GuiComponentData;
import edu.mimuw.sovaide.domain.plugin.PluginResult;
import edu.mimuw.sovaide.domain.plugin.PluginSova;

public class LongClassAnalyzerPlugin implements PluginSova {

	private static final int LONG_CLASS_THRESHOLD = 1000;

	@Override
	public String getName() {
		return "Long Class Analyzer";
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
	public PluginResult execute(String projectId, DatabaseInterfaces dbInterfaces, String filePath) {
		GraphDBFacade graphDBFacade = dbInterfaces.graphDBFacade();

		List<GraphNode> entities = graphDBFacade.findNodes("Entity", Map.of("projectId", projectId));

		List<Map<String, Object>> longClasses = new ArrayList<>();
		int totalClasses = 0;
		int longClassCount = 0;

		for (GraphNode entity : entities) {
			String content = entity.getProperties().getOrDefault("content", "").toString();
			if (content.isEmpty()) {
				continue;
			}

			try {
				CompilationUnit unit = StaticJavaParser.parse(content);

				for (var type : unit.getTypes()) {
					if (type instanceof ClassOrInterfaceDeclaration) {
						totalClasses++;
						int lineCount = countLines(content);

						if (lineCount > LONG_CLASS_THRESHOLD) {
							longClassCount++;

							String packageName = unit.getPackageDeclaration()
								.map(NodeWithName::getNameAsString)
								.orElse("(root package)");

							Map<String, Object> classInfo = new HashMap<>();
							classInfo.put("name", type.getNameAsString());
							classInfo.put("packageName", packageName);
							classInfo.put("lineCount", lineCount);
							classInfo.put("fullName", packageName + "." + type.getNameAsString());

							longClasses.add(classInfo);
						}
					}
				}
			} catch (Exception e) {
				System.err.println("Error parsing entity " + entity.getId() + ": " + e.getMessage());
			}
		}

		// Sort long classes by line count (descending)
		longClasses.sort((a, b) -> Integer.compare(
			(Integer) b.get("lineCount"),
			(Integer) a.get("lineCount")
		));

		// Create HTML table for long classes
		StringBuilder tableHtml = new StringBuilder();
		tableHtml.append("<div style='font-family: Arial, sans-serif;'>");
		tableHtml.append("<h2 style='color: #333; margin-bottom: 20px;'>Long Class Analysis Results</h2>");
		tableHtml.append("<div style='background: #f0f0f0; padding: 15px; border-radius: 5px; margin-bottom: 20px;'>");
		tableHtml.append("<h3 style='margin: 0; color: #666;'>Summary</h3>");
		tableHtml.append("<p style='font-size: 18px; margin: 10px 0;'><strong>");
		tableHtml.append(longClassCount).append("/").append(totalClasses);
		tableHtml.append("</strong> classes are longer than ").append(LONG_CLASS_THRESHOLD).append(" lines</p>");

		if (totalClasses > 0) {
			double percentage = (double) longClassCount / totalClasses * 100;
			tableHtml.append("<p style='margin: 5px 0;'>That's <strong>")
				.append(String.format("%.1f%%", percentage))
				.append("</strong> of all classes in the project</p>");
		}
		tableHtml.append("</div>");

		if (!longClasses.isEmpty()) {
			tableHtml.append("<h3 style='color: #333; margin-bottom: 15px;'>Classes with more than ")
				.append(LONG_CLASS_THRESHOLD).append(" lines:</h3>");
			tableHtml.append("<table style='width: 100%; border-collapse: collapse; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>");
			tableHtml.append("<thead>");
			tableHtml.append("<tr style='background-color: #4CAF50; color: white;'>");
			tableHtml.append("<th style='padding: 12px; text-align: left; border: 1px solid #ddd;'>Class Name</th>");
			tableHtml.append("<th style='padding: 12px; text-align: left; border: 1px solid #ddd;'>Package</th>");
			tableHtml.append("<th style='padding: 12px; text-align: right; border: 1px solid #ddd;'>Lines of Code</th>");
			tableHtml.append("</tr>");
			tableHtml.append("</thead>");
			tableHtml.append("<tbody>");

			for (int i = 0; i < longClasses.size(); i++) {
				Map<String, Object> classInfo = longClasses.get(i);
				String rowColor = i % 2 == 0 ? "#f9f9f9" : "#ffffff";

				tableHtml.append("<tr style='background-color: ").append(rowColor).append(";'>");
				tableHtml.append("<td style='padding: 10px; border: 1px solid #ddd; font-weight: bold;'>")
					.append(classInfo.get("name")).append("</td>");
				tableHtml.append("<td style='padding: 10px; border: 1px solid #ddd; color: #666;'>")
					.append(classInfo.get("packageName")).append("</td>");
				tableHtml.append("<td style='padding: 10px; border: 1px solid #ddd; text-align: right; font-weight: bold; color: #d32f2f;'>")
					.append(classInfo.get("lineCount")).append("</td>");
				tableHtml.append("</tr>");
			}
			tableHtml.append("</tbody>");
			tableHtml.append("</table>");
		} else {
			tableHtml.append("<p style='color: #4CAF50; font-size: 16px; font-weight: bold;'>")
				.append("Great! No classes exceed ").append(LONG_CLASS_THRESHOLD).append(" lines.</p>");
		}
		tableHtml.append("</div>");

		Map<String, Object> data = Map.of(
			"type", "html",
			"html", tableHtml.toString()
		);

		Map<String, Object> config = Map.of(
			"padding", "20px",
			"border", "1px solid #ddd",
			"borderRadius", "8px",
			"backgroundColor", "#ffffff",
			"minHeight", "200px",
			"overflow", "auto"
		);

		return new PluginResult(projectId, getName(), new GuiComponentData("Custom", data, config));
	}

	private int countLines(String content) {
		if (content == null || content.isEmpty()) {
			return 0;
		}
		return content.split("\r\n|\r|\n").length;
	}
}
