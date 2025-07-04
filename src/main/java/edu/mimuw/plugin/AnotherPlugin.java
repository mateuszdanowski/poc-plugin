package edu.mimuw.plugin;

import java.util.List;

import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.model.Project;
import edu.mimuw.sovaide.domain.plugin.PluginSova;
import edu.mimuw.sovaide.domain.model.repository.ProjectRepository;

public class AnotherPlugin implements PluginSova {
	@Override
	public void execute(ProjectRepository repository, GraphDBFacade graphDBFacade) {
		System.out.println("Hello from AnotherPlugin!");
		Project project = new Project();
		project.setName("Project created by a plugin!");
		repository.save(project);

		List<Project> allProjects = repository.findAll();
		System.out.println("All projects in the repository:");
		for (Project p : allProjects) {
			System.out.println(" - " + p.getName() + " (ID: " + p.getId() + ") with " + p.getFiles().size() + " files");
		}
	}
}
