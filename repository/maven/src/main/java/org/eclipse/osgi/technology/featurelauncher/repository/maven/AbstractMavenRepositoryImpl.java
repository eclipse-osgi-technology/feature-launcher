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
package org.eclipse.osgi.technology.featurelauncher.repository.maven;

import static org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryConstants.ARTIFACT_REPOSITORY_NAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.maven.internal.impl.resolver.MavenSessionBuilderSupplier;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.jdk.JdkTransporterFactory;
import org.eclipse.osgi.technology.featurelauncher.repository.spi.FileSystemRepository;
import org.osgi.service.feature.ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 160.2.1.3 Remote Repositories
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
abstract class AbstractMavenRepositoryImpl implements FileSystemRepository {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractMavenRepositoryImpl.class);

	private final Map<String, Object> configurationProperties;
	private final String name;
	private final Path localRepositoryPath;

	public AbstractMavenRepositoryImpl(
			Path localRepositoryPath,
			Supplier<String> defaultNameSupplier,
			Map<String, Object> configurationProperties) {
			
		this.configurationProperties = new HashMap<>(configurationProperties);
		
		this.name = Optional.ofNullable(this.configurationProperties.get(ARTIFACT_REPOSITORY_NAME))
				.map(String::valueOf)
				.orElseGet(defaultNameSupplier);

		this.localRepositoryPath = localRepositoryPath;
	}

	public String getName() {
		return name;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.repository.ArtifactRepository#getArtifact(org.osgi.service.feature.ID)
	 */
	@Override
	public InputStream getArtifactData(ID id) {
		Objects.requireNonNull(id, "ID cannot be null!");

		Path path = getArtifactPath(id);
		if (path != null) {
			File file = path.toFile();

			if (file.exists()) {
				try {
					return new FileInputStream(file);
				} catch (FileNotFoundException e) {
					LOG.error(String.format("Error getting artifact ID '%s'", id.toString()), e);
				}
			} else {
				LOG.warn(String.format("Artifact ID '%s' does not exist in this repository!", id.toString()));
			}
		}

		return null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.technology.featurelauncher.impl.repository.EnhancedArtifactRepository#getArtifactPath(org.osgi.service.feature.ID)
	 */
	@Override
	public Path getArtifactPath(ID id) {
		Objects.requireNonNull(id, "ID cannot be null!");

		try (RepositorySystem repositorySystem = newRepositorySystem();
				CloseableSession repositorySystemSession = newSession(repositorySystem)) {

			Artifact artifact = new DefaultArtifact(id.toString());

			ArtifactRequest artifactRequest = new ArtifactRequest();
			artifactRequest.setArtifact(artifact);
			decorateArtifactRequest(artifactRequest);

			ArtifactResult artifactResult = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);

			if (artifactResult.isResolved() && !artifactResult.isMissing()) {
				return artifactResult.getArtifact().getPath();
			}

		} catch (ArtifactResolutionException e) {
			LOG.warn(String.format("Unable to get artifact ID '%s'", id.toString()));
		}

		return null;
	}
	
	protected void decorateArtifactRequest(ArtifactRequest request) {
		
	}

	/* 
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.technology.featurelauncher.impl.repository.FileSystemArtifactRepository#getLocalRepositoryPath()
	 */
	@Override
	public Path getLocalRepositoryPath() {
		return localRepositoryPath;
	}

	private RepositorySystem newRepositorySystem() {
		return new RepositorySystemSupplier() {

			@Override
			protected Map<String, TransporterFactory> createTransporterFactories() {
				HashMap<String, TransporterFactory> result = new HashMap<>();
		        result.put(FileTransporterFactory.NAME, new FileTransporterFactory());
		        result.put(
		                JdkTransporterFactory.NAME,
		                new JdkTransporterFactory(getChecksumExtractor(), getPathProcessor()));
		        return result;
			}
		}.get();
	}

	private CloseableSession newSession(RepositorySystem system) {
		RepositorySystemSession uninitializedSession = new DefaultRepositorySystemSession(h -> false);

		LocalRepository localRepository = new LocalRepository(localRepositoryPath);
		LocalRepositoryManager localRepositoryManager = system.newLocalRepositoryManager(uninitializedSession,
				localRepository);

		MavenSessionBuilderSupplier sessionBuilderSupplier = new MavenSessionBuilderSupplier(system);

		RepositorySystemSession.SessionBuilder sessionBuilder = sessionBuilderSupplier.get();
		sessionBuilder.setLocalRepositoryManager(localRepositoryManager);

		return sessionBuilder.build();
	}
}
