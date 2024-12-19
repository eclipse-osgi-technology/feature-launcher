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

import static org.osgi.service.featurelauncher.repository.ArtifactRepositoryConstants.ARTIFACT_REPOSITORY_NAME;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.featurelauncher.FeatureLauncher;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory;

import com.kentyou.featurelauncher.common.repository.impl.WrappingArtifactRepository;
import com.kentyou.featurelauncher.common.util.impl.ServiceLoaderUtil;
import com.kentyou.featurelauncher.repository.spi.NamedArtifactRepository;

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

	private static FeatureService featureService = ServiceLoaderUtil.loadFeatureService();

	private FeatureLauncher featureLauncher = ServiceLoaderUtil.loadFeatureLauncherService();

	private Path defaultFrameworkStorageDir;

	private Map<String, String> defaultFrameworkProperties;

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

		List<NamedArtifactRepository> artifactRepositories = getArtifactRepositories(featureLauncher,
				userSpecifiedArtifactRepositories, useDefaultRepos);

		try {
			this.defaultFrameworkStorageDir = createDefaultFrameworkStorageDir();
		} catch (IOException e) {
			throw new FeatureLauncherCliException("Could not create default framework storage directory!", e);
		}

		this.defaultFrameworkProperties = getDefaultFrameworkProperties(defaultFrameworkStorageDir);

		frameworkProperties = !frameworkProperties.isEmpty() ? frameworkProperties : defaultFrameworkProperties;

		System.out.println(String.format("Launching feature %s", feature.getID()));
		System.out.println("------------------------------------------------------------------------");

		FeatureLauncher.LaunchBuilder featureLaunchBuilder = featureLauncher.launch(feature);

		System.out.println("Using artifact repositories: ");
		for (NamedArtifactRepository artifactRepository : artifactRepositories) {
			System.out.println(artifactRepository);
		}
		System.out.println("------------------------------------------------------------------------");

		artifactRepositories.forEach(featureLaunchBuilder::withRepository);

		if (!frameworkProperties.isEmpty()) {
			System.out.println("Using framework properties: ");
			for (Map.Entry<String, String> frameworkPropertyEntry : frameworkProperties.entrySet()) {
				System.out.println(
						String.format("%s = %s", frameworkPropertyEntry.getKey(), frameworkPropertyEntry.getValue()));
			}
			System.out.println("------------------------------------------------------------------------");

			featureLaunchBuilder.withFrameworkProperties(frameworkProperties);
		}

		if (!configuration.isEmpty()) {
			System.out.println("Using configuration: ");
			for (Map.Entry<String, Object> configurationEntry : configuration.entrySet()) {
				System.out
						.println(String.format("%s = %s", configurationEntry.getKey(), configurationEntry.getValue()));
			}
			System.out.println("------------------------------------------------------------------------");

			featureLaunchBuilder.withConfiguration(configuration);
		}

		if (!variables.isEmpty()) {
			System.out.println("Using variables: ");
			for (Map.Entry<String, Object> variableEntry : variables.entrySet()) {
				System.out.println(String.format("%s = %s", variableEntry.getKey(), variableEntry.getValue()));
			}
			System.out.println("------------------------------------------------------------------------");

			featureLaunchBuilder.withVariables(variables);
		}

		if (!decorators.isEmpty()) {
			System.out.println("Using decorators: ");
			for (Class<?> decorator : decorators) {
				System.out.println(String.format("%s", decorator));
			}
			System.out.println("------------------------------------------------------------------------");

			for (Class<?> decorator : decorators) {
				try {
					featureLaunchBuilder
							.withDecorator((FeatureDecorator) decorator.getDeclaredConstructor().newInstance());
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new FeatureLauncherCliException("Could not create instance of FeatureDecorator!", e);
				}
			}
		}

		if (!extensionHandlers.isEmpty()) {
			System.out.println("Using extension handlers: ");
			for (Map.Entry<String, Class<?>> extensionHandlerEntry : extensionHandlers.entrySet()) {
				System.out.println(
						String.format("%s = %s", extensionHandlerEntry.getKey(), extensionHandlerEntry.getValue()));
			}
			System.out.println("------------------------------------------------------------------------");

			for (Map.Entry<String, Class<?>> extensionHandler : extensionHandlers.entrySet()) {
				try {
					featureLaunchBuilder.withExtensionHandler(extensionHandler.getKey(),
							(FeatureExtensionHandler) extensionHandler.getValue().getDeclaredConstructor()
									.newInstance());
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					throw new FeatureLauncherCliException("Could not create instance of FeatureExtensionHandler!", e);
				}
			}
		}

		if (!dryRun) {
			Framework osgiFramework = featureLaunchBuilder.launchFramework();

			try {
				osgiFramework.waitForStop(0);
			} catch (InterruptedException e) {
				System.err.println("Error stopping framework!");
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new FeatureLauncherCli()).execute(args);

		System.exit(exitCode);
	}

	private List<NamedArtifactRepository> getArtifactRepositories(ArtifactRepositoryFactory artifactRepositoryFactory,
			Map<URI, Map<String, Object>> userSpecifiedRemoteArtifactRepositories, boolean useDefaultRepos) {

		List<NamedArtifactRepository> artifactRepositories = new ArrayList<>();

		int i = 0;
		for (Map.Entry<URI, Map<String, Object>> userSpecifiedRemoteArtifactRepositoryEntry : userSpecifiedRemoteArtifactRepositories
				.entrySet()) {
			Map<String, Object> configurationProperties = userSpecifiedRemoteArtifactRepositoryEntry.getValue();
			
			String name = String.valueOf(configurationProperties.getOrDefault(ARTIFACT_REPOSITORY_NAME, "repo-arg-" + i));
			
			ArtifactRepository userSpecifiedRemoteArtifactRepository = artifactRepositoryFactory
					.createRepository(userSpecifiedRemoteArtifactRepositoryEntry.getKey(), configurationProperties);
			
			addPossiblyNamedRepo(artifactRepositories, userSpecifiedRemoteArtifactRepository, name);
		}

		if (useDefaultRepos) {
			Path defaultM2RepositoryPath;
			try {
				defaultM2RepositoryPath = getDefaultM2RepositoryPath();
			} catch (IOException e) {
				throw new FeatureLauncherCliException("Could not obtain default M2 artifact repository path!", e);
			}
			try {
				ArtifactRepository local = artifactRepositoryFactory.createRepository(
						defaultM2RepositoryPath.toUri(), Map.of(ARTIFACT_REPOSITORY_NAME, "local"));
				
				addPossiblyNamedRepo(artifactRepositories, local, "local");
				
				ArtifactRepository remote = artifactRepositoryFactory.createRepository(
						URI.create("https://repo1.maven.org/maven2/"), 
						Map.of(ARTIFACT_REPOSITORY_NAME, "central", 
								"localRepositoryPath", defaultM2RepositoryPath.toAbsolutePath().toString()));
				
				addPossiblyNamedRepo(artifactRepositories, remote, "central");
			} catch (Exception e) {
				throw new FeatureLauncherCliException("Could not create default artifact repositories!", e);
			}
		}

		if (!userSpecifiedRemoteArtifactRepositories.isEmpty()) {

		}

		return artifactRepositories;
	}

	private void addPossiblyNamedRepo(List<NamedArtifactRepository> artifactRepositories, ArtifactRepository possiblyNamed, String altName) {
		if(possiblyNamed instanceof NamedArtifactRepository nar) {
			artifactRepositories.add(nar);
		} else {
			artifactRepositories.add(new WrappingArtifactRepository(possiblyNamed, altName));
		}
	}

	static Path getDefaultM2RepositoryPath() throws IOException {
		File userHome = new File(System.getProperty("user.home"));

		return Paths.get(userHome.getCanonicalPath(), ".m2", "repository");
	}
	
	private Map<String, String> getDefaultFrameworkProperties(Path defaultFrameworkStorageDir) {
		return Map.of(Constants.FRAMEWORK_STORAGE, defaultFrameworkStorageDir.toString(),
				Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
	}

	private Path createDefaultFrameworkStorageDir() throws IOException {
		return Files.createTempDirectory("osgi_");
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
