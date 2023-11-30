/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.build.mavenplugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.gradle.api.DefaultTask;
import org.gradle.api.Task;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import org.springframework.boot.build.mavenplugin.PluginXmlParser.Mojo;
import org.springframework.boot.build.mavenplugin.PluginXmlParser.Parameter;
import org.springframework.boot.build.mavenplugin.PluginXmlParser.Plugin;

/**
 * A {@link Task} to document the plugin's goals.
 *
 * @author Andy Wilkinson
 */
public class DocumentPluginGoals extends DefaultTask {

	private final PluginXmlParser parser = new PluginXmlParser();

	private File pluginXml;

	private File outputDir;

	private Map<String, String> goalSections;

	@OutputDirectory
	public File getOutputDir() {
		return this.outputDir;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	@Input
	public Map<String, String> getGoalSections() {
		return this.goalSections;
	}

	public void setGoalSections(Map<String, String> goalSections) {
		this.goalSections = goalSections;
	}

	@InputFile
	public File getPluginXml() {
		return this.pluginXml;
	}

	public void setPluginXml(File pluginXml) {
		this.pluginXml = pluginXml;
	}

	@TaskAction
	public void documentPluginGoals() throws IOException {
		Plugin plugin = this.parser.parse(this.pluginXml);
		writeOverview(plugin);
		for (Mojo mojo : plugin.getMojos()) {
			documentMojo(plugin, mojo);
		}
	}

	private void writeOverview(Plugin plugin) throws IOException {
		try (PrintWriter writer = new PrintWriter(new FileWriter(new File(this.outputDir, "overview.adoc")))) {
			writer.println("[cols=\"1,3\"]");
			writer.println("|===");
			writer.println("| Goal | Description");
			writer.println();
			for (Mojo mojo : plugin.getMojos()) {
				writer.printf("| <<%s,%s:%s>>%n", goalSectionId(mojo), plugin.getGoalPrefix(), mojo.getGoal());
				writer.printf("| %s%n", mojo.getDescription());
				writer.println();
			}
			writer.println("|===");
		}
	}

	private void documentMojo(Plugin plugin, Mojo mojo) throws IOException {
		try (PrintWriter writer = new PrintWriter(new FileWriter(new File(this.outputDir, mojo.getGoal() + ".adoc")))) {
			String sectionId = goalSectionId(mojo);
			writer.println();
			writer.println();
			writer.printf("[[%s]]%n", sectionId);
			writer.printf("= `%s:%s`%n", plugin.getGoalPrefix(), mojo.getGoal());
			writer.printf("`%s:%s:%s`%n", plugin.getGroupId(), plugin.getArtifactId(), plugin.getVersion());
			writer.println();
			writer.println(mojo.getDescription());
			List<Parameter> parameters = mojo.getParameters().stream().filter(Parameter::isEditable).toList();
			List<Parameter> requiredParameters = parameters.stream().filter(Parameter::isRequired).toList();
			String detailsSectionId = sectionId + ".parameter-details";
			if (!requiredParameters.isEmpty()) {
				writer.println();
				writer.println();
				writer.printf("[[%s.required-parameters]]%n", sectionId);
				writer.println("== Required parameters");
				writeParametersTable(writer, detailsSectionId, requiredParameters);
			}
			List<Parameter> optionalParameters = parameters.stream()
				.filter((parameter) -> !parameter.isRequired())
				.toList();
			if (!optionalParameters.isEmpty()) {
				writer.println();
				writer.println();
				writer.printf("[[%s.optional-parameters]]%n", sectionId);
				writer.println("== Optional parameters");
				writeParametersTable(writer, detailsSectionId, optionalParameters);
			}
			writer.println();
			writer.println();
			writer.printf("[[%s]]%n", detailsSectionId);
			writer.println("== Parameter details");
			writeParameterDetails(writer, parameters, detailsSectionId);
		}
	}

	private String goalSectionId(Mojo mojo) {
		String goalSection = this.goalSections.get(mojo.getGoal());
		if (goalSection == null) {
			throw new IllegalStateException("Goal '" + mojo.getGoal() + "' has not be assigned to a section");
		}
		String sectionId = goalSection + "." + mojo.getGoal() + "-goal";
		return sectionId;
	}

	private void writeParametersTable(PrintWriter writer, String detailsSectionId, List<Parameter> parameters) {
		writer.println("[cols=\"3,2,3\"]");
		writer.println("|===");
		writer.println("| Name | Type | Default");
		writer.println();
		for (Parameter parameter : parameters) {
			String name = parameter.getName();
			writer.printf("| <<%s.%s,%s>>%n", detailsSectionId, parameterId(name), name);
			writer.printf("| `%s`%n", typeNameToJavadocLink(shortTypeName(parameter.getType()), parameter.getType()));
			String defaultValue = parameter.getDefaultValue();
			if (defaultValue != null) {
				writer.printf("| `%s`%n", defaultValue);
			}
			else {
				writer.println("|");
			}
			writer.println();
		}
		writer.println("|===");
	}

	private void writeParameterDetails(PrintWriter writer, List<Parameter> parameters, String sectionId) {
		for (Parameter parameter : parameters) {
			String name = parameter.getName();
			writer.println();
			writer.println();
			writer.printf("[[%s.%s]]%n", sectionId, parameterId(name));
			writer.printf("=== `%s`%n", name);
			writer.println(parameter.getDescription());
			writer.println();
			writer.println("[cols=\"10h,90\"]");
			writer.println("|===");
			writer.println();
			writeDetail(writer, "Name", name);
			writeDetail(writer, "Type", typeNameToJavadocLink(parameter.getType()));
			writeOptionalDetail(writer, "Default value", parameter.getDefaultValue());
			writeOptionalDetail(writer, "User property", parameter.getUserProperty());
			writeOptionalDetail(writer, "Since", parameter.getSince());
			writer.println("|===");
		}
	}

	private String parameterId(String name) {
		StringBuilder id = new StringBuilder(name.length() + 4);
		for (char c : name.toCharArray()) {
			if (Character.isLowerCase(c)) {
				id.append(c);
			}
			else {
				id.append("-");
				id.append(Character.toLowerCase(c));
			}
		}
		return id.toString();
	}

	private void writeDetail(PrintWriter writer, String name, String value) {
		writer.printf("| %s%n", name);
		writer.printf("| `%s`%n", value);
		writer.println();
	}

	private void writeOptionalDetail(PrintWriter writer, String name, String value) {
		writer.printf("| %s%n", name);
		if (value != null) {
			writer.printf("| `%s`%n", value);
		}
		else {
			writer.println("|");
		}
		writer.println();
	}

	private String shortTypeName(String name) {
		if (name.lastIndexOf('.') >= 0) {
			name = name.substring(name.lastIndexOf('.') + 1);
		}
		if (name.lastIndexOf('$') >= 0) {
			name = name.substring(name.lastIndexOf('$') + 1);
		}
		return name;
	}

	private String typeNameToJavadocLink(String name) {
		return typeNameToJavadocLink(name, name);
	}

	private String typeNameToJavadocLink(String shortName, String name) {
		if (name.startsWith("org.springframework.boot.maven")) {
			return "{spring-boot-docs}/maven-plugin/api/" + typeNameToJavadocPath(name) + ".html[" + shortName + "]";
		}
		if (name.startsWith("org.springframework.boot")) {
			return "{spring-boot-docs}/api/" + typeNameToJavadocPath(name) + ".html[" + shortName + "]";
		}
		return shortName;
	}

	private String typeNameToJavadocPath(String name) {
		return name.replace(".", "/").replace("$", ".");
	}

}
