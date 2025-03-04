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
package org.eclipse.osgi.technology.featurelauncher.extensions.hash.checker;

import static org.eclipse.osgi.technology.featurelauncher.extensions.hash.checker.BundleHashChecker.HASH_CHECKER_EXTENSION_NAME;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.apache.felix.feature.impl.FeatureServiceImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;

import com.kentyou.prototype.featurelauncher.common.decorator.impl.DecoratorBuilderFactoryImpl;
import com.kentyou.prototype.featurelauncher.common.decorator.impl.FeatureExtensionHandlerBuilderImpl;

class BundleHashCheckerTests {

	static final Path ARTIFACTS = Paths.get("src/test/resources/artifacts");
	static final Path FEATURES = Paths.get("src/test/resources/features");
	
	static Properties aHashes;
	static Properties eHashes;
	
	static ArtifactRepository ar;
	
	@BeforeAll
	static void loadHashes() throws IOException {
		aHashes = new Properties();
		try (BufferedReader br = Files.newBufferedReader(ARTIFACTS.resolve("a.hashes"))) {
			aHashes.load(br);
		}
		eHashes = new Properties();
		try (BufferedReader br = Files.newBufferedReader(ARTIFACTS.resolve("e.hashes"))) {
			eHashes.load(br);
		}
	}

	@BeforeAll
	static void setupRepo() throws IOException {
		ar = id -> {
			try {
				return Files.newInputStream(ARTIFACTS.resolve(id.getArtifactId()));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		};
	}
	
	FeatureService featureService = new FeatureServiceImpl();
	
	
	@ParameterizedTest
	@ValueSource(strings= {"no-hashes.json", "with-hashes.json", "with-bad-hashes-allowed.json",
			"with-unknown-hashes.json", "with-hashes-no-unverified2.json", 
			"with-invalid-hashes-permitted.json"})
	void testSuccessfulValidation(String featureFile) throws Exception {
		try (BufferedReader br = Files.newBufferedReader(FEATURES.resolve(featureFile))) {
			Feature feature = featureService.readFeature(br);
			
			FeatureExtensionHandler bundleHashChecker = new BundleHashChecker();
			
			assertSame(feature, bundleHashChecker.handle(feature, 
					feature.getExtensions().get(HASH_CHECKER_EXTENSION_NAME),
					List.of(ar), new FeatureExtensionHandlerBuilderImpl(featureService, feature),
					new DecoratorBuilderFactoryImpl(featureService)));
		};
	}
	
	@ParameterizedTest
	@ValueSource(strings = {"with-bad-hashes.json", "with-unknown-hashes-forbidden.json",
			"not-json-extension.json", "with-hashes-no-unverified.json", "with-invalid-hashes.json"})
	void testSuccessfulRejection(String featureFile) throws Exception {
		try (BufferedReader br = Files.newBufferedReader(FEATURES.resolve(featureFile))) {
			Feature feature = featureService.readFeature(br);
			
			FeatureExtensionHandler bundleHashChecker = new BundleHashChecker();
			
			assertThrowsExactly(AbandonOperationException.class, () -> bundleHashChecker.handle(feature, 
					feature.getExtensions().get(HASH_CHECKER_EXTENSION_NAME),
					List.of(ar), new FeatureExtensionHandlerBuilderImpl(featureService, feature),
					new DecoratorBuilderFactoryImpl(featureService)));
		};
	}
}
