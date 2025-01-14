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

import static org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryConstants.ARTIFACT_REPOSITORY_NAME;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.osgi.technology.featurelauncher.common.decorator.impl.DecorationContext;
import org.eclipse.osgi.technology.featurelauncher.common.decorator.impl.LaunchFrameworkFeatureExtensionHandler;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;

import org.eclipse.osgi.technology.featurelauncher.repository.spi.Repository;
import org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryFactory;
import org.eclipse.osgi.technology.featurelauncher.launch.spi.SecondStageLauncher;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.IParameterConsumer;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/**
 * 160.4.2.4 The Feature Launcher Command Line
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 8, 2024
 */

// @formatter:off
@Command(
		name = "", 
		description = FeatureLauncherCli.DESCRIPTION, 
		mixinStandardHelpOptions = true, 
		version = FeatureLauncherCli.VERSION, 
		header = FeatureLauncherCli.HEADING, 
		headerHeading = "@|bold,underline Usage|@:%n%n", 
		descriptionHeading = "%n@|bold,underline Description|@:%n%n", 
		parameterListHeading = "%n@|bold,underline Parameters|@:%n", 
		optionListHeading = "%n@|bold,underline Options|@:%n", 
		sortOptions = false, 
		abbreviateSynopsis = true)
// @formatter:on
public class FeatureLauncherCli implements Runnable {
	static final String HEADING = "The Feature Launcher Command Line 1.0";
	static final String DESCRIPTION = "In order to support the Zero Code goal of the "
			+ "Feature Launcher Service it is not sufficient to provide a Java API, "
			+ "it must also be possible to launch a feature from the command line "
			+ "in a standard way. To support this implementations of the Feature "
			+ "Launcher provides an executable JAR file which allows a Feature "
			+ "to be launched from the command line.";
	static final String VERSION = HEADING + " 1.0";
	static final int EXITCODE_SUCCESS = 0;
	static final String REMOTE_ARTIFACT_REPOSITORY_URI_VALUE = "REMOTE_ARTIFACT_REPOSITORY_URI";

	@ArgGroup(exclusive = true, multiplicity = "1", order = -10)
	private FeatureFromJsonOrFilePath featureFromJsonOrFilePath;

	@Option(names = { "-a",
			"--artifact-repository" }, paramLabel = "uri,[key=value]", description = "Specifies an artifact repository URI and optionally one "
					+ "or more configuration properties for that artifact repository, "
					+ "such as those described in Remote Repositories on page 1381. "
					+ "This property may be repeated to add more than one artifact "
					+ "repository.", order = -9, mapFallbackValue = REMOTE_ARTIFACT_REPOSITORY_URI_VALUE, split = ",", parameterConsumer = UserSpecifiedArtifactRepositoryParameterConsumer.class)
	private Map<URI, Map<String, Object>> userSpecifiedArtifactRepositories;

	@Option(names = {
			"--impl-default-repos" }, description = "Use default local and remote repositories instead of defining them.")
	private boolean useDefaultRepos;

	@Option(names = { "-d", "--decorator" }, paramLabel = "class name", description = "Provides "
			+ "the name of a decorator class that should be used when launching "
			+ "the feature. The decorator class must be public, available on the "
			+ "classpath, and have a public zero-argument constructor. This "
			+ "property may be repeated to add more than one decorator.", order = -8)
	private List<Class<?>> decorators;

	@Option(names = { "-e",
			"--extension-handler" }, paramLabel = "extension name=class name", description = "Provides the name of an extension, and the extension handler "
					+ "class that should be used to handle the extension when launching "
					+ "the feature. The extension handler class must be public, available "
					+ "on the classpath, and have a public zero-argument constructor. This "
					+ "property may be repeated to add more than one extension handler.", order = -7)
	private Map<String, Class<?>> extensionHandlers;

	@Option(names = { "-l",
			"--launch-property" }, paramLabel = "key=value", description = "Provides one or more launch properties that should be passed "
					+ "to the framework when it is launched.", order = -6)
	private Map<String, String> frameworkProperties;

