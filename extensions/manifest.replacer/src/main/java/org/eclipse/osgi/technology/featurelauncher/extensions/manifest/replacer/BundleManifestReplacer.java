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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.eclipse.osgi.technology.featurelauncher.repository.spi.FileSystemRepository;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.FeatureExtension.Type;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

public class BundleManifestReplacer implements FeatureExtensionHandler {
	
	/**
	 * The recommended extension name for this extension handler
	 */
	public static final String MANIFEST_REPLACER_EXTENSION_NAME = "eclipse.osgi.technology.manifest.replacer"; 

	public static final String WORKING_DIRECTORY = "working_directory";

	private static final Logger LOG = LoggerFactory.getLogger(BundleManifestReplacer.class);
	
	@Override
	public Feature handle(Feature feature, FeatureExtension extension,
			List<ArtifactRepository> repositories,
			FeatureExtensionHandlerBuilder decoratedFeatureBuilder, DecoratorBuilderFactory factory)
			throws AbandonOperationException {
		if(!MANIFEST_REPLACER_EXTENSION_NAME.equals(extension.getName())) {
			LOG.warn("The recommended extension name for using the manifest replacer is {}, but it is being called for extensions named {}");
		}
		
		if(extension.getType() != Type.JSON) {
			LOG.error("The manifest replacer requires JSON configuration not {}", extension.getType());
			throw new AbandonOperationException("The configuration of the manifest replacer feature extension must be JSON.");
		}
		try {
			JsonObject config;
			String json = extension.getJSON();
			if(json == null || json.isBlank()) {
				config = Json.createObjectBuilder().build();
			} else {
				try (JsonReader reader = Json.createReader(new StringReader(json))) {
					config = reader.readObject();
				};
			}
			
			Path baseFolder = Optional.ofNullable(config.getString(WORKING_DIRECTORY, null))
					.map(Paths::get)
					.orElse(Files.createTempDirectory(MANIFEST_REPLACER_EXTENSION_NAME))
					.resolve(feature.getID().toString());
			
			Map<ID, Path> manifestReplacedBundles = new HashMap<>();
			
			for (FeatureBundle fb : feature.getBundles()) {
				ID fbId = fb.getID();
				Map<String,String> manifest = fb.getMetadata().entrySet().stream()
						.filter(e -> e.getKey().startsWith(MANIFEST_REPLACER_EXTENSION_NAME))
						.collect(Collectors.toMap(
								e -> e.getKey().substring(MANIFEST_REPLACER_EXTENSION_NAME.length() + 1),
								e -> e.getValue().toString()));
				
				if(manifest.isEmpty()) {
					continue;
				}
				
				Manifest m = new Manifest();
				Attributes a = m.getMainAttributes();
				a.put(Attributes.Name.MANIFEST_VERSION, "1.0");
				manifest.forEach((k,v) -> a.putValue(k,v));
				
				Path outputPath = baseFolder
						.resolve(fbId.getGroupId())
						.resolve(fbId.getArtifactId())
						.resolve(fbId.toString());
				Files.createDirectories(outputPath.getParent());
				manifestReplacedBundles.put(fbId, outputPath);
				
				try(JarInputStream is = new JarInputStream(repositories.stream()
						.map(r -> r.getArtifact(fbId))
						.filter(Objects::nonNull)
						.findFirst()
						.orElseThrow(() -> new AbandonOperationException("Unable to locate feature bundle " 
								+ fbId + " in a repository")));
						JarOutputStream os = new JarOutputStream(
								new BufferedOutputStream(Files.newOutputStream(outputPath)), m)) {
					
					JarEntry je;
					while((je = is.getNextJarEntry()) != null) {
						if("META-INF/MANIFEST.MF".equals(je.getName())) {
							continue;
						}
						os.putNextEntry(new JarEntry(je.getRealName()));
						is.transferTo(os);
					}
				} catch (IOException ioe) {
					throw new AbandonOperationException("Failed to generate jar with updated manifest for "
							+ fbId, ioe);
				}
			}
			
			if(!manifestReplacedBundles.isEmpty()) {
				ArtifactRepository virtual = new ManifestReplacingArtifactRepository(feature.getID(), manifestReplacedBundles); 
				repositories.add(0, virtual);
			}
		} catch(IOException | RuntimeException e) {
			throw new AbandonOperationException("Unable to process extension " + extension.getName()
					+ " for feature " + feature.getID());
		}
		
		return feature;
	}

	private static class ManifestReplacingArtifactRepository implements ArtifactRepository, FileSystemRepository {

		private final ID featureId;
		private final Map<ID, Path> manifestReplacedBundles;
		
		public ManifestReplacingArtifactRepository(ID featureId, Map<ID, Path> manifestReplacedBundles) {
			this.featureId = featureId;
			this.manifestReplacedBundles = Map.copyOf(manifestReplacedBundles);
		}

		@Override
		public InputStream getArtifactData(ID id) {
			Path path = manifestReplacedBundles.get(id);
			
			if(path != null) {
				try {
					return Files.newInputStream(path);
				} catch (IOException e) {
					throw new RuntimeException("Failed to open manifest replaced file for feature bundle " + id);
				}
			} else {
				return null;
			}
		}

		@Override
		public String getName() {
			return "Virtual repository for manifest replaced bundles in feature " + featureId;
		}

		@Override
		public Path getArtifactPath(ID id) {
			return manifestReplacedBundles.get(id);
		}

		@Override
		public Path getLocalRepositoryPath() {
			// We don't expose a root path
			return null;
		}

		@Override
		public InputStream getArtifact(ID id) {
			return getArtifactData(id);
		}
		
	}
}
