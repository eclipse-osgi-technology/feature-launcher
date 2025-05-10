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

import static org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryConstants.ARTIFACT_REPOSITORY_NAME;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.osgi.technology.featurelauncher.common.decorator.impl.DecorationContext;
import org.eclipse.osgi.technology.featurelauncher.common.decorator.impl.LaunchFrameworkFeatureExtensionHandler;
import org.eclipse.osgi.technology.featurelauncher.common.decorator.impl.MutableRepositoryList;
import org.eclipse.osgi.technology.featurelauncher.launch.spi.SecondStageLauncher;
import org.eclipse.osgi.technology.featurelauncher.repository.common.osgi.ArtifactRepositoryAdapter;
import org.eclipse.osgi.technology.featurelauncher.repository.spi.Repository;
import org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryFactory;
import org.osgi.service.feature.Feature;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;

public final class FeatureLauncherCli implements Runnable {

	private static final int EXIT_OK = 0;
	private static final int EXIT_ERROR = 1;
	private static final int EXIT_CLI = 2;

	private final Options opts;

	private Path defaultFrameworkStorageDir;

	public static void main(String[] rawArgs) {
		int code = cli(rawArgs);
		System.exit(code);
	}

	static int cli(String[] rawArgs) {
		int code;
		try {
			if (rawArgs.length == 0 || Help.isHelpRequested(rawArgs)) {
				Help.printUsage();
				System.err.println(Help.ERROR_MISSING_FEATURE_OR_FILE);
				return EXIT_ERROR;
			}
			if (Help.isVersionRequested(rawArgs)) {

				Help.printVersion();
				return EXIT_OK;
			}

			Options parsed = Options.parse(rawArgs);
			new FeatureLauncherCli(parsed).run();
			code = EXIT_OK;
		} catch (FeatureLauncherCliException ex) {
			System.err.println(ex.getMessage());
			if (ex.getCause() != null) {
				ex.getCause().printStackTrace(System.err);
			}
			code = EXIT_CLI;
		} catch (Throwable t) {
			System.err.println("Unexpected error: " + t.getMessage());
			t.printStackTrace(System.err);
			code = EXIT_ERROR;
		}
		return code;
	}

	private FeatureLauncherCli(Options opts) {
		this.opts = opts;
	}

	@Override
	public void run() {

		MutableRepositoryList repositories = getRepositories(Help.REPO_FACTORY(), opts.userRepos(),
				opts.useDefaultRepos());

		try {
			defaultFrameworkStorageDir = createDefaultFrameworkStorageDir();
		} catch (IOException e) {
			throw new FeatureLauncherCliException("Could not create framework storage dir", e);
		}

		Map<String, String> fwkProps = new HashMap<>(opts.frameworkProps());
		fwkProps.putIfAbsent("org.osgi.framework.storage", defaultFrameworkStorageDir.toString());
		fwkProps.putIfAbsent("org.osgi.framework.storage.clean", "onFirstInit");

		System.out.printf("Launching feature %s%n", opts.feature().getID());
		System.out.println("------------------------------------------------------------------------");
		printSection("Using artifact repositories", repositories);
		printSection("Using framework properties", fwkProps);
		printSection("Using configuration", opts.configuration());
		printSection("Using variables", opts.variables());
		printSection("Using decorators", opts.decorators());
		printSection("Using extension handlers", opts.extensionHandlers());

		List<FeatureDecorator> decoratorInstances = instantiateDecorators(opts.decorators());
		Map<String, FeatureExtensionHandler> extHandlerInstances = instantiateExtHandlers(opts.extensionHandlers());

		LaunchFrameworkFeatureExtensionHandler lffehi = new LaunchFrameworkFeatureExtensionHandler();
		DecorationContext<LaunchFrameworkFeatureExtensionHandler> ctx = new DecorationContext<>(lffehi);

		Feature decorated;
		try {
			decorated = ctx.executeFeatureDecorators(Help.FEATURE_SERVICE(), opts.feature(), repositories,
					decoratorInstances);
			decorated = ctx.executeFeatureExtensionHandlers(Help.FEATURE_SERVICE(), decorated, repositories,
					extHandlerInstances);
		} catch (AbandonOperationException aoe) {
			throw new FeatureLauncherCliException("Feature decoration failed", aoe);
		}

		Optional<Object> locatedFrameworkFactory = lffehi.getLocatedFrameworkFactory();
		URL[] secondStageClasspath = buildSecondStageClasspath();

		ClassLoader parentLoader = locatedFrameworkFactory.map(o -> o.getClass().getClassLoader())
				.orElseGet(FeatureLauncherCli.class::getClassLoader);

		SecondStageLauncher secondStage = ServiceLoader
				.load(SecondStageLauncher.class, URLClassLoader.newInstance(secondStageClasspath, parentLoader))
				.findFirst().orElseThrow(() -> new NoSuchElementException("Unable to load second-stage launcher"));

		if (opts.dryRun()) {
			System.out.println("Dry-run requested – framework will NOT be started.");
			return;
		}
		try {
			secondStage.launch(decorated, ctx, repositories, locatedFrameworkFactory, opts.variables(),
					opts.configuration(), fwkProps).waitForStop(0);
		} catch (InterruptedException ie) {
			System.err.println("Terminated by interruption.");
			Thread.currentThread().interrupt();
		}
	}

