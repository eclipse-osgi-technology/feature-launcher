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

import static org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryConstants.ARTIFACT_REPOSITORY_NAME;
import static org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryConstants.DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME;
import static org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryConstants.LOCAL_ARTIFACT_REPOSITORY_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.eclipse.osgi.technology.featurelauncher.repository.spi.Repository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.service.feature.ID;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * As defined in: "160.2.1.3 Remote Repositories"
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 23, 2024
 */
public abstract class RemoteRepositoryTest extends AbstractRepositoryTest {

	private HttpServer httpServer;
	
	private URI remoteURI;
	
	@TempDir
	Path localCache;

	@BeforeEach
	void startServer() throws Exception {
		httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		httpServer.createContext("/repo", new HttpHandler() {
			
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				Path path = Paths.get(exchange.getRequestURI().getPath());
				path = Paths.get(exchange.getHttpContext().getPath()).relativize(path);
				path = localM2RepositoryPath.resolve(path);
				
				if(Files.isRegularFile(path)) {
					exchange.sendResponseHeaders(200, 0);
					Files.newInputStream(path)
						.transferTo(exchange.getResponseBody());
				} else {
					exchange.sendResponseHeaders(404, -1);
				}
				exchange.close();
			}
		});
		httpServer.start();
		
		remoteURI = new URI("http", null, httpServer.getAddress().getHostString(),
				httpServer.getAddress().getPort(), "/repo", null, null);
	}
	
	@AfterEach
	void stopServer() {
		httpServer.stop(0);
	}
	
	protected abstract Class<? extends Repository> getRemoteRepoImplType() throws Exception;
	
	@Test
	public void testCreateRemoteArtifactRepository() throws Exception {
		Repository remoteRepository = artifactRepositoryFactory.createRepository(remoteURI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME, LOCAL_ARTIFACT_REPOSITORY_PATH,
						localCache.toString())); // path to local repository is needed for remote repository
															// as well

		assertNotNull(remoteRepository);
		assertTrue(getRemoteRepoImplType().isInstance(remoteRepository));
	}

	@Test
	public void testCreateRemoteArtifactRepositoryWithTemporaryLocalArtifactRepository() throws Exception {
		Repository remoteRepository = artifactRepositoryFactory.createRepository(remoteURI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME));

		assertNotNull(remoteRepository);
		assertTrue(getRemoteRepoImplType().isInstance(remoteRepository));
	}

	@Test
	public void testCreateRemoteArtifactRepositoryNullURI() {
		assertThrows(NullPointerException.class,
				() -> artifactRepositoryFactory.createRepository(null, Collections.emptyMap()));
	}

	@Test
	public void testCreateRemoteArtifactRepositoryNullConfigurationProperties() {
		assertThrows(NullPointerException.class,
				() -> artifactRepositoryFactory.createRepository(remoteURI, null));
	}

	@Test
	public void testCreateRemoteArtifactRepositoryEmptyConfigurationProperties() throws Exception {
		Repository remoteRepository = artifactRepositoryFactory.createRepository(remoteURI,
				Map.of());

		assertNotNull(remoteRepository);
		assertTrue(getRemoteRepoImplType().isInstance(remoteRepository));
	}

	@Test
	public void testCreateRemoteArtifactRepositoryNoRepositoryName() throws Exception {
		Repository remoteRepository = artifactRepositoryFactory.createRepository(remoteURI,
						Map.of(LOCAL_ARTIFACT_REPOSITORY_PATH, localCache.toString()));
		assertNotNull(remoteRepository);
		assertTrue(getRemoteRepoImplType().isInstance(remoteRepository));
	}

	@Test
	public void testCreateRemoteArtifactRepositoryPathDoesNotExist() throws IOException {
		Path nonExistingRepositoryPath = Paths.get(FileSystems.getDefault().getSeparator(), "tmp",
				UUID.randomUUID().toString());

		assertThrows(IllegalArgumentException.class,
				() -> artifactRepositoryFactory.createRepository(remoteURI,
						Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME, LOCAL_ARTIFACT_REPOSITORY_PATH,
								nonExistingRepositoryPath.toString())));
	}

	@Test
	public void testCreateRemoteArtifactRepositoryPathNotADirectory() throws IOException {
		File tmpFile = File.createTempFile("localArtifactRepositoryTest", "tmp");
		tmpFile.deleteOnExit();

		assertThrows(IllegalArgumentException.class,
				() -> artifactRepositoryFactory.createRepository(remoteURI,
						Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME, LOCAL_ARTIFACT_REPOSITORY_PATH,
								tmpFile.toPath().toString())));
	}

	@Test
	public void testGetArtifactFromRemoteArtifactRepository() throws Exception {
		Repository remoteRepository = artifactRepositoryFactory.createRepository(remoteURI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME, LOCAL_ARTIFACT_REPOSITORY_PATH,
						localCache.toString()));

		assertNotNull(remoteRepository);
		assertTrue(getRemoteRepoImplType().isInstance(remoteRepository));

		ID artifactId = featureService.getIDfromMavenCoordinates("org.apache.felix:org.apache.felix.webconsole:4.8.8");
		assertNotNull(artifactId);

		try (JarInputStream jarIs = new JarInputStream(remoteRepository.getArtifactData(artifactId))) {
			Manifest jarMf = jarIs.getManifest();
			assertTrue(jarMf != null);

			Attributes jarAttributes = jarMf.getMainAttributes();
			assertTrue(jarAttributes != null);
			assertEquals("org.apache.felix.webconsole", jarAttributes.getValue("Bundle-SymbolicName"));
		}
	}
}
