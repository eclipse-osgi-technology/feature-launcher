/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Stefan Bischof - initial implementation
 */
package org.eclipse.osgi.technology.featurelauncher.launch.cli.plain;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

import org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryFactory;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureService;

class Help {

	static final String MISSING_REQUIRED_PARAMETER_FOR_OPTION_FEATURE_FILE_FEATURE_FILE_PATH = "Missing required parameter for option '--feature-file' (feature file path)";
	private static final String HEADING = "The Feature Launcher Command Line";
	private static final String VERSION = HEADING + "  1.0";

	static final String ERROR_MISSING_FEATURE_OR_FILE = "Missing required argument (specify one of these): (<feature json> | -f=feature file path)";

	static final FeatureService FEATURE_SERVICE() {
		return ServiceLoader.load(FeatureService.class).findFirst()
				.orElseThrow(() -> new NoSuchElementException("No Feature Service available"));
	}

	static final RepositoryFactory REPO_FACTORY() {
		return ServiceLoader.load(RepositoryFactory.class).findFirst()
				.orElseThrow(() -> new NoSuchElementException("No Repository Factory available"));
	}

	static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

	static Feature readFeatureFromFile(String pathStr) {
		Path p = Paths.get(pathStr);
		if (!Files.isRegularFile(p)) {
			throw new FeatureLauncherCliException(
					MISSING_REQUIRED_PARAMETER_FOR_OPTION_FEATURE_FILE_FEATURE_FILE_PATH + p);
		}
		return readFeature(p);
	}

	static Feature readFeature(Path p) {
		try (Reader r = Files.newBufferedReader(p)) {
			return FEATURE_SERVICE().readFeature(r);
		} catch (IOException ioe) {
			throw new FeatureLauncherCliException("Cannot read feature file: " + p, ioe);
		}
	}

	static Feature readFeature(String json) {
		try (Reader r = new StringReader(json)) {
			return FEATURE_SERVICE().readFeature(r);
		} catch (IOException ioe) {
			throw new FeatureLauncherCliException("Cannot read feature JSON", ioe);
		}
	}

	static boolean isHelpRequested(String[] a) {
		return contains(a, "-h", "--help");
	}

	static boolean isVersionRequested(String[] a) {
		return contains(a, "-V", "--version");
	}

	private static boolean contains(String[] a, String... opts) {
		return Arrays.stream(a).anyMatch(s -> Arrays.asList(opts).contains(s));
	}

	static void printUsage() {
		System.out.println(HEADING);
		System.out.println("Version   : " + VERSION);
		System.out.println("Run time  : " + java.time.LocalDateTime.now().format(TIME));
		System.out.println();
		System.out.println("Usage:");
		System.out.println("  feature-launcher <feature-json>");
		System.out.println("  feature-launcher --feature-file <path>");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  -a, --artifact-repository uri[,k=v]*   Add remote repository");
		System.out.println("      --impl-default-repos               Use ~/.m2 + Maven Central");
		System.out.println("  -d, --decorator fqcn                   Add FeatureDecorator");
		System.out.println("  -e, --extension-handler name=fqcn      Add FeatureExtensionHandler");
		System.out.println("  -l, --launch-property k=v              Framework property");
		System.out.println("  -v, --variable-override k=v            Variable override");
		System.out.println("  -c, --configuration k=v                Implementation configuration");
		System.out.println("      --impl-dry-run                     Evaluate only; do not launch");
		System.out.println("  -h, --help                             Show this help");
		System.out.println("  -V, --version                          Print version");
		System.out.println();
	}

	public static void printVersion() {
		System.out.println(VERSION);

	}
}