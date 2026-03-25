/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Stefan Bischof - initial
 */
package org.eclipse.osgi.technology.featurelauncher.feature.mavenplugin;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.FeatureBundleMetadata;
import org.eclipse.osgi.technology.featurelauncher.featureservice.base.FeatureServiceImpl;
import org.eclipse.osgi.technology.featurelauncher.featureservice.base.IDImpl;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBuilder;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.ID;

import aQute.bnd.build.Container;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.maven.lib.resolve.DependencyResolver;
import aQute.bnd.maven.lib.resolve.Operation;
import aQute.bnd.maven.lib.resolve.PostProcessor;
import aQute.bnd.maven.lib.resolve.Scope;

/**
 * Abstract base class for feature-related Mojos, providing shared Maven
 * injection points and bundle resolution logic.
 */
public abstract class AbstractFeatureMojo extends AbstractMojo {

	protected static final String OSGI_FEATURE_VAR = "osgi.feature.var.";
	protected static final String OSGI_FEATURE_CAT = "osgi.feature.category";

	@Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
	@SuppressWarnings("deprecation")
	protected ArtifactRepository localRepository;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	protected MavenProject project;

	@Parameter(defaultValue = "${settings}", readonly = true)
	protected Settings settings;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	protected RepositorySystemSession repositorySession;

	@Parameter(defaultValue = "${session}", readonly = true)
	protected MavenSession session;

	@Parameter(property = "feature.skip", defaultValue = "false")
	protected boolean skip;

	@Parameter
	protected Include include;

	@Parameter
	protected Exclude exclude;

	@Parameter(property = "feature.outputDirectory", defaultValue = "${project.build.directory}")
	protected File outputDirectory;

	@Parameter(defaultValue = "${project.basedir}")
	protected File baseDir;

	@Component
	protected ProjectDependenciesResolver resolver;

	@Component
	protected MavenProjectHelper projectHelper;

	@Component
	protected RepositorySystem repositorySystem;

	@Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
	protected List<RemoteRepository> remoteRepositories;

	@Component
	@SuppressWarnings("deprecation")
	protected org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

	protected FeatureServiceImpl featureService = new FeatureServiceImpl();

	/**
	 * Resolves all bundles from include minus exclude.
	 */
	protected List<FeatureBundleMetadata> resolveAllBundles() throws IOException, MojoExecutionException {
		List<FeatureBundleMetadata> featureBundles = new ArrayList<>();

		if (include != null) {
			getLog().info("Processing include section...");
			List<FeatureBundleMetadata> includedBundles = calcBundles(include);
			featureBundles.addAll(includedBundles);
			getLog().info("Included " + includedBundles.size() + " bundles");
		}

		if (exclude != null) {
			getLog().info("Processing exclude section...");
			List<FeatureBundleMetadata> excludedBundles = calcBundles(exclude);
			int beforeSize = featureBundles.size();
			featureBundles.removeAll(excludedBundles);
			int removedCount = beforeSize - featureBundles.size();
			getLog().info("Excluded " + removedCount + " bundles (matched " + excludedBundles.size() + " exclude entries)");
		}

		getLog().info("Final feature bundle count: " + featureBundles.size());
		for (FeatureBundleMetadata fbm : featureBundles) {
			getLog().info("  Feature bundle: " + fbm.id());
		}

		return featureBundles;
	}

	/**
	 * Extracts variables from project properties with prefix "osgi.feature.var.".
	 */
	protected Map<String, Object> extractProjectVariables() {
		Map<String, Object> variables = new HashMap<>();
		Properties properties = project.getProperties();
		for (Entry<Object, Object> e : properties.entrySet()) {
			String key = e.getKey().toString();

			if (key.startsWith(OSGI_FEATURE_VAR)) {
				key = key.replace(OSGI_FEATURE_VAR, "");
				int i = key.indexOf("_");
				if (i < 0) {
					i = 0;
				}
				String type = key.substring(0, i);
				String varKey = key.substring(i + 1);
				Object value = e.getValue();

				Object varValue = switch (type) {
				case "" -> value.toString();
				case "Integer" -> Integer.parseInt(value.toString());
				case "Double" -> Double.parseDouble(value.toString());
				case "Float" -> Float.parseFloat(value.toString());
				case "Boolean" -> Boolean.parseBoolean(value.toString());
				case "String" -> value.toString();
				default -> throw new IllegalArgumentException("Unknown variable type: " + type);
				};

				variables.put(varKey, varValue);
			}
		}
		return variables;
	}

