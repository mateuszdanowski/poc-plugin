package edu.mimuw.plugin;

import edu.mimuw.sovaide.domain.model.Project;
import edu.mimuw.sovaide.domain.plugin.PluginSova;
import edu.mimuw.sovaide.domain.repository.ProjectRepository;

public class ExamplePlugin implements PluginSova {

	@Override
	public void execute(ProjectRepository repository) {
		System.out.println("Hello from ExamplePlugin!");
		Project project = new Project();
		project.setName("Project created by a plugin!");
		repository.save(project);
	}
}
