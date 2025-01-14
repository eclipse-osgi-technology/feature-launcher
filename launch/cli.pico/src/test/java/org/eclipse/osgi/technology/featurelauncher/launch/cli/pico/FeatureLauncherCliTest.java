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
package org.eclipse.osgi.technology.featurelauncher.launch.cli.pico;

import static org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryConstants.DEFAULT_REMOTE_ARTIFACT_REPOSITORY_TYPE;
import static org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryConstants.REMOTE_ARTIFACT_REPOSITORY_TYPE;
import static org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryConstants.REMOTE_ARTIFACT_REPOSITORY_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.osgi.service.featurelauncher.repository.ArtifactRepositoryConstants.ARTIFACT_REPOSITORY_NAME;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import picocli.CommandLine;

/**
 * Tests {@link org.eclipse.osgi.technology.cli.FeatureLauncherCli}
 * 
 * As defined in: "160.4.2.4 The Feature Launcher Command Line"
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 10, 2024
 */
public class FeatureLauncherCliTest {
	private static final String FEATURE_FILE_ID = "org.eclipse.osgi.technology.featurelauncher:gogo-console-feature:1.0";
	private static final String DECORATOR_CLASS_NAME = AddExtensionFeatureDecorator.class.getName();
	private static final String EXTENSION_HANDLER_NAME = AddVariableExtensionHandler.class.getSimpleName();
	private static final String EXTENSION_HANDLER_CLASS_NAME = AddVariableExtensionHandler.class.getName();
	private static final String LAUNCH_PROPERTY_1_KEY = "key1";
	private static final String LAUNCH_PROPERTY_1_VALUE = "value1";
	private static final String LAUNCH_PROPERTY_2_KEY = "key2";
	private static final String LAUNCH_PROPERTY_2_VALUE = "value2";
	private static final String VARIABLE_1_KEY = "key1";
	private static final String VARIABLE_1_VALUE = "value1";
	private static final String VARIABLE_2_KEY = "key2";
	private static final String VARIABLE_2_VALUE = "value2";
	private static final String CONFIGURATION_1_KEY = "key1";
	private static final String CONFIGURATION_1_VALUE = "value1";
	private static final String CONFIGURATION_2_KEY = "key2";
	private static final String CONFIGURATION_2_VALUE = "value2";

	private static Path FEATURE_FILE_PATH;
	private static Path EXTENDED_FEATURE_FILE_PATH;
	private static URI LOCAL_ARTIFACT_REPOSITORY_URI;

	final PrintStream originalOut = System.out;
	final PrintStream originalErr = System.err;
	final ByteArrayOutputStream out = new ByteArrayOutputStream();
	final ByteArrayOutputStream err = new ByteArrayOutputStream();

	@BeforeAll
	public static void oneTimeSetUp() throws URISyntaxException, IOException {
		FEATURE_FILE_PATH = Paths
				.get(FeatureLauncherCliTest.class.getResource("/features/gogo-console-feature.json").toURI());
		EXTENDED_FEATURE_FILE_PATH = Paths
				.get(FeatureLauncherCliTest.class.getResource("/features/gogo-console-feature-extension.json").toURI());

		LOCAL_ARTIFACT_REPOSITORY_URI = FeatureLauncherCli.getDefaultM2RepositoryPath().toUri();
	}

	@BeforeEach
	public void setUpStreams() {
		out.reset();
		err.reset();
		System.setOut(new PrintStream(out));
		System.setErr(new PrintStream(err));
	}
	
	@BeforeEach
	public void clearSecondStages() {
		TestSecondStageLauncher.launchers.clear();
	}

	@AfterEach
	public void restoreStreams() {
		System.setOut(originalOut);
		System.setErr(originalErr);
	}
	
	@Test
	public void testFeatureFileParameter() throws IOException {
		List<String> args = List.of("--impl-default-repos", Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode, () -> err.toString());

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		
		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);
		
