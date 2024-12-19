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
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

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
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;

import com.kentyou.prototype.featurelauncher.common.decorator.impl.DecorationContext;

/**
 * Tests
 * {@link com.kentyou.featurelauncher.impl.util.FeatureDecorationUtil.executeFeatureDecorators(Feature,
 * List<FeatureDecorator>)}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 28, 2024
 */
public class FeatureDecoratorImplTest {
	FeatureService featureService;
	Feature feature;
	DecorationContext<FeatureExtensionHandler> util;

	@BeforeEach
	public void setUp() throws URISyntaxException, IOException {
		// Load the Feature Service
		featureService = ServiceLoader.load(FeatureService.class).findFirst().get();

		util = new DecorationContext<>((f,e,b,d) -> f, List.of());

		// Read feature
		Path featureJSONPath = Paths.get(getClass().getResource("/features/gogo-console-feature.json").toURI());

		try (Reader featureJSONReader = Files.newBufferedReader(featureJSONPath)) {
			feature = featureService.readFeature(featureJSONReader);
			assertNotNull(feature);
		}
	}

	@Test
	public void testFeatureBundlesDecorator() throws URISyntaxException, IOException, AbandonOperationException {
		List<FeatureBundle> featureBundles = feature.getBundles();
		assertEquals(3, featureBundles.size());

		assertEquals("org.apache.felix:org.apache.felix.gogo.command:1.1.2", featureBundles.get(0).getID().toString());
		assertEquals("org.apache.felix:org.apache.felix.gogo.shell:1.1.4", featureBundles.get(1).getID().toString());
		assertEquals("org.apache.felix:org.apache.felix.gogo.runtime:1.1.6", featureBundles.get(2).getID().toString());

		FeatureDecorator featureBundlesDecorator = new FeatureDecorator() {

			@Override
			public Feature decorate(Feature feature, FeatureDecoratorBuilder decoratedFeatureBuilder,
					DecoratorBuilderFactory factory) throws AbandonOperationException {

				ID slf4jApiId = featureService.getID("org.slf4j", "slf4j-api", "2.0.9");
				ID slf4jSimpleId = featureService.getID("org.slf4j", "slf4j-simple", "2.0.9");

				FeatureBundle slf4jApiFeatureBundle = factory.newBundleBuilder(slf4jApiId).build();
				FeatureBundle slf4jSimpleFeatureBundle = factory.newBundleBuilder(slf4jSimpleId).build();

				return decoratedFeatureBuilder.setBundles(List.of(slf4jApiFeatureBundle, slf4jSimpleFeatureBundle))
						.build();
			}
		};

		Feature decoratedFeature = util.executeFeatureDecorators(featureService, feature,
				List.of(featureBundlesDecorator));
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
	public void testFeatureConfigurationsDecorator() throws URISyntaxException, IOException, AbandonOperationException {
		String testFeatureConfigurationPid = "test-pid";

		Map<String, FeatureConfiguration> featureConfigurations = feature.getConfigurations();
		assertEquals(0, featureConfigurations.size());

		FeatureDecorator featureConfigurationsDecorator = new FeatureDecorator() {

			@Override
			public Feature decorate(Feature feature, FeatureDecoratorBuilder decoratedFeatureBuilder,
					DecoratorBuilderFactory factory) throws AbandonOperationException {

				FeatureConfiguration testFeatureConfiguration = factory
						.newConfigurationBuilder(testFeatureConfigurationPid)
						.addValues(Map.of("conf1key", "conf1val", "conf2key", "conf2val")).build();

				return decoratedFeatureBuilder.setConfigurations(List.of(testFeatureConfiguration)).build();
			}
		};

		Feature decoratedFeature = util.executeFeatureDecorators(featureService, feature,
				List.of(featureConfigurationsDecorator));
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
	public void testFeatureVariablesDecorator() throws URISyntaxException, IOException, AbandonOperationException {
		Map<String, Object> featureVariables = feature.getVariables();
		assertEquals(0, featureVariables.size());

		FeatureDecorator featureVariablesDecorator = new FeatureDecorator() {

			@Override
			public Feature decorate(Feature feature, FeatureDecoratorBuilder decoratedFeatureBuilder,
					DecoratorBuilderFactory factory) throws AbandonOperationException {

				return decoratedFeatureBuilder.setVariables(Map.of("var1key", "var1value", "var2key", "var2value"))
						.build();
			}
		};

		Feature decoratedFeature = util.executeFeatureDecorators(featureService, feature,
				List.of(featureVariablesDecorator));
		assertNotNull(decoratedFeature);

		assertEquals(feature.getName(), decoratedFeature.getName());
		assertEquals(feature.getDescription(), decoratedFeature.getDescription());
		assertEquals(feature.isComplete(), decoratedFeature.isComplete());

		Map<String, Object> decoratedFeatureVariables = decoratedFeature.getVariables();
		assertEquals(2, decoratedFeatureVariables.size());
	}

	@Test
	public void testFeatureExtensionsDecorator() throws URISyntaxException, IOException, AbandonOperationException {
		String testFeatureExtensionName = "test-ext-name";

		Map<String, FeatureExtension> featureExtensions = feature.getExtensions();
		assertEquals(0, featureExtensions.size());

		FeatureDecorator featureExtensionsDecorator = new FeatureDecorator() {

			@Override
			public Feature decorate(Feature feature, FeatureDecoratorBuilder decoratedFeatureBuilder,
					DecoratorBuilderFactory factory) throws AbandonOperationException {

				FeatureExtension testFeatureExtension = factory.newExtensionBuilder(testFeatureExtensionName,
						FeatureExtension.Type.TEXT, FeatureExtension.Kind.OPTIONAL).build();

				return decoratedFeatureBuilder.setExtensions(List.of(testFeatureExtension)).build();
			}
		};

		Feature decoratedFeature = util.executeFeatureDecorators(featureService, feature,
				List.of(featureExtensionsDecorator));
		assertNotNull(decoratedFeature);

		assertEquals(feature.getName(), decoratedFeature.getName());
		assertEquals(feature.getDescription(), decoratedFeature.getDescription());
		assertEquals(feature.isComplete(), decoratedFeature.isComplete());

		Map<String, FeatureExtension> decoratedFeatureExtensions = decoratedFeature.getExtensions();
		assertEquals(1, decoratedFeatureExtensions.size());

		assertTrue(decoratedFeatureExtensions.containsKey(testFeatureExtensionName));
		assertEquals(testFeatureExtensionName, decoratedFeatureExtensions.get(testFeatureExtensionName).getName());
	}

	@Test
	public void testFeatureNoOpDecorator() throws URISyntaxException, IOException, AbandonOperationException {
		List<FeatureBundle> featureBundles = feature.getBundles();
		assertEquals(3, featureBundles.size());

		assertEquals("org.apache.felix:org.apache.felix.gogo.command:1.1.2", featureBundles.get(0).getID().toString());
		assertEquals("org.apache.felix:org.apache.felix.gogo.shell:1.1.4", featureBundles.get(1).getID().toString());
		assertEquals("org.apache.felix:org.apache.felix.gogo.runtime:1.1.6", featureBundles.get(2).getID().toString());

		FeatureDecorator featureNoOpDecorator = new FeatureDecorator() {

			@Override
			public Feature decorate(Feature feature, FeatureDecoratorBuilder decoratedFeatureBuilder,
					DecoratorBuilderFactory factory) throws AbandonOperationException {

				return feature;
			}
		};

		Feature decoratedFeature = util.executeFeatureDecorators(featureService, feature,
				List.of(featureNoOpDecorator));
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
	public void testFeatureInvalidDecorator() throws URISyntaxException, IOException, AbandonOperationException {
		FeatureDecorator featureInvalidDecorator = new FeatureDecorator() {

			@Override
			public Feature decorate(Feature feature, FeatureDecoratorBuilder decoratedFeatureBuilder,
					DecoratorBuilderFactory factory) throws AbandonOperationException {

				ID invalidFeatureID = featureService.getID("someGroupId", "someArtifactId", "0.0.0");

				return featureService.getBuilderFactory().newFeatureBuilder(invalidFeatureID).build();
			}
		};

		assertThrows(AbandonOperationException.class, () -> util
				.executeFeatureDecorators(featureService, feature, List.of(featureInvalidDecorator)));
	}
}