	@Option(names = { "-v",
			"--variable-override" }, paramLabel = "key=value", description = "Provides one or more variables that should be used to set or "
					+ "override variables defined in the feature.", order = -5)
	private Map<String, Object> variables;

	@Option(names = { "-c",
			"--configuration" }, paramLabel = "key=value", description = "Provides one or more configuration properties that should "
					+ "be used to control implementation specific behaviour.", order = -4)
	private Map<String, Object> configuration;

	@Option(names = {
			"--impl-dry-run" }, description = "Evaluates all options, processes them and displays output, but does not launch framework. Hidden option used for testing", hidden = true)
	private boolean dryRun;

	@Spec
	private CommandSpec commandSpec;

	private static FeatureService featureService = ServiceLoader.load(FeatureService.class)
			.findFirst().orElseThrow(() -> new NoSuchElementException("No Feature Service available"));

	private RepositoryFactory repoFactory = ServiceLoader.load(RepositoryFactory.class)
			.findFirst().orElseThrow(() -> new NoSuchElementException("No Repository Factory available"));
	
	private Path defaultFrameworkStorageDir;

	public void run() {
		if (commandSpec.commandLine().getParseResult().expandedArgs().isEmpty()) {
			commandSpec.commandLine().usage(commandSpec.commandLine().getOut());
			return;
		}

		Feature feature = (featureFromJsonOrFilePath.featureFromJson != null)
				? featureFromJsonOrFilePath.featureFromJson
				: featureFromJsonOrFilePath.featureFromFilePath;

		userSpecifiedArtifactRepositories = (userSpecifiedArtifactRepositories != null)
				? userSpecifiedArtifactRepositories
				: Collections.emptyMap();

		decorators = (decorators != null) ? decorators : Collections.emptyList();
		extensionHandlers = (extensionHandlers != null) ? extensionHandlers : Collections.emptyMap();
		frameworkProperties = (frameworkProperties != null) ? frameworkProperties : Collections.emptyMap();
		variables = (variables != null) ? variables : Collections.emptyMap();
		configuration = (configuration != null) ? configuration : Collections.emptyMap();

		List<Repository> repositories = getRepositories(repoFactory,
				userSpecifiedArtifactRepositories, useDefaultRepos);

		try {
			this.defaultFrameworkStorageDir = createDefaultFrameworkStorageDir();
		} catch (IOException e) {
			throw new FeatureLauncherCliException("Could not create default framework storage directory!", e);
		}

		Map<String, String> fwkProperties = new HashMap<String, String>();
				
		fwkProperties.putAll(getDefaultFrameworkProperties(defaultFrameworkStorageDir));
		fwkProperties.putAll(frameworkProperties);

		frameworkProperties = Map.copyOf(fwkProperties);

		System.out.println(String.format("Launching feature %s", feature.getID()));
		System.out.println("------------------------------------------------------------------------");

		System.out.println("Using artifact repositories: ");
		for (Repository artifactRepository : repositories) {
			System.out.println(artifactRepository);
		}
		System.out.println("------------------------------------------------------------------------");

//		artifactRepositories.forEach(featureLaunchBuilder::withRepository);

		if (!frameworkProperties.isEmpty()) {
			System.out.println("Using framework properties: ");
			for (Map.Entry<String, String> frameworkPropertyEntry : frameworkProperties.entrySet()) {
				System.out.println(
						String.format("%s = %s", frameworkPropertyEntry.getKey(), frameworkPropertyEntry.getValue()));
			}
			System.out.println("------------------------------------------------------------------------");

//			featureLaunchBuilder.withFrameworkProperties(frameworkProperties);
		}

		if (!configuration.isEmpty()) {
			System.out.println("Using configuration: ");
			for (Map.Entry<String, Object> configurationEntry : configuration.entrySet()) {
				System.out
						.println(String.format("%s = %s", configurationEntry.getKey(), configurationEntry.getValue()));
			}
			System.out.println("------------------------------------------------------------------------");

//			featureLaunchBuilder.withConfiguration(configuration);
		}

		if (!variables.isEmpty()) {
			System.out.println("Using variables: ");
			for (Map.Entry<String, Object> variableEntry : variables.entrySet()) {
				System.out.println(String.format("%s = %s", variableEntry.getKey(), variableEntry.getValue()));
			}
			System.out.println("------------------------------------------------------------------------");

//			featureLaunchBuilder.withVariables(variables);
		}

		List<FeatureDecorator> decoratorInstances = new ArrayList<>();
		if (!decorators.isEmpty()) {
			System.out.println("Using decorators: ");
			for (Class<?> decorator : decorators) {
				System.out.println(String.format("%s", decorator));
			}
			System.out.println("------------------------------------------------------------------------");

			for (Class<?> decorator : decorators) {
				try {
					decoratorInstances
							.add((FeatureDecorator) decorator.getDeclaredConstructor().newInstance());
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new FeatureLauncherCliException("Could not create instance of FeatureDecorator!", e);
				}
			}
		}

		Map<String, FeatureExtensionHandler> featureExtensionHandlerInstances = new HashMap<>();
		if (!extensionHandlers.isEmpty()) {
			System.out.println("Using extension handlers: ");
			for (Map.Entry<String, Class<?>> extensionHandlerEntry : extensionHandlers.entrySet()) {
				System.out.println(
						String.format("%s = %s", extensionHandlerEntry.getKey(), extensionHandlerEntry.getValue()));
			}
			System.out.println("------------------------------------------------------------------------");

			for (Map.Entry<String, Class<?>> extensionHandler : extensionHandlers.entrySet()) {
				try {
					featureExtensionHandlerInstances.put(extensionHandler.getKey(),
							(FeatureExtensionHandler) extensionHandler.getValue().getDeclaredConstructor()
									.newInstance());
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new FeatureLauncherCliException("Could not create instance of FeatureExtensionHandler!", e);
				}
			}
		}

		LaunchFrameworkFeatureExtensionHandler lffehi = new LaunchFrameworkFeatureExtensionHandler(repositories);
		DecorationContext<LaunchFrameworkFeatureExtensionHandler> context = new DecorationContext<>(lffehi, repositories);
		
		try {
			feature = context.executeFeatureDecorators(featureService, feature, decoratorInstances);
			feature = context.executeFeatureExtensionHandlers(featureService, feature, featureExtensionHandlerInstances);
		} catch (AbandonOperationException aoe) {
			throw new FeatureLauncherCliException("Feature Decoration failed", aoe);
		}
		
		Optional<Object> locatedFrameworkFactory = lffehi.getLocatedFrameworkFactory();
		
		URL[] secondStageClasspath = getSecondStageClasspath();
		
		ClassLoader parentLoader;
		if(locatedFrameworkFactory.isEmpty()) {
			System.out.println("The feature " + feature.getID() + 
					" does not include a launch framework. A framework must be available on the current classpath");
			parentLoader = FeatureLauncherCli.class.getClassLoader();
		} else {
			System.out.println("The feature " + feature.getID() + 
					" includes a launch framework. This will be used as the classpath");
			parentLoader = locatedFrameworkFactory.get().getClass().getClassLoader();
		}
		
		SecondStageLauncher secondStage = ServiceLoader.load(SecondStageLauncher.class, 
				URLClassLoader.newInstance(secondStageClasspath, parentLoader)).findFirst()
				.orElseThrow(() -> new NoSuchElementException("Unable to load the second stage launcher"));
		
		
		if (!dryRun) {
			try {
				secondStage.launch(feature, context, repositories, locatedFrameworkFactory, 
						variables, configuration, frameworkProperties).waitForStop(0);
			} catch (InterruptedException e) {
				System.err.println("Terminated by being interrupted");
			}
		}
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args);

