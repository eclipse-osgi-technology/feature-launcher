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
package com.kentyou.featurelauncher.repository.maven;

import static com.kentyou.featurelauncher.repository.spi.RepositoryConstants.DEFAULT_REMOTE_ARTIFACT_REPOSITORY_TYPE;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 160.2.1.3 Remote Repositories
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
class RemoteRepositoryImpl extends AbstractMavenRepositoryImpl {
	private static final Logger LOG = LoggerFactory.getLogger(RemoteRepositoryImpl.class);

	private final URI repositoryURI;
	private final RemoteRepository remoteRepository;

	public RemoteRepositoryImpl(URI repositoryURI, Path localPath, Map<String, Object> configurationProperties) {
		
		super(localPath == null ? createTemporaryLocalArtifactRepository() : localPath,
				() -> String.format("remote-%s-%s", localPath, UUID.randomUUID()), 
				configurationProperties);
		
		this.repositoryURI = repositoryURI;
		//TODO authentication and policy configuration
		// @formatter:off
		this.remoteRepository = new RemoteRepository.Builder(
				getName(), 
				DEFAULT_REMOTE_ARTIFACT_REPOSITORY_TYPE, 
				this.repositoryURI.toASCIIString())
				.build();
		// @formatter:on
	}

	
	
	@Override
	protected void decorateArtifactRequest(ArtifactRequest request) {
		super.decorateArtifactRequest(request);
		request.addRepository(remoteRepository);
	}

	private static Path createTemporaryLocalArtifactRepository() {
		try {
			Path localRepositoryPath = Files.createTempDirectory("featurelauncherM2repo_");

			deleteOnShutdown(localRepositoryPath);

			return localRepositoryPath;

		} catch (IOException e) {
			throw new IllegalStateException("Could not create temporary local artifact repository!", e);
		}
	}

	private static void deleteOnShutdown(Path localRepositoryPath) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					FileSystemUtil.recursivelyDelete(localRepositoryPath);
				} catch (IOException e) {
					LOG.warn("Could not delete temporary local artifact repository!");
				}
			}
		});
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RemoteArtifactRepositoryImpl [name=" + getName() + ", repositoryURI=" + repositoryURI + 
				 ", localRepositoryPath=" + getLocalRepositoryPath() + "]";
	}
}
