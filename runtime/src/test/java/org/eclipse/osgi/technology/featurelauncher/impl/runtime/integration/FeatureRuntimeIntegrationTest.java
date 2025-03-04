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
package org.eclipse.osgi.technology.featurelauncher.impl.runtime.integration;

import static org.eclipse.osgi.technology.featurelauncher.common.osgi.util.impl.ConfigurationUtil.CONFIGURATIONS_FILTER;
import static org.eclipse.osgi.technology.featurelauncher.common.osgi.util.impl.ConfigurationUtil.constructConfigurationsFilter;
import static org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryConstants.DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME;
import static org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryConstants.DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME;
import static org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryConstants.LOCAL_ARTIFACT_REPOSITORY_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.osgi.service.featurelauncher.repository.ArtifactRepositoryConstants.ARTIFACT_REPOSITORY_NAME;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.osgi.technology.featurelauncher.impl.runtime.FeatureRuntimeConfigurationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.osgi.service.cm.Configuration;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.runtime.FeatureRuntime;
import org.osgi.service.featurelauncher.runtime.FeatureRuntimeConstants;
import org.osgi.service.featurelauncher.runtime.InstalledBundle;
import org.osgi.service.featurelauncher.runtime.InstalledConfiguration;
import org.osgi.service.featurelauncher.runtime.InstalledFeature;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.common.annotation.config.InjectConfiguration;
import org.osgi.test.common.annotation.config.WithConfiguration;
import org.osgi.test.common.service.ServiceAware;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Tests {@link org.eclipse.osgi.technology.featurelauncher.impl.runtime.FeatureRuntimeImpl}
 * 
 * As defined in: "160.5 The Feature Runtime Service"
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 2, 2024
 */
public class FeatureRuntimeIntegrationTest {
	private static final String REMOTE_ARTIFACT_REPOSITORY_PATH = "remoteRepositoryPath";
	
	Path localRepositoryPath;
	
	Path remoteRepositoryPath;
	
	HttpServer httpServer;
	
	URI remoteURI;

	@TempDir
	Path localCache;