	private static void printSection(String title, Iterable<?> values) {
		if (values != null && values.iterator().hasNext()) {
			System.out.println(title + ":");
			values.forEach(v -> System.out.println("  " + v));
			System.out.println("------------------------------------------------------------------------");
		}
	}

	private static void printSection(String title, Map<?, ?> values) {
		if (values != null && !values.isEmpty()) {
			System.out.println(title + ":");
			values.forEach((k, v) -> System.out.printf("  %s = %s%n", k, v));
			System.out.println("------------------------------------------------------------------------");
		}
	}

	private static List<FeatureDecorator> instantiateDecorators(List<Class<?>> classes) {
		List<FeatureDecorator> list = new ArrayList<>();
		for (Class<?> c : classes) {
			try {
				list.add((FeatureDecorator) c.getDeclaredConstructor().newInstance());
			} catch (Exception e) {
				throw new FeatureLauncherCliException("Cannot instantiate decorator " + c, e);
			}
		}
		return list;
	}

	private static Map<String, FeatureExtensionHandler> instantiateExtHandlers(Map<String, Class<?>> map) {
		Map<String, FeatureExtensionHandler> m = new LinkedHashMap<>();
		for (Map.Entry<String, Class<?>> e : map.entrySet()) {
			try {
				m.put(e.getKey(), (FeatureExtensionHandler) e.getValue().getDeclaredConstructor().newInstance());
			} catch (Exception ex) {
				throw new FeatureLauncherCliException("Cannot instantiate extension handler " + e, ex);
			}
		}
		return m;
	}

	private MutableRepositoryList getRepositories(RepositoryFactory factory, Map<URI, Map<String, Object>> userRepos,
			boolean useDefaults) {

		List<Repository> repos = new ArrayList<>();

		int i = 0;
		for (Map.Entry<URI, Map<String, Object>> e : userRepos.entrySet()) {
			Map<String, Object> props = new LinkedHashMap<>(e.getValue());
			props.putIfAbsent(ARTIFACT_REPOSITORY_NAME, "repo-arg-" + (i++));
			repos.add(factory.createRepository(e.getKey(), props));
		}

		if (useDefaults) {
			try {
				Path m2 = getDefaultM2RepositoryPath();
				repos.add(factory.createRepository(m2.toUri(), Map.of(ARTIFACT_REPOSITORY_NAME, "local")));
				repos.add(factory.createRepository(URI.create("https://repo1.maven.org/maven2/"),
						Map.of(ARTIFACT_REPOSITORY_NAME, "central", "localRepositoryPath", m2.toString())));
			} catch (Exception ex) {
				throw new FeatureLauncherCliException("Could not create default repos", ex);
			}
		}
		return new MutableRepositoryList(
				repos.stream().<ArtifactRepository>map(ArtifactRepositoryAdapter::new).toList());
	}

	static Path getDefaultM2RepositoryPath() throws IOException {
		return Paths.get(new File(System.getProperty("user.home")).getCanonicalPath(), ".m2", "repository");
	}

	private static Path createDefaultFrameworkStorageDir() throws IOException {
		return Files.createTempDirectory("osgi_");
	}

	private URL[] buildSecondStageClasspath() {
		ClassLoader cl = getClass().getClassLoader();
		try (InputStream is = cl.getResourceAsStream("META-INF/second-stage-classpath");
				BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

			return br.lines().flatMap(line -> locateClasspathEntries(cl, line)).map(this::flattenIfNestedJar)
					.toArray(URL[]::new);

		} catch (IOException ioe) {
			throw new FeatureLauncherCliException("Unable to build second stage classpath", ioe);
		}
	}

	private Stream<URL> locateClasspathEntries(ClassLoader cl, String path) {
		try {
			if (path.endsWith("/")) {
				String clsPath = getClass().getName().replace('.', '/') + ".class";
				Enumeration<URL> roots = cl.getResources(clsPath);

				int pkgTokens = getClass().getPackage().getName().split("\\.").length;
				String rel = Stream.generate(() -> "..").limit(pkgTokens).collect(Collectors.joining("/", "", path));

				return Collections.list(roots).stream().map(u -> {
					try {
						return u.toURI().resolve(rel).toURL();
					} catch (Exception e) {
						return null;
					}
				});
			} else {
				return Collections.list(cl.getResources(path)).stream();
			}
		} catch (IOException ioe) {
			return Stream.empty();
		}
	}

	private URL flattenIfNestedJar(URL url) {
		if (url.getPath().endsWith("/")) {
			return url;
		}
		try {
			Path tmp = Files.createTempFile("featurelauncher", "secondstage");
			try (OutputStream os = Files.newOutputStream(tmp, StandardOpenOption.WRITE);
					InputStream is = url.openStream()) {
				is.transferTo(os);
			}
			return tmp.toUri().toURL();
		} catch (IOException ioe) {
			throw new FeatureLauncherCliException("Unable to expand nested JAR " + url, ioe);
		}
	}

}