	/**
	 * Extracts categories from project properties with prefix "osgi.feature.category".
	 */
	protected List<String> extractProjectCategories() {
		List<String> categories = new ArrayList<>();
		Properties properties = project.getProperties();
		for (Entry<Object, Object> e : properties.entrySet()) {
			String key = e.getKey().toString();
			if (key.startsWith(OSGI_FEATURE_CAT)) {
				categories.add(e.getValue().toString());
			}
		}
		return categories;
	}

	/**
	 * Builds the project GAV string.
	 */
	protected String buildProjectGav() {
		return project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion();
	}

	protected List<FeatureBundleMetadata> calcBundles(Source source) throws IOException, MojoExecutionException {
		List<FeatureBundleMetadata> resolvedBundles = new ArrayList<>();

		if (source.maven != null && source.maven.mavenDependencies) {

			getLog().info("Adding Maven dependencies with scopes: " + source.maven.scopes);
			boolean includeTransitive = true;
			PostProcessor postProcessor = (a) -> a;
			boolean useMavenDependencies = true;
			boolean includeDependencyManagement = false;
			DependencyResolver depResolver = new DependencyResolver(project, repositorySession, resolver,
					repositorySystem, artifactFactory, source.maven.scopes, includeTransitive, postProcessor,
					useMavenDependencies, includeDependencyManagement);

			Map<File, ArtifactResult> artifactMap = depResolver.resolve();

			if (artifactMap.isEmpty()) {
				getLog().info("no dependencys defined");
			}
			for (Entry<File, ArtifactResult> e : artifactMap.entrySet()) {
				ArtifactResult ar = e.getValue();
				getLog().info("Artifact: " + ar);

				String type = ar.getArtifact().getExtension();
				Optional<String> oType = type.isEmpty() ? Optional.empty() : Optional.of(type);
				String classifier = ar.getArtifact().getClassifier();
				Optional<String> oClassifier = classifier.isEmpty() ? Optional.empty() : Optional.of(classifier);

				IDImpl id = new IDImpl(ar.getArtifact().getGroupId(), ar.getArtifact().getArtifactId(),
						ar.getArtifact().getVersion(), oType, oClassifier);
				File aFile = e.getKey();

				FeatureBundleMetadata fbm = new FeatureBundleMetadata(id, aFile, new HashMap<>());

				resolvedBundles.add(fbm);
			}
		}

		resolvedBundles.addAll(filesFromBndRun(source.bndruns.getFiles(baseDir, "*.bndrun")));

		// Resolve bundles from existing feature JSON files
		List<File> featureFiles = source.features.getFiles(baseDir, "*.json");
		if (!featureFiles.isEmpty()) {
			resolvedBundles.addAll(bundlesFromFeatureFiles(featureFiles));
		}

		return resolvedBundles;
	}

	protected List<FeatureBundleMetadata> filesFromBndRun(List<File> bndrunFiles) {
		List<FeatureBundleMetadata> bundles = new ArrayList<>();

		getLog().info("Reading bundles from Bndrun files...");
		if (bndrunFiles.isEmpty()) {
			getLog().info("no Bndrun files defined");
			return List.of();
		}

		BndrunContainer container = new BndrunContainer.Builder(project, session, repositorySession, resolver,
				artifactFactory, repositorySystem).setIncludeDependencyManagement(true)
				.setScopes(Set.of(Scope.compile, Scope.runtime)).setUseMavenDependencies(true).build();

		for (File runFile : bndrunFiles) {
			getLog().info("Processing Bndrun file: " + runFile);

			Operation operation = (bndrunFile, taskname, bndrun) -> {

				Collection<Container> runbundles = bndrun.getRunbundles();
				getLog().info("Found " + runbundles.size() + " -runbundles in " + runFile.getName());

				for (Container cont : runbundles) {
					File f = cont.getFile();
					ID id = GavFromFile.parseIDFromPath(f.toPath(), Path.of(localRepository.getBasedir()));
					getLog().info("  Bundle: " + id);
					FeatureBundleMetadata fbm = new FeatureBundleMetadata(id, cont.getFile(), new HashMap<>());
					bundles.add(fbm);
				}
				return 0;
			};

			try {
				container.execute(runFile, "feature", outputDirectory, operation);
			} catch (Exception e) {
				getLog().error("Failed to process Bndrun file: " + runFile, e);
			}
		}

		return bundles;
	}