	@BeforeEach
	public void setUp(@InjectConfiguration(withConfig = @WithConfiguration(pid = "kentyou.featurelauncher.runtime")) Configuration config) throws Exception {
		// Obtain path of dedicated local Maven repository
		if (System.getProperty(LOCAL_ARTIFACT_REPOSITORY_PATH) == null) {
			throw new IllegalStateException("Local Maven repository is not defined!");
		}

		localRepositoryPath = Paths.get(System.getProperty(LOCAL_ARTIFACT_REPOSITORY_PATH));

		if (System.getProperty(REMOTE_ARTIFACT_REPOSITORY_PATH) == null) {
			throw new IllegalStateException("Remote Maven repository is not defined!");
		}
		
		remoteRepositoryPath = Paths.get(System.getProperty(REMOTE_ARTIFACT_REPOSITORY_PATH));
		
	
		httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		httpServer.createContext("/repo", new HttpHandler() {
			
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				Path path = Paths.get(exchange.getRequestURI().getPath());
				path = Paths.get(exchange.getHttpContext().getPath()).relativize(path);
				path = remoteRepositoryPath.resolve(path);
				
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

		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("local.repositories", List.of(localRepositoryPath.toString()));
		props.put("remote.repositories", List.of(
					String.format("%s,%s=%s", localRepositoryPath.toUri(), ARTIFACT_REPOSITORY_NAME, DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME),
					String.format("%s,%s=%s", remoteURI, ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME)
				));
		props.put("remote.repositories.enabled", true);
		props.put("configured", true);
		
		config.updateIfDifferent(props);
	}
	
	@AfterEach
	void stopServer() {
		httpServer.stop(0);
	}
	
	@InjectService(cardinality = 0, filter = "(configured=true)") 
	ServiceAware<FeatureRuntime> featureRuntimeServiceAware;
	
	@Test
	public void testGetDefaultRepositories()
			throws Exception {
		FeatureRuntime featureRuntimeService = featureRuntimeServiceAware.waitForService(5000);
		assertNotNull(featureRuntimeService);

		Map<String, ArtifactRepository> defaultArtifactRepositories = featureRuntimeService.getDefaultRepositories();
		assertNotNull(defaultArtifactRepositories);
		assertFalse(defaultArtifactRepositories.isEmpty());
		assertEquals(2, defaultArtifactRepositories.size());
		assertTrue(defaultArtifactRepositories.containsKey(DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME));
		assertTrue(defaultArtifactRepositories.containsKey(DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME));
	}

	@Test
	public void testInstallFeatureWithNoConfigWithDefaultRepositories()
			throws Exception {
		FeatureRuntime featureRuntimeService = featureRuntimeServiceAware.waitForService(5000);
		assertNotNull(featureRuntimeService);

		try (InputStream featureIs = getClass().getClassLoader()
				.getResourceAsStream("/features/gogo-console-feature.json");
				Reader featureReader = new BufferedReader(
						new InputStreamReader(featureIs, Charset.forName("UTF-8").newDecoder()));) {

			// Install Feature using default repositories
			// @formatter:off
			InstalledFeature installedFeature = featureRuntimeService.install(featureReader)
					.useDefaultRepositories(true)
					.install();
			// @formatter:on
			assertNotNull(installedFeature);
			assertFalse(installedFeature.isInitialLaunch());
			assertFalse(installedFeature.isDecorated());

			assertNotNull(installedFeature.getFeature().getID());
			assertEquals("org.eclipse.osgi.technology.featurelauncher:gogo-console-feature:1.0",
					installedFeature.getFeature().getID().toString());

			assertNotNull(installedFeature.getInstalledBundles());
			List<InstalledBundle> installedBundles = installedFeature.getInstalledBundles();
			assertEquals(3, installedBundles.size());

			assertEquals("org.apache.felix.gogo.command", installedBundles.get(0).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.shell", installedBundles.get(1).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(1).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.runtime", installedBundles.get(2).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(2).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			// Verify also via installed features
			List<InstalledFeature> installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertFalse(installedFeatures.isEmpty());
			assertEquals(1, installedFeatures.size());

			// Remove feature
			featureRuntimeService.remove(installedFeature.getFeature().getID());

			// Verify again via installed features
			installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertTrue(installedFeatures.isEmpty());
			assertEquals(0, installedFeatures.size());
		}
	}

	@Test
	public void testInstallFeatureWithNoConfigWithDefaultRepositoriesWithDummyDecorator()
			throws Exception {
		FeatureRuntime featureRuntimeService = featureRuntimeServiceAware.waitForService(5000);
		assertNotNull(featureRuntimeService);

		FeatureDecorator featureDummyDecorator = new FeatureDecorator() {

			@Override
			public Feature decorate(Feature feature, List<ArtifactRepository> repositories, 
					FeatureDecoratorBuilder decoratedFeatureBuilder,
					DecoratorBuilderFactory factory) throws AbandonOperationException {

				return decoratedFeatureBuilder.build();
			}
		};

		try (InputStream featureIs = getClass().getClassLoader()
				.getResourceAsStream("/features/gogo-console-feature.json");
				Reader featureReader = new BufferedReader(
						new InputStreamReader(featureIs, Charset.forName("UTF-8").newDecoder()));) {

			// Install Feature using default repositories
			// @formatter:off
			InstalledFeature installedFeature = featureRuntimeService.install(featureReader)
					.useDefaultRepositories(true)
					.withDecorator(featureDummyDecorator)
					.install();
			// @formatter:on
			assertNotNull(installedFeature);
			assertFalse(installedFeature.isInitialLaunch());
			assertTrue(installedFeature.isDecorated());

			assertNotNull(installedFeature.getFeature().getID());
			assertEquals("org.eclipse.osgi.technology.featurelauncher:gogo-console-feature:jar:osgi.feature.decorated:1.0",
					installedFeature.getFeature().getID().toString());

			assertNotNull(installedFeature.getInstalledBundles());
			List<InstalledBundle> installedBundles = installedFeature.getInstalledBundles();
			assertEquals(3, installedBundles.size());

			assertEquals("org.apache.felix.gogo.command", installedBundles.get(0).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.shell", installedBundles.get(1).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(1).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.runtime", installedBundles.get(2).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(2).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			// Verify also via installed features
			List<InstalledFeature> installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertFalse(installedFeatures.isEmpty());
			assertEquals(1, installedFeatures.size());

			// Remove feature
			featureRuntimeService.remove(installedFeature.getOriginalFeature().getID());

			// Verify again via installed features
			installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertTrue(installedFeatures.isEmpty());
			assertEquals(0, installedFeatures.size());
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	public void testInstallFeatureWithNoConfigWithCustomRepositories(boolean useDefault)
			throws Exception {
		FeatureRuntime featureRuntimeService = featureRuntimeServiceAware.waitForService(5000);
		assertNotNull(featureRuntimeService);

		// Set up a repositories
		ArtifactRepository localArtifactRepository = featureRuntimeService.createRepository(localRepositoryPath);
		assertNotNull(localArtifactRepository);

		ArtifactRepository remoteRepository = featureRuntimeService.createRepository(remoteURI,
				Map.of(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME, LOCAL_ARTIFACT_REPOSITORY_PATH,
						localCache.toString())); // path to local repository is needed for remote repository
															// as well
		assertNotNull(remoteRepository);

		try (InputStream featureIs = getClass().getClassLoader()
				.getResourceAsStream("/features/gogo-console-feature.json");
				Reader featureReader = new BufferedReader(
						new InputStreamReader(featureIs, Charset.forName("UTF-8").newDecoder()));) {

			// Install Feature using default repositories
			// @formatter:off
			InstalledFeature installedFeature = featureRuntimeService.install(featureReader)
					.useDefaultRepositories(useDefault)
					.addRepository(DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME, localArtifactRepository)
					.addRepository(DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME, remoteRepository)
					.install();
			// @formatter:on
			assertNotNull(installedFeature);
			assertFalse(installedFeature.isInitialLaunch());
			assertFalse(installedFeature.isDecorated());

			assertNotNull(installedFeature.getFeature().getID());
			assertEquals("org.eclipse.osgi.technology.featurelauncher:gogo-console-feature:1.0",
					installedFeature.getFeature().getID().toString());

			assertNotNull(installedFeature.getInstalledBundles());
			List<InstalledBundle> installedBundles = installedFeature.getInstalledBundles();
			assertEquals(3, installedBundles.size());

			assertEquals("org.apache.felix.gogo.command", installedBundles.get(0).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.shell", installedBundles.get(1).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(1).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.runtime", installedBundles.get(2).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(2).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			// Verify also via installed features
			List<InstalledFeature> installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertFalse(installedFeatures.isEmpty());
			assertEquals(1, installedFeatures.size());

			// Remove feature
			featureRuntimeService.remove(installedFeature.getFeature().getID());

			// Verify again via installed features
			installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertTrue(installedFeatures.isEmpty());
			assertEquals(0, installedFeatures.size());
		}
	}

	@Test
	public void testInstallFeatureWithConfigWithDefaultRepositories(
			@InjectService FeatureRuntimeConfigurationManager featureRuntimeConfigurationManagerService,
			@InjectService FeatureService featureService)
			throws Exception {

		FeatureRuntime featureRuntimeService = featureRuntimeServiceAware.waitForService(5000);
		assertNotNull(featureRuntimeService);

		ID externalFeatureId = featureService.getIDfromMavenCoordinates(FeatureRuntimeConstants.EXTERNAL_FEATURE_ID);
		assertNotNull(externalFeatureId);

		try (InputStream featureIs = getClass().getClassLoader()
				.getResourceAsStream("/features/console-webconsole-feature.integration-tests.json");
				Reader featureReader = new BufferedReader(
						new InputStreamReader(featureIs, Charset.forName("UTF-8").newDecoder()));) {

			// Install Feature using default repositories
			// @formatter:off
			InstalledFeature installedFeature = featureRuntimeService.install(featureReader)
					.useDefaultRepositories(true)
					.install();
			// @formatter:on

			assertNotNull(installedFeature);
			assertFalse(installedFeature.isInitialLaunch());
			assertFalse(installedFeature.isDecorated());

			assertNotNull(installedFeature.getFeature().getID());
			assertEquals("org.eclipse.osgi.technology.featurelauncher:console-webconsole-feature:1.0",
					installedFeature.getFeature().getID().toString());

			assertNotNull(installedFeature.getInstalledBundles());
			List<InstalledBundle> installedBundles = installedFeature.getInstalledBundles();
			assertEquals(14, installedBundles.size());

			assertEquals("org.apache.felix.configadmin", installedBundles.get(0).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(installedFeature.getFeature().getID()));
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(externalFeatureId));

			assertEquals("org.apache.felix.gogo.command", installedBundles.get(1).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(1).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.shell", installedBundles.get(2).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(2).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.runtime", installedBundles.get(3).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(3).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("biz.aQute.gogo.commands.provider", installedBundles.get(4).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(4).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.webconsole", installedBundles.get(13).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(13).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			List<InstalledConfiguration> installedConfigurations = installedFeature.getInstalledConfigurations();
			assertFalse(installedConfigurations.isEmpty());
			assertEquals(2, installedConfigurations.size());

			assertEquals("org.apache.felix.http~httpFeatureLauncherTest", installedConfigurations.get(0).getPid());
			assertTrue(installedConfigurations.get(0).getFactoryPid().isPresent());
			assertEquals("org.apache.felix.http", installedConfigurations.get(0).getFactoryPid().get());

			assertEquals("org.apache.felix.webconsole.internal.servlet.OsgiManager",
					installedConfigurations.get(1).getPid());

			List<Configuration> configurations = featureRuntimeConfigurationManagerService
					.getConfigurations(constructConfigurationsFilter());
			assertFalse(configurations.isEmpty());
			assertEquals(2, configurations.size());

			for (Configuration configuration : configurations) {
				assertTrue(Boolean.valueOf(String.valueOf(configuration.getProperties().get(CONFIGURATIONS_FILTER)))
						.booleanValue());
			}

			// Verify also via installed features
			List<InstalledFeature> installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertFalse(installedFeatures.isEmpty());
			assertEquals(1, installedFeatures.size());

			// Remove feature
			featureRuntimeService.remove(installedFeature.getFeature().getID());

			// Verify again via installed features
			installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertTrue(installedFeatures.isEmpty());
			assertEquals(0, installedFeatures.size());
		}
	}

	@Test
	public void testUpdateFeatureWithConfigWithDefaultRepositories(
			@InjectService FeatureRuntimeConfigurationManager featureRuntimeConfigurationManagerService,
			@InjectService FeatureService featureService)
			throws Exception {

		FeatureRuntime featureRuntimeService = featureRuntimeServiceAware.waitForService(5000);
		assertNotNull(featureRuntimeService);

		ID externalFeatureId = featureService.getIDfromMavenCoordinates(FeatureRuntimeConstants.EXTERNAL_FEATURE_ID);
		assertNotNull(externalFeatureId);

		// Install Feature using default repositories
		try (InputStream featureIs = getClass().getClassLoader()
				.getResourceAsStream("/features/gogo-console-feature.json");
				Reader featureReader = new BufferedReader(
						new InputStreamReader(featureIs, Charset.forName("UTF-8").newDecoder()));) {

			// @formatter:off
			InstalledFeature installedFeature = featureRuntimeService.install(featureReader)
					.useDefaultRepositories(true)
					.install();
			// @formatter:on
			assertNotNull(installedFeature);
			assertFalse(installedFeature.isInitialLaunch());
			assertFalse(installedFeature.isDecorated());

			assertNotNull(installedFeature.getFeature().getID());
			assertEquals("org.eclipse.osgi.technology.featurelauncher:gogo-console-feature:1.0",
					installedFeature.getFeature().getID().toString());

			assertNotNull(installedFeature.getInstalledBundles());
			List<InstalledBundle> installedBundles = installedFeature.getInstalledBundles();
			assertEquals(3, installedBundles.size());

			assertEquals("org.apache.felix.gogo.command", installedBundles.get(0).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.shell", installedBundles.get(1).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(1).getOwningFeatures().contains(installedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.runtime", installedBundles.get(2).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(2).getOwningFeatures().contains(installedFeature.getFeature().getID()));
		}

		// Verify via installed features
		List<InstalledFeature> installedFeatures = featureRuntimeService.getInstalledFeatures();
		assertFalse(installedFeatures.isEmpty());
		assertEquals(1, installedFeatures.size());

		ID featureId = installedFeatures.get(0).getFeature().getID();

		// Update Feature with same ID with additional bundles
		try (InputStream featureIs = getClass().getClassLoader()
				.getResourceAsStream("/features/gogo-console-feature.update-with-webconsole.json");
				Reader featureReader = new BufferedReader(
						new InputStreamReader(featureIs, Charset.forName("UTF-8").newDecoder()));) {

			// Update Feature using default repositories
			// @formatter:off
			InstalledFeature updatedFeature = featureRuntimeService.update(featureId, featureReader)
					.useDefaultRepositories(true)
					.update();
			// @formatter:on

			assertNotNull(updatedFeature);
			assertFalse(updatedFeature.isInitialLaunch());
			assertFalse(updatedFeature.isDecorated());

			assertNotNull(updatedFeature.getFeature().getID());
			assertEquals("org.eclipse.osgi.technology.featurelauncher:gogo-console-feature:1.0",
					updatedFeature.getFeature().getID().toString());

			assertNotNull(updatedFeature.getInstalledBundles());
			List<InstalledBundle> installedBundles = updatedFeature.getInstalledBundles();
			assertEquals(14, installedBundles.size());

			assertEquals("org.apache.felix.configadmin", installedBundles.get(0).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(updatedFeature.getFeature().getID()));
			assertTrue(installedBundles.get(0).getOwningFeatures().contains(externalFeatureId));

			assertEquals("org.apache.felix.gogo.command", installedBundles.get(1).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(1).getOwningFeatures().contains(updatedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.shell", installedBundles.get(2).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(2).getOwningFeatures().contains(updatedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.gogo.runtime", installedBundles.get(3).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(3).getOwningFeatures().contains(updatedFeature.getFeature().getID()));

			assertEquals("biz.aQute.gogo.commands.provider", installedBundles.get(4).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(4).getOwningFeatures().contains(updatedFeature.getFeature().getID()));

			assertEquals("org.apache.felix.webconsole", installedBundles.get(13).getBundle().getSymbolicName());
			assertTrue(installedBundles.get(13).getOwningFeatures().contains(updatedFeature.getFeature().getID()));

			List<InstalledConfiguration> installedConfigurations = updatedFeature.getInstalledConfigurations();
			assertFalse(installedConfigurations.isEmpty());
			assertEquals(2, installedConfigurations.size());

			assertEquals("org.apache.felix.http~httpFeatureLauncherTest", installedConfigurations.get(0).getPid());
			assertTrue(installedConfigurations.get(0).getFactoryPid().isPresent());
			assertEquals("org.apache.felix.http", installedConfigurations.get(0).getFactoryPid().get());

			assertEquals("org.apache.felix.webconsole.internal.servlet.OsgiManager",
					installedConfigurations.get(1).getPid());

			List<Configuration> configurations = featureRuntimeConfigurationManagerService
					.getConfigurations(constructConfigurationsFilter());
			assertFalse(configurations.isEmpty());
			assertEquals(2, configurations.size());

			for (Configuration configuration : configurations) {
				assertTrue(Boolean.valueOf(String.valueOf(configuration.getProperties().get(CONFIGURATIONS_FILTER)))
						.booleanValue());
			}

			// Remove feature
			featureRuntimeService.remove(updatedFeature.getFeature().getID());

			// Verify again via installed features
			installedFeatures = featureRuntimeService.getInstalledFeatures();
			assertTrue(installedFeatures.isEmpty());
			assertEquals(0, installedFeatures.size());
		}
	}
}
