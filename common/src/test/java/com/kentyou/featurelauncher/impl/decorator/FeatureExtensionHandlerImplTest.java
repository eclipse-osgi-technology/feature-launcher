/**
 * Copyright (c) 2024 Kentyou and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Kentyou - initial implementation
 */
package com.kentyou.featurelauncher.impl.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;

import com.kentyou.featurelauncher.common.decorator.impl.DecorationContext;
import com.kentyou.featurelauncher.common.util.impl.ServiceLoaderUtil;

/**
 * Tests
 * {@link com.kentyou.featurelauncher.impl.util.FeatureDecorationUtil.executeFeatureExtensionHandlers(Feature,
 * Map<String, FeatureExtensionHandler>)}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 28, 2024
 */
public class FeatureExtensionHandlerImplTest {
	private static final String FEATURE_EXTENSION_NAME = "DUMMY_EXTENSION";

	FeatureService featureService;
	Feature feature;
	DecorationContext util;

	@BeforeEach
	public void setUp() throws URISyntaxException, IOException {
		// Load the Feature Service
		featureService = ServiceLoaderUtil.loadFeatureService();
		assertNotNull(featureService);

		util = new DecorationContext(List.of());
		
		// Read feature
		Path featureJSONPath = Paths
				.get(getClass().getResource("/features/gogo-console-feature-with-dummy-extension.json").toURI());

		try (Reader featureJSONReader = Files.newBufferedReader(featureJSONPath)) {
			feature = featureService.readFeature(featureJSONReader);
			assertNotNull(feature);
		}
	}

	@Test
	public void testFeatureBundlesExtensionHandler() throws URISyntaxException, IOException, AbandonOperationException {
		Map<String, FeatureExtension> featureExtensions = feature.getExtensions();
		assertEquals(1, featureExtensions.size());
		assertEquals(FEATURE_EXTENSION_NAME, featureExtensions.get(FEATURE_EXTENSION_NAME).getName());

		List<FeatureBundle> featureBundles = feature.getBundles();
		assertEquals(3, featureBundles.size());

		assertEquals("org.apache.felix:org.apache.felix.gogo.command:1.1.2", featureBundles.get(0).getID().toString());
		assertEquals("org.apache.felix:org.apache.felix.gogo.shell:1.1.4", featureBundles.get(1).getID().toString());
		assertEquals("org.apache.felix:org.apache.felix.gogo.runtime:1.1.6", featureBundles.get(2).getID().toString());

		FeatureExtensionHandler featureBundlesExtensionHandler = new FeatureExtensionHandler() {

			@Override
			public Feature handle(Feature feature, FeatureExtension extension,
					FeatureExtensionHandlerBuilder decoratedFeatureBuilder, DecoratorBuilderFactory factory)
					throws AbandonOperationException {

				ID slf4jApiId = featureService.getID("org.slf4j", "slf4j-api", "2.0.9");
				ID slf4jSimpleId = featureService.getID("org.slf4j", "slf4j-simple", "2.0.9");

				FeatureBundle slf4jApiFeatureBundle = factory.newBundleBuilder(slf4jApiId).build();
				FeatureBundle slf4jSimpleFeatureBundle = factory.newBundleBuilder(slf4jSimpleId).build();

				return decoratedFeatureBuilder.setBundles(List.of(slf4jApiFeatureBundle, slf4jSimpleFeatureBundle))
						.build();
			}
		};

		Feature decoratedFeature = util.executeFeatureExtensionHandlers(featureService, feature,
				Map.of(FEATURE_EXTENSION_NAME, featureBundlesExtensionHandler));
		assertNotNull(decoratedFeature);

		assertEquals(feature.getName(), decoratedFeature.getName());
		assertEquals(feature.getDescription(), decoratedFeature.getDescription());
		assertEquals(feature.isComplete(), decoratedFeature.isComplete());

		List<FeatureBundle> decoratedFeatureBundles = decoratedFeature.getBundles();
		assertEquals(2, decoratedFeatureBundles.size());

		assertEquals("org.slf4j:slf4j-api:2.0.9", decoratedFeatureBundles.get(0).getID().toString());
		assertEquals("org.slf4j:slf4j-simple:2.0.9", decoratedFeatureBundles.get(1).getID().toString());
	}