	/**
	 * Reads bundles from existing feature JSON files and resolves them as
	 * FeatureBundleMetadata. This enables using bundles from one feature
	 * as include or exclude source for another feature.
	 */
	protected List<FeatureBundleMetadata> bundlesFromFeatureFiles(List<File> featureFiles) throws IOException {
		List<FeatureBundleMetadata> resolved = new ArrayList<>();

		for (File file : featureFiles) {
			getLog().info("Reading bundles from feature file: " + file);

			String jsonContent = Files.readString(file.toPath());
			Feature feature = featureService.readFeature(new StringReader(jsonContent));

			for (FeatureBundle bundle : feature.getBundles()) {
				ID bundleId = bundle.getID();
				try {
					File resolvedFile = resolveArtifact(bundleId);
					FeatureBundleMetadata fbm = new FeatureBundleMetadata(bundleId, resolvedFile, new HashMap<>());
					resolved.add(fbm);
					getLog().info("  From feature: " + bundleId);
				} catch (Exception e) {
					getLog().warn("Could not resolve bundle from feature: " + bundleId + " - " + e.getMessage());
				}
			}
		}

		getLog().info("Resolved " + resolved.size() + " bundles from " + featureFiles.size() + " feature file(s)");
		return resolved;
	}

	protected File resolveArtifact(ID id) throws Exception {
		getLog().info("Resolving Maven artifact: " + id);

		org.eclipse.aether.artifact.DefaultArtifact artifact = new org.eclipse.aether.artifact.DefaultArtifact(
				id.toString());
		ArtifactRequest request = new ArtifactRequest(artifact, remoteRepositories, null);

		ArtifactResult result = repositorySystem.resolveArtifact(repositorySession, request);
		File artifactFile = result.getArtifact().getFile();

		getLog().info("Resolved artifact file: " + artifactFile);
		return artifactFile;
	}

	/**
	 * Resolves all bundle JARs via Maven (local cache → remote repos) and copies
	 * them into a Maven GAV directory layout.
	 * Pattern: {@code repoDir/org/osgi/org.osgi.resource/1.0.0/org.osgi.resource-1.0.0.jar}
	 *
	 * @throws MojoExecutionException if any artifact cannot be resolved
	 */
	protected void copyBundlesToGavLayout(List<FeatureBundleMetadata> bundles, Path repoDir)
			throws IOException, MojoExecutionException {
		Files.createDirectories(repoDir);
		for (FeatureBundleMetadata fbm : bundles) {
			resolveAndCopyToGavLayout(fbm, repoDir);
		}
	}

