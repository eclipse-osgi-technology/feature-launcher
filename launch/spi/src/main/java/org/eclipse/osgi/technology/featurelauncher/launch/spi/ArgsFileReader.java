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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads launcher CLI arguments from a text file (args-file).
 *
 * <p>This enables distroless/chiseled containers (no shell) to configure the
 * launcher via a mounted or baked-in file instead of shell scripts.
 *
 * <p>Format: one argument per line. Lines starting with {@code #} are comments.
 * Empty lines are ignored. A line containing an option followed by a space and
 * its value (e.g., {@code -f /path/to/feature.json}) is split into two arguments.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>System property {@value #SYSTEM_PROPERTY} (if set)</li>
 *   <li>Default path {@value #DEFAULT_PATH}</li>
 * </ol>
 *
 * <p>Environment variable substitution is supported using {@code ${VAR}} or
 * {@code ${VAR:-default}} syntax. This allows args-files to be configured
 * via environment variables without requiring a shell.
 *
 * <p>Example args-file:
 * <pre>
 * # Bootstrap feature
 * -f /app/bootstrap/bootstrap.json
 * -a file:///app/repo,overlayBase=/app/initial-content-load
 * -l org.osgi.framework.storage=${OSGI_FRAMEWORK_STORAGE:-/app/storage/framework}
 * --impl-default-repos
 * </pre>
 */
public final class ArgsFileReader {

	/**
	 * System property to override the default args-file path.
	 */
	public static final String SYSTEM_PROPERTY = "launcher.argsfile";

	/**
	 * Default path where the launcher looks for an args-file.
	 */
	public static final String DEFAULT_PATH = "/app/launcher.args";

	// Matches ${VAR} or ${VAR:-default}
	private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{([^}:]+)(?::-((?:[^}]|\\}(?!\\}))*?))?\\}");

	private ArgsFileReader() {}

	/**
	 * Reads arguments from the args-file, using the system property or default path.
	 *
	 * @return parsed arguments, or empty array if no file found
	 */
	public static String[] readArgsFile() {
		String customPath = System.getProperty(SYSTEM_PROPERTY);
		Path argsFile = customPath != null ? Path.of(customPath) : Path.of(DEFAULT_PATH);
		return readArgsFile(argsFile);
	}

	/**
	 * Reads arguments from the specified file.
	 *
	 * @param path path to the args-file
	 * @return parsed arguments, or empty array if file does not exist
	 */
	public static String[] readArgsFile(Path path) {
		if (!Files.isRegularFile(path)) {
			return new String[0];
		}

		List<String> args = new ArrayList<>();
		List<String> lines;
		try {
			lines = Files.readAllLines(path);
		} catch (IOException e) {
			System.err.println("Failed to read args-file: " + path + " - " + e.getMessage());
			return new String[0];
		}

		for (String rawLine : lines) {
			String line = substituteEnvVars(rawLine.trim());

			// Skip comments and empty lines
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}

			// Check if line contains an option followed by its value
			if (line.startsWith("-")) {
				int spaceIdx = findOptionValueSeparator(line);
				if (spaceIdx > 0) {
					args.add(line.substring(0, spaceIdx));
					args.add(line.substring(spaceIdx + 1));
				} else {
					args.add(line);
				}
			} else {
				// Non-option argument (e.g., inline feature JSON)
				args.add(line);
			}
		}

		return args.toArray(new String[0]);
	}

	/**
	 * Finds the separator between an option and its value.
	 * For options like {@code -f /path} or {@code -l key=value}, finds the first space
	 * after the option name. For long options like {@code --impl-default-repos}, returns -1.
	 *
	 * Does not split on spaces within the value part (after the first space).
	 */
	private static int findOptionValueSeparator(String line) {
		// Long option with no value (flags): --impl-default-repos, --impl-dry-run
		// Long option with value: --feature-file /path
		// Short option with value: -f /path, -a uri,k=v, -l key=value

		int spaceIdx = line.indexOf(' ');
		if (spaceIdx < 0) {
			return -1; // No space = flag-only option
		}

		// Check if the part before the space looks like an option name
		String optPart = line.substring(0, spaceIdx);
		if (optPart.startsWith("--") || (optPart.startsWith("-") && optPart.length() == 2)) {
			return spaceIdx;
		}

		// For -a, -l, etc. with = but no space in option part
		return -1;
	}

	/**
	 * Substitutes environment variable references in a string.
	 * Supports {@code ${VAR}} and {@code ${VAR:-default}} syntax.
	 * If the variable is not set and no default is provided, the placeholder is removed.
	 */
	static String substituteEnvVars(String line) {
		if (line == null || !line.contains("${")) {
			return line;
		}

		Matcher matcher = ENV_VAR_PATTERN.matcher(line);
		StringBuilder sb = new StringBuilder();
		while (matcher.find()) {
			String varName = matcher.group(1);
			String defaultValue = matcher.group(2);
			String envValue = System.getenv(varName);
			String replacement;
			if (envValue != null) {
				replacement = envValue;
			} else if (defaultValue != null) {
				replacement = defaultValue;
			} else {
				replacement = "";
			}
			matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}
}
