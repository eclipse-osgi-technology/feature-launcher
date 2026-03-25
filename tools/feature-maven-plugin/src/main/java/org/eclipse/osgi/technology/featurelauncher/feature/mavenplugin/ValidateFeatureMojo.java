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
package org.eclipse.osgi.technology.featurelauncher.feature.mavenplugin;

import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.FeatureBundleMetadata;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureConfiguration;

import aQute.bnd.osgi.Jar;
import aQute.service.reporter.Report.Location;
import aQute.service.reporter.Reporter;
import aQute.service.reporter.Reporter.SetLocation;
import biz.aQute.bnd.reporter.component.dto.AttributeDefinitionDTO;
import biz.aQute.bnd.reporter.component.dto.ComponentDescriptionDTO;
import biz.aQute.bnd.reporter.component.dto.ObjectClassDefinitionDTO;
import biz.aQute.bnd.reporter.plugins.entries.bundle.ComponentsPlugin;
import biz.aQute.bnd.reporter.plugins.entries.bundle.MetatypesPlugin;

/**
 * Validates a feature JSON file's configurations against the metadata
 * (DS components and Metatype) of its bundles.
 *
 * <p>For each configuration entry in the feature, validates:
 * <ul>
 *   <li>A matching component PID exists in the resolved bundles</li>
 *   <li>Configuration property keys match Metatype attribute IDs (if Metatype available)</li>
 *   <li>Required attributes (no default value) are present</li>
 * </ul>
 *
 * <p>Reports warnings for unmatched PIDs and missing required attributes.
 * Fails the build if {@code failOnError} is true and errors are found.
 */
@Mojo(name = "validate-feature", defaultPhase = LifecyclePhase.VERIFY)
public class ValidateFeatureMojo extends AbstractFeatureMojo {

	@Parameter(property = "feature.validate.featureFile", defaultValue = "${project.build.directory}/feature.json")
	private File featureFile;

	@Parameter(property = "feature.validate.failOnError", defaultValue = "false")
	private boolean failOnError;

