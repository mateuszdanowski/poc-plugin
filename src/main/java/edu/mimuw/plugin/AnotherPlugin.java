package edu.mimuw.plugin;

import java.util.List;

import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.model.Project;
import edu.mimuw.sovaide.domain.plugin.PluginResult;
import edu.mimuw.sovaide.domain.plugin.PluginSova;
import edu.mimuw.sovaide.domain.model.repository.ProjectRepository;

public class AnotherPlugin implements PluginSova {

	@Override
	public String getName() {
		return "Mock project creator";
	}

	@Override
	public String getType() {
		return "INPUT";
	}

	@Override
	public boolean isAcceptingFile() {
		return false;
	}

	@Override
	public PluginResult execute(String projectId, ProjectRepository repository, GraphDBFacade graphDBFacade, String fileUrl) {
		System.out.println("Hello from AnotherPlugin!");
		Project project = new Project();
		project.setName("Project created by a plugin!");
		repository.save(project);

		List<Project> allProjects = repository.findAll();
		System.out.println("All projects in the repository:");
		for (Project p : allProjects) {
			System.out.println(" - " + p.getName() + " (ID: " + p.getId() + ") with " + p.getFiles().size() + " files");
		}
		return null;
	}
}
