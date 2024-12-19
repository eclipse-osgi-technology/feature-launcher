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
package com.kentyou.featurelauncher.cli;

import static com.kentyou.featurelauncher.repository.spi.ArtifactRepositoryConstants.DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME;
import static com.kentyou.featurelauncher.repository.spi.ArtifactRepositoryConstants.DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME;
import static com.kentyou.featurelauncher.repository.spi.ArtifactRepositoryConstants.DEFAULT_REMOTE_ARTIFACT_REPOSITORY_TYPE;
import static com.kentyou.featurelauncher.repository.spi.ArtifactRepositoryConstants.REMOTE_ARTIFACT_REPOSITORY_TYPE;
import static com.kentyou.featurelauncher.repository.spi.ArtifactRepositoryConstants.REMOTE_ARTIFACT_REPOSITORY_URI;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Tests {@link com.kentyou.featurelauncher.cli.FeatureLauncherCli}
 * 
 * As defined in: "160.4.2.4 The Feature Launcher Command Line"
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 10, 2024
 */
public class FeatureLauncherCliTest {
	private static final String FEATURE_FILE_ID = "com.kentyou.featurelauncher:gogo-console-feature:1.0";
	private static final String DECORATOR_CLASS_NAME = "com.kentyou.featurelauncher.cli.NoOpFeatureDecorator";
	private static final String EXTENSION_HANDLER_NAME = "NoOpFeatureExtensionHandler";
	private static final String EXTENSION_HANDLER_CLASS_NAME = "com.kentyou.featurelauncher.cli.NoOpFeatureExtensionHandler";
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
	private static URI LOCAL_ARTIFACT_REPOSITORY_URI;

	final PrintStream originalOut = System.out;
	final PrintStream originalErr = System.err;
	final ByteArrayOutputStream out = new ByteArrayOutputStream();
	final ByteArrayOutputStream err = new ByteArrayOutputStream();

	@BeforeAll
	public static void oneTimeSetUp() throws URISyntaxException, IOException {
		FEATURE_FILE_PATH = Paths
				.get(FeatureLauncherCliTest.class.getResource("/features/gogo-console-feature.json").toURI());

		LOCAL_ARTIFACT_REPOSITORY_URI = FeatureLauncherCli.getDefaultM2RepositoryPath().toUri();
	}

	@BeforeEach
	public void setUpStreams() {
		out.reset();
		err.reset();
		System.setOut(new PrintStream(out));
		System.setErr(new PrintStream(err));
	}

	@AfterEach
	public void restoreStreams() {
		System.setOut(originalOut);
		System.setErr(originalErr);
	}
	
