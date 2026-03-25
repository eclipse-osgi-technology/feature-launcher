/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Stefan Bischof - initial
 */
package org.eclipse.osgi.technology.featurelauncher.feature.generator.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.eclipse.osgi.technology.featurelauncher.feature.generator.FeatureBundleMetadata;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.FeatureGenerator;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.FeatureMetadata;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.Setting;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.impl.pebble.PebbleExtension;
import org.osgi.service.feature.ID;

import aQute.bnd.osgi.Jar;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.component.dto.ComponentDescriptionDTO;
import biz.aQute.bnd.reporter.component.dto.ObjectClassDefinitionDTO;
import biz.aQute.bnd.reporter.manifest.dto.OSGiHeadersDTO;
import biz.aQute.bnd.reporter.maven.dto.ChecksumDTO;
import biz.aQute.bnd.reporter.maven.dto.MavenCoordinatesDTO;
import biz.aQute.bnd.reporter.plugins.entries.bundle.ChecksumPlugin;
import biz.aQute.bnd.reporter.plugins.entries.bundle.ComponentsPlugin;
import biz.aQute.bnd.reporter.plugins.entries.bundle.ManifestPlugin;
import biz.aQute.bnd.reporter.plugins.entries.bundle.MavenCoordinatePlugin;
import biz.aQute.bnd.reporter.plugins.entries.bundle.MetatypesPlugin;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.template.PebbleTemplate;

public class FeatureGeneratorImpl implements FeatureGenerator {

	@Override
	public File generate(Setting setting, FeatureMetadata metadata, List<FeatureBundleMetadata> bundles,
	        Map<String, Object> variables) throws Exception {
		Path json = setting.outputDirectory().toPath().resolve(setting.fileName());
		Files.createDirectories(setting.outputDirectory().toPath());

		Reporter reporter = new BaseReporter();

		MavenCoordinatePlugin mcp = new MavenCoordinatePlugin();
		mcp.setReporter(reporter);

		ChecksumPlugin checksumPlugin = new ChecksumPlugin();
		checksumPlugin.setReporter(reporter);

		ComponentsPlugin componentsPlugin = new ComponentsPlugin();
		componentsPlugin.setReporter(reporter);

		MetatypesPlugin metatypesPlugin = new MetatypesPlugin();
		metatypesPlugin.setReporter(reporter);

		ManifestPlugin manifestPlugin = new ManifestPlugin();
		manifestPlugin.setReporter(reporter);

		List<ID> ids = new ArrayList<>();
		Map<ID, File> jarToFile = new HashMap<>();
		Map<ID, MavenCoordinatesDTO> jarToMavenCoordinates = new HashMap<>();
		Map<ID, List<ComponentDescriptionDTO>> jarToComponentDesc = new HashMap<>();
		Map<ID, List<ObjectClassDefinitionDTO>> jarToOCD = new HashMap<>();
		Map<ID, OSGiHeadersDTO> jarToOSGiHeaders = new HashMap<>();
		Map<ID, ChecksumDTO> jarToChecksum = new HashMap<>();

		Locale locale = Locale.getDefault();
		for (FeatureBundleMetadata bundle : bundles) {
			File bundleFile = bundle.file();
			ID bundleId = bundle.id();

			try (Jar jar = new Jar(bundleFile)) {
				ids.add(bundleId);
				jarToFile.put(bundleId, bundleFile);

				MavenCoordinatesDTO mcd = mcp.extract(jar, locale);
				jarToMavenCoordinates.put(bundleId, mcd);

				List<ComponentDescriptionDTO> componentDescriptionDTOs = componentsPlugin.extract(jar, locale);
				jarToComponentDesc.put(bundleId, componentDescriptionDTOs);

				List<ObjectClassDefinitionDTO> ocds = metatypesPlugin.extract(jar, locale);
				jarToOCD.put(bundleId, ocds);

				OSGiHeadersDTO osGiHeadersDTO = manifestPlugin.extract(jar, locale);
				jarToOSGiHeaders.put(bundleId, osGiHeadersDTO);

				ChecksumDTO checksumDTO = checksumPlugin.extract(jar, locale);
				jarToChecksum.put(bundleId, checksumDTO);
			}
		}

		ClasspathLoader loader = new ClasspathLoader(FeatureGeneratorImpl.class.getClassLoader());
		loader.setPrefix("templates");

		// Create Pebble Engine
		PebbleEngine engine = new PebbleEngine.Builder().autoEscaping(false).loader(loader)
		        .extension(new PebbleExtension()).build();

		File jsonFile = writeFeatureJson(setting, metadata, variables, json, ids, jarToMavenCoordinates,
		        jarToComponentDesc, jarToOCD, jarToOSGiHeaders, jarToChecksum, engine);

		Path repoFolder = setting.outputDirectory().toPath().resolve("repo");
		Files.createDirectories(repoFolder);

		for (FeatureBundleMetadata bundle : bundles) {
			ID id = bundle.id();
			List<ComponentDescriptionDTO> componentDesc = jarToComponentDesc.get(id);
			List<ObjectClassDefinitionDTO> oCD = jarToOCD.get(id);

			if (componentDesc == null || componentDesc.isEmpty() || oCD == null || oCD.isEmpty()) {
				continue;
			}
			writeBundleConfig(setting, id, componentDesc, oCD, engine);

			for (ComponentDescriptionDTO component : componentDesc) {
				writeConfig(setting, component, oCD, engine);
			}
		}

		for (FeatureBundleMetadata bundle : bundles) {
			copyToMavenRepoLayout(bundle.id(), bundle.file().toPath(), repoFolder);
		}
		return jsonFile;
	}

