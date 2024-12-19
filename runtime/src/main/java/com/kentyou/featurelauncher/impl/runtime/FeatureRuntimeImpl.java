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
package com.kentyou.featurelauncher.impl.runtime;

import static org.osgi.service.feature.FeatureExtension.Kind.MANDATORY;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.BUNDLE_START_LEVEL_METADATA;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.cm.Configuration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory;
import org.osgi.service.featurelauncher.runtime.FeatureRuntime;
import org.osgi.service.featurelauncher.runtime.FeatureRuntimeConstants;
import org.osgi.service.featurelauncher.runtime.FeatureRuntimeException;
import org.osgi.service.featurelauncher.runtime.InstalledBundle;
import org.osgi.service.featurelauncher.runtime.InstalledConfiguration;
import org.osgi.service.featurelauncher.runtime.InstalledFeature;
import org.osgi.service.featurelauncher.runtime.MergeOperationType;
import org.osgi.service.featurelauncher.runtime.RuntimeBundleMerge;
import org.osgi.service.featurelauncher.runtime.RuntimeBundleMerge.BundleMapping;
import org.osgi.service.featurelauncher.runtime.RuntimeBundleMerge.FeatureBundleDefinition;
import org.osgi.service.featurelauncher.runtime.RuntimeConfigurationMerge;
import org.osgi.service.featurelauncher.runtime.RuntimeConfigurationMerge.FeatureConfigurationDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kentyou.featurelauncher.common.decorator.impl.DecorationContext;
import com.kentyou.featurelauncher.common.repository.impl.WrappingArtifactRepository;
import com.kentyou.featurelauncher.repository.spi.FileSystemArtifactRepository;
import com.kentyou.featurelauncher.repository.spi.NamedArtifactRepository;


