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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.feature.Feature;

import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.ConfigSetting;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.FeatureBundleMetadata;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.FeatureBundleSetting;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.FeatureGenerator;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.FeatureMetadata;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.HashSetting;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.Setting;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.impl.FeatureGeneratorImpl;

/**
 * Mojo to generate an OSGi feature JSON file based on project configuration.
 */
@Mojo(name = "create-feature", defaultPhase = LifecyclePhase.PACKAGE)
public class FeatureGeneratorMojo extends AbstractFeatureMojo {

	@Parameter(property = "feature.outputFileName", defaultValue = "feature.json")
	private String outputFileName;

	@Parameter(property = "config.by.feature", defaultValue = "false")
	private boolean configByFeature;

	@Parameter(property = "config.by.bundle", defaultValue = "false")
	private boolean configByBundle;

	@Parameter(property = "config.by.pid", defaultValue = "false")
	private boolean configByPid;

	@Parameter(property = "bundle.export", defaultValue = "true")
	private boolean bundleExport;

	@Parameter(property = "bundle.hash.md5", defaultValue = "false")
	private boolean bundleHashMd5;
	@Parameter(property = "bundle.hash.sha1", defaultValue = "false")
	private boolean bundleHashSha1;
	@Parameter(property = "bundle.hash.sha256", defaultValue = "false")
	private boolean bundleHashSha256;
	@Parameter(property = "bundle.hash.sha512", defaultValue = "false")
	private boolean bundleHashSha512;

	/**
	 * Feature files whose variables, configurations, and extensions are merged
	 * into the generated feature. Bundles are NOT merged (use include/features for that).
	 * Later files override earlier ones on key conflicts.
	 */
	@Parameter
	private List<File> mergeFeatures;

	/**
	 * If set, copies resolved bundles to this directory in Maven GAV layout.
	 * Example: {@code ${project.build.directory}/repo}
	 */
	@Parameter(property = "feature.repoDirectory")
	private File repoDirectory;

	/**
	 * If set, copies the generated feature.json into this directory.
	 * Example: {@code ${project.build.directory}/features}
	 */
	@Parameter(property = "feature.featuresDirectory")
	private File featuresDirectory;

	private FeatureGenerator generator = new FeatureGeneratorImpl();

	public void execute() throws MojoExecutionException {
		getLog().info("Starting OSGi Feature generation for project: " + project.getArtifactId());

		if (skip) {
			getLog().info("Feature generation skipped by configuration.");
			return;
		}

		try {
			List<FeatureBundleMetadata> featureBundles = resolveAllBundles();

			String gav = buildProjectGav();

			Optional<String> name = Optional.ofNullable(project.getName());
			Optional<String> description = Optional.ofNullable(project.getDescription());
			Optional<String> docURL = Optional
					.ofNullable(project.getIssueManagement() != null ? project.getIssueManagement().getUrl() : null);
			Optional<String> license = Optional.ofNullable(getProjectLicense());
			Optional<String> scm = Optional.ofNullable(project.getScm() != null ? project.getScm().getUrl() : null);
			Optional<String> vendor = Optional.ofNullable(getProjectVendor());
			List<String> categories = extractProjectCategories();

			boolean complete = false;

			Map<String, Object> variables = extractProjectVariables();

			FeatureMetadata fmd = new FeatureMetadata(gav, name, description, docURL, license, scm, vendor, categories,
					complete);

			FeatureBundleSetting fbs = new FeatureBundleSetting(bundleExport,
					new HashSetting(bundleHashMd5, bundleHashSha1, bundleHashSha256, bundleHashSha512));

			ConfigSetting cs = new ConfigSetting(configByFeature, configByBundle, configByPid);

			Setting setting = new Setting(outputDirectory, outputFileName, fbs, cs);

			for (FeatureBundleMetadata b : featureBundles) {
				getLog().warn("Bundle: " + b.id() + " -> " + b.file());
			}
			File file = generator.generate(setting, fmd, featureBundles, variables);

			// Merge variables/configurations/extensions from other features
			if (mergeFeatures != null && !mergeFeatures.isEmpty()) {
				file = mergeIntoGeneratedFeature(file);
			}

			getLog().info("Feature file written to: " + file.toPath().toAbsolutePath());

			// Export bundles to repo directory (Maven GAV layout)
			if (repoDirectory != null) {
				copyBundlesToGavLayout(featureBundles, repoDirectory.toPath());
				getLog().info("Bundles exported to repo: " + repoDirectory.getAbsolutePath());
			}

			// Export feature.json to features directory
			if (featuresDirectory != null) {
				Files.createDirectories(featuresDirectory.toPath());
				Files.copy(file.toPath(),
						featuresDirectory.toPath().resolve(file.getName()),
						StandardCopyOption.REPLACE_EXISTING);
				getLog().info("Feature exported to: " + featuresDirectory.getAbsolutePath());
			}

			projectHelper.attachArtifact(project, "json", file);
			getLog().info("Feature file attached to project artifacts.");

		} catch (Exception e) {
			throw new MojoExecutionException("Error generating feature: " + e.getMessage(), e);
		}
	}

	private File mergeIntoGeneratedFeature(File generatedFile) throws IOException {
		getLog().info("Merging " + mergeFeatures.size() + " feature(s) into generated feature...");

		Feature base = parseFeatureFile(generatedFile);

		List<Feature> sources = new ArrayList<>();
		for (File mergeFile : mergeFeatures) {
			if (mergeFile.exists()) {
				sources.add(parseFeatureFile(mergeFile));
				getLog().info("  Merge source: " + mergeFile);
			} else {
				getLog().warn("  Merge source not found, skipping: " + mergeFile);
			}
		}

		if (!sources.isEmpty()) {
			Feature merged = mergeFeatures(base, sources);
			writeFeature(merged, generatedFile);
			getLog().info("Feature merge complete.");
		}

		return generatedFile;
	}
}