		assertEquals(Map.of(), ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	@Test
	public void testMissingFeatureFileParameter() {
		List<String> args = List.of();

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(2, exitCode);

		assertTrue(err.toString()
				.contains("Missing required argument (specify one of these): (<feature json> | -f=feature file path)"));
		assertTrue(TestSecondStageLauncher.launchers.isEmpty());
	}

	@Test
	public void testFeatureFileOptionShort() {
		List<String> args = List.of("--impl-default-repos",
				buildOptionArgs("-f", FEATURE_FILE_PATH.toString()));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		
		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);
		
		assertEquals(Map.of(), ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	@Test
	public void testFeatureFileOptionShortDryRun() {
		List<String> args = List.of("--impl-dry-run", "--impl-default-repos",
				buildOptionArgs("-f", FEATURE_FILE_PATH.toString()));
		
		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);
		
		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(TestSecondStageLauncher.launchers.isEmpty());
	}

	@Test
	public void testFeatureFileOptionLong() {
		List<String> args = List.of("--impl-default-repos",
				buildOptionArgs("--feature-file", FEATURE_FILE_PATH.toString()));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		
		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);
		
		assertEquals(Map.of(), ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	@Test
	public void testMissingRequiredParameterForFeatureFileOptionShort() {
		List<String> args = List.of("-f");

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(2, exitCode);

		assertTrue(
				err.toString().contains("Missing required parameter for option '--feature-file' (feature file path)"));
		assertTrue(TestSecondStageLauncher.launchers.isEmpty());
	}

	@Test
	public void testMissingRequiredParameterForFeatureFileOptionLong() {
		List<String> args = List.of("--feature-file");

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(2, exitCode);

		assertTrue(
				err.toString().contains("Missing required parameter for option '--feature-file' (feature file path)"));
		assertTrue(TestSecondStageLauncher.launchers.isEmpty());
	}

	@Test
	public void testInvalidValueForFeatureFileOptionShort() {
		List<String> args = List.of("-f=");

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(2, exitCode);

		assertTrue(err.toString().contains("Invalid value for option '--feature-file'"));
		assertTrue(TestSecondStageLauncher.launchers.isEmpty());
	}

	@Test
	public void testInvalidValueForFeatureFileOptionLong() {
		List<String> args = List.of("--feature-file=");

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(2, exitCode);

		assertTrue(err.toString().contains("Invalid value for option '--feature-file'"));
		assertTrue(TestSecondStageLauncher.launchers.isEmpty());
	}

	@Test
	public void testArtifactRepositoryOptionShort() throws IOException {
		List<String> args = List.of(
				buildArtifactRepositoryOptionArgs(false, LOCAL_ARTIFACT_REPOSITORY_URI,
						Map.entry(ARTIFACT_REPOSITORY_NAME, "testLocal")),
				buildArtifactRepositoryOptionArgs(false, REMOTE_ARTIFACT_REPOSITORY_URI,
						Map.entry(ARTIFACT_REPOSITORY_NAME, "testRemote"),
						Map.entry(REMOTE_ARTIFACT_REPOSITORY_TYPE, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_TYPE)),
				Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using artifact repositories:"));
		String path = LOCAL_ARTIFACT_REPOSITORY_URI.getPath();
		if(path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		assertTrue(out.toString().contains(String.format("localRepositoryPath=%s", path)));
		assertTrue(out.toString().contains(String.format("name=%s", "testLocal")));
		assertTrue(out.toString().contains(String.format("repositoryURI=%s", REMOTE_ARTIFACT_REPOSITORY_URI)));
		assertTrue(out.toString().contains(String.format("name=%s", "testRemote")));

		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);
		
		assertEquals(Map.of(), ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals("testLocal", ssl.getRepositories().get(0).getName());
		assertEquals("testRemote", ssl.getRepositories().get(1).getName());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	@Test
	public void testArtifactRepositoryOptionLong() throws IOException {
		List<String> args = List.of(
				buildArtifactRepositoryOptionArgs(true, LOCAL_ARTIFACT_REPOSITORY_URI,
						Map.entry(ARTIFACT_REPOSITORY_NAME, "testLocal")),
				buildArtifactRepositoryOptionArgs(true, REMOTE_ARTIFACT_REPOSITORY_URI,
						Map.entry(ARTIFACT_REPOSITORY_NAME, "testRemote"),
						Map.entry(REMOTE_ARTIFACT_REPOSITORY_TYPE, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_TYPE)),
				Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using artifact repositories:"));
		String path = LOCAL_ARTIFACT_REPOSITORY_URI.getPath();
		if(path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		assertTrue(out.toString().contains(String.format("localRepositoryPath=%s", path)));
		assertTrue(out.toString().contains(String.format("name=%s", "testLocal")));
		assertTrue(out.toString().contains(String.format("repositoryURI=%s", REMOTE_ARTIFACT_REPOSITORY_URI)));
		assertTrue(out.toString().contains(String.format("name=%s", "testRemote")));

		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);
		
		assertEquals(Map.of(), ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals("testLocal", ssl.getRepositories().get(0).getName());
		assertEquals("testRemote", ssl.getRepositories().get(1).getName());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	@Test
	public void testDecoratorOptionShort() throws IOException {
		List<String> args = List.of("--impl-default-repos",
				buildOptionArgs("-d", DECORATOR_CLASS_NAME), Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode, () -> err.toString());

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using decorators:"));
		assertTrue(out.toString().contains(String.format("class %s", DECORATOR_CLASS_NAME)));

		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);
		
		assertEquals(Map.of(), ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertTrue(ssl.getFeature().getExtensions().containsKey("testExtension"));
		assertEquals(List.of("foobar"), ssl.getFeature().getExtensions().get("testExtension").getText());
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	@Test
	public void testDecoratorOptionLong() throws IOException {
		List<String> args = List.of("--impl-default-repos",
				buildOptionArgs("--decorator", DECORATOR_CLASS_NAME), Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using decorators:"));
		assertTrue(out.toString().contains(String.format("class %s", DECORATOR_CLASS_NAME)));

		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);
		
		assertEquals(Map.of(), ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertTrue(ssl.getFeature().getExtensions().containsKey("testExtension"));
		assertEquals(List.of("foobar"), ssl.getFeature().getExtensions().get("testExtension").getText());
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	@Test
	public void testExtensionHandlerOptionShort() throws IOException {
		List<String> args = List.of("--impl-default-repos",
				buildOptionArgs("-e", EXTENSION_HANDLER_NAME, EXTENSION_HANDLER_CLASS_NAME),
				Files.readString(EXTENDED_FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using extension handlers:"));
		assertTrue(out.toString()
				.contains(String.format("%s = class %s", EXTENSION_HANDLER_NAME, EXTENSION_HANDLER_CLASS_NAME)));

		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);
		
		assertEquals(Map.of(), ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertEquals("fizzbuzz", ssl.getFeature().getVariables().get("addedVariable"));
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	@Test
	public void testExtensionHandlerOptionLong() throws IOException {
		List<String> args = List.of("--impl-default-repos",
				buildOptionArgs("--extension-handler", EXTENSION_HANDLER_NAME, EXTENSION_HANDLER_CLASS_NAME),
				Files.readString(EXTENDED_FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using extension handlers:"));
		assertTrue(out.toString()
				.contains(String.format("%s = class %s", EXTENSION_HANDLER_NAME, EXTENSION_HANDLER_CLASS_NAME)));

		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);
		
		assertEquals(Map.of(), ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertEquals("fizzbuzz", ssl.getFeature().getVariables().get("addedVariable"));
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	@Test
	public void testLaunchPropertyOptionShort() throws IOException {
		List<String> args = List.of("--impl-default-repos",
				buildOptionArgs("-l", LAUNCH_PROPERTY_1_KEY, LAUNCH_PROPERTY_1_VALUE),
				buildOptionArgs("-l", LAUNCH_PROPERTY_2_KEY, LAUNCH_PROPERTY_2_VALUE),
				Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using framework properties:"));
		assertTrue(out.toString().contains(String.format("%s = %s", LAUNCH_PROPERTY_1_KEY, LAUNCH_PROPERTY_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", LAUNCH_PROPERTY_2_KEY, LAUNCH_PROPERTY_2_VALUE)));

		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);
		
		assertEquals(Map.of(), ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertTrue(ssl.getFrameworkProperties().entrySet()
				.containsAll(Map.of(LAUNCH_PROPERTY_1_KEY, LAUNCH_PROPERTY_1_VALUE,
						LAUNCH_PROPERTY_2_KEY, LAUNCH_PROPERTY_2_VALUE).entrySet()));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	@Test
	public void testLaunchPropertyOptionLong() throws IOException {
		List<String> args = List.of("--impl-default-repos",
				buildOptionArgs("--launch-property", LAUNCH_PROPERTY_1_KEY, LAUNCH_PROPERTY_1_VALUE),
				buildOptionArgs("--launch-property", LAUNCH_PROPERTY_2_KEY, LAUNCH_PROPERTY_2_VALUE),
				Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using framework properties:"));
		assertTrue(out.toString().contains(String.format("%s = %s", LAUNCH_PROPERTY_1_KEY, LAUNCH_PROPERTY_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", LAUNCH_PROPERTY_2_KEY, LAUNCH_PROPERTY_2_VALUE)));

		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);
		
		assertEquals(Map.of(), ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertTrue(ssl.getFrameworkProperties().entrySet()
				.containsAll(Map.of(LAUNCH_PROPERTY_1_KEY, LAUNCH_PROPERTY_1_VALUE,
						LAUNCH_PROPERTY_2_KEY, LAUNCH_PROPERTY_2_VALUE).entrySet()));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	@Test
	public void testVariableOverrideOptionShort() throws IOException {
		List<String> args = List.of("--impl-default-repos",
				buildOptionArgs("-v", VARIABLE_1_KEY, VARIABLE_1_VALUE),
				buildOptionArgs("-v", VARIABLE_2_KEY, VARIABLE_2_VALUE), Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using variables:"));
		assertTrue(out.toString().contains(String.format("%s = %s", VARIABLE_1_KEY, VARIABLE_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", VARIABLE_2_KEY, VARIABLE_2_VALUE)));

		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);
		
		assertEquals(Map.of(), ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals(Map.of(VARIABLE_1_KEY, VARIABLE_1_VALUE,
				VARIABLE_2_KEY, VARIABLE_2_VALUE), ssl.getVariableOverrides());
	}

	@Test
	public void testVariableOverrideOptionLong() throws IOException {
		List<String> args = List.of("--impl-default-repos",
				buildOptionArgs("--variable-override", VARIABLE_1_KEY, VARIABLE_1_VALUE),
				buildOptionArgs("--variable-override", VARIABLE_2_KEY, VARIABLE_2_VALUE),
				Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using variables:"));
		assertTrue(out.toString().contains(String.format("%s = %s", VARIABLE_1_KEY, VARIABLE_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", VARIABLE_2_KEY, VARIABLE_2_VALUE)));

		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);
		
		assertEquals(Map.of(), ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals(Map.of(VARIABLE_1_KEY, VARIABLE_1_VALUE,
				VARIABLE_2_KEY, VARIABLE_2_VALUE), ssl.getVariableOverrides());
	}

	@Test
	public void testConfigurationOptionShort() throws IOException {
		List<String> args = List.of("--impl-default-repos",
				buildOptionArgs("-c", CONFIGURATION_1_KEY, CONFIGURATION_1_VALUE),
				buildOptionArgs("-c", CONFIGURATION_2_KEY, CONFIGURATION_2_VALUE),
				Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using configuration:"));
		assertTrue(out.toString().contains(String.format("%s = %s", CONFIGURATION_1_KEY, CONFIGURATION_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", CONFIGURATION_2_KEY, CONFIGURATION_2_VALUE)));

		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);
		
		assertEquals(Map.of(CONFIGURATION_1_KEY, CONFIGURATION_1_VALUE,
				CONFIGURATION_2_KEY, CONFIGURATION_2_VALUE), ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	@Test
	public void testConfigurationOptionLong() throws IOException {
		List<String> args = List.of("--impl-default-repos",
				buildOptionArgs("--configuration", CONFIGURATION_1_KEY, CONFIGURATION_1_VALUE),
				buildOptionArgs("--configuration", CONFIGURATION_2_KEY, CONFIGURATION_2_VALUE),
				Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using configuration:"));
		assertTrue(out.toString().contains(String.format("%s = %s", CONFIGURATION_1_KEY, CONFIGURATION_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", CONFIGURATION_2_KEY, CONFIGURATION_2_VALUE)));

		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);
		
		assertEquals(Map.of(CONFIGURATION_1_KEY, CONFIGURATION_1_VALUE,
				CONFIGURATION_2_KEY, CONFIGURATION_2_VALUE), ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	private String buildOptionArgs(String optionName, String element) {
		StringBuilder args = new StringBuilder();
		args.append(optionName);
		args.append("=");
		args.append(element);
		return args.toString();
	}

	private String buildOptionArgs(String optionName, String key, String value) {
		StringBuilder args = new StringBuilder();
		args.append(optionName);
		args.append("=");
		args.append(key);
		args.append("=");
		args.append(value);
		return args.toString();
	}

	@SafeVarargs
	private String buildArtifactRepositoryOptionArgs(boolean longOption, URI uri,
			Map.Entry<String, String>... properties) {
		StringBuilder args = new StringBuilder();
		if (longOption) {
			args.append("--artifact-repository");
		} else {
			args.append("-a");
		}
		args.append("=");
		args.append(uri);
		if (properties != null) {
			for (Map.Entry<String, String> property : properties) {
				args.append(",");
				args.append(property.getKey());
				args.append("=");
				args.append(property.getValue());
			}
		}

		return args.toString();
	}
}
