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
 *     Stefan Bischof - rework parameterized
 */
package org.eclipse.osgi.technology.featurelauncher.launch.cli.plain;

import static org.assertj.core.api.Assertions.assertThatCharSequence;
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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.osgi.technology.featurelauncher.repository.common.osgi.ArtifactRepositoryAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
		String[] args = splitCommandLine("--impl-default-repos", escapeJson(Files.readString(FEATURE_FILE_PATH)));

		int exitCode = FeatureLauncherCli.cli(args);
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
		String[] args = splitCommandLine();

		int exitCode = FeatureLauncherCli.cli(args);
		assertEquals(1, exitCode);

		assertThatCharSequence(err.toString()).contains(Help.ERROR_MISSING_FEATURE_OR_FILE);
		assertTrue(TestSecondStageLauncher.launchers.isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = { "--feature-file=", "--feature-file ", "-f=", "-f " })
	public void testFeatureFile(String argStart) {
		String[] args = splitCommandLine("--impl-default-repos",
		        buildOptionArgs(argStart, FEATURE_FILE_PATH.toString()));

		int exitCode = FeatureLauncherCli.cli(args);

		assertEquals(0, exitCode, err.toString(Charset.defaultCharset()));

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

	@ParameterizedTest
	@ValueSource(strings = { "--feature-file=", "--feature-file ", "-f=", "-f " })
	public void testFeatureFileDryRun(String argStart) {
		String[] args = splitCommandLine("--impl-dry-run", "--impl-default-repos",
		        buildOptionArgs(argStart, FEATURE_FILE_PATH.toString()));

		int exitCode = FeatureLauncherCli.cli(args);
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(TestSecondStageLauncher.launchers.isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = { "--feature-file=", "--feature-file ", "-f=", "-f " })
	public void testMissingRequiredParameterForFeatureFile(String argStart) {
		String[] args = splitCommandLine(argStart);

		int exitCode = FeatureLauncherCli.cli(args);
		assertEquals(2, exitCode);

		assertThatCharSequence(err.toString())
		        .contains(Help.MISSING_REQUIRED_PARAMETER_FOR_OPTION_FEATURE_FILE_FEATURE_FILE_PATH);
		assertTrue(TestSecondStageLauncher.launchers.isEmpty());
	}

	@Test
	public void testArtifactRepository() throws IOException {
		String[] args = splitCommandLine(
		        buildArtifactRepositoryOptionArgs(false, LOCAL_ARTIFACT_REPOSITORY_URI,
		                Map.entry(ARTIFACT_REPOSITORY_NAME, "testLocal")),
		        buildArtifactRepositoryOptionArgs(false, REMOTE_ARTIFACT_REPOSITORY_URI,
		                Map.entry(ARTIFACT_REPOSITORY_NAME, "testRemote"),
		                Map.entry(REMOTE_ARTIFACT_REPOSITORY_TYPE, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_TYPE)),
		        escapeJson(Files.readString(FEATURE_FILE_PATH)));

		int exitCode = FeatureLauncherCli.cli(args);
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using artifact repositories:"));
		String path = LOCAL_ARTIFACT_REPOSITORY_URI.getPath();
		if (path.endsWith("/")) {
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
		assertEquals("testLocal", ((ArtifactRepositoryAdapter) ssl.getRepositories().get(0)).unwrap().getName());
		assertEquals("testRemote", ((ArtifactRepositoryAdapter) ssl.getRepositories().get(1)).unwrap().getName());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	@Test
	public void testArtifactRepositoryOptionLong() throws IOException {
		String[] args = splitCommandLine(
		        buildArtifactRepositoryOptionArgs(true, LOCAL_ARTIFACT_REPOSITORY_URI,
		                Map.entry(ARTIFACT_REPOSITORY_NAME, "testLocal")),
		        buildArtifactRepositoryOptionArgs(true, REMOTE_ARTIFACT_REPOSITORY_URI,
		                Map.entry(ARTIFACT_REPOSITORY_NAME, "testRemote"),
		                Map.entry(REMOTE_ARTIFACT_REPOSITORY_TYPE, DEFAULT_REMOTE_ARTIFACT_REPOSITORY_TYPE)),
		        escapeJson(Files.readString(FEATURE_FILE_PATH)));

		int exitCode = FeatureLauncherCli.cli(args);
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using artifact repositories:"));
		String path = LOCAL_ARTIFACT_REPOSITORY_URI.getPath();
		if (path.endsWith("/")) {
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
		assertEquals("testLocal", ((ArtifactRepositoryAdapter) ssl.getRepositories().get(0)).unwrap().getName());
		assertEquals("testRemote", ((ArtifactRepositoryAdapter) ssl.getRepositories().get(1)).unwrap().getName());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	@ParameterizedTest
	@ValueSource(strings = { "--decorator=", "--decorator ", "-d=", "-d " })
	public void testDecorator(String argStart) throws IOException {
		String[] args = splitCommandLine("--impl-default-repos", buildOptionArgs(argStart, DECORATOR_CLASS_NAME),
		        escapeJson(Files.readString(FEATURE_FILE_PATH)));

		int exitCode = FeatureLauncherCli.cli(args);
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

	@ParameterizedTest
	@ValueSource(strings = { "--extension-handler=", "--extension-handler ", "-e=", "-e " })
	public void testExtensionHandler(String argStart) throws IOException {
		String[] args = splitCommandLine("--impl-default-repos",
		        buildOptionArgs(argStart, EXTENSION_HANDLER_NAME, EXTENSION_HANDLER_CLASS_NAME),
		        escapeJson(Files.readString(EXTENDED_FEATURE_FILE_PATH)));

		int exitCode = FeatureLauncherCli.cli(args);
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

	@ParameterizedTest
	@ValueSource(strings = { "--launch-property=", "--launch-property ", "-l=", "-l " })
	public void testLaunchProperty(String argStart) throws IOException {
		String[] args = splitCommandLine("--impl-default-repos",
		        buildOptionArgs(argStart, LAUNCH_PROPERTY_1_KEY, LAUNCH_PROPERTY_1_VALUE),
		        buildOptionArgs(argStart, LAUNCH_PROPERTY_2_KEY, LAUNCH_PROPERTY_2_VALUE),
		        escapeJson(Files.readString(FEATURE_FILE_PATH)));

		int exitCode = FeatureLauncherCli.cli(args);
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
		assertTrue(ssl.getFrameworkProperties().entrySet().containsAll(
		        Map.of(LAUNCH_PROPERTY_1_KEY, LAUNCH_PROPERTY_1_VALUE, LAUNCH_PROPERTY_2_KEY, LAUNCH_PROPERTY_2_VALUE)
		                .entrySet()));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	@ParameterizedTest
	@ValueSource(strings = { "--variable-override=", "--variable-override ", "-v=", "-v " })
	public void testVariableOverride(String argStart) throws IOException {
		String[] args = splitCommandLine("--impl-default-repos",
		        buildOptionArgs(argStart, VARIABLE_1_KEY, VARIABLE_1_VALUE),
		        buildOptionArgs(argStart, VARIABLE_2_KEY, VARIABLE_2_VALUE),
		        escapeJson(Files.readString(FEATURE_FILE_PATH)));

		int exitCode = FeatureLauncherCli.cli(args);
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
		assertEquals(Map.of(VARIABLE_1_KEY, VARIABLE_1_VALUE, VARIABLE_2_KEY, VARIABLE_2_VALUE),
		        ssl.getVariableOverrides());
	}

	@ParameterizedTest
	@ValueSource(strings = { "--configuration=", "--configuration ", "-c=", "-c " })
	public void testConfiguration(String argStart) throws IOException {
		String[] args = splitCommandLine("--impl-default-repos",
		        buildOptionArgs(argStart, CONFIGURATION_1_KEY, CONFIGURATION_1_VALUE),
		        buildOptionArgs(argStart, CONFIGURATION_2_KEY, CONFIGURATION_2_VALUE),
		        escapeJson(Files.readString(FEATURE_FILE_PATH)));

		int exitCode = FeatureLauncherCli.cli(args);
		assertEquals(0, exitCode);

		assertTrue(out.toString().contains(String.format("Launching feature %s", FEATURE_FILE_ID)));
		assertTrue(out.toString().contains("Using configuration:"));
		assertTrue(out.toString().contains(String.format("%s = %s", CONFIGURATION_1_KEY, CONFIGURATION_1_VALUE)));
		assertTrue(out.toString().contains(String.format("%s = %s", CONFIGURATION_2_KEY, CONFIGURATION_2_VALUE)));

		TestSecondStageLauncher ssl = TestSecondStageLauncher.launchers.get(0);

		assertEquals(Map.of(CONFIGURATION_1_KEY, CONFIGURATION_1_VALUE, CONFIGURATION_2_KEY, CONFIGURATION_2_VALUE),
		        ssl.getConfigurationProperties());
		assertNotNull(ssl.getContext());
		assertEquals(FEATURE_FILE_ID, ssl.getFeature().getID().toString());
		assertTrue(ssl.getFrameworkFactory().isEmpty());
		assertTrue(ssl.getFrameworkProperties().containsKey("org.osgi.framework.storage"));
		assertEquals(2, ssl.getRepositories().size());
		assertEquals(Map.of(), ssl.getVariableOverrides());
	}

	private String escapeJson(String string) {
		return "'" + string + "'";
	}

	private String buildOptionArgs(String optionName, String element) {
		StringBuilder args = new StringBuilder();
		args.append(optionName);

		args.append(element);
		return args.toString();
	}

	private String buildOptionArgs(String optionName, String key, String value) {
		StringBuilder args = new StringBuilder();
		args.append(optionName);

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

	public static String[] splitCommandLine(String... commands) {
		String command = Stream.of(commands).collect(Collectors.joining(" "));
		List<String> tokens = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;
		char quoteChar = 0;

		for (int i = 0; i < command.length(); i++) {
			char c = command.charAt(i);

			if (inQuotes) {
				if (c == quoteChar) {
					inQuotes = false;
				} else {
					current.append(c);
				}
			} else {
				if (Character.isWhitespace(c)) {
					if (current.length() > 0) {
						tokens.add(current.toString());
						current.setLength(0);
					}
				} else if (c == '"' || c == '\'') {
					inQuotes = true;
					quoteChar = c;
				} else {
					current.append(c);
				}
			}
		}

		if (current.length() > 0) {
			tokens.add(current.toString());
		}

		return tokens.toArray(new String[0]);
	}

}
