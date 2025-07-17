package org.eclipse.osgi.technology.featurelauncher.feature.mavenplugin;

import static aQute.bnd.maven.lib.resolve.BndrunContainer.report;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
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
import org.eclipse.osgi.technology.featurelauncher.feature.generator.ConfigSetting;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.FeatureBundleMetadata;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.FeatureBundleSetting;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.FeatureGenerator;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.FeatureMetadata;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.HashSetting;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.Setting;
import org.eclipse.osgi.technology.featurelauncher.feature.generator.impl.FeatureGeneratorImpl;
import org.eclipse.osgi.technology.featurelauncher.featureservice.base.FeatureServiceImpl;
import org.eclipse.osgi.technology.featurelauncher.featureservice.base.IDImpl;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Container;
import aQute.bnd.maven.lib.resolve.BndrunContainer;
import aQute.bnd.maven.lib.resolve.DependencyResolver;
import aQute.bnd.maven.lib.resolve.Operation;
import aQute.bnd.maven.lib.resolve.PostProcessor;
import aQute.bnd.maven.lib.resolve.Scope;
import biz.aQute.resolve.ResolveProcess;

/**
 * Mojo to generate an OSGi feature JSON file based on project configuration.
 */

@Mojo(name = "create-feature", defaultPhase = LifecyclePhase.PACKAGE)
public class FeatureGeneratorMojo extends AbstractMojo {

	private static final String OSGI_FEATURE_VAR = "osgi.feature.var.";
	private static final String OSGI_FEATURE_CAT = "osgi.feature.category";

	private static final Logger logger = LoggerFactory.getLogger(FeatureGeneratorMojo.class);

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(defaultValue = "${settings}", readonly = true)
	private Settings settings;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
	private RepositorySystemSession repositorySession;

	@Parameter(defaultValue = "${session}", readonly = true)
	private MavenSession session;

	@Parameter(property = "feature.skip", defaultValue = "false")
	private boolean skip;

	@Parameter
	private Include include;

	@Parameter
	private Exclude exclude;

	@Parameter(property = "feature.outputDirectory", defaultValue = "${project.build.directory}")
	private File outputDirectory;

	@Parameter(property = "feature.outputFileName", defaultValue = "feature.json")
	private String outputFileName;

	@Parameter(defaultValue = "${project.basedir}")
	private File baseDir;

	@Parameter(property = "config.by.feature", defaultValue = "false")
	private boolean configByFeature;

	@Parameter(property = "config.by.bundle", defaultValue = "false")
	private boolean configByBundle;

	@Parameter(property = "config.by.pid", defaultValue = "false")
	private boolean configByPid;

	@Parameter(property = "bundle.export", defaultValue = "true")
	private boolean bundleExport;

	@Parameter(property = "bundle.hash.md5", defaultValue = "false")
	private boolean bundleHashMd5;
	@Parameter(property = "bundle.hash.sha1", defaultValue = "false")
	private boolean bundleHashSha1;
	@Parameter(property = "bundle.hash.sha256", defaultValue = "false")
	private boolean bundleHashSha256;
	@Parameter(property = "bundle.hash.sha512", defaultValue = "false")
	private boolean bundleHashSha512;

	@Component
	private ProjectDependenciesResolver resolver;

	@Component
	private MavenProjectHelper projectHelper;

	@Component
	private RepositorySystem repositorySystem;

	@Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
	private List<RemoteRepository> remoteRepositories;

	@Component
	@SuppressWarnings("deprecation")
	protected org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

	private FeatureServiceImpl featureService = new FeatureServiceImpl();

	private FeatureGenerator generator = new FeatureGeneratorImpl();