	/**
	 * Resolves a single bundle JAR via Maven's artifact resolution (local cache first,
	 * then remote repositories) and copies it into the Maven GAV directory layout.
	 *
	 * @throws MojoExecutionException if the artifact cannot be resolved
	 */
	protected void resolveAndCopyToGavLayout(FeatureBundleMetadata fbm, Path repoDir)
			throws IOException, MojoExecutionException {
		ID id = fbm.id();
		String groupId = id.getGroupId();
		String artifactId = id.getArtifactId();
		String version = id.getVersion();

		// Resolve via Maven: local cache first, then remote repos
		File resolvedFile = resolveArtifactFile(id);

		// Build GAV path: org/osgi/org.osgi.resource/1.0.0/org.osgi.resource-1.0.0.jar
		Path groupPath = repoDir;
		for (String part : groupId.split("\\.")) {
			groupPath = groupPath.resolve(part);
		}
		Path versionDir = groupPath.resolve(artifactId).resolve(version);
		Files.createDirectories(versionDir);

		String fileName = artifactId + "-" + version + ".jar";
		Path target = versionDir.resolve(fileName);
		Files.copy(resolvedFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

		getLog().info("Resolved and staged: " + id + " -> " + target);
	}

	/**
	 * Resolves an artifact JAR via Maven's repository system.
	 * Checks local repository cache first; if not present, downloads from
	 * configured remote repositories.
	 *
	 * @param id the artifact GAV coordinates
	 * @return the resolved JAR file (from local cache or freshly downloaded)
	 * @throws MojoExecutionException if resolution fails
	 */
	protected File resolveArtifactFile(ID id) throws MojoExecutionException {
		String coords = id.getGroupId() + ":" + id.getArtifactId() + ":"
				+ id.getType().orElse("jar") + ":" + id.getVersion();

		getLog().debug("Resolving artifact via Maven: " + coords);

		try {
			org.eclipse.aether.artifact.DefaultArtifact artifact =
					new org.eclipse.aether.artifact.DefaultArtifact(coords);
			ArtifactRequest request = new ArtifactRequest(artifact, remoteRepositories, null);

			ArtifactResult result = repositorySystem.resolveArtifact(repositorySession, request);
			File artifactFile = result.getArtifact().getFile();

			if (artifactFile == null || !artifactFile.exists()) {
				throw new MojoExecutionException("Resolved artifact file not found for: " + coords);
			}

			getLog().debug("Resolved: " + coords + " -> " + artifactFile);
			return artifactFile;
		} catch (org.eclipse.aether.resolution.ArtifactResolutionException e) {
			throw new MojoExecutionException("Failed to resolve artifact: " + coords
					+ " (checked local cache and remote repositories)", e);
		}
	}

	/**
	 * Returns the project license string or null.
	 */
	protected String getProjectLicense() {
		if (project.getLicenses().isEmpty()) {
			return null;
		}
		return project.getLicenses().stream().map(License::getName).collect(java.util.stream.Collectors.joining(","));
	}

	/**
	 * Returns the project vendor (organization name) or null.
	 */
	protected String getProjectVendor() {
		return project.getOrganization() != null ? project.getOrganization().getName() : null;
	}

	/**
	 * Parses a feature JSON file into a Feature object.
	 */
	protected Feature parseFeatureFile(File file) throws IOException {
		String json = Files.readString(file.toPath());
		return featureService.readFeature(new StringReader(json));
	}

	/**
	 * Merges variables, configurations, and extensions from source features
	 * into a base feature. Bundles from source features are NOT merged.
	 *
	 * <p>Later sources override earlier ones on key conflicts (last-wins).
	 *
	 * @param base the feature to merge into
	 * @param sources features whose variables/configurations/extensions are merged
	 * @return a new feature with merged content
	 */
	protected Feature mergeFeatures(Feature base, List<Feature> sources) {
		FeatureBuilder builder = featureService.getBuilderFactory()
				.newFeatureBuilder(base.getID());

		// Copy base metadata
		base.getName().ifPresent(builder::setName);
		base.getDescription().ifPresent(builder::setDescription);
		base.getDocURL().ifPresent(builder::setDocURL);
		base.getLicense().ifPresent(builder::setLicense);
		base.getSCM().ifPresent(builder::setSCM);
		base.getVendor().ifPresent(builder::setVendor);
		builder.setComplete(base.isComplete());

		// Copy base bundles
		builder.addBundles(base.getBundles().toArray(new FeatureBundle[0]));

		// Copy base categories
		builder.addCategories(base.getCategories().toArray(new String[0]));

		// Start with base variables, configurations, extensions
		builder.addVariables(base.getVariables());
		builder.addConfigurations(base.getConfigurations().values().toArray(new FeatureConfiguration[0]));
		builder.addExtensions(base.getExtensions().values().toArray(new FeatureExtension[0]));

		// Merge from each source (last-wins on conflict)
		for (Feature source : sources) {
			if (!source.getVariables().isEmpty()) {
				builder.addVariables(source.getVariables());
				getLog().info("  Merged " + source.getVariables().size() + " variable(s) from " + source.getID());
			}
			if (!source.getConfigurations().isEmpty()) {
				builder.addConfigurations(
						source.getConfigurations().values().toArray(new FeatureConfiguration[0]));
				getLog().info("  Merged " + source.getConfigurations().size()
						+ " configuration(s) from " + source.getID());
			}
			if (!source.getExtensions().isEmpty()) {
				builder.addExtensions(
						source.getExtensions().values().toArray(new FeatureExtension[0]));
				getLog().info("  Merged " + source.getExtensions().size()
						+ " extension(s) from " + source.getID());
			}
		}

		return builder.build();
	}

	/**
	 * Writes a Feature to a JSON file using the FeatureService.
	 */
	protected void writeFeature(Feature feature, File outputFile) throws IOException {
		java.io.StringWriter sw = new java.io.StringWriter();
		featureService.writeFeature(feature, sw);
		Files.writeString(outputFile.toPath(), sw.toString());
	}
}