	@Test
	public void testFeatureFileParameter() throws IOException {
		List<String> args = List.of("--impl-dry-run", "--impl-default-repos", Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertEquals("", err.toString());
	}

	@Test
	public void testMissingFeatureFileParameter() {
		List<String> args = List.of("--impl-dry-run");

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(2, exitCode);

		assertTrue(err.toString()
				.contains("Missing required argument (specify one of these): (<feature json> | -f=feature file path)"));
	}

	@Test
	public void testFeatureFileOptionShort() {
		List<String> args = List.of("--impl-dry-run", "--impl-default-repos",
				buildOptionArgs("-f", FEATURE_FILE_PATH.toString()));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertEquals("", err.toString());
	}

	@Test
	public void testFeatureFileOptionLong() {
		List<String> args = List.of("--impl-dry-run", "--impl-default-repos",
				buildOptionArgs("--feature-file", FEATURE_FILE_PATH.toString()));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertEquals("", err.toString());
	}

	@Test
	public void testMissingRequiredParameterForFeatureFileOptionShort() {
		List<String> args = List.of("--impl-dry-run", "-f");

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(2, exitCode);

		assertTrue(
				err.toString().contains("Missing required parameter for option '--feature-file' (feature file path)"));
	}

	@Test
	public void testMissingRequiredParameterForFeatureFileOptionLong() {
		List<String> args = List.of("--impl-dry-run", "--feature-file");

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(2, exitCode);

		assertTrue(
				err.toString().contains("Missing required parameter for option '--feature-file' (feature file path)"));
	}

	@Test
	public void testInvalidValueForFeatureFileOptionShort() {
		List<String> args = List.of("--impl-dry-run", "-f=");

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(2, exitCode);

		assertTrue(err.toString().contains("Invalid value for option '--feature-file'"));
	}

	@Test
	public void testInvalidValueForFeatureFileOptionLong() {
		List<String> args = List.of("--impl-dry-run", "--feature-file=");

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(2, exitCode);

		assertTrue(err.toString().contains("Invalid value for option '--feature-file'"));
	}

	@Test
	public void testArtifactRepositoryOptionShort() throws IOException {
		List<String> args = List.of("--impl-dry-run",
				buildArtifactRepositoryOptionArgs(false, LOCAL_ARTIFACT_REPOSITORY_URI,
						Map.entry(ARTIFACT_REPOSITORY_NAME, DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME)),
				buildArtifactRepositoryOptionArgs(false, REMOTE_ARTIFACT_REPOSITORY_URI,
						Map.entry(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME),
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
		assertTrue(out.toString().contains(String.format("name=%s", DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME)));
		assertTrue(out.toString().contains(String.format("repositoryURI=%s", REMOTE_ARTIFACT_REPOSITORY_URI)));
		assertTrue(out.toString().contains(String.format("name=%s", DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME)));

		assertEquals("", err.toString());
	}

	@Test
	public void testArtifactRepositoryOptionLong() throws IOException {
		List<String> args = List.of("--impl-dry-run",
				buildArtifactRepositoryOptionArgs(true, LOCAL_ARTIFACT_REPOSITORY_URI,
						Map.entry(ARTIFACT_REPOSITORY_NAME, DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME)),
				buildArtifactRepositoryOptionArgs(true, REMOTE_ARTIFACT_REPOSITORY_URI,
						Map.entry(ARTIFACT_REPOSITORY_NAME, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME),
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
		assertTrue(out.toString().contains(String.format("name=%s", DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME)));
		assertTrue(out.toString().contains(String.format("repositoryURI=%s", REMOTE_ARTIFACT_REPOSITORY_URI)));
		assertTrue(out.toString().contains(String.format("name=%s", DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME)));

		assertEquals("", err.toString());
	}

	@Test
	public void testDecoratorOptionShort() throws IOException {
		List<String> args = List.of("--impl-dry-run", "--impl-default-repos",
				buildOptionArgs("-d", DECORATOR_CLASS_NAME), Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using decorators:"));
		assertTrue(out.toString().contains(String.format("class %s", DECORATOR_CLASS_NAME)));

		assertEquals("", err.toString());
	}

	@Test
	public void testDecoratorOptionLong() throws IOException {
		List<String> args = List.of("--impl-dry-run", "--impl-default-repos",
				buildOptionArgs("--decorator", DECORATOR_CLASS_NAME), Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using decorators:"));
		assertTrue(out.toString().contains(String.format("class %s", DECORATOR_CLASS_NAME)));

		assertEquals("", err.toString());
	}

	@Test
	public void testExtensionHandlerOptionShort() throws IOException {
		List<String> args = List.of("--impl-dry-run", "--impl-default-repos",
				buildOptionArgs("-e", EXTENSION_HANDLER_NAME, EXTENSION_HANDLER_CLASS_NAME),
				Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using extension handlers:"));
		assertTrue(out.toString()
				.contains(String.format("%s = class %s", EXTENSION_HANDLER_NAME, EXTENSION_HANDLER_CLASS_NAME)));

		assertEquals("", err.toString());
	}

	@Test
	public void testExtensionHandlerOptionLong() throws IOException {
		List<String> args = List.of("--impl-dry-run", "--impl-default-repos",
				buildOptionArgs("--extension-handler", EXTENSION_HANDLER_NAME, EXTENSION_HANDLER_CLASS_NAME),
				Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using extension handlers:"));
		assertTrue(out.toString()
				.contains(String.format("%s = class %s", EXTENSION_HANDLER_NAME, EXTENSION_HANDLER_CLASS_NAME)));

		assertEquals("", err.toString());
	}

	@Test
	public void testLaunchPropertyOptionShort() throws IOException {
		List<String> args = List.of("--impl-dry-run", "--impl-default-repos",
				buildOptionArgs("-l", LAUNCH_PROPERTY_1_KEY, LAUNCH_PROPERTY_1_VALUE),
				buildOptionArgs("--variable-override", LAUNCH_PROPERTY_2_KEY, LAUNCH_PROPERTY_2_VALUE),
				Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using framework properties:"));
		assertTrue(out.toString().contains(String.format("%s = %s", LAUNCH_PROPERTY_1_KEY, LAUNCH_PROPERTY_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", LAUNCH_PROPERTY_2_KEY, LAUNCH_PROPERTY_2_VALUE)));

		assertEquals("", err.toString());
	}

	@Test
	public void testLaunchPropertyOptionLong() throws IOException {
		List<String> args = List.of("--impl-dry-run", "--impl-default-repos",
				buildOptionArgs("--launch-property", LAUNCH_PROPERTY_1_KEY, LAUNCH_PROPERTY_1_VALUE),
				buildOptionArgs("--variable-override", LAUNCH_PROPERTY_2_KEY, LAUNCH_PROPERTY_2_VALUE),
				Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using framework properties:"));
		assertTrue(out.toString().contains(String.format("%s = %s", LAUNCH_PROPERTY_1_KEY, LAUNCH_PROPERTY_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", LAUNCH_PROPERTY_2_KEY, LAUNCH_PROPERTY_2_VALUE)));

		assertEquals("", err.toString());
	}

	@Test
	public void testVariableOverrideOptionShort() throws IOException {
		List<String> args = List.of("--impl-dry-run", "--impl-default-repos",
				buildOptionArgs("-v", VARIABLE_1_KEY, VARIABLE_1_VALUE),
				buildOptionArgs("-v", VARIABLE_2_KEY, VARIABLE_2_VALUE), Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using variables:"));
		assertTrue(out.toString().contains(String.format("%s = %s", VARIABLE_1_KEY, VARIABLE_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", VARIABLE_2_KEY, VARIABLE_2_VALUE)));

		assertEquals("", err.toString());
	}

	@Test
	public void testVariableOverrideOptionLong() throws IOException {
		List<String> args = List.of("--impl-dry-run", "--impl-default-repos",
				buildOptionArgs("--variable-override", VARIABLE_1_KEY, VARIABLE_1_VALUE),
				buildOptionArgs("--variable-override", VARIABLE_2_KEY, VARIABLE_2_VALUE),
				Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using variables:"));
		assertTrue(out.toString().contains(String.format("%s = %s", VARIABLE_1_KEY, VARIABLE_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", VARIABLE_2_KEY, VARIABLE_2_VALUE)));

		assertEquals("", err.toString());
	}

	@Test
	public void testConfigurationOptionShort() throws IOException {
		List<String> args = List.of("--impl-dry-run", "--impl-default-repos",
				buildOptionArgs("-c", CONFIGURATION_1_KEY, CONFIGURATION_1_VALUE),
				buildOptionArgs("--variable-override", CONFIGURATION_2_KEY, CONFIGURATION_2_VALUE),
				Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using configuration:"));
		assertTrue(out.toString().contains(String.format("%s = %s", CONFIGURATION_1_KEY, CONFIGURATION_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", CONFIGURATION_2_KEY, CONFIGURATION_2_VALUE)));

		assertEquals("", err.toString());
	}

	@Test
	public void testConfigurationOptionLong() throws IOException {
		List<String> args = List.of("--impl-dry-run", "--impl-default-repos",
				buildOptionArgs("--configuration", CONFIGURATION_1_KEY, CONFIGURATION_1_VALUE),
				buildOptionArgs("--variable-override", CONFIGURATION_2_KEY, CONFIGURATION_2_VALUE),
				Files.readString(FEATURE_FILE_PATH));

		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args.toArray(String[]::new));
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using configuration:"));
		assertTrue(out.toString().contains(String.format("%s = %s", CONFIGURATION_1_KEY, CONFIGURATION_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", CONFIGURATION_2_KEY, CONFIGURATION_2_VALUE)));

		assertEquals("", err.toString());
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
