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
package org.eclipse.osgi.technology.featurelauncher.repository.tests;

import static org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryConstants.LOCAL_ARTIFACT_REPOSITORY_PATH;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ServiceLoader;

import org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.osgi.service.feature.FeatureService;

/**
 * Tests
 * {@link org.eclipse.osgi.technology.featurelauncher.repository.maven.impl.LocalArtifactRepositoryImpl}
 * and
 * {@link org.eclipse.osgi.technology.featurelauncher.repository.maven.impl.ArtifactRepositoryFactoryImpl}
 * 
 * As defined in: "160.2.1.2 Local Repositories"
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 17, 2024
 */
public abstract class AbstractRepositoryTest {
	
	protected RepositoryFactory artifactRepositoryFactory;
	protected FeatureService featureService;
	

	@BeforeEach
	public void setUpServices() throws Exception {
		// Load the Artifact Repository Factory
		artifactRepositoryFactory = getRepositoryFactory();

		// Load the Feature Service
		featureService = getFeatureService();
	}
	
	protected RepositoryFactory getRepositoryFactory() throws Exception {
		return ServiceLoader.load(RepositoryFactory.class)
				.findFirst().get();
	}
	
	protected FeatureService getFeatureService() throws Exception {
		return ServiceLoader.load(FeatureService.class)
				.findFirst().get();
	}
	
	protected Path localM2RepositoryPath;
	
	@BeforeEach
	void setupRepoPath() throws Exception {
		this.localM2RepositoryPath = getLocalRepoPath();
		
		if (!Files.isDirectory(localM2RepositoryPath) || 
				!Files.newDirectoryStream(localM2RepositoryPath).iterator().hasNext()) {
			throw new IllegalStateException("Local Maven repository does not exist!");
		}
	}
	
	protected Path getLocalRepoPath() throws Exception {
		// Obtain path of dedicated local Maven repository
		return Paths.get(System.getProperty(LOCAL_ARTIFACT_REPOSITORY_PATH, 
				"target/m2Repo"));
	}
}