	@Test
	public void testFeatureConfigurationsExtensionHandler()
			throws URISyntaxException, IOException, AbandonOperationException {
		String testFeatureConfigurationPid = "test-pid";

		Map<String, FeatureExtension> featureExtensions = feature.getExtensions();
		assertEquals(1, featureExtensions.size());
		assertEquals(FEATURE_EXTENSION_NAME, featureExtensions.get(FEATURE_EXTENSION_NAME).getName());

		Map<String, FeatureConfiguration> featureConfigurations = feature.getConfigurations();
		assertEquals(0, featureConfigurations.size());

		FeatureExtensionHandler featureConfigurationsExtensionHandler = new FeatureExtensionHandler() {

			@Override
			public Feature handle(Feature feature, FeatureExtension extension,
					FeatureExtensionHandlerBuilder decoratedFeatureBuilder, DecoratorBuilderFactory factory)
					throws AbandonOperationException {

				FeatureConfiguration testFeatureConfiguration = factory
						.newConfigurationBuilder(testFeatureConfigurationPid)
						.addValues(Map.of("conf1key", "conf1val", "conf2key", "conf2val")).build();

				return decoratedFeatureBuilder.setConfigurations(List.of(testFeatureConfiguration)).build();
			}
		};

		Feature decoratedFeature = util.executeFeatureExtensionHandlers(featureService, feature,
				Map.of(FEATURE_EXTENSION_NAME, featureConfigurationsExtensionHandler));
		assertNotNull(decoratedFeature);

		assertEquals(feature.getName(), decoratedFeature.getName());
		assertEquals(feature.getDescription(), decoratedFeature.getDescription());
		assertEquals(feature.isComplete(), decoratedFeature.isComplete());

		Map<String, FeatureConfiguration> decoratedFeatureConfigurations = decoratedFeature.getConfigurations();
		assertEquals(1, decoratedFeatureConfigurations.size());

		assertTrue(decoratedFeatureConfigurations.containsKey(testFeatureConfigurationPid));
		assertEquals(testFeatureConfigurationPid,
				decoratedFeatureConfigurations.get(testFeatureConfigurationPid).getPid());
	}

	@Test
	public void testFeatureVariablesExtensionHandler()
			throws URISyntaxException, IOException, AbandonOperationException {
		Map<String, FeatureExtension> featureExtensions = feature.getExtensions();
		assertEquals(1, featureExtensions.size());
		assertEquals(FEATURE_EXTENSION_NAME, featureExtensions.get(FEATURE_EXTENSION_NAME).getName());

		Map<String, Object> featureVariables = feature.getVariables();
		assertEquals(0, featureVariables.size());

		FeatureExtensionHandler featureVariablesExtensionHandler = new FeatureExtensionHandler() {

			@Override
			public Feature handle(Feature feature, FeatureExtension extension,
					FeatureExtensionHandlerBuilder decoratedFeatureBuilder, DecoratorBuilderFactory factory)
					throws AbandonOperationException {

				return decoratedFeatureBuilder.setVariables(Map.of("var1key", "var1value", "var2key", "var2value"))
						.build();
			}
		};

		Feature decoratedFeature = util.executeFeatureExtensionHandlers(featureService, feature,
				Map.of(FEATURE_EXTENSION_NAME, featureVariablesExtensionHandler));
		assertNotNull(decoratedFeature);

		assertEquals(feature.getName(), decoratedFeature.getName());
		assertEquals(feature.getDescription(), decoratedFeature.getDescription());
		assertEquals(feature.isComplete(), decoratedFeature.isComplete());

		Map<String, Object> decoratedFeatureVariables = decoratedFeature.getVariables();
		assertEquals(2, decoratedFeatureVariables.size());
	}

