package org.eclipse.osgi.technology.featurelauncher.feature.generator.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

		Reporter reborter = new BaseReporter();

		MavenCoordinatePlugin mcp = new MavenCoordinatePlugin();
		mcp.setReporter(reborter);

		ChecksumPlugin checksumPlugin = new ChecksumPlugin();
		checksumPlugin.setReporter(reborter);

		ComponentsPlugin componentsPlugin = new ComponentsPlugin();
		componentsPlugin.setReporter(reborter);

		MetatypesPlugin metatypesPlugin = new MetatypesPlugin();
		metatypesPlugin.setReporter(reborter);

		ManifestPlugin manifestPlugin = new ManifestPlugin();
		manifestPlugin.setReporter(reborter);

		List<ID> ids = new ArrayList<>();
		Map<ID, File> jarToFile = new HashMap<>();
		Map<ID, MavenCoordinatesDTO> jarToMavenCoordinates = new HashMap<>();
		Map<ID, List<ComponentDescriptionDTO>> jarToCompnendDesc = new HashMap<>();
		Map<ID, List<ObjectClassDefinitionDTO>> jarToOCD = new HashMap<>();
		Map<ID, OSGiHeadersDTO> jarToOSGiHeaders = new HashMap<>();
		Map<ID, ChecksumDTO> jarToOChecksum = new HashMap<>();

		Locale locale = Locale.getDefault();
		for (FeatureBundleMetadata bundle : bundles) {
			File bundleFile = bundle.file();
			ID bundleId = bundle.id();

			Jar jar = new Jar(bundleFile);
			ids.add(bundleId);
			jarToFile.put(bundleId, bundleFile);

			MavenCoordinatesDTO mcd = mcp.extract(jar, locale);
			jarToMavenCoordinates.put(bundleId, mcd);

			List<ComponentDescriptionDTO> componentDescriptionDTOs = componentsPlugin.extract(jar, locale);
			jarToCompnendDesc.put(bundleId, componentDescriptionDTOs);

			List<ObjectClassDefinitionDTO> ocds = metatypesPlugin.extract(jar, locale);
			jarToOCD.put(bundleId, ocds);

			OSGiHeadersDTO osGiHeadersDTO = manifestPlugin.extract(jar, locale);
			jarToOSGiHeaders.put(bundleId, osGiHeadersDTO);

			ChecksumDTO checksumDTO = checksumPlugin.extract(jar, locale);
			jarToOChecksum.put(bundleId, checksumDTO);

		}

		ClasspathLoader loader = new ClasspathLoader(FeatureGeneratorImpl.class.getClassLoader());
		loader.setPrefix("templates");

		// Pebble Engine erstellen
		PebbleEngine engine = new PebbleEngine.Builder().autoEscaping(false).loader(loader)
		        .extension(new PebbleExtension()).build();

		PebbleTemplate template = engine.getTemplate("feature.peb");
		Map<String, Object> context = new HashMap<>();
		context.put("setting", setting);
		context.put("meta", metadata);
		context.put("variables", variables);
		context.put("ids", ids);
		context.put("mvncoordinatesMap", jarToMavenCoordinates);
		context.put("componentsMap", jarToCompnendDesc);
		context.put("ocdsMap", jarToOCD);
		context.put("osgiheaderMap", jarToOSGiHeaders);
		context.put("checksumMap", jarToOChecksum);

		// In Datei schreiben
		try (Writer writer = new FileWriter(json.toFile())) {
			template.evaluate(writer, context);
		}

		return json.toFile();
	}

}
