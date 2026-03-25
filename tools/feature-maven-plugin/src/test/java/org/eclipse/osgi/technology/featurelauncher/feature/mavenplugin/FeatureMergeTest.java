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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.osgi.technology.featurelauncher.featureservice.base.FeatureServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBuilder;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureExtension;

/**
 * Tests the feature merge logic in AbstractFeatureMojo.mergeFeatures().
 */
class FeatureMergeTest {

	private FeatureServiceImpl featureService;
	private TestMergeHelper helper;

	@BeforeEach
	void setUp() {
		featureService = new FeatureServiceImpl();
		helper = new TestMergeHelper();
		helper.featureService = featureService;
	}

	@Test
	void mergeVariables_additive() {
		Feature base = buildFeature("org.example:base:1.0", Map.of("varA", "a"), Map.of(), Map.of());
		Feature source = buildFeature("org.example:source:1.0", Map.of("varB", "b"), Map.of(), Map.of());

		Feature merged = helper.mergeFeatures(base, List.of(source));

		assertEquals("a", merged.getVariables().get("varA"));
		assertEquals("b", merged.getVariables().get("varB"));
		assertEquals(2, merged.getVariables().size());
	}

	@Test
	void mergeVariables_lastWins() {
		Feature base = buildFeature("org.example:base:1.0", Map.of("key", "base-value"), Map.of(), Map.of());
		Feature source = buildFeature("org.example:source:1.0", Map.of("key", "source-value"), Map.of(), Map.of());

		Feature merged = helper.mergeFeatures(base, List.of(source));

		assertEquals("source-value", merged.getVariables().get("key"));
	}

	@Test
	void mergeConfigurations_additive() throws Exception {
		Feature base = buildFeatureWithConfig("org.example:base:1.0", "com.example.base", Map.of("k1", "v1"));
		Feature source = buildFeatureWithConfig("org.example:source:1.0", "com.example.source", Map.of("k2", "v2"));

		Feature merged = helper.mergeFeatures(base, List.of(source));

		assertEquals(2, merged.getConfigurations().size());
		assertTrue(merged.getConfigurations().containsKey("com.example.base"));
		assertTrue(merged.getConfigurations().containsKey("com.example.source"));
	}

	@Test
	void mergeConfigurations_lastWins() throws Exception {
		Feature base = buildFeatureWithConfig("org.example:base:1.0", "com.example.pid", Map.of("k", "base"));
		Feature source = buildFeatureWithConfig("org.example:source:1.0", "com.example.pid", Map.of("k", "source"));

		Feature merged = helper.mergeFeatures(base, List.of(source));

		assertEquals(1, merged.getConfigurations().size());
		FeatureConfiguration config = merged.getConfigurations().get("com.example.pid");
		assertEquals("source", config.getValues().get("k"));
	}

	@Test
	void mergeExtensions_lastWins() throws Exception {
		Feature base = buildFeatureWithExtension("org.example:base:1.0", "myext", "base-text");
		Feature source = buildFeatureWithExtension("org.example:source:1.0", "myext", "source-text");

		Feature merged = helper.mergeFeatures(base, List.of(source));

		assertEquals(1, merged.getExtensions().size());
		FeatureExtension ext = merged.getExtensions().get("myext");
		assertTrue(ext.getText().contains("source-text"));
	}

	@Test
	void mergeBundlesNotCopied() {
		// Base has no bundles, source has bundles — source bundles should NOT be merged
		Feature base = buildFeature("org.example:base:1.0", Map.of(), Map.of(), Map.of());
		Feature source = featureService.getBuilderFactory()
				.newFeatureBuilder(featureService.getIDfromMavenCoordinates("org.example:source:1.0"))
				.addBundles(featureService.getBuilderFactory()
						.newBundleBuilder(featureService.getIDfromMavenCoordinates("org.example:bundle:1.0"))
						.build())
				.addVariable("merged-var", "yes")
				.build();

		Feature merged = helper.mergeFeatures(base, List.of(source));

		assertTrue(merged.getBundles().isEmpty());
		assertEquals("yes", merged.getVariables().get("merged-var"));
	}

	@Test
	void mergePreservesBaseMetadata() {
		Feature base = featureService.getBuilderFactory()
				.newFeatureBuilder(featureService.getIDfromMavenCoordinates("org.example:base:1.0"))
				.setName("Base Name")
				.setDescription("Base Desc")
				.setVendor("Base Vendor")
				.build();
		Feature source = buildFeature("org.example:source:1.0", Map.of("var", "val"), Map.of(), Map.of());

		Feature merged = helper.mergeFeatures(base, List.of(source));

		assertEquals("Base Name", merged.getName().orElse(null));
		assertEquals("Base Desc", merged.getDescription().orElse(null));
		assertEquals("Base Vendor", merged.getVendor().orElse(null));
		assertEquals("val", merged.getVariables().get("var"));
	}

	@Test
	void mergeEmptySources() {
		Feature base = buildFeature("org.example:base:1.0", Map.of("k", "v"), Map.of(), Map.of());

		Feature merged = helper.mergeFeatures(base, List.of());

		assertEquals(1, merged.getVariables().size());
		assertEquals("v", merged.getVariables().get("k"));
	}

	// --- helpers ---

	private Feature buildFeature(String gav, Map<String, Object> variables,
			Map<String, Object> unusedConfigs, Map<String, Object> unusedExtensions) {
		FeatureBuilder builder = featureService.getBuilderFactory()
				.newFeatureBuilder(featureService.getIDfromMavenCoordinates(gav));
		builder.addVariables(variables);
		return builder.build();
	}

	private Feature buildFeatureWithConfig(String gav, String pid, Map<String, Object> props) {
		FeatureBuilder builder = featureService.getBuilderFactory()
				.newFeatureBuilder(featureService.getIDfromMavenCoordinates(gav));
		FeatureConfiguration config = featureService.getBuilderFactory()
				.newConfigurationBuilder(pid)
				.addValues(props)
				.build();
		builder.addConfigurations(config);
		return builder.build();
	}

	private Feature buildFeatureWithExtension(String gav, String name, String text) {
		FeatureBuilder builder = featureService.getBuilderFactory()
				.newFeatureBuilder(featureService.getIDfromMavenCoordinates(gav));
		FeatureExtension ext = featureService.getBuilderFactory()
				.newExtensionBuilder(name, FeatureExtension.Type.TEXT, FeatureExtension.Kind.OPTIONAL)
				.addText(text)
				.build();
		builder.addExtensions(ext);
		return builder.build();
	}

	/**
	 * Minimal subclass to access protected mergeFeatures() without Maven injection.
	 */
	static class TestMergeHelper extends AbstractFeatureMojo {
		@Override
		public void execute() {}

		@Override
		public Feature mergeFeatures(Feature base, List<Feature> sources) {
			return super.mergeFeatures(base, sources);
		}
	}
}