	public void execute() throws MojoExecutionException {
		getLog().info("Starting OSGi Feature generation for project: " + project.getArtifactId());

		if (skip) {
			getLog().info("Feature generation skipped by configuration.");
			return;
		}

		try {
			List<org.eclipse.osgi.technology.featurelauncher.feature.generator.FeatureBundleMetadata> featureBundles = new ArrayList<>();

			if (include != null) {
				getLog().info("Processing include section...");
				featureBundles.addAll(calcBundles(include));
			}

			if (exclude != null) {
				getLog().info("Processing exclude section...");
				featureBundles.removeAll(calcBundles(exclude));
			}
			String gav = project.getGroupId() + ":";
			gav = gav + project.getArtifactId() + ":";
			// type and classifier
			gav = gav + project.getVersion();

			Optional<String> name = Optional.ofNullable(project.getName());
			Optional<String> description = Optional.ofNullable(project.getDescription());
			Optional<String> docURL = Optional
			        .ofNullable(project.getIssueManagement() != null ? project.getIssueManagement().getUrl() : null);
			Optional<String> license = Optional.ofNullable(project.getLicenses().isEmpty() ? null
			        : project.getLicenses().stream().map(License::getName).collect(Collectors.joining(",")));
			Optional<String> scm = Optional.ofNullable(project.getScm() != null ? project.getScm().getUrl() : null);
			Optional<String> vendor = Optional
			        .ofNullable(project.getOrganization() != null ? project.getOrganization().getName() : null);
			List<String> categories = new ArrayList<>();

			boolean complete = false;

			Map<String, Object> variables = new HashMap<>();
			Properties properties = project.getProperties();
			for (Entry<Object, Object> e : properties.entrySet()) {
				String key = e.getKey().toString();

				if (key.startsWith(OSGI_FEATURE_CAT)) {

					String varValue = e.getValue().toString();
					categories.add(varValue);

				} else if (key.startsWith(OSGI_FEATURE_VAR)) {
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

					default -> throw new IllegalArgumentException();
					};

					variables.put(varKey, varValue);
				}
			}

			FeatureMetadata fmd = new FeatureMetadata(gav, name, description, docURL, license, scm, vendor, categories,
			        complete);

			FeatureBundleSetting fbs = new FeatureBundleSetting(bundleExport,
			        new HashSetting(bundleHashMd5, bundleHashSha1, bundleHashSha256, bundleHashSha512));

			ConfigSetting cs = new ConfigSetting(configByFeature, configByBundle, configByPid);

			Setting setting = new Setting(outputDirectory, outputFileName, fbs, cs);

			File file = generator.generate(setting, fmd, featureBundles, variables);

			getLog().info("Feature file written to: " + file.toPath().toAbsolutePath());

			projectHelper.attachArtifact(project, "json", file);
			getLog().info("Feature file attached to project artifacts.");

		} catch (Exception e) {
			throw new MojoExecutionException("Error generating feature: " + e.getMessage(), e);
		}
	}

	private List<FeatureBundleMetadata> calcBundles(Source source) throws IOException, MojoExecutionException {
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

//		resolvedBundles.addAll(source.bundles.getFiles(baseDir));
		resolvedBundles.addAll(filesFromBndRun(source.bndruns.getFiles(baseDir, "*.bndrun")));
//		resolvedBundles.addAll(filesFromFeature(source.features.getFiles(baseDir, "*.json")));

		return resolvedBundles;
	}

	private List<FeatureBundleMetadata> filesFromBndRun(List<File> bndrunFiles) {
		List<FeatureBundleMetadata> bundles = new ArrayList<>();

		getLog().info("Resolving bundles from Bndrun files...");
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

				if (true) {
					try {
						String runBundles = bndrun.resolve(false, false);
						if (bndrun.isOk()) {
							logger.info("{}: {}", aQute.bnd.osgi.Constants.RUNBUNDLES, runBundles);
							bndrun.setProperty(aQute.bnd.osgi.Constants.RUNBUNDLES, runBundles);
						}
					} catch (org.osgi.service.resolver.ResolutionException re) {
						logger.error(ResolveProcess.format(re, true));
						throw re;
					} finally {
						int errors = report(bndrun);
						if (errors > 0) {
							return errors;
						}
					}
				}
				for (Container cont : bndrun.getRunbundles()) {

					getLog().warn(cont.getAttributes().toString());
					getLog().warn(cont.toString());
					getLog().warn(cont.getType().toString());
					getLog().warn(cont.getBundleId().toString());
					getLog().warn(cont.getProject().toString());

					String groupId = cont.getAttributes().get("maven-groupId");
					String artifactId = cont.getAttributes().get("maven-artifactId");
					String version = cont.getAttributes().get("maven-version");
					getLog().warn(groupId);
					getLog().warn(groupId);
					getLog().warn(version);

					groupId = "gid";
					artifactId = "aid";
					version = "0.0.0";
					FeatureBundleMetadata fbm = new FeatureBundleMetadata(
					        new IDImpl(groupId, artifactId, version, Optional.empty(), Optional.empty()),
					        cont.getFile(), new HashMap<>());
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

	private List<File> filesFromFeature(List<File> featureFiles) throws IOException {
		List<File> resolved = new ArrayList<>();

		for (File file : featureFiles) {
			getLog().info("Processing feature JSON file: " + file);

			String jsonContent = Files.readString(file.toPath());
			Feature feature = featureService.readFeature(new StringReader(jsonContent));

			for (FeatureBundle bundle : feature.getBundles()) {
				ID bundleId = bundle.getID();
				try {
					File resolvedFile = resolveArtifact(bundleId);
					resolved.add(resolvedFile);
				} catch (Exception e) {
					getLog().error("Could not resolve bundle: " + bundleId, e);
				}
			}
		}
		return resolved;
	}

	private File resolveArtifact(ID id) throws Exception {
		getLog().info("Resolving Maven artifact: " + id);

		org.eclipse.aether.artifact.DefaultArtifact artifact = new org.eclipse.aether.artifact.DefaultArtifact(
		        id.toString());
		ArtifactRequest request = new ArtifactRequest(artifact, remoteRepositories, null);

		ArtifactResult result = repositorySystem.resolveArtifact(repositorySession, request);
		File artifactFile = result.getArtifact().getFile();

		getLog().info("Resolved artifact file: " + artifactFile);
		return artifactFile;
	}
}