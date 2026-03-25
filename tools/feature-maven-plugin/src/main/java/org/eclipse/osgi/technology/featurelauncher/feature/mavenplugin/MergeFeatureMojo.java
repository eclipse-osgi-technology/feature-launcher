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
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.osgi.service.feature.Feature;

/**
 * Merges variables, configurations, and extensions from one or more source
 * features into a target feature file. Bundles are NOT merged.
 *
 * <p>This is useful for composing a final feature from multiple partial features,
 * e.g. merging framework configuration extensions from a base feature with
 * application-specific configurations from another.
 *
 * <p>Merge order: the target feature is the base, then each source is merged
 * in order. Later sources override earlier ones on key conflicts (last-wins).
 *
 * <p>Example usage:
 * <pre>
 * &lt;execution&gt;
 *   &lt;goals&gt;&lt;goal&gt;merge-feature&lt;/goal&gt;&lt;/goals&gt;
 *   &lt;configuration&gt;
 *     &lt;targetFeature&gt;${project.build.directory}/feature.json&lt;/targetFeature&gt;
 *     &lt;mergeFeatures&gt;
 *       &lt;mergeFeature&gt;${project.basedir}/../base/target/features/base.json&lt;/mergeFeature&gt;
 *       &lt;mergeFeature&gt;${project.basedir}/../config/target/features/config.json&lt;/mergeFeature&gt;
 *     &lt;/mergeFeatures&gt;
 *     &lt;mergeOutputFile&gt;${project.build.directory}/merged-feature.json&lt;/mergeOutputFile&gt;
 *   &lt;/configuration&gt;
 * &lt;/execution&gt;
 * </pre>
 */
@Mojo(name = "merge-feature", defaultPhase = LifecyclePhase.PACKAGE)
public class MergeFeatureMojo extends AbstractFeatureMojo {

	/**
	 * The target feature file to merge INTO. This feature's bundles, metadata,
	 * variables, configurations, and extensions form the base.
	 */
	@Parameter(property = "feature.merge.targetFeature", required = true)
	private File targetFeature;

	/**
	 * Feature files whose variables, configurations, and extensions are merged
	 * into the target. Later files override earlier ones on key conflicts.
	 */
	@Parameter(required = true)
	private List<File> mergeFeatures;

	/**
	 * Output file for the merged feature. Defaults to overwriting the target.
	 */
	@Parameter(property = "feature.merge.outputFile")
	private File mergeOutputFile;

	@Parameter(property = "feature.merge.skip", defaultValue = "false")
	private boolean mergeSkip;

	@Override
	public void execute() throws MojoExecutionException {
		if (skip || mergeSkip) {
			getLog().info("Feature merge skipped.");
			return;
		}

		if (targetFeature == null || !targetFeature.exists()) {
			throw new MojoExecutionException("Target feature file not found: " + targetFeature);
		}

		if (mergeFeatures == null || mergeFeatures.isEmpty()) {
			getLog().info("No merge sources specified — nothing to merge.");
			return;
		}

		try {
			getLog().info("Merging features into: " + targetFeature);

			Feature base = parseFeatureFile(targetFeature);

			List<Feature> sources = new ArrayList<>();
			for (File mergeFile : mergeFeatures) {
				if (mergeFile.exists()) {
					sources.add(parseFeatureFile(mergeFile));
					getLog().info("  Merge source: " + mergeFile);
				} else {
					getLog().warn("  Merge source not found, skipping: " + mergeFile);
				}
			}

			if (sources.isEmpty()) {
				getLog().info("No valid merge sources — nothing to merge.");
				return;
			}

			Feature merged = mergeFeatures(base, sources);

			File output = mergeOutputFile != null ? mergeOutputFile : targetFeature;
			writeFeature(merged, output);

			getLog().info("Merged feature written to: " + output.toPath().toAbsolutePath());
			getLog().info("Merge complete: " + merged.getVariables().size() + " variable(s), "
					+ merged.getConfigurations().size() + " configuration(s), "
					+ merged.getExtensions().size() + " extension(s)");

		} catch (Exception e) {
			throw new MojoExecutionException("Error merging features: " + e.getMessage(), e);
		}
	}
}
