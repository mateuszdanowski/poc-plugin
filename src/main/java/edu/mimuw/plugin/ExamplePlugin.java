package edu.mimuw.plugin;

import java.util.Map;

import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.plugin.PluginSova;
import edu.mimuw.sovaide.domain.model.repository.ProjectRepository;

public class ExamplePlugin implements PluginSova {

	@Override
	public void execute(ProjectRepository repository, GraphDBFacade graphDBFacade) {
		System.out.println("Hello from ExamplePlugin!");
		graphDBFacade.createNode("ExampleNode", Map.of("prop1", "val1"));
	}
}
