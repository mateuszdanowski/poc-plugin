package edu.mimuw.plugin.jarparse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import edu.mimuw.sovaide.domain.graph.GraphDBFacade;
import edu.mimuw.sovaide.domain.model.Entity;
import edu.mimuw.sovaide.domain.model.EntityKind;
import edu.mimuw.sovaide.domain.model.File;
import edu.mimuw.sovaide.domain.model.FileKind;
import edu.mimuw.sovaide.domain.model.Member;
import edu.mimuw.sovaide.domain.model.MemberKind;
import edu.mimuw.sovaide.domain.model.Project;
import edu.mimuw.sovaide.domain.model.repository.ProjectRepository;

public class JarParseService {

	private final ProjectRepository projectRepository;
	private final GraphDBFacade graphDBFacade;

	public JarParseService(ProjectRepository projectRepository, GraphDBFacade graphDBFacade) {
		this.projectRepository = projectRepository;
		this.graphDBFacade = graphDBFacade;
	}

	public void parse(Project project, String localFilePath) {
		String projectId = project.getId();
		List<File> allFiles = new ArrayList<>();

		try (JarFile jarFile = new JarFile(localFilePath)) {
			System.out.println("Parsing JAR file: " + localFilePath);
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				if (entry.isDirectory()) continue;
				String entryName = entry.getName();
				String content;
				try (InputStream is = jarFile.getInputStream(entry)) {
					content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
					FileKind kind = readFileKind(entryName);
					List<Entity> entities = findEntities(entryName, content, kind, projectId);
					File file = new File();
					file.setProjectId(projectId);
					file.setKind(kind);
					file.setPath(entryName);
					file.setContent(content);
					file.setEntities(entities);
					allFiles.add(file);
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}

		project.setFiles(allFiles);
		projectRepository.save(project);
	}

	private List<Entity> findEntities(String entryName, String content, FileKind kind, String projectId) {
		if (kind == FileKind.SOURCE_FILE || kind == FileKind.TEST_FILE) {
			try {
				final String cont = content;
				CompilationUnit unit = StaticJavaParser.parse(cont);
				return unit.getTypes().stream().map(type -> {
					EntityKind entityKind = findEntityKind(type);
					List<Member> members = (entityKind == EntityKind.CLASS || entityKind == EntityKind.INTERFACE) ?
							getMembers(type, projectId) : List.of();
					Entity entity = new Entity();
					entity.setProjectId(projectId);
					entity.setKind(entityKind);
					entity.setContent(content);
					entity.setName(type.getNameAsString());
					entity.setMembers(members);
					return entity;
				}).toList();
			} catch (ParseProblemException e) {
				System.err.println("Error when parsing " + entryName + ": " + e.getMessage());
			}
		}
		return List.of();
	}

	private List<Member> getMembers(TypeDeclaration<?> type, String projectId) {
		return type.getMembers().stream().map(member -> {
			MemberKind memberKind;
			if (member.isFieldDeclaration()) {
				memberKind = MemberKind.FIELD;
			} else if (member.isMethodDeclaration()) {
				memberKind = MemberKind.METHOD;
			} else if (member.isConstructorDeclaration()) {
				memberKind = MemberKind.CONSTRUCTOR;
			} else {
				memberKind = MemberKind.OTHER;
			}
			Member memberObj = new Member();
			memberObj.setProjectId(projectId);
			memberObj.setContent(member.toString());
			memberObj.setKind(memberKind);
			return memberObj;
		}).toList();
	}

	private EntityKind findEntityKind(TypeDeclaration<?> type) {
		if (type.isClassOrInterfaceDeclaration()) {
			ClassOrInterfaceDeclaration coid = type.asClassOrInterfaceDeclaration();
			if (coid.isInterface()) {
				return EntityKind.INTERFACE;
			} else {
				return EntityKind.CLASS;
			}
		} else if (type.isEnumDeclaration()) {
			return EntityKind.ENUM;
		} else if (type.isAnnotationDeclaration()) {
			return EntityKind.ANNOTATION;
		} else if (type.isRecordDeclaration()) {
			return EntityKind.RECORD;
		} else {
			return EntityKind.OTHER;
		}
	}

	private FileKind readFileKind(String entryName) {
		if (entryName.endsWith(".java")) {
			return FileKind.SOURCE_FILE;
		} else if (entryName.endsWith("Test.java")) {
			return FileKind.TEST_FILE;
		} else if (entryName.startsWith("resources/") ||
				entryName.contains("/resources/") ||
				isResourceFile(entryName)) {
			return FileKind.RESOURCE;
		}
		return FileKind.OTHER;
	}

	private boolean isResourceFile(String fileName) {
		return fileName.matches(".+\\.(properties|xml|yml|yaml|json|txt|css|js|html|jsp|sql|md)$") ||
				fileName.matches("^META-INF/.+") ||
				fileName.matches("^static/.+") ||
				fileName.matches("^templates/.+");
	}
}
