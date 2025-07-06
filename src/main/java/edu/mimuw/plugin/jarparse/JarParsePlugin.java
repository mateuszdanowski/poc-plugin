package edu.mimuw.plugin.jarparse;

import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.model.repository.ProjectRepository;
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
		repository.findById(projectId).ifPresent(project -> jarParseService.parse(project, fileUrl));
		return null;
	}
}
