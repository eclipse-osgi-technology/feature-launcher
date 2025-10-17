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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.feature.Feature;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;

/**
 * Immutable carrier for everything the CLI can configure and Parser.
 */
public record Options(Feature feature, Map<URI, Map<String, Object>> userRepos, boolean useDefaultRepos,
        List<Class<?>> decorators, Map<String, Class<?>> extensionHandlers, Map<String, String> frameworkProps,
        Map<String, Object> variables, Map<String, Object> configuration, boolean dryRun) {

	static Options parse(String[] argv) {
		List<String> args = new ArrayList<>(Arrays.asList(argv));
		Iterator<String> it = args.iterator();

		Feature feature = null;
		Map<URI, Map<String, Object>> repos = new LinkedHashMap<>();
		boolean useDefaults = false;
		List<Class<?>> decorators = new ArrayList<>();
		Map<String, Class<?>> extHandlers = new LinkedHashMap<>();
		Map<String, String> fwkProps = new LinkedHashMap<>();
		Map<String, Object> vars = new LinkedHashMap<>();
		Map<String, Object> cfg = new LinkedHashMap<>();
		boolean dryRun = false;

		while (it.hasNext()) {
			String raw = it.next();

			String tok = raw;
			String tail = null;

			if (raw.startsWith("-")) {
				int eq = raw.indexOf('=');
				int spc = raw.indexOf(' ');

				if (eq < 0) {
					eq = Integer.MAX_VALUE;

				}
				if (spc < 0) {
					spc = Integer.MAX_VALUE;
				}
				int sep = eq > spc ? spc : eq;
				if (sep > 1 && sep < Integer.MAX_VALUE) { // at least "-x=" or "--x="
					tok = raw.substring(0, sep);
					tail = raw.substring(sep + 1);
				}
			}

			if (tail != null) {
				it = prependTailToIterator(tail, it);
			}

			switch (tok) {

			case "-f", "--feature-file" -> {

				if (feature != null) {
					throw new FeatureLauncherCliException("Feature already specified");
				}
				if (it.hasNext()) {
					feature = Help.readFeatureFromFile(it.next());
				} else {
					throw new FeatureLauncherCliException(
					        Help.MISSING_REQUIRED_PARAMETER_FOR_OPTION_FEATURE_FILE_FEATURE_FILE_PATH);
				}
			}
			case "-a", "--artifact-repository" -> {
				ensureHasNext(it, tok);
				parseArtifactRepository(it.next(), repos);
			}
			case "--impl-default-repos" -> useDefaults = true;
			case "--impl-dry-run" -> dryRun = true;

			case "-d", "--decorator" -> {
				ensureHasNext(it, tok);
				decorators.add(loadClass(it.next(), FeatureDecorator.class));
			}
			case "-e", "--extension-handler" -> {
				ensureHasNext(it, tok);
				String[] nv = splitKeyVal(it.next(), "name=class");
				extHandlers.put(nv[0], loadClass(nv[1], FeatureExtensionHandler.class));
			}
			case "-l", "--launch-property" -> addKvTo(it, tok, fwkProps, "key=value");
			case "-v", "--variable-override" -> addKvTo(it, tok, vars, "key=value");
			case "-c", "--configuration" -> addKvTo(it, tok, cfg, "key=value");

			default -> {
				if (tok.startsWith("-")) {
					throw new FeatureLauncherCliException("Unknown option: " + tok);
				}
				if (feature != null) {
					throw new FeatureLauncherCliException("Feature already specified");
				}
				StringBuilder sb = new StringBuilder(tok);
				while (it.hasNext()) {
					sb.append(" ");
					sb.append(it.next());
				}
				feature = Help.readFeature(sb.toString());
			}
			}
		}

		if (feature == null) {
			throw new FeatureLauncherCliException("No feature specified (JSON or --feature-file)");
		}

		return new Options(feature, repos, useDefaults, decorators, extHandlers, fwkProps, vars, cfg, dryRun);
	}

	private static void ensureHasNext(Iterator<String> it, String opt) {
		if (!it.hasNext()) {
			throw new FeatureLauncherCliException(opt + " requires an argument");
		}
	}

	private static void addKvTo(Iterator<String> it, String opt, Map<String, ?> target, String syntaxHint) {

		ensureHasNext(it, opt);
		String[] kv = splitKeyVal(it.next(), syntaxHint);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) target;
		map.put(kv[0], kv[1]);
	}

	private static String[] splitKeyVal(String raw, String hint) {
		int eq = raw.indexOf('=');
		if (eq < 1) {
			throw new FeatureLauncherCliException("Expected " + hint + " but got '" + raw + "'");
		}
		return new String[] { raw.substring(0, eq), raw.substring(eq + 1) };
	}

	private static void parseArtifactRepository(String raw, Map<URI, Map<String, Object>> repos) {
		List<String> parts = new ArrayList<>(Arrays.asList(raw.split(",")));
		if (parts.isEmpty()) {
			throw new FeatureLauncherCliException("Empty repository spec");
		}

		URI uri = URI.create(parts.remove(0));
		Map<String, Object> cfg = repos.computeIfAbsent(uri, u -> new LinkedHashMap<>());
		for (String kv : parts) {
			String[] p = splitKeyVal(kv, "k=v");
			cfg.put(p[0], p[1]);
		}
	}

	private static <T> Class<?> loadClass(String fqcn, Class<T> expectedType) {
		try {
			Class<?> c = Class.forName(fqcn, false, FeatureLauncherCli.class.getClassLoader());
			if (!expectedType.isAssignableFrom(c)) {
				throw new FeatureLauncherCliException(fqcn + " does not implement " + expectedType.getSimpleName());
			}
			return c;
		} catch (ClassNotFoundException cnfe) {
			throw new FeatureLauncherCliException("Cannot load class " + fqcn, cnfe);
		}
	}

	private static Iterator<String> prependTailToIterator(String first, Iterator<String> rest) {
		return new Iterator<>() {
			boolean done = false;

			@Override
			public boolean hasNext() {
				return !done || rest.hasNext();
			}

			@Override
			public String next() {
				if (!done) {
					done = true;
					return first;
				}
				return rest.next();
			}
		};
	}
}
