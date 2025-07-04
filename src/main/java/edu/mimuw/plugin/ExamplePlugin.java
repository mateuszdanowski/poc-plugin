package edu.mimuw.plugin;

import java.util.Map;

import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.plugin.PluginSova;
import edu.mimuw.sovaide.domain.model.repository.ProjectRepository;

public class ExamplePlugin implements PluginSova {

	@Override
	public void execute(String projectId, ProjectRepository repository, GraphDBFacade graphDBFacade) {
		System.out.println("Hello from ExamplePlugin!");
		graphDBFacade.createNode("ExampleNode", Map.of("prop1", "val1", "projectId", projectId));

		long count = graphDBFacade.findNodes("Entity", Map.of("projectId", projectId)).size();
		System.out.println("Found " + count + " entities in project " + projectId);
	}
}