		System.exit(exitCode);
	}

	private List<Repository> getRepositories(RepositoryFactory artifactRepositoryFactory,
			Map<URI, Map<String, Object>> userSpecifiedRemoteArtifactRepositories, boolean useDefaultRepos) {

		List<Repository> artifactRepositories = new ArrayList<>();

		int i = 0;
		for (Map.Entry<URI, Map<String, Object>> userSpecifiedRemoteArtifactRepositoryEntry : userSpecifiedRemoteArtifactRepositories
				.entrySet()) {
			Map<String, Object> configurationProperties = userSpecifiedRemoteArtifactRepositoryEntry.getValue();
			final int counter = i;
			configurationProperties.computeIfAbsent(ARTIFACT_REPOSITORY_NAME, k -> "repo-arg-" + counter);
			
			Repository userSpecifiedRemoteArtifactRepository = artifactRepositoryFactory
					.createRepository(userSpecifiedRemoteArtifactRepositoryEntry.getKey(), configurationProperties);
			
			artifactRepositories.add(userSpecifiedRemoteArtifactRepository);
			i++;
		}

		if (useDefaultRepos) {
			Path defaultM2RepositoryPath;
			try {
				defaultM2RepositoryPath = getDefaultM2RepositoryPath();
			} catch (IOException e) {
				throw new FeatureLauncherCliException("Could not obtain default M2 artifact repository path!", e);
			}
			try {
				Repository local = artifactRepositoryFactory.createRepository(
						defaultM2RepositoryPath.toUri(), Map.of(ARTIFACT_REPOSITORY_NAME, "local"));
				
				artifactRepositories.add(local);
				
				Repository remote = artifactRepositoryFactory.createRepository(
						URI.create("https://repo1.maven.org/maven2/"), 
						Map.of(ARTIFACT_REPOSITORY_NAME, "central", 
								"localRepositoryPath", defaultM2RepositoryPath.toAbsolutePath().toString()));
				
				artifactRepositories.add(remote);
			} catch (Exception e) {
				throw new FeatureLauncherCliException("Could not create default artifact repositories!", e);
			}
		}

		return artifactRepositories;
	}

	static Path getDefaultM2RepositoryPath() throws IOException {
		File userHome = new File(System.getProperty("user.home"));

		return Paths.get(userHome.getCanonicalPath(), ".m2", "repository");
	}
	
	private Map<String, String> getDefaultFrameworkProperties(Path defaultFrameworkStorageDir) {
		return Map.of("org.osgi.framework.storage", defaultFrameworkStorageDir.toString(),
				"org.osgi.framework.storage.clean", "onFirstInit");
	}

	private Path createDefaultFrameworkStorageDir() throws IOException {
		return Files.createTempDirectory("osgi_");
	}
	
	private URL[] getSecondStageClasspath() {
		ClassLoader loader = getClass().getClassLoader();
		try (InputStream is = loader.getResourceAsStream("META-INF/second-stage-classpath");
			BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			return br.lines().flatMap(s -> findClasspathEntries(loader, s))
					.map(this::flattenNestedJars)
					.toArray(URL[]::new);
		} catch (IOException e) {
			throw new FeatureLauncherCliException("An error occurred generating the second stage classpath.", e);
		}
	}

	private Stream<? extends URL> findClasspathEntries(ClassLoader loader, String s) {
		Stream<URL> url;
		try {
			if(s.endsWith("/")) {
				String name = getClass().getName();
				Enumeration<URL> tmp = loader.getResources(name.replace('.', '/') + ".class");
				if(tmp != null) {
					int tokens = getClass().getPackage().getName().split("\\.").length;
					String relativePath = Stream.generate(() -> "..").limit(tokens).collect(Collectors.joining("/", "", s));
					url = Collections.list(tmp).stream().map(u -> {
						try {
							return u.toURI().resolve(relativePath).toURL();
						} catch (Exception e) {
							System.out.println("Failed to map resource " + s + "on the classpath " + e.getMessage());
							return null;
						}
					});
				} else {
					System.out.println("Unable to locate resource " + s + "on the classpath");
					url = Stream.empty();
				}
			} else {
				url = Collections.list(loader.getResources(s)).stream();
			}
		} catch (IOException ioe) {
			System.out.println("Error locating resource " + s + "on the classpath " + ioe.getMessage());
			url = Stream.empty();
		}
		return url;
	}
	
	private URL flattenNestedJars(URL url) {
		if(url.getPath().endsWith("/")) {
			return url;
		} else {
			try {
				Path tempFile = Files.createTempFile("featurelauncher", "secondstage");
				try (OutputStream os = Files.newOutputStream(tempFile, StandardOpenOption.WRITE);
					InputStream is = url.openStream();) {
					is.transferTo(os);
				}
				return tempFile.toUri().toURL();
			} catch (IOException e) {
				throw new FeatureLauncherCliException("Unable to expand nested classpath " + url, e);
			}
		}
	}

	private static boolean isJsonFile(Path p) {
		if ((p != null) && Files.isRegularFile(p)) {
			String fileName = p.getFileName().toString();
			return "json".equalsIgnoreCase(fileName.substring(fileName.lastIndexOf(".") + 1));
		}

		return false;
	}

	private static Feature readFeature(Path featureFilePath) {
		try (Reader featureFilePathReader = Files.newBufferedReader(featureFilePath)) {
			return featureService.readFeature(featureFilePathReader);
		} catch (IOException e) {
			throw new FeatureLauncherCliException("Error reading feature from file!", e);
		}
	}

	private static Feature readFeature(String featureJSON) {
		try (Reader featureJSONReader = new StringReader(featureJSON)) {
			return featureService.readFeature(featureJSONReader);
		} catch (IOException e) {
			throw new FeatureLauncherCliException("Error reading feature from string!", e);
		}
	}

	static class FeatureFromJsonOrFilePath {
		@Parameters(arity = "1", paramLabel = "<feature json>", description = "JSON representation of the Feature to be launched.", converter = FeatureFromJsonConverter.class)
		Feature featureFromJson;

		@Option(names = { "-f",
				"--feature-file" }, arity = "1", paramLabel = "feature file path", description = "Specifies "
						+ "the location of a file containing the feature JSON. If used "
						+ "then the <feature json> must be omitted. This provides the "
						+ "feature that must be launched. Feature files in this "
						+ "directory: ${COMPLETION-CANDIDATES}", order = -10, converter = FeatureFromFilePathConverter.class, completionCandidates = FeatureFilePathCompleter.class)
		Feature featureFromFilePath;
	}

	static class UserSpecifiedArtifactRepositoryParameterConsumer implements IParameterConsumer {

		@Override
		public void consumeParameters(Stack<String> args, ArgSpec argSpec, CommandSpec commandSpec) {
			Map<URI, Map<String, Object>> repos = null;
			
			if(argSpec.isValueGettable()) {
				repos = argSpec.getValue();
			}
			if(repos == null) {
				repos = new LinkedHashMap<>();
				argSpec.setValue(repos);
			}
			
			String raw = args.pop();
			
			List<String> rawArtifactRepositoryDefinition = new ArrayList<>(Arrays.asList(raw.split(",")));

			URI artifactRepositoryURI = URI.create(rawArtifactRepositoryDefinition.remove(0));
			
			Map<String, Object> artifactRepositoryConfigurationProperties = repos.computeIfAbsent(artifactRepositoryURI, x -> new HashMap<>());

			rawArtifactRepositoryDefinition.stream()
				.filter(s -> s.contains("="))
				.map(s -> s.split("=", 2))
				.forEach(s -> artifactRepositoryConfigurationProperties.put(s[0], s[1]));
		}
	}

	static class FeatureFromJsonConverter implements ITypeConverter<Feature> {

		@Override
		public Feature convert(String value) throws Exception {
			if (value == null || value.isBlank()) {
				throw new FeatureLauncherCliException("Please provide Feature JSON!");
			}

			return readFeature(value);
		}
	}

	static class FeatureFromFilePathConverter implements ITypeConverter<Feature> {

		@Override
		public Feature convert(String value) throws Exception {
			Path path = Paths.get(value);

			if (!isJsonFile(path)) {
				throw new FeatureLauncherCliException("File not found! Please provide path to existing feature file");
			}

			return readFeature(path);
		}
	}

	static class FeatureFilePathCompleter implements Iterable<String> {
		@Override
		public Iterator<String> iterator() {
			try {
				// @formatter:off
				return Files.list(Paths.get("."))
						.filter(p -> isJsonFile(p))
						.map(p -> p.toString())
						.iterator();
				// @formatter:on
			} catch (IOException e) {
				System.err.println("Error listing feature files!");
				e.printStackTrace();
			}
			return Collections.emptyIterator();
		}
	}
}