	@Parameter(property = "feature.validate.skip", defaultValue = "false")
	private boolean validateSkip;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip || validateSkip) {
			getLog().info("Feature validation skipped.");
			return;
		}

		if (featureFile == null || !featureFile.exists()) {
			getLog().warn("Feature file not found: " + featureFile + " — skipping validation.");
			return;
		}

		getLog().info("Validating feature: " + featureFile);

		try {
			// Parse the feature file
			String json = Files.readString(featureFile.toPath());
			Feature feature = featureService.readFeature(new StringReader(json));

			// Resolve bundles
			List<FeatureBundleMetadata> bundles = resolveAllBundles();

			// Extract component/metatype metadata from all bundles
			Map<String, ComponentDescriptionDTO> componentsByPid = new HashMap<>();
			Map<String, ObjectClassDefinitionDTO> metatypesByPid = new HashMap<>();
			extractMetadata(bundles, componentsByPid, metatypesByPid);

			// Validate configurations
			List<String> errors = new ArrayList<>();
			List<String> warnings = new ArrayList<>();

			Map<String, FeatureConfiguration> configs = feature.getConfigurations();
			if (configs.isEmpty()) {
				getLog().info("No configurations in feature — nothing to validate.");
				return;
			}

			for (Map.Entry<String, FeatureConfiguration> entry : configs.entrySet()) {
				String pid = entry.getKey();
				FeatureConfiguration config = entry.getValue();

				validateConfiguration(pid, config, componentsByPid, metatypesByPid, errors, warnings);
			}

			// Report results
			for (String warning : warnings) {
				getLog().warn("VALIDATION: " + warning);
			}
			for (String error : errors) {
				getLog().error("VALIDATION: " + error);
			}

			getLog().info("Validation complete: " + configs.size() + " configuration(s), "
					+ errors.size() + " error(s), " + warnings.size() + " warning(s)");

			if (!errors.isEmpty() && failOnError) {
				throw new MojoFailureException(
						"Feature validation failed with " + errors.size() + " error(s). Set failOnError=false to continue.");
			}

		} catch (MojoFailureException mfe) {
			throw mfe;
		} catch (Exception e) {
			throw new MojoExecutionException("Error validating feature: " + e.getMessage(), e);
		}
	}

	private void extractMetadata(List<FeatureBundleMetadata> bundles,
			Map<String, ComponentDescriptionDTO> componentsByPid,
			Map<String, ObjectClassDefinitionDTO> metatypesByPid) {

		ComponentsPlugin componentsPlugin = new ComponentsPlugin();
		componentsPlugin.setReporter(new NoOpReporter());
		MetatypesPlugin metatypesPlugin = new MetatypesPlugin();
		metatypesPlugin.setReporter(new NoOpReporter());

		Locale locale = Locale.getDefault();

		for (FeatureBundleMetadata bundle : bundles) {
			File file = bundle.file();
			if (file == null || !file.exists()) continue;

			try (Jar jar = new Jar(file)) {
				List<ComponentDescriptionDTO> components = componentsPlugin.extract(jar, locale);
				List<ObjectClassDefinitionDTO> metatypes = metatypesPlugin.extract(jar, locale);

				if (components != null) {
					for (ComponentDescriptionDTO comp : components) {
						if (comp.configurationPid != null) {
							for (String pid : comp.configurationPid) {
								componentsByPid.put(pid, comp);
							}
						}
					}
				}

				if (metatypes != null) {
					for (ObjectClassDefinitionDTO ocd : metatypes) {
						if (ocd.pids != null) {
							for (String pid : ocd.pids) {
								metatypesByPid.put(pid, ocd);
							}
						}
						if (ocd.factoryPids != null) {
							for (String pid : ocd.factoryPids) {
								metatypesByPid.put(pid, ocd);
							}
						}
					}
				}
			} catch (Exception e) {
				getLog().debug("Could not extract metadata from " + file + ": " + e.getMessage());
			}
		}

		getLog().info("Extracted metadata: " + componentsByPid.size() + " component PID(s), "
				+ metatypesByPid.size() + " metatype PID(s)");
	}

	private void validateConfiguration(String pid, FeatureConfiguration config,
			Map<String, ComponentDescriptionDTO> componentsByPid,
			Map<String, ObjectClassDefinitionDTO> metatypesByPid,
			List<String> errors, List<String> warnings) {

		// Strip factory instance suffix (e.g. "com.example.Pid~instance1" → "com.example.Pid")
		String basePid = pid.contains("~") ? pid.substring(0, pid.indexOf('~')) : pid;

		// Check if a component with this PID exists
		ComponentDescriptionDTO component = componentsByPid.get(basePid);
		if (component == null) {
			warnings.add("Configuration PID '" + pid + "' has no matching component in the resolved bundles");
		}

		// Check against Metatype if available
		ObjectClassDefinitionDTO ocd = metatypesByPid.get(basePid);
		if (ocd == null) {
			warnings.add("Configuration PID '" + pid + "' has no Metatype definition — cannot validate attributes");
			return;
		}

		if (ocd.attributes == null || ocd.attributes.isEmpty()) {
			return;
		}

		// Validate configured keys against Metatype attributes
		Set<String> configKeys = new HashSet<>(config.getValues().keySet());
		Set<String> knownAttrIds = new HashSet<>();

		for (AttributeDefinitionDTO attr : ocd.attributes) {
			knownAttrIds.add(attr.id);

			// Check if required attributes are present
			if (attr.required && !configKeys.contains(attr.id)) {
				errors.add("PID '" + pid + "': required attribute '" + attr.id + "' (type: " + attr.type + ") is missing");
			}
		}

		// Warn about unknown keys
		for (String key : configKeys) {
			if (!knownAttrIds.contains(key) && !key.startsWith(".")) {
				warnings.add("PID '" + pid + "': property '" + key + "' is not defined in Metatype");
			}
		}
	}

	private static class NoOpReporter implements Reporter {
		@Override public SetLocation error(String s, Object... args) { return null; }
		@Override public SetLocation warning(String s, Object... args) { return null; }
		@Override public SetLocation exception(Throwable t, String s, Object... args) { return null; }
		@Override public void trace(String s, Object... args) {}
		@Override public void progress(float v, String s, Object... args) {}
		@Override public boolean isPedantic() { return false; }
		@Override public Location getLocation(String s) { return null; }
		@Override public boolean isOk() { return true; }
		@Override public List<String> getErrors() { return List.of(); }
		@Override public List<String> getWarnings() { return List.of(); }
	}
}