	private File writeBundleConfig(Setting setting, ID id, List<ComponentDescriptionDTO> componentDesc,
	        List<ObjectClassDefinitionDTO> oCD, PebbleEngine engine) throws IOException {
		PebbleTemplate template = engine.getTemplate("bundleConfigs.peb");
		Map<String, Object> context = new HashMap<>();
		context.put("components", componentDesc);
		context.put("ocds", oCD);

		File json = setting.outputDirectory().toPath().resolve("bundle.cfg_" + id.toString() + ".json").toFile();
		try (Writer writer = new FileWriter(json)) {
			template.evaluate(writer, context);
		}

		return json;
	}

	private File writeConfig(Setting setting, ComponentDescriptionDTO componentDesc,
	        List<ObjectClassDefinitionDTO> oCD, PebbleEngine engine) throws IOException {
		PebbleTemplate template = engine.getTemplate("configs.peb");
		Map<String, Object> context = new HashMap<>();
		context.put("component", componentDesc);
		context.put("ocds", oCD);

		File json = setting.outputDirectory().toPath()
		        .resolve("cfg_" + componentDesc.configurationPid.toString() + ".json").toFile();
		try (Writer writer = new FileWriter(json)) {
			template.evaluate(writer, context);
		}

		return json;
	}

	private File writeFeatureJson(Setting setting, FeatureMetadata metadata, Map<String, Object> variables, Path json,
	        List<ID> ids, Map<ID, MavenCoordinatesDTO> jarToMavenCoordinates,
	        Map<ID, List<ComponentDescriptionDTO>> jarToComponentDesc, Map<ID, List<ObjectClassDefinitionDTO>> jarToOCD,
	        Map<ID, OSGiHeadersDTO> jarToOSGiHeaders, Map<ID, ChecksumDTO> jarToChecksum, PebbleEngine engine)
	        throws IOException {
		PebbleTemplate template = engine.getTemplate("feature.peb");
		Map<String, Object> context = new HashMap<>();
		context.put("setting", setting);
		context.put("meta", metadata);
		context.put("variables", variables);
		context.put("ids", ids);
		context.put("mvncoordinatesMap", jarToMavenCoordinates);
		context.put("componentsMap", jarToComponentDesc);
		context.put("ocdsMap", jarToOCD);
		context.put("osgiheaderMap", jarToOSGiHeaders);
		context.put("checksumMap", jarToChecksum);

		try (Writer writer = new FileWriter(json.toFile())) {
			template.evaluate(writer, context);
		}

		return json.toFile();
	}

	/**
	 * Copies a file into a target directory following the standard Maven-repository
	 * layout.
	 *
	 * <p>
	 * The resulting path will be:
	 *
	 * <pre>{@code
	 * <targetDir>/<groupId path>/<artifactId>/<version>/<artifactId>-<version>[-<classifier>].<type>
	 * }</pre>
	 *
	 * @param id        Maven coordinates (groupId, artifactId, version, optional
	 *                  type/classifier)
	 * @param source    the source file (e.g. a JAR, POM, AAR)
	 * @param targetDir the base directory that should act as the root of the
	 *                  repository layout
	 * @return the {@link Path} of the copied file
	 * @throws IOException if the file cannot be copied
	 */
	private static Path copyToMavenRepoLayout(ID id, Path source, Path targetDir) throws IOException {
		String groupPath = id.getGroupId().replace('.', '/');
		String artifactId = id.getArtifactId();
		String version = id.getVersion();
		String type = id.getType().orElse("jar");
		Optional<String> classifier = id.getClassifier();

		StringBuilder fileName = new StringBuilder();
		fileName.append(artifactId).append("-").append(version);
		classifier.ifPresent(c -> fileName.append("-").append(c));
		fileName.append(".").append(type);

		Path targetPath = targetDir.resolve(groupPath).resolve(artifactId).resolve(version)
		        .resolve(fileName.toString());

		Files.createDirectories(targetPath.getParent());
		return Files.copy(source, targetPath, StandardCopyOption.REPLACE_EXISTING);
	}
}