	@Test
	public void testFeatureNoOpExtensionHandler() throws URISyntaxException, IOException, AbandonOperationException {
		Map<String, FeatureExtension> featureExtensions = feature.getExtensions();
		assertEquals(1, featureExtensions.size());
		assertEquals(FEATURE_EXTENSION_NAME, featureExtensions.get(FEATURE_EXTENSION_NAME).getName());

		List<FeatureBundle> featureBundles = feature.getBundles();
		assertEquals(3, featureBundles.size());

		assertEquals("org.apache.felix:org.apache.felix.gogo.command:1.1.2", featureBundles.get(0).getID().toString());
		assertEquals("org.apache.felix:org.apache.felix.gogo.shell:1.1.4", featureBundles.get(1).getID().toString());
		assertEquals("org.apache.felix:org.apache.felix.gogo.runtime:1.1.6", featureBundles.get(2).getID().toString());

		FeatureExtensionHandler featureNoOpExtensionHandler = new FeatureExtensionHandler() {

			@Override
			public Feature handle(Feature feature, FeatureExtension extension,
					FeatureExtensionHandlerBuilder decoratedFeatureBuilder, DecoratorBuilderFactory factory)
					throws AbandonOperationException {

				return feature;
			}
		};

		Feature decoratedFeature = util.executeFeatureExtensionHandlers(featureService, feature,
				Map.of(FEATURE_EXTENSION_NAME, featureNoOpExtensionHandler));
		assertNotNull(decoratedFeature);

		assertEquals(feature.getName(), decoratedFeature.getName());
		assertEquals(feature.getDescription(), decoratedFeature.getDescription());
		assertEquals(feature.isComplete(), decoratedFeature.isComplete());

		List<FeatureBundle> decoratedFeatureBundles = decoratedFeature.getBundles();
		assertEquals(3, decoratedFeatureBundles.size());

		assertEquals("org.apache.felix:org.apache.felix.gogo.command:1.1.2",
				decoratedFeatureBundles.get(0).getID().toString());
		assertEquals("org.apache.felix:org.apache.felix.gogo.shell:1.1.4",
				decoratedFeatureBundles.get(1).getID().toString());
		assertEquals("org.apache.felix:org.apache.felix.gogo.runtime:1.1.6",
				decoratedFeatureBundles.get(2).getID().toString());
	}

	@Test
	public void testFeatureInvalidExtensionHandler() throws URISyntaxException, IOException, AbandonOperationException {
		Map<String, FeatureExtension> featureExtensions = feature.getExtensions();
		assertEquals(1, featureExtensions.size());
		assertEquals(FEATURE_EXTENSION_NAME, featureExtensions.get(FEATURE_EXTENSION_NAME).getName());

		FeatureExtensionHandler featureInvalidExtensionHandler = new FeatureExtensionHandler() {

			@Override
			public Feature handle(Feature feature, FeatureExtension extension,
					FeatureExtensionHandlerBuilder decoratedFeatureBuilder, DecoratorBuilderFactory factory)
					throws AbandonOperationException {

				ID invalidFeatureID = featureService.getID("someGroupId", "someArtifactId", "0.0.0");

				return featureService.getBuilderFactory().newFeatureBuilder(invalidFeatureID).build();
			}
		};

		assertThrows(AbandonOperationException.class,
				() -> util.executeFeatureExtensionHandlers(featureService, feature,
						Map.of(FEATURE_EXTENSION_NAME, featureInvalidExtensionHandler)));
	}

	@Test
	public void testFeatureNoExtensionHandler() throws URISyntaxException, IOException, AbandonOperationException {
		Map<String, FeatureExtension> featureExtensions = feature.getExtensions();
		assertEquals(1, featureExtensions.size());
		assertEquals(FEATURE_EXTENSION_NAME, featureExtensions.get(FEATURE_EXTENSION_NAME).getName());

		assertThrows(AbandonOperationException.class, () -> util
				.executeFeatureExtensionHandlers(featureService, feature, Collections.emptyMap()));
	}
}