/**
 * 160.5 The Feature Runtime Service
 * 
 * Some parts based on {@link org.eclipse.sensinact.gateway.launcher.FeatureLauncher}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
@Component(configurationPid = "kentyou.featurelauncher.runtime")
public class FeatureRuntimeImpl implements FeatureRuntime {
	private static final Logger LOG = LoggerFactory.getLogger(FeatureRuntimeImpl.class);

	private final FeatureRuntimeConfigurationManager featureRuntimeConfigurationManager;
	
	private final ArtifactRepositoryFactory artifactRepositoryFactory;

	private FeatureService featureService;

	private BundleContext bundleContext;

	private final Map<String, ArtifactRepository> defaultArtifactRepositories;

	// Bundles installed by this feature runtime
	private final Map<ID, Bundle> installedBundlesByIdentifier;

	// Lists of bundles for each feature installed
	private final Map<ID, List<ID>> installedFeaturesToBundles;

	// List of configurations for each feature installed
	private final Map<ID, Collection<String>> installedFeaturesToConfigurations;

	// List of installed features
	private final List<InstalledFeature> installedFeatures;

	// Bundles already present in running framework
	private final Map<Map.Entry<String, String>, Long> existingBundles;

	// Allows faster lookup of bundle symbolic name and version
	private final Map<ID, Map.Entry<String, String>> bundleIdsToSymbolicNamesVersions;

	// ID of the virtual external feature representing ownership of a bundle or
	// configuration that was deployed by another management agent
	private ID externalFeatureId;

	
	public static @interface Config {
		
		public String[] local_repositories() default { "~/.m2/repository" };
		
		public String[] remote_repositories() default {"https://repo1.maven.org/maven2/,localRepositoryPath=~/.m2/repository"};
		
		public boolean local_repositories_enabled() default false;
		
		public boolean remote_repositories_enabled() default false;
	}
	
	@Activate
	public FeatureRuntimeImpl(BundleContext context, Config config,
			@Reference ArtifactRepositoryFactory arf, @Reference FeatureRuntimeConfigurationManager frcm) {
		this.bundleContext = context;
		this.artifactRepositoryFactory = arf;
		this.featureRuntimeConfigurationManager = frcm;

		try {
			Map<String, NamedArtifactRepository> repos = new HashMap<String, NamedArtifactRepository>();
			String userHome = System.getProperty("user.home");
			if(config.local_repositories_enabled()) {
				for(String localRepo : config.local_repositories()) {
					if(localRepo.startsWith("~/")) {
						localRepo = userHome + localRepo.substring(1);
					}
					ArtifactRepository ar = artifactRepositoryFactory.createRepository(Paths.get(localRepo));
					if(ar instanceof NamedArtifactRepository nar) {
						repos.put(nar.getName(), nar);
					} else {
						WrappingArtifactRepository war = new WrappingArtifactRepository(ar, userHome);
						repos.put(userHome, war);
					}
				}
			}
			if(config.remote_repositories_enabled()) {
				for(String remoteRepo : config.remote_repositories()) {
					String[] remote = remoteRepo.split(",");
					URI uri = URI.create(remote[0]);
					Map<String, Object> props = new HashMap<>();
					for(int i = 1; i < remote.length; i++) {
						String[] tokens = remote[i].split("=", 2);
						if(tokens.length == 2) {
							props.put(tokens[0], tokens[1]);
						}
					}
					
					ArtifactRepository ar = artifactRepositoryFactory.createRepository(uri, props);
					if(ar instanceof NamedArtifactRepository nar) {
						repos.put(nar.getName(), nar);
					} else {
						WrappingArtifactRepository war = new WrappingArtifactRepository(ar, userHome);
						repos.put(userHome, war);
					}
				}
			}
			
			this.defaultArtifactRepositories = Map.copyOf(repos);
			
			// collect symbolic names of bundles already present in running framework
			this.existingBundles = getExistingBundles();
		} catch (Exception e) {
			throw new FeatureRuntimeException("Could not create default artifact repositories!");
		}

		this.installedBundlesByIdentifier = new HashMap<>();
		this.installedFeaturesToBundles = new HashMap<>();
		this.installedFeaturesToConfigurations = new HashMap<>();
		this.installedFeatures = new ArrayList<>();
		this.bundleIdsToSymbolicNamesVersions = new HashMap<>();

		LOG.info("Started FeatureRuntime!");
	}

	@Reference
	private void setFeatureService(FeatureService featureService) {
		this.featureService = featureService;
		setExternalFeatureId();
	}

	@Override
	public ArtifactRepository createRepository(Path path) {
		return artifactRepositoryFactory.createRepository(path);
	}

	@Override
	public ArtifactRepository createRepository(URI uri, Map<String, Object> props) {
		return artifactRepositoryFactory.createRepository(uri, props);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#getDefaultRepositories()
	 */
	@Override
	public Map<String, ArtifactRepository> getDefaultRepositories() {
		return defaultArtifactRepositories;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#install(org.osgi.service.feature.Feature)
	 */
	@Override
	public InstallOperationBuilder install(Feature feature) {
		Objects.requireNonNull(feature, "Feature cannot be null!");

		return new InstallOperationBuilderImpl(feature);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#install(java.io.Reader)
	 */
	@Override
	public InstallOperationBuilder install(Reader jsonReader) {
		Objects.requireNonNull(jsonReader, "Feature JSON cannot be null!");

		try {
			Feature feature = featureService.readFeature(jsonReader);

			return install(feature);

		} catch (IOException e) {
			throw new FeatureRuntimeException("Error reading feature!", e);
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#getInstalledFeatures()
	 */
	@Override
	public List<InstalledFeature> getInstalledFeatures() {
		return installedFeatures;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#remove(org.osgi.service.feature.ID)
	 */
	@Override
	public void remove(ID featureId) {
		Objects.requireNonNull(featureId, "Feature ID cannot be null!");

		InstalledFeature installedFeature = getInstalledFeatureById(featureId);

		Objects.requireNonNull(installedFeature,
				String.format("No feature matching %s ID could be found!", featureId.toString()));

		new RemoveOperationBuilderImpl(installedFeature.getFeature()).remove();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#update(org.osgi.service.feature.ID, org.osgi.service.feature.Feature)
	 */
	@Override
	public UpdateOperationBuilder update(ID featureId, Feature feature) {
		Objects.requireNonNull(featureId, "Feature ID cannot be null!");
		Objects.requireNonNull(feature, "Feature cannot be null!");

		return new UpdateOperationBuilderImpl(feature);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime#update(org.osgi.service.feature.ID, java.io.Reader)
	 */
	@Override
	public UpdateOperationBuilder update(ID featureId, Reader jsonReader) {
		Objects.requireNonNull(featureId, "Feature ID cannot be null!");
		Objects.requireNonNull(jsonReader, "Feature JSON cannot be null!");

		try {
			Feature feature = featureService.readFeature(jsonReader);

			return update(featureId, feature);

		} catch (IOException e) {
			throw new FeatureRuntimeException("Error reading feature!", e);
		}
	}

	abstract class AbstractOperationBuilderImpl<T extends OperationBuilder<T>> implements OperationBuilder<T> {
		protected DecorationContext decorationUtil;
		protected Feature feature;
		protected boolean isCompleted;
		protected boolean useDefaultRepositories;
		protected Map<String, ArtifactRepository> artifactRepositories;
		protected RuntimeBundleMerge runtimeBundleMerge;
		protected RuntimeConfigurationMerge runtimeConfigurationMerge;
		protected Map<String, Object> variables;
		protected List<FeatureDecorator> decorators;
		protected Map<String, FeatureExtensionHandler> extensionHandlers;

		public AbstractOperationBuilderImpl(Feature feature) {
			Objects.requireNonNull(feature, "Feature cannot be null!");

			this.feature = feature;
			this.isCompleted = false;
			this.useDefaultRepositories = true;
			this.artifactRepositories = new HashMap<>();
			this.variables = new HashMap<>();
			this.decorators = new ArrayList<>();
			this.extensionHandlers = new HashMap<>();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#addRepository(java.lang.String, org.osgi.service.featurelauncher.repository.ArtifactRepository)
		 */
		@Override
		public T addRepository(String name, ArtifactRepository repository) {
			Objects.requireNonNull(name, "Artifact Repository name cannot be null!");
			Objects.requireNonNull(repository, "Artifact Repository cannot be null!");

			ensureNotCompletedYet();

			this.artifactRepositories.put(name, repository);

			return castThis();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#useDefaultRepositories(boolean)
		 */
		@Override
		public T useDefaultRepositories(boolean include) {
			ensureNotCompletedYet();

			this.useDefaultRepositories = include;

			return castThis();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#withBundleMerge(org.osgi.service.featurelauncher.runtime.RuntimeBundleMerge)
		 */
		@Override
		public T withBundleMerge(RuntimeBundleMerge merge) {
			Objects.requireNonNull(merge, "Runtime bundle merge cannot be null!");

			ensureNotCompletedYet();

			this.runtimeBundleMerge = merge;

			return castThis();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#withConfigurationMerge(org.osgi.service.featurelauncher.runtime.RuntimeConfigurationMerge)
		 */
		@Override
		public T withConfigurationMerge(RuntimeConfigurationMerge merge) {
			Objects.requireNonNull(merge, "Runtime configuration merge cannot be null!");

			ensureNotCompletedYet();

			this.runtimeConfigurationMerge = merge;

			return castThis();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#withVariables(java.util.Map)
		 */
		@Override
		public T withVariables(Map<String, Object> variables) {
			Objects.requireNonNull(variables, "Variables cannot be null!");

			ensureNotCompletedYet();

			this.variables = variables;

			return castThis();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#withDecorator(org.osgi.service.featurelauncher.decorator.FeatureDecorator)
		 */
		@Override
		public T withDecorator(FeatureDecorator decorator) {
			Objects.requireNonNull(decorator, "Feature Decorator cannot be null!");

			ensureNotCompletedYet();

			this.decorators.add(decorator);

			return castThis();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#withExtensionHandler(java.lang.String, org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler)
		 */
		@Override
		public T withExtensionHandler(String extensionName, FeatureExtensionHandler extensionHandler) {
			Objects.requireNonNull(extensionName, "Feature extension name cannot be null!");
			Objects.requireNonNull(extensionHandler, "Feature extension handler cannot be null!");

			ensureNotCompletedYet();

			this.extensionHandlers.put(extensionName, extensionHandler);

			return castThis();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.OperationBuilder#complete()
		 */
		@Override
		public InstalledFeature complete() throws FeatureRuntimeException {
			this.isCompleted = true;

			if (this.useDefaultRepositories) {
				getDefaultRepositories().forEach((k, v) -> this.artifactRepositories.putIfAbsent(k, v));
			}

			decorationUtil = new DecorationContext(List.copyOf(this.artifactRepositories.values()));

			return addOrUpdateFeature(feature);
		}

		protected InstalledFeature addOrUpdateFeature(Feature feature) {
			ID featureId = feature.getID();

			validateFeatureExtensions(feature);

			// @formatter:off
	        List<ID> featureBundlesIDs = feature.getBundles().stream()
	        		.map(featureBundle -> featureBundle.getID())
	        		.collect(Collectors.toList());
	        // @formatter:on

			// Check if feature is already installed or out of date
			if (installedFeaturesToBundles.containsKey(featureId)) {
				LOG.info(String.format("Updating feature %s", featureId));

				if (installedFeaturesToBundles.get(featureId).equals(featureBundlesIDs)) {
					// No work to do, already installed
					LOG.info(String.format("The feature %s is already up to date", featureId));

					return getInstalledFeatureById(featureId);
				} else {
					// Feature is out of date - remove and re-install
					LOG.info(String.format("The feature %s is out of date and will be removed and re-installed",
							featureId));

					removeFeature(featureId);
				}
			}

			// Feature Decoration
			Feature originalFeature = feature;
			try {
				feature = decorationUtil.executeFeatureDecorators(featureService, feature, decorators);

				feature = decorationUtil.executeFeatureExtensionHandlers(featureService, feature, extensionHandlers);
			} catch (AbandonOperationException e) {
				throw new FeatureRuntimeException("Feature decoration handling failed!", e);
			}

			// Install bundles
			List<InstalledBundle> installedBundles = installBundles(feature, featureBundlesIDs);

			// Install configurations
			List<InstalledConfiguration> installedConfigurations = installConfigurations(feature);

			// Start bundles
			startBundles(featureId, installedBundles);

			// construct installed feature
			InstalledFeature installedFeature = constructInstalledFeature(feature, originalFeature,
					feature != originalFeature, false, installedBundles, installedConfigurations);

			// update "owning features" in other 'installedFeatures'
			updateInstalledFeaturesOnAddOrUpdate(installedFeature);

			installedFeatures.add(installedFeature);

			return installedFeature;
		}

		// TODO: clarify with Tim understanding / how this is currently implemented and
		// integrate this then
		protected Stream<BundleMapping> maybeRunBundleMerge(MergeOperationType operation, Feature feature) {
			if (runtimeBundleMerge != null) {

				for (FeatureBundle featureBundle : feature.getBundles()) {
					ID featureBundleId = featureBundle.getID();

					List<InstalledBundle> conflictingInstalledBundles = new ArrayList<>();

					List<FeatureBundleDefinition> conflictingFeatureBundles = new ArrayList<>();

					for (InstalledFeature existingFeature : installedFeatures) {
						for (InstalledBundle existingInstalledBundle : existingFeature.getInstalledBundles()) {

							boolean isInConflict = ((featureBundleId.getGroupId())
									.equals(existingInstalledBundle.getBundleId().getGroupId())
									&& (featureBundleId.getArtifactId())
											.equals(existingInstalledBundle.getBundleId().getArtifactId())
									&& !featureBundleId.equals(existingInstalledBundle.getBundleId()));

							if (isInConflict) {
								conflictingInstalledBundles.add(existingInstalledBundle);

								FeatureBundleDefinition conflictingFeatureBundle = new FeatureBundleDefinition() {

									@Override
									public FeatureBundle getFeatureBundle() {
										// @formatter:off
										return getFeature().getBundles().stream()
												.filter(fb -> featureBundleId.equals(fb.getID()))
												.findFirst()
												.orElseThrow();
										// @formatter:on
									}

									@Override
									public Feature getFeature() {
										return existingFeature.isDecorated() ? existingFeature.getOriginalFeature()
												: existingFeature.getFeature();
									}
								};

								conflictingFeatureBundles.add(conflictingFeatureBundle);
							}
						}
					}

					if (!conflictingInstalledBundles.isEmpty()) {
						// @formatter:off
						return runtimeBundleMerge.mergeBundle(
								operation, 
								feature,
								featureBundle,
								conflictingInstalledBundles,
								conflictingFeatureBundles);
						// @formatter:on
					}
				}
			}

			return Stream.empty();
		}

		// TODO: clarify with Tim understanding / how this is currently implemented and
		// integrate this then
		protected Map<String, Object> maybeRunConfigurationMerge(MergeOperationType operation, Feature feature) {
			if (runtimeConfigurationMerge != null) {

				for (Map.Entry<String, FeatureConfiguration> featureConfigurationEntry : feature.getConfigurations()
						.entrySet()) {
					String featureConfigurationPID = featureConfigurationEntry.getKey();
					FeatureConfiguration featureConfiguration = featureConfigurationEntry.getValue();

					InstalledConfiguration conflictingInstalledConfiguration = null;

					List<FeatureConfigurationDefinition> conflictingFeatureConfigurations = new ArrayList<>();

					INSTALLED_FEATURES: for (InstalledFeature existingFeature : installedFeatures) {
						for (InstalledConfiguration existingInstalledConfiguration : existingFeature
								.getInstalledConfigurations()) {

							boolean isInConflict = featureConfigurationPID
									.equals(existingInstalledConfiguration.getPid());

							if (isInConflict) {
								conflictingInstalledConfiguration = existingInstalledConfiguration;

								FeatureConfigurationDefinition conflictingFeatureConfiguration = new FeatureConfigurationDefinition() {

									@Override
									public FeatureConfiguration getFeatureConfiguration() {
										// @formatter:off
										return getFeature().getConfigurations().values().stream()
												.filter(fc -> featureConfigurationPID.equals(fc.getPid()))
												.findFirst()
												.orElseThrow();
										// @formatter:on
									}

									@Override
									public Feature getFeature() {
										return existingFeature.isDecorated() ? existingFeature.getOriginalFeature()
												: existingFeature.getFeature();
									}
								};

								conflictingFeatureConfigurations.add(conflictingFeatureConfiguration);

								break INSTALLED_FEATURES;
							}
						}
					}

					if (conflictingInstalledConfiguration != null) {

						// @formatter:off
						return runtimeConfigurationMerge.mergeConfiguration(
								operation,
								feature,
								featureConfiguration,
								conflictingInstalledConfiguration,
								conflictingFeatureConfigurations);
						// @formatter:on
					}
				}
			}

			return Collections.emptyMap();
		}

		protected void removeFeature(ID featureId) {
			// remove only those bundles which are not referenced by other features
			Deque<ID> bundleIDsForRemoval = getBundleIDsForRemoval(featureId);

			stopBundles(bundleIDsForRemoval);

			uninstallBundles(bundleIDsForRemoval);

			// remove only those configurations which are not referenced by other features
			Set<String> configurationPIDsForRemoval = getConfigurationPIDsForRemoval(featureId);

			removeFeatureConfigurations(configurationPIDsForRemoval);

			// remove feature from list of installed features
			installedFeatures.removeIf(f -> featureId.equals(f.getFeature().getID()));

			// update "owning features" in other installed features
			updateInstalledFeaturesOnRemove(featureId);
		}

		protected List<InstalledBundle> installBundles(Feature feature, List<ID> featureBundles) {
			List<InstalledBundle> installedBundles = new ArrayList<>();
			for (FeatureBundle featureBundle : feature.getBundles()) {
				ID bundleId = featureBundle.getID();

				boolean bundleAlreadyInstalledByRuntime = installedBundlesByIdentifier.containsKey(bundleId);

				if (!bundleAlreadyInstalledByRuntime) {

					Bundle bundle = null;

					try {
						bundle = installBundle(bundleId);

						if (bundle != null) {
							installedBundlesByIdentifier.put(bundleId, bundle);

							maybeSetBundleStartLevel(bundle, featureBundle.getMetadata());

							installedBundles.add(constructInstalledBundle(bundleId, bundle,
									constructOwningFeatures(feature.getID())));
						}

					} catch (BundleException e) {
						if (BundleException.DUPLICATE_BUNDLE_ERROR == e.getType()
								|| (BundleException.REJECTED_BY_HOOK == e.getType())) {
							LOG.info(String.format("Bundle %s duplicates bundle already present in running framework!",
									bundleId));

							ID aliasBundleId = getAliasBundleId(bundleId);

							installedBundles.add(constructExternallyInstalledBundle(feature.getID(), bundleId,
									(aliasBundleId != null) ? List.of(aliasBundleId) : Collections.emptyList()));

						} else {
							throw new FeatureRuntimeException(String.format("Could not install bundle '%s'!", bundleId),
									e);
						}

					} catch (IOException e) {
						throw new FeatureRuntimeException(String.format("Could not install bundle '%s'!", bundleId), e);
					}

				} else {
					LOG.info(String.format("Bundle %s duplicates bundle already installed by feature runtime!",
							bundleId));

					installedBundles.add(constructAlreadyInstalledBundle(feature.getID(), bundleId));
				}
			}

			installedFeaturesToBundles.put(feature.getID(), featureBundles);

			return installedBundles;
		}

		protected ID getAliasBundleId(ID bundleId) {
			final Map.Entry<String, String> bundleSymbolicNameAndVersion = getBundleSymbolicNameAndVersion(bundleId);
			if (bundleSymbolicNameAndVersion != null) {
				// @formatter:off
				return bundleIdsToSymbolicNamesVersions.entrySet().stream()
						.filter(entry -> ((bundleSymbolicNameAndVersion.getKey())
								.equals(entry.getValue().getKey())
								&& (bundleSymbolicNameAndVersion.getValue())
										.equals(entry.getValue().getValue())))
						.map(entry -> entry.getKey())
						.findFirst()
						.orElse(null);
				// @formatter:on
			}
			return null;
		}

		protected Bundle installBundle(ID featureBundleID) throws IOException, BundleException {
			try (InputStream featureBundleIs = getArtifact(featureBundleID)) {
				if (featureBundleIs.available() != 0) {
					Bundle installedBundle = bundleContext.installBundle(featureBundleID.toString(), featureBundleIs);

					LOG.info(String.format("Installed bundle '%s'", installedBundle.getSymbolicName()));

					return installedBundle;
				}
			}

			return null;
		}

		protected List<InstalledConfiguration> installConfigurations(Feature feature) {
			List<InstalledConfiguration> installedConfigurations = new ArrayList<>();

			Map<String, Configuration> allExistingConfigurations;

			try {
				allExistingConfigurations = featureRuntimeConfigurationManager.getAllConfigurations();
			} catch (IOException | InvalidSyntaxException e) {
				throw new FeatureRuntimeException("Error retrieving existing configurations!", e);
			}

			for (Map.Entry<String, FeatureConfiguration> featureConfigurationEntry : feature.getConfigurations()
					.entrySet()) {
				String configurationPid = featureConfigurationEntry.getKey();
				FeatureConfiguration featureConfiguration = featureConfigurationEntry.getValue();

				boolean configurationAlreadyInstalledByRuntime = isConfigurationInstalledByRuntime(configurationPid);

				if (!allExistingConfigurations.containsKey(configurationPid)) {

					featureRuntimeConfigurationManager.createConfiguration(featureConfiguration,
							mergeVariables(feature));

					installedConfigurations.add(constructInstalledConfiguration(featureConfiguration,
							constructOwningFeatures(feature.getID())));

					LOG.info(String.format("Installed configuration %s", configurationPid));

				} else {

					if (configurationAlreadyInstalledByRuntime) {
						LOG.info(String.format(
								"Configuration %s duplicates configuration already installed by feature runtime!",
								configurationPid));

						installedConfigurations.add(constructAlreadyInstalledConfiguration(feature.getID(),
								configurationPid, featureConfiguration));
					} else {
						LOG.info(String.format(
								"Configuration %s duplicates configuration already present in running framework!",
								configurationPid));

						installedConfigurations
								.add(constructExternallyInstalledConfiguration(feature.getID(), featureConfiguration));
					}
				}
			}

			List<String> featureConfigurationsPIDs = feature.getConfigurations().keySet().stream()
					.collect(Collectors.toList());

			installedFeaturesToConfigurations.put(feature.getID(), featureConfigurationsPIDs);

			return installedConfigurations;
		}

		protected void startBundles(ID featureId, List<InstalledBundle> installedBundles) {
			for (InstalledBundle installedBundle : installedBundles) {
				try {
					if (installedBundle.getBundle().getState() == Bundle.INSTALLED) {
						BundleRevision rev = installedBundle.getBundle().adapt(BundleRevision.class);
						if (rev != null && (rev.getTypes() & BundleRevision.TYPE_FRAGMENT) == 0) {
							// Start all but fragment bundles
							installedBundle.getBundle().start();
						} else {
							LOG.info(String.format("Not starting bundle %s as it is a fragment",
									installedBundle.getBundle().getSymbolicName()));
						}
					}
				} catch (Exception e) {
					LOG.warn(String.format("An error occurred starting a bundle in feature %s", featureId));
				}
			}
		}

		protected void stopBundles(Deque<ID> bundleIDsToStop) {
			for (ID bundleIDToStop : bundleIDsToStop) {
				Bundle bundleForRemoval = installedBundlesByIdentifier.get(bundleIDToStop);
				if (bundleForRemoval != null) {
					try {
						BundleRevision rev = bundleForRemoval.adapt(BundleRevision.class);
						if (rev != null && (rev.getTypes() & BundleRevision.TYPE_FRAGMENT) == 0) {
							bundleForRemoval.stop();
						}
					} catch (BundleException e) {
						LOG.warn(String.format("An error occurred stopping bundle %s", bundleIDToStop), e);
					}
				}
			}
		}

		protected void uninstallBundles(Deque<ID> bundleIDsToUninstall) {
			for (ID bundleIDToRemove : bundleIDsToUninstall) {
				Bundle bundleForRemoval = installedBundlesByIdentifier.remove(bundleIDToRemove);
				if (bundleForRemoval != null) {
					try {
						bundleForRemoval.uninstall();
					} catch (BundleException e) {
						LOG.warn(String.format("An error occurred uninstalling bundle %s", bundleIDToRemove), e);
					}
				}
			}
		}

		protected Deque<ID> getBundleIDsForRemoval(ID featureId) {
			// Get all the bundles to remove in "install order", clearing the features map
			Set<ID> bundlesToRemove = installedFeaturesToBundles.remove(featureId).stream()
					.collect(Collectors.toCollection(LinkedHashSet::new));

			// Create a deque of bundles to remove, in the order they should be removed
			Deque<ID> orderedBundleIDsForRemoval = new LinkedList<>();
			for (ID bundleToRemove : bundlesToRemove) {
				// Only remove the bundle if no remaining features reference it
				if (installedFeaturesToBundles.values().stream().noneMatch(c -> c.contains(bundleToRemove))) {
					// Add to the start of the deque, so that we reverse the install order
					orderedBundleIDsForRemoval.addFirst(bundleToRemove);

					LOG.info(String.format("Bundle %s is no longer required and will be removed", bundleToRemove));
				}
			}

			return orderedBundleIDsForRemoval;
		}

		protected void removeFeatureConfigurations(Set<String> configurationPIDsForRemoval) {
			featureRuntimeConfigurationManager.removeConfigurations(configurationPIDsForRemoval);
		}

		protected Set<String> getConfigurationPIDsForRemoval(ID featureId) {
			Set<String> configurationPIDsForRemoval = new HashSet<>();

			if (installedFeaturesToConfigurations.containsKey(featureId)) {
				Set<String> featureConfigurationPIDsToRemove = installedFeaturesToConfigurations.remove(featureId)
						.stream().collect(Collectors.toSet());

				for (String featureConfigurationPIDToRemove : featureConfigurationPIDsToRemove) {
					if (installedFeaturesToConfigurations.values().stream()
							.noneMatch(c -> c.contains(featureConfigurationPIDToRemove))) {
						configurationPIDsForRemoval.add(featureConfigurationPIDToRemove);

						LOG.info(String.format("Configuration %s will be removed", featureConfigurationPIDToRemove));
					}
				}
			}

			return configurationPIDsForRemoval;
		}

		protected InstalledFeature constructInstalledFeature(Feature feature, Feature originalFeature,
				boolean isDecorated, boolean isInitialLaunch, List<InstalledBundle> installedBundles,
				List<InstalledConfiguration> installedConfigurations) {
			// @formatter:off
			return new InstalledFeatureImpl(
					feature, 
					originalFeature, 
					isDecorated, 
					isInitialLaunch, 
					installedBundles,
					installedConfigurations);
			// @formatter:on
		}

		protected int getBundleStartLevel(Bundle bundle) {
			return bundle.adapt(BundleStartLevel.class).getStartLevel();
		}

		protected void maybeSetBundleStartLevel(Bundle bundle, Map<String, Object> metadata) {
			if (metadata.containsKey(BUNDLE_START_LEVEL_METADATA)) {
				int startlevel = Integer.valueOf(metadata.get(BUNDLE_START_LEVEL_METADATA).toString()).intValue();

				bundle.adapt(BundleStartLevel.class).setStartLevel(startlevel);
			}
		}

		protected InstalledBundle constructInstalledBundle(ID bundleId, Bundle bundle, List<ID> owningFeatures) {
			return constructInstalledBundle(bundleId, Collections.emptyList(), bundle, owningFeatures);
		}

		protected InstalledBundle constructInstalledBundle(ID bundleId, List<ID> aliases, Bundle bundle,
				List<ID> owningFeatures) {
			int startLevel = getBundleStartLevel(bundle);

			return new InstalledBundleImpl(bundleId, aliases, bundle, startLevel, owningFeatures);
		}

		protected InstalledBundle constructAlreadyInstalledBundle(ID featureId, ID bundleId) {
			return constructInstalledBundle(bundleId, Collections.emptyList(),
					installedBundlesByIdentifier.get(bundleId), constructBundleOwningFeatures(featureId, bundleId));
		}

		protected InstalledBundle constructExternallyInstalledBundle(ID featureId, ID bundleId, List<ID> aliases) {
			Bundle bundle = null;

			Map.Entry<String, String> bundleSymbolicNameAndVersion = getBundleSymbolicNameAndVersion(bundleId);
			if ((bundleSymbolicNameAndVersion != null) && (existingBundles.containsKey(bundleSymbolicNameAndVersion))) {
				bundle = bundleContext.getBundle(existingBundles.get(bundleSymbolicNameAndVersion).longValue());
			}

			return constructInstalledBundle(bundleId, aliases, bundle,
					constructOwningFeatures(featureId, externalFeatureId));
		}

		protected List<ID> constructOwningFeatures(ID... featureIds) {
			List<ID> owningFeatures = new ArrayList<>();
			owningFeatures.addAll(List.of(featureIds));
			return owningFeatures;
		}

		protected List<ID> constructBundleOwningFeatures(ID featureId, ID bundleId) {
			List<ID> owningFeatures = new ArrayList<>();
			owningFeatures.add(featureId);
			owningFeatures.addAll(getBundleOwningFeatures(bundleId));
			return owningFeatures;
		}

		protected List<ID> getBundleOwningFeatures(ID bundleId) {
			// @formatter:off
			return installedFeaturesToBundles.entrySet().stream()
					.filter(e -> e.getValue().contains(bundleId))
					.map(e -> e.getKey())
					.toList();
			// @formatter:on
		}

		protected InstalledConfiguration constructInstalledConfiguration(FeatureConfiguration featureConfiguration,
				List<ID> owningFeatures) {
			return new InstalledConfigurationImpl(featureConfiguration.getPid(), featureConfiguration.getFactoryPid(),
					featureConfiguration.getValues(), owningFeatures);
		}

		protected InstalledConfiguration constructAlreadyInstalledConfiguration(ID featureId, String configurationPid,
				FeatureConfiguration featureConfiguration) {
			return constructInstalledConfiguration(featureConfiguration,
					constructConfigurationOwningFeatures(featureId, configurationPid));
		}

		protected InstalledConfiguration constructExternallyInstalledConfiguration(ID featureId,
				FeatureConfiguration featureConfiguration) {
			return constructInstalledConfiguration(featureConfiguration,
					constructOwningFeatures(featureId, externalFeatureId));
		}

		protected List<ID> constructConfigurationOwningFeatures(ID featureId, String configurationPid) {
			List<ID> owningFeatures = new ArrayList<>();
			owningFeatures.add(featureId);
			owningFeatures.addAll(getConfigurationOwningFeatures(configurationPid));
			return owningFeatures;
		}

		protected List<ID> getConfigurationOwningFeatures(String configurationPid) {
			// @formatter:off
			return installedFeaturesToConfigurations.entrySet().stream()
					.filter(e -> e.getValue().contains(configurationPid))
					.map(e -> e.getKey())
					.toList();
			// @formatter:on
		}

		protected Map.Entry<String, String> getBundleSymbolicNameAndVersion(ID featureBundleID) {
			if (bundleIdsToSymbolicNamesVersions.containsKey(featureBundleID)) {
				return bundleIdsToSymbolicNamesVersions.get(featureBundleID);
			} else {
				Path featureBundlePath = getArtifactPath(featureBundleID);
				if (featureBundlePath != null) {
					try (JarFile featureBundleJarFile = new JarFile(featureBundlePath.toFile())) {
						Manifest featureBundleJarMf = featureBundleJarFile.getManifest();
						if ((featureBundleJarMf != null) && (featureBundleJarMf.getMainAttributes() != null)) {
							String featureBundleSymbolicName = featureBundleJarMf.getMainAttributes()
									.getValue("Bundle-SymbolicName");
							String featureBundleVersion = featureBundleJarMf.getMainAttributes()
									.getValue("Bundle-Version");

							if ((featureBundleSymbolicName != null) && (featureBundleVersion != null)) {
								Map.Entry<String, String> bundleSymbolicNameAndVersion = Map
										.entry(featureBundleSymbolicName, featureBundleVersion);

								bundleIdsToSymbolicNamesVersions.put(featureBundleID, bundleSymbolicNameAndVersion);

								return bundleSymbolicNameAndVersion;
							}
						}
					} catch (IOException e) {
						LOG.error(
								String.format("Error getting symbolic name and version for bundle %s", featureBundleID),
								e);
					}
				}
			}

			return null;
		}

		protected boolean isConfigurationInstalledByRuntime(String configurationPid) {
			// @formatter:off
			return installedFeaturesToConfigurations.values().stream()
					.flatMap(pids -> pids.stream())
					.anyMatch(pid -> configurationPid.equals(pid));
			// @formatter:on
		}

		protected Path getArtifactPath(ID featureBundleID) {
			for (ArtifactRepository artifactRepository : artifactRepositories.values()) {
				if (FileSystemArtifactRepository.class.isInstance(artifactRepository)) {
					Path featureBundlePath = ((FileSystemArtifactRepository) artifactRepository)
							.getArtifactPath(featureBundleID);
					if (featureBundlePath != null) {
						return featureBundlePath;
					}
				}
			}

			return null;
		}

		protected InputStream getArtifact(ID featureBundleID) {
			for (ArtifactRepository artifactRepository : artifactRepositories.values()) {
				InputStream featureBundleIs = artifactRepository.getArtifact(featureBundleID);
				if (featureBundleIs != null) {
					return featureBundleIs;
				}
			}

			return InputStream.nullInputStream();
		}

		protected void validateFeatureExtensions(Feature feature) {
			List<String> unknownMandatoryFeatureExtensions = feature.getExtensions().entrySet().stream()
					.filter(e -> e.getValue().getKind() == MANDATORY).map(Map.Entry::getKey)
					.collect(Collectors.toList());
			if (!unknownMandatoryFeatureExtensions.isEmpty()) {
				throw new FeatureRuntimeException(
						String.format("The feature %d has mandatory extensions for which are not understood",
								unknownMandatoryFeatureExtensions.size()));
			}
		}

		protected Map<String, Object> mergeVariables(Feature feature) {
			Map<String, Object> allVariables = new HashMap<>(feature.getVariables());

			if (!variables.isEmpty()) {
				allVariables.putAll(variables);
			}

			ensureVariablesNotNull(allVariables);

			return allVariables;
		}

		private void ensureVariablesNotNull(Map<String, Object> allVariables) {
			for (Map.Entry<String, Object> variable : allVariables.entrySet()) {
				if (variable.getValue() == null) {
					throw new FeatureRuntimeException(
							String.format("No value provided for variable %s!", variable.getKey()));
				}
			}
		}

		protected void ensureNotCompletedYet() {
			if (this.isCompleted == true) {
				throw new IllegalStateException("Operation already completed!");
			}
		}

		@SuppressWarnings("unchecked")
		protected T castThis() {
			return (T) this;
		}
	}

	public class InstallOperationBuilderImpl extends AbstractOperationBuilderImpl<InstallOperationBuilder>
			implements InstallOperationBuilder {

		public InstallOperationBuilderImpl(Feature feature) {
			super(feature);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.InstallOperationBuilder#install()
		 */
		@Override
		public InstalledFeature install() {
			return complete();
		}
	}

	public class UpdateOperationBuilderImpl extends AbstractOperationBuilderImpl<UpdateOperationBuilder>
			implements UpdateOperationBuilder {

		public UpdateOperationBuilderImpl(Feature feature) {
			super(feature);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.runtime.FeatureRuntime.UpdateOperationBuilder#update()
		 */
		@Override
		public InstalledFeature update() {
			return complete();
		}
	}

	public class RemoveOperationBuilderImpl extends AbstractOperationBuilderImpl<RemoveOperationBuilder>
			implements RemoveOperationBuilder {

		public RemoveOperationBuilderImpl(Feature feature) {
			super(feature);
		}

		/* 
		 * (non-Javadoc)
		 * @see com.kentyou.featurelauncher.impl.runtime.FeatureRuntimeImpl.RemoveOperationBuilder#remove()
		 */
		@Override
		public void remove() {
			removeFeature(this.feature.getID());
		}
	}

	protected InstalledFeature getInstalledFeatureById(ID featureId) {
		// @formatter:off
		return installedFeatures.stream()
				.filter(f -> ((f.isDecorated() && featureId.equals(f.getOriginalFeature().getID())) 
						|| (!f.isDecorated() && featureId.equals(f.getFeature().getID()))))
				.findFirst()
				.orElse(null);
		// @formatter:on
	}

	private void updateInstalledFeaturesOnAddOrUpdate(InstalledFeature installedFeature) {
		ID featureId = installedFeature.getFeature().getID();

		// @formatter:off
		List<ID> installedFeatureBundlesIDs = installedFeature.getInstalledBundles().stream()
				.map(ib -> ib.getBundleId())
				.toList();
		// @formatter:on

		// @formatter:off
		List<String> installedFeatureConfigurationsPIDs = installedFeature.getInstalledConfigurations().stream()
				.map(ic -> ic.getPid())
				.toList();
		// @formatter:on

		for (InstalledFeature existingFeature : installedFeatures) {
			for (InstalledBundle existingFeatureBundle : existingFeature.getInstalledBundles()) {
				if (installedFeatureBundlesIDs.contains(existingFeatureBundle.getBundleId())) {
					existingFeatureBundle.getOwningFeatures().add(featureId);
					LOG.info(String.format("Added feature %s to owning features of bundle %s", featureId,
							existingFeatureBundle.getBundleId()));
				}
			}

			for (InstalledConfiguration existingFeatureConfiguration : existingFeature.getInstalledConfigurations()) {
				if (installedFeatureConfigurationsPIDs.contains(existingFeatureConfiguration.getPid())) {
					existingFeatureConfiguration.getOwningFeatures().add(featureId);
					LOG.info(String.format("Added feature %s to owning features of configuration %s", featureId,
							existingFeatureConfiguration.getPid()));
				}
			}
		}
	}

	private void updateInstalledFeaturesOnRemove(ID featureId) {
		for (InstalledFeature existingFeature : installedFeatures) {
			// @formatter:off
			boolean isFeatureBundlesReferenced = existingFeature.getInstalledBundles().stream()
					.flatMap(ib -> ib.getOwningFeatures().stream())
					.anyMatch(ofId -> featureId.equals(ofId));
			// @formatter:on

			// @formatter:off
			boolean isFeatureConfigurationsReferenced = existingFeature.getInstalledConfigurations().stream()
					.flatMap(ic -> ic.getOwningFeatures().stream())
					.anyMatch(ofId -> featureId.equals(ofId));
			// @formatter:on

			if (isFeatureBundlesReferenced) {

				// update bundles' "owning features"
				for (InstalledBundle installedFeatureBundle : existingFeature.getInstalledBundles()) {
					if (installedFeatureBundle.getOwningFeatures().removeIf(ofId -> featureId.equals(ofId))) {
						LOG.info(String.format("Removed feature %s from owning features of bundle %s", featureId,
								installedFeatureBundle.getBundleId()));
					}
				}
			}

			if (isFeatureConfigurationsReferenced) {

				// update configurations' "owning features"
				for (InstalledConfiguration installedFeatureConfiguration : existingFeature
						.getInstalledConfigurations()) {
					if (installedFeatureConfiguration.getOwningFeatures().removeIf(ofId -> featureId.equals(ofId))) {
						LOG.info(String.format("Removed feature %s from owning features of configuration %s", featureId,
								installedFeatureConfiguration.getPid()));
					}
				}
			}
		}
	}

	private Map<Map.Entry<String, String>, Long> getExistingBundles() {
		// @formatter:off
		return Arrays.stream(bundleContext.getBundles()).collect(Collectors.toMap(
				b -> Map.entry(b.getSymbolicName(), b.getVersion().toString()), 
				b -> Long.valueOf(b.getBundleId())));
		// @formatter:on
	}

	private void setExternalFeatureId() {
		externalFeatureId = featureService.getIDfromMavenCoordinates(FeatureRuntimeConstants.EXTERNAL_FEATURE_ID);
	}

	// TODO: maybe add this to org.osgi.service.featurelauncher.runtime.FeatureRuntime ?
	public interface RemoveOperationBuilder extends OperationBuilder<RemoveOperationBuilder> {
		void remove();
	}
}
