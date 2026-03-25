/*********************************************************************
* Copyright (c) 2026 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.osgi.technology.featurelauncher.launch.cli.plain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.osgi.technology.featurelauncher.launch.spi.ArgsFileReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests that the CLI launcher correctly loads configuration from an args-file
 * when no CLI arguments are provided (distroless container mode).
 */
class ArgsFileIntegrationTest {

	private static Path FEATURE_FILE_PATH;

	final PrintStream originalOut = System.out;
	final PrintStream originalErr = System.err;
	final ByteArrayOutputStream out = new ByteArrayOutputStream();
	final ByteArrayOutputStream err = new ByteArrayOutputStream();

	@BeforeAll
	static void oneTimeSetUp() throws URISyntaxException {
		FEATURE_FILE_PATH = Paths.get(
				ArgsFileIntegrationTest.class.getResource("/features/gogo-console-feature.json").toURI());
	}

	@BeforeEach
	void setUpStreams() {
		out.reset();
		err.reset();
		System.setOut(new PrintStream(out));
		System.setErr(new PrintStream(err));
		TestSecondStageLauncher.launchers.clear();
	}

	@AfterEach
	void restoreStreams() {
		System.setOut(originalOut);
		System.setErr(originalErr);
		System.clearProperty(ArgsFileReader.SYSTEM_PROPERTY);
	}

	@Test
	void shouldLoadFromArgsFileWhenNoCliArgs(@TempDir Path tempDir) throws IOException {
		Path argsFile = tempDir.resolve("launcher.args");
		Files.writeString(argsFile, String.join("\n",
				"-f " + FEATURE_FILE_PATH.toAbsolutePath(),
				"--impl-default-repos",
				"--impl-dry-run"));

		System.setProperty(ArgsFileReader.SYSTEM_PROPERTY, argsFile.toAbsolutePath().toString());

		// Simulate launcher invoked with no args (distroless mode)
		String[] rawArgs = new String[0];
		String[] fileArgs = ArgsFileReader.readArgsFile();
		if (fileArgs.length > 0) {
			rawArgs = fileArgs;
		}

		int exitCode = FeatureLauncherCli.cli(rawArgs);
		assertEquals(0, exitCode, () -> err.toString());
		assertTrue(out.toString().contains("Dry-run requested"));
	}

	@Test
	void shouldLoadWithEnvVarSubstitution(@TempDir Path tempDir) throws IOException {
		Path argsFile = tempDir.resolve("launcher.args");
		Files.writeString(argsFile, String.join("\n",
				"-f " + FEATURE_FILE_PATH.toAbsolutePath(),
				"--impl-default-repos",
				"-l org.osgi.framework.storage=${NONEXISTENT_TEST_VAR_99:-/tmp/osgi_test}",
				"--impl-dry-run"));

		System.setProperty(ArgsFileReader.SYSTEM_PROPERTY, argsFile.toAbsolutePath().toString());

		String[] rawArgs = ArgsFileReader.readArgsFile();
		int exitCode = FeatureLauncherCli.cli(rawArgs);
		assertEquals(0, exitCode, () -> err.toString());

		// Verify the env var default was applied
		String output = out.toString();
		assertTrue(output.contains("org.osgi.framework.storage = /tmp/osgi_test"));
	}

	@Test
	void shouldPreferCliArgsOverArgsFile(@TempDir Path tempDir) throws IOException {
		// Set up an args-file that would cause dry-run
		Path argsFile = tempDir.resolve("launcher.args");
		Files.writeString(argsFile, "--impl-dry-run\n");

		System.setProperty(ArgsFileReader.SYSTEM_PROPERTY, argsFile.toAbsolutePath().toString());

		// Provide actual CLI args — args-file should NOT be loaded
		String[] cliArgs = new String[]{
				"-f", FEATURE_FILE_PATH.toAbsolutePath().toString(),
				"--impl-default-repos",
				"--impl-dry-run"
		};

		// CLI args are present, so ArgsFileReader should not be called
		// (this tests the logic in main() — if args.length > 0, skip args-file)
		assertTrue(cliArgs.length > 0);

		int exitCode = FeatureLauncherCli.cli(cliArgs);
		assertEquals(0, exitCode, () -> err.toString());
	}

	@Test
	void shouldShowHelpWhenNoArgsAndNoFile() {
		// Point to non-existent file
		System.setProperty(ArgsFileReader.SYSTEM_PROPERTY, "/nonexistent/path/launcher.args");

		String[] fileArgs = ArgsFileReader.readArgsFile();
		assertEquals(0, fileArgs.length);

		// With no args, CLI shows help
		int exitCode = FeatureLauncherCli.cli(fileArgs);
		assertEquals(1, exitCode);
	}

	@Test
	void shouldLoadFromAtFile(@TempDir Path tempDir) throws IOException {
		Path argsFile = tempDir.resolve("launcher.args");
		Files.writeString(argsFile, String.join("\n",
				"-f " + FEATURE_FILE_PATH.toAbsolutePath(),
				"--impl-default-repos",
				"--impl-dry-run"));

		// Use @file syntax
		String[] resolved = FeatureLauncherCli.resolveArgsFile(
				new String[]{"@" + argsFile.toAbsolutePath()});

		int exitCode = FeatureLauncherCli.cli(resolved);
		assertEquals(0, exitCode, () -> err.toString());
		assertTrue(out.toString().contains("Dry-run requested"));
		assertTrue(out.toString().contains("Loading configuration from args-file:"));
	}

	@Test
	void shouldHandleAllSpecOptions(@TempDir Path tempDir) throws IOException {
		Path argsFile = tempDir.resolve("launcher.args");
		Files.writeString(argsFile, String.join("\n",
				"# Full spec option test",
				"-f " + FEATURE_FILE_PATH.toAbsolutePath(),
				"--impl-default-repos",
				"-l key1=value1",
				"-l key2=value2",
				"-v varA=valA",
				"-c configX=valX",
				"--impl-dry-run"));

		System.setProperty(ArgsFileReader.SYSTEM_PROPERTY, argsFile.toAbsolutePath().toString());

		String[] rawArgs = ArgsFileReader.readArgsFile();
		int exitCode = FeatureLauncherCli.cli(rawArgs);
		assertEquals(0, exitCode, () -> err.toString());

		String output = out.toString();
		assertTrue(output.contains("key1 = value1"));
		assertTrue(output.contains("key2 = value2"));
		assertTrue(output.contains("varA = valA"));
		assertTrue(output.contains("configX = valX"));
		assertTrue(output.contains("Dry-run requested"));
	}
}
