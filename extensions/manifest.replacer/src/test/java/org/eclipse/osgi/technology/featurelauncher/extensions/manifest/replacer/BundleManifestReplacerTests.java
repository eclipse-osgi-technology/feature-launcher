/**
 * Copyright (c) 2025 Kentyou and others.
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
package org.eclipse.osgi.technology.featurelauncher.extensions.manifest.replacer;

import static org.eclipse.osgi.technology.featurelauncher.extensions.manifest.replacer.BundleManifestReplacer.MANIFEST_REPLACER_EXTENSION_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.felix.feature.impl.FeatureServiceImpl;
import org.eclipse.osgi.technology.featurelauncher.common.decorator.impl.DecoratorBuilderFactoryImpl;
import org.eclipse.osgi.technology.featurelauncher.common.decorator.impl.FeatureExtensionHandlerBuilderImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;

class BundleManifestReplacerTests {

	static final Path BUNDLES = Paths.get("src/test/resources/bundles");
	static final Path FEATURES = Paths.get("src/test/resources/features");
	static final Path OUTPUT = Paths.get("target/test-repo/");
	
	static Properties aHashes;
	static Properties eHashes;
	
	static ArtifactRepository ar;
	
	@BeforeAll
	static void makeJars() throws IOException {
		
		Files.createDirectories(OUTPUT);
		
		Files.newDirectoryStream(BUNDLES)
			.forEach(p -> {
				if(Files.isDirectory(p)) {
					try (InputStream is = Files.newInputStream(p.resolve("META-INF/MANIFEST.MF"));
						JarOutputStream jos = new JarOutputStream(
							Files.newOutputStream(OUTPUT.resolve(p.getFileName())))) {
						jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
						is.transferTo(jos);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});
		ar = id -> {
			try {
				return Files.newInputStream(OUTPUT.resolve(id.getArtifactId()));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		};
	}
	
	FeatureService featureService = new FeatureServiceImpl();
	
	@Test
	void noManifests() throws Exception {
		doChecks("no-manifests.json", "test", "1.2.3", "test2", "4.5.6", false);
	}

	@Test
	void withManifests() throws Exception {
		doChecks("with-manifests.json", "test-update", "2.3.4", "test2", "4.5.6", true);
	}

	@Test
	void notJson() throws Exception {
		checkValidateFails("not-json-extension.json");
	}


	void doChecks(String file, String symbolicName1, String version1, String symbolicName2, String version2,
			boolean listChange) throws IOException, AbandonOperationException {
		try (BufferedReader br = Files.newBufferedReader(FEATURES.resolve(file))) {
			Feature feature = featureService.readFeature(br);
			
			FeatureExtensionHandler bundleManifestReplacer = new BundleManifestReplacer();
			
			List<ArtifactRepository> list = new ArrayList<ArtifactRepository>(List.of(ar));
			
			assertSame(feature, bundleManifestReplacer.handle(feature, 
					feature.getExtensions().get(MANIFEST_REPLACER_EXTENSION_NAME),
					list, new FeatureExtensionHandlerBuilderImpl(featureService, feature),
					new DecoratorBuilderFactoryImpl(featureService)));
			
			if(listChange) {
				assertEquals(2, list.size());
				assertSame(ar, list.get(1));
			} else {
				assertEquals(List.of(ar), list);
			}
			
			checkFeatureBundle(symbolicName1, version1, list, feature.getBundles().get(0));
			checkFeatureBundle(symbolicName2, version2, list, feature.getBundles().get(1));
		};
	}


	void checkFeatureBundle(String symbolicName1, String version1, List<ArtifactRepository> list, FeatureBundle fb)
			throws IOException {
		try (JarInputStream jis = new JarInputStream(
				list.stream().map(a -> a.getArtifact(fb.getID())).filter(Objects::nonNull).findFirst().get())) {
			Manifest manifest = jis.getManifest();
			Attributes attributes = manifest.getMainAttributes();
			assertEquals(symbolicName1, attributes.getValue("Bundle-SymbolicName"));
			assertEquals(version1, attributes.getValue("Bundle-Version"));
		}
	}
	
	void checkValidateFails(String file) throws IOException, AbandonOperationException {
		try (BufferedReader br = Files.newBufferedReader(FEATURES.resolve(file))) {
			Feature feature = featureService.readFeature(br);
			
			FeatureExtensionHandler bundleManifestReplacer = new BundleManifestReplacer();
			
			List<ArtifactRepository> list = new ArrayList<ArtifactRepository>(List.of(ar));
			
			assertThrowsExactly(AbandonOperationException.class, () -> bundleManifestReplacer.handle(feature, 
					feature.getExtensions().get(MANIFEST_REPLACER_EXTENSION_NAME),
					list, new FeatureExtensionHandlerBuilderImpl(featureService, feature),
					new DecoratorBuilderFactoryImpl(featureService)));
		};
	}
}
