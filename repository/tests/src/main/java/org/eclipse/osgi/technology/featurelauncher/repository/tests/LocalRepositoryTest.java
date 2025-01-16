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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.eclipse.osgi.technology.featurelauncher.repository.spi.Repository;
import org.junit.jupiter.api.Test;
import org.osgi.service.feature.ID;

/**
 * As defined in: "160.2.1.2 Local Repositories"
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 17, 2024
 */
public abstract class LocalRepositoryTest extends AbstractRepositoryTest{
	
	protected abstract Class<? extends Repository> getLocalRepoImplType() throws Exception;
	
	@Test
	public void testCreateLocalArtifactRepository() throws Exception {
		Repository localArtifactRepository = artifactRepositoryFactory.createRepository(localM2RepositoryPath);

		assertNotNull(localArtifactRepository);
		assertTrue(getLocalRepoImplType().isInstance(localArtifactRepository));
	}

	@Test
	public void testCreateLocalArtifactRepositoryNullPath() {
		assertThrows(NullPointerException.class, () -> artifactRepositoryFactory.createRepository(null));
	}

	@Test
	public void testCreateLocalArtifactRepositoryPathDoesNotExist() throws IOException {
		Path nonExistingRepositoryPath = Paths.get(FileSystems.getDefault().getSeparator(), "tmp",
				UUID.randomUUID().toString());

		assertThrows(IllegalArgumentException.class,
				() -> artifactRepositoryFactory.createRepository(nonExistingRepositoryPath));
	}

	@Test
	public void testCreateLocalArtifactRepositoryPathNotADirectory() throws IOException {
		File tmpFile = File.createTempFile("localArtifactRepositoryTest", "tmp");
		tmpFile.deleteOnExit();

		assertThrows(IllegalArgumentException.class,
				() -> artifactRepositoryFactory.createRepository(tmpFile.toPath()));
	}

	@Test
	public void testGetArtifactFromLocalArtifactRepository() throws Exception {
		Repository localArtifactRepository = artifactRepositoryFactory.createRepository(localM2RepositoryPath);

		assertNotNull(localArtifactRepository);
		assertTrue(getLocalRepoImplType().isInstance(localArtifactRepository));

		ID artifactId = featureService.getIDfromMavenCoordinates("org.osgi:org.osgi.service.feature:1.0.0");
		assertNotNull(artifactId);

		try (JarInputStream jarIs = new JarInputStream(localArtifactRepository.getArtifactData(artifactId))) {
			Manifest jarMf = jarIs.getManifest();
			assertTrue(jarMf != null);

			Attributes jarAttributes = jarMf.getMainAttributes();
			assertTrue(jarAttributes != null);
			assertEquals("org.osgi.service.feature", jarAttributes.getValue("Bundle-SymbolicName"));
		}
	}
}
