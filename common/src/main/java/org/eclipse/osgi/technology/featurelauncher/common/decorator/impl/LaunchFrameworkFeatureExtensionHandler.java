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
package org.eclipse.osgi.technology.featurelauncher.common.decorator.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureArtifact;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.FeatureExtension.Type;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.osgi.technology.featurelauncher.repository.spi.FileSystemRepository;
import org.eclipse.osgi.technology.featurelauncher.repository.spi.Repository;

/**
 * Implementation of {@link FeatureExtensionHandler}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
public class LaunchFrameworkFeatureExtensionHandler implements FeatureExtensionHandler {
	private static final Logger LOG = LoggerFactory.getLogger(LaunchFrameworkFeatureExtensionHandler.class);

	private static final String FF_SERVICE_PATH = "META-INF/services/org.osgi.framework.launch.FrameworkFactory";
	
	private final List<? extends Repository> artifactRepositories;
	
	private Optional<Object> locatedFramework = Optional.empty();

	public LaunchFrameworkFeatureExtensionHandler(List<? extends Repository> artifactRepositories) {
		this.artifactRepositories = artifactRepositories;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler#handle(org.osgi.service.feature.Feature, org.osgi.service.feature.FeatureExtension, org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler.FeatureExtensionHandlerBuilder, org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory)
	 */
	@Override
	public Feature handle(Feature feature, FeatureExtension extension,
			FeatureExtensionHandlerBuilder decoratedFeatureBuilder, DecoratorBuilderFactory factory)
			throws AbandonOperationException {
		
		if(extension.getType() != Type.ARTIFACTS) {
			throw new AbandonOperationException("The Launch extension was not of type ARTIFACTS");
		}
		try {
			getClass().getClassLoader().loadClass("org.osgi.framework.Bundle");
			LOG.warn("OSGi Framework API detected on the classpath. Launch framework selection may fail");
		} catch (ClassNotFoundException cnfe) {
			LOG.debug("Success. No OSGi API visible on the classpath. Launch framework selection should work correctly");
		}
		
		for (FeatureArtifact featureArtifact : extension.getArtifacts()) {
			locatedFramework = findFrameworkFactory(featureArtifact);
			if(locatedFramework.isPresent()) {
				break;
			}
		}
		
		if(locatedFramework.isEmpty() && DecorationContext.isExtensionMandatory(extension)) {
			throw new AbandonOperationException("No suitable OSGi framework implementation could be selected for the mandatory launch extension!");
		}
		
		return feature;
	}

	public Optional<Object> getLocatedFrameworkFactory() {
		return locatedFramework;
	}
		
	private Optional<Object> findFrameworkFactory(FeatureArtifact featureArtifact) {
		Path artifactPath = getArtifactPath(featureArtifact.getID(), artifactRepositories);
		
		if(artifactPath == null) {
			LOG.debug("Unable to find the framework artifact {}", featureArtifact.getID());
			return Optional.empty();
		}

		// We don't use service loader as we want to target exactly this one artifact file
		try (JarFile jar = new JarFile(artifactPath.toFile())) {
			JarEntry je = jar.getJarEntry(FF_SERVICE_PATH);
			if(je == null) {
				LOG.warn("The framework artifact {} did not declare a FrameworkFactory service file", featureArtifact.getID());
				return Optional.empty();
			}
			List<String> classNames;
			try(BufferedReader br = new BufferedReader(new InputStreamReader(jar.getInputStream(je), StandardCharsets.UTF_8))) {
				classNames = br.lines().collect(Collectors.toList());
			}
			
			if(classNames.isEmpty()) {
				LOG.warn("The framework artifact {} did not declare any FrameworkFactory services", featureArtifact.getID());
				return Optional.empty();
			}
			
			URLClassLoader urlClassLoader = URLClassLoader.newInstance(
					new URL[] { artifactPath.toUri().toURL() },
						getClass().getClassLoader());
			
			Class<?> fwkFactory;
			try {
				fwkFactory = urlClassLoader.loadClass("org.osgi.framework.launch.FrameworkFactory");
			} catch (Exception e) {
				LOG.warn("Unable to load the framework factory interface for the framework artifact {}.", 
						featureArtifact.getID(), e);
				return Optional.empty();
			}
			
			for(String className : classNames) {
				Class<?> clz;
				try {
					clz = urlClassLoader.loadClass(className);
				} catch (Exception e) {
					LOG.warn("Unable to load factory class {} for the framework artifact {}.", 
							className, featureArtifact.getID(), e);
					continue;
				}
				
				if(fwkFactory.isAssignableFrom(clz)) {
					Object ff;
					try {
						ff = clz.getConstructor().newInstance();
					} catch (Exception e) {
						LOG.warn("The factory class {} for the framework artifact {} could not be instantiated.", 
								className, featureArtifact.getID());
						continue;
					}
					if(LOG.isDebugEnabled()) {
						LOG.debug("Found Framework Factory {} from artifact {}", className, featureArtifact.getID());
					}
					return Optional.of(ff);
				} else {
					LOG.warn("The factory class {} for the framework artifact {} was not a FrameworkFactory.", 
							className, featureArtifact.getID());
					continue;
				}
			}
		} catch (Exception e1) {
			LOG.warn("Failed to discover a framework factory from artifact {}",
					featureArtifact.getID(), e1);
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("No Framework Factory found in artifact {}", featureArtifact.getID());
		}
		return Optional.empty();
	}

	private Path getArtifactPath(ID artifactId, List<? extends Repository> repositories) {
		for (Repository repo : repositories) {
			Path artifactPath = ((FileSystemRepository) repo).getArtifactPath(artifactId);
			if (artifactPath != null) {
				return artifactPath;
			}
		}

		return null;
	}
}
