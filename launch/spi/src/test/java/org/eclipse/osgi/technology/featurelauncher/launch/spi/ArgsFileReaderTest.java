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
package org.eclipse.osgi.technology.featurelauncher.launch.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArgsFileReaderTest {

	@TempDir
	Path tempDir;

	@Test
	void testFileNotFound() {
		String[] args = ArgsFileReader.readArgsFile(tempDir.resolve("nonexistent.args"));
		assertEquals(0, args.length);
	}

	@Test
	void testEmptyFile() throws Exception {
		Path file = tempDir.resolve("empty.args");
		Files.writeString(file, "");
		String[] args = ArgsFileReader.readArgsFile(file);
		assertEquals(0, args.length);
	}

	@Test
	void testCommentsAndBlankLines() throws Exception {
		Path file = tempDir.resolve("comments.args");
		Files.writeString(file, """
				# This is a comment

				# Another comment

				""");
		String[] args = ArgsFileReader.readArgsFile(file);
		assertEquals(0, args.length);
	}

	@Test
	void testShortOptionWithValue() throws Exception {
		Path file = tempDir.resolve("short.args");
		Files.writeString(file, "-f /app/bootstrap.json\n");
		String[] args = ArgsFileReader.readArgsFile(file);
		assertArrayEquals(new String[]{"-f", "/app/bootstrap.json"}, args);
	}

	@Test
	void testLongOptionWithValue() throws Exception {
		Path file = tempDir.resolve("long.args");
		Files.writeString(file, "--feature-file /app/bootstrap.json\n");
		String[] args = ArgsFileReader.readArgsFile(file);
		assertArrayEquals(new String[]{"--feature-file", "/app/bootstrap.json"}, args);
	}

	@Test
	void testFlagOnlyOption() throws Exception {
		Path file = tempDir.resolve("flag.args");
		Files.writeString(file, "--impl-default-repos\n");
		String[] args = ArgsFileReader.readArgsFile(file);
		assertArrayEquals(new String[]{"--impl-default-repos"}, args);
	}

	@Test
	void testLaunchPropertyWithEquals() throws Exception {
		Path file = tempDir.resolve("prop.args");
		Files.writeString(file, "-l org.osgi.framework.storage=/app/storage\n");
		String[] args = ArgsFileReader.readArgsFile(file);
		assertArrayEquals(new String[]{"-l", "org.osgi.framework.storage=/app/storage"}, args);
	}

	@Test
	void testArtifactRepositoryWithComma() throws Exception {
		Path file = tempDir.resolve("repo.args");
		Files.writeString(file, "-a file:///app/repo,overlayBase=/app/volumes\n");
		String[] args = ArgsFileReader.readArgsFile(file);
		assertArrayEquals(new String[]{"-a", "file:///app/repo,overlayBase=/app/volumes"}, args);
	}

	@Test
	void testFullConfigFile() throws Exception {
		Path file = tempDir.resolve("full.args");
		Files.writeString(file, """
				# Bootstrap feature
				-f /app/bootstrap/bootstrap.json

				# Repositories
				-a file:///app/repo,overlayBase=/app/initial-content-load
				-a https://repo1.maven.org/maven2/,name=central

				# Framework properties
				-l org.osgi.framework.storage=/app/storage/framework
				-l org.osgi.framework.storage.clean=onFirstInit

				# Extension handler
				-e eclipse.osgi.technology.hash.checker=org.example.HashChecker

				# Flags
				--impl-default-repos
				""");
		String[] args = ArgsFileReader.readArgsFile(file);
		assertEquals(13, args.length);

		// Verify structure: 6 option+value pairs + 1 flag = 13 args
		assertEquals("-f", args[0]);
		assertEquals("/app/bootstrap/bootstrap.json", args[1]);
		assertEquals("-a", args[2]);
		assertEquals("file:///app/repo,overlayBase=/app/initial-content-load", args[3]);
		assertEquals("-a", args[4]);
		assertEquals("https://repo1.maven.org/maven2/,name=central", args[5]);
		assertEquals("-l", args[6]);
		assertEquals("org.osgi.framework.storage=/app/storage/framework", args[7]);
		assertEquals("-l", args[8]);
		assertEquals("org.osgi.framework.storage.clean=onFirstInit", args[9]);
		assertEquals("-e", args[10]);
		assertEquals("eclipse.osgi.technology.hash.checker=org.example.HashChecker", args[11]);
		assertEquals("--impl-default-repos", args[12]);
	}

	@Test
	void testDryRunFlag() throws Exception {
		Path file = tempDir.resolve("dryrun.args");
		Files.writeString(file, """
				-f /app/feature.json
				--impl-dry-run
				""");
		String[] args = ArgsFileReader.readArgsFile(file);
		assertArrayEquals(new String[]{"-f", "/app/feature.json", "--impl-dry-run"}, args);
	}

	@Test
	void testVariableOverride() throws Exception {
		Path file = tempDir.resolve("vars.args");
		Files.writeString(file, "-v myVar=myValue\n");
		String[] args = ArgsFileReader.readArgsFile(file);
		assertArrayEquals(new String[]{"-v", "myVar=myValue"}, args);
	}

	@Test
	void testConfigurationOption() throws Exception {
		Path file = tempDir.resolve("config.args");
		Files.writeString(file, "-c impl.key=value\n");
		String[] args = ArgsFileReader.readArgsFile(file);
		assertArrayEquals(new String[]{"-c", "impl.key=value"}, args);
	}

	@Test
	void testDecoratorOption() throws Exception {
		Path file = tempDir.resolve("decorator.args");
		Files.writeString(file, "-d com.example.MyDecorator\n");
		String[] args = ArgsFileReader.readArgsFile(file);
		assertArrayEquals(new String[]{"-d", "com.example.MyDecorator"}, args);
	}

	// --- Environment variable substitution tests ---

	@Test
	void testEnvVarSubstitutionWithDefault() {
		// PATH is always set in the environment
		String result = ArgsFileReader.substituteEnvVars("${PATH:-/default}");
		assertEquals(System.getenv("PATH"), result);
	}

	@Test
	void testEnvVarSubstitutionDefaultUsed() {
		String result = ArgsFileReader.substituteEnvVars("${NONEXISTENT_VAR_12345:-/app/storage}");
		assertEquals("/app/storage", result);
	}

	@Test
	void testEnvVarSubstitutionNoDefault() {
		String result = ArgsFileReader.substituteEnvVars("${NONEXISTENT_VAR_12345}");
		assertEquals("", result);
	}

	@Test
	void testEnvVarSubstitutionInArgsFile() throws Exception {
		Path file = tempDir.resolve("envvar.args");
		Files.writeString(file, """
				-l org.osgi.framework.storage=${NONEXISTENT_VAR_12345:-/app/storage/framework}
				-l org.osgi.framework.storage.clean=${NONEXISTENT_VAR_67890:-onFirstInit}
				""");
		String[] args = ArgsFileReader.readArgsFile(file);
		assertEquals(4, args.length);
		assertEquals("-l", args[0]);
		assertEquals("org.osgi.framework.storage=/app/storage/framework", args[1]);
		assertEquals("-l", args[2]);
		assertEquals("org.osgi.framework.storage.clean=onFirstInit", args[3]);
	}

	@Test
	void testEnvVarSubstitutionMultipleInOneLine() {
		String result = ArgsFileReader.substituteEnvVars(
				"${NONEXISTENT_A:-hello}-${NONEXISTENT_B:-world}");
		assertEquals("hello-world", result);
	}

	@Test
	void testEnvVarSubstitutionNoPlaceholders() {
		String result = ArgsFileReader.substituteEnvVars("-f /app/bootstrap.json");
		assertEquals("-f /app/bootstrap.json", result);
	}

	@Test
	void testSystemPropertyOverride() throws Exception {
		Path file = tempDir.resolve("custom.args");
		Files.writeString(file, "--impl-dry-run\n");

		String oldValue = System.getProperty(ArgsFileReader.SYSTEM_PROPERTY);
		try {
			System.setProperty(ArgsFileReader.SYSTEM_PROPERTY, file.toAbsolutePath().toString());
			String[] args = ArgsFileReader.readArgsFile();
			assertArrayEquals(new String[]{"--impl-dry-run"}, args);
		} finally {
			if (oldValue != null) {
				System.setProperty(ArgsFileReader.SYSTEM_PROPERTY, oldValue);
			} else {
				System.clearProperty(ArgsFileReader.SYSTEM_PROPERTY);
			}
		}
	}
}
