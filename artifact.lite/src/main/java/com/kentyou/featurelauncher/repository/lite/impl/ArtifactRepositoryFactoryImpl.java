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
package com.kentyou.featurelauncher.repository.lite.impl;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 160.2.1 The Artifact Repository Factory
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
@Component
public class ArtifactRepositoryFactoryImpl implements ArtifactRepositoryFactory {
	private static final Logger LOG = LoggerFactory.getLogger(ArtifactRepositoryFactoryImpl.class);
	
	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory#createRepository(java.nio.file.Path)
	 */
	@Override
	public ArtifactRepository createRepository(Path path) {
		return createRepository(path, Map.of());
	}
	
	private ArtifactRepository createRepository(Path path, Map<String, Object> configurationProperties) {
		Objects.requireNonNull(path, "Path cannot be null!");
		validateDirectory(path);
		return new LocalArtifactRepositoryImpl(path, configurationProperties);
	}

	private static void validateDirectory(Path path) {
		if(!Files.isDirectory(path)) {
			throw new IllegalArgumentException("The repository directory " + path + " does not exist");
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory#createRepository(java.net.URI, java.util.Map)
	 */
	@Override
	public ArtifactRepository createRepository(URI uri, Map<String, Object> configurationProperties) {
		Objects.requireNonNull(uri, "URI cannot be null!");
		Objects.requireNonNull(configurationProperties, "Configuration properties cannot be null!");

		if(isLocalArtifactRepository(uri)) {
			LOG.debug("Creating local repository for URI {}", uri);
			return createRepository(Path.of(uri), configurationProperties);
		}
		
		throw new UnsupportedOperationException("Remote repositories are not yet supported");
	}
	
	public static boolean isLocalArtifactRepository(URI uri) {
		return "file".equals(uri.getScheme());
	}
}
