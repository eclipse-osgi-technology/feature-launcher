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
package org.eclipse.osgi.technology.featurelauncher.launch.secondstage;

import static java.util.stream.Collectors.toList;
import static org.eclipse.osgi.technology.featurelauncher.launch.secondstage.FeatureLauncherConfigurationManager.CONFIGURATION_TIMEOUT_DEFAULT;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.BUNDLE_START_LEVEL_METADATA;
import static org.osgi.service.featurelauncher.FeatureLauncherConstants.CONFIGURATION_TIMEOUT;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.osgi.technology.featurelauncher.common.decorator.impl.DecorationContext;
import org.eclipse.osgi.technology.featurelauncher.common.osgi.util.impl.BundleEventUtil;
import org.eclipse.osgi.technology.featurelauncher.common.osgi.util.impl.FrameworkEventUtil;
import org.eclipse.osgi.technology.featurelauncher.common.util.impl.FileSystemUtil;
import org.eclipse.osgi.technology.featurelauncher.common.util.impl.VariablesUtil;
import org.eclipse.osgi.technology.featurelauncher.launch.spi.SecondStageLauncher;
import org.eclipse.osgi.technology.featurelauncher.repository.common.osgi.ArtifactRepositoryAdapter;
import org.eclipse.osgi.technology.featurelauncher.repository.spi.Repository;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.LaunchException;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 160.4 The Feature Launcher
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
public class SecondStageLauncherImpl implements SecondStageLauncher {
	private static final Logger LOG = LoggerFactory.getLogger(SecondStageLauncherImpl.class);

	@Override
	public LaunchResult launch(Feature feature, DecorationContext<?> context,
			List<? extends Repository> repositories, Optional<Object> frameworkFactory,
			Map<String, Object> variableOverrides, Map<String, Object> configurationProperties,
			Map<String, String> frameworkProperties) {
		List<ArtifactRepository> artifactRepos = repositories.stream().map(ArtifactRepositoryAdapter::new)
				.collect(toList());
		Framework fwk = launchFramework(feature, context, artifactRepos, frameworkFactory,
				variableOverrides, configurationProperties, frameworkProperties);
		return t -> fwk.waitForStop(t);
	}

	public Framework launchFramework(Feature feature, DecorationContext<?> context,
			List<? extends ArtifactRepository> repositories, Optional<Object> featureFrameworkFactory,
			Map<String, Object> variableOverrides, Map<String, Object> configurationProperties,
			Map<String, String> frameworkProperties) {
		
		Objects.requireNonNull(feature, "Feature is required!");

		if (repositories.isEmpty()) {
			throw new NullPointerException("At least one Artifact Repository is required!");
		}

		////////////////////////////////////////
		// 160.3.1: Overriding Feature Variables
		Map<String, Object> variablesToUse = mergeVariables(feature, variableOverrides);
		
		///////////////////////////////////////////////////
		// 160.4.2.1: Providing Framework Launch Properties
		Map<String, String> frameworkPropertiesToUse = mergeFrameworkProperties(
				context, frameworkProperties, variablesToUse);
		
		/////////////////////////////////////////////////
		// 160.4.3.2: Locating a framework implementation
		FrameworkFactory frameworkFactory = FrameworkFactoryLocator.locateFrameworkFactory(
				configurationProperties, featureFrameworkFactory);

		///////////////////////////////////////////
		// 160.4.3.3: Creating a Framework instance
		Framework framework = createFramework(context, frameworkFactory, frameworkPropertiesToUse);

		/////////////////////////////////////////////////////////
		// 160.4.3.4: Installing bundles and configurations
		List<Bundle> bundles = installBundles(framework, feature, repositories);

		FeatureLauncherConfigurationManager flcm = createConfigurationAdminTracker(framework.getBundleContext(), feature, variablesToUse);

		long configurationTimeout = getConfigurationTimeout(configurationProperties);

		//////////////////////////////////////////
		// 160.4.3.5: Starting the framework
		startFramework(context, framework, feature, bundles, configurationProperties, variablesToUse, frameworkPropertiesToUse);

		maybeWaitForConfigurationAdminTracker(flcm, configurationTimeout);

		return framework;
	}

	private Map<String, String> mergeFrameworkProperties(DecorationContext<?> context,
			Map<String, String> frameworkProperties, Map<String, Object> variables) {
		Map<String, Object> rawProperties = new HashMap<>(context.getFrameworkHandler().getFrameworkProperties());

		frameworkProperties.entrySet().forEach(e -> {
			if(e.getValue() == null) {
				rawProperties.remove(e.getKey());
			} else {
				rawProperties.put(e.getKey(), e.getValue());
			}
		});

		Map<String, Object> properties = VariablesUtil.maybeSubstituteVariables(rawProperties,
				variables);

		return properties.entrySet().stream()
				.collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue())));
	}

	private Map<String, Object> mergeVariables(Feature feature, Map<String, Object> variableOverrides) {
		Map<String, Object> allVariables = new HashMap<>(feature.getVariables());
		allVariables.putAll(variableOverrides);
		ensureVariablesNotNull(allVariables);

		return allVariables;
	}

	private void ensureVariablesNotNull(Map<String, Object> allVariables) {
		for (Map.Entry<String, Object> variable : allVariables.entrySet()) {
			if (variable.getValue() == null) {
				throw new LaunchException(String.format("No value provided for variable %s!", variable.getKey()));
			}
		}
	}

	private Framework createFramework(DecorationContext<?> context, 
			FrameworkFactory frameworkFactory, Map<String, String> frameworkProperties) {
		Framework framework = frameworkFactory.newFramework(frameworkProperties);
		try {
			framework.init();

			maybeSetInitialBundleStartLevel(context, framework);

			addLogListeners(framework);

		} catch (BundleException e) {
			throw new LaunchException("Could not initialize framework!", e);
		}
		return framework;
	}

	private void maybeSetFrameworkStartLevel(DecorationContext<?> context, Framework framework) {
		context.getStartLevelHandler().getMinimumFrameworkStartLevel()
			.ifPresent(sl -> {
				FrameworkStartLevel startLevel = framework.adapt(FrameworkStartLevel.class);
				if(startLevel.getStartLevel() < sl) {
					Semaphore sem = new Semaphore(0);
					startLevel.setStartLevel(sl, fe -> sem.release());
					try {
						sem.acquire();
					} catch (InterruptedException e) {
						throw new LaunchException("Interrupted while waiting for the start levels to change");
					}
				}
			});
	}

	private void maybeSetInitialBundleStartLevel(DecorationContext<?> context, Framework framework) {
		context.getStartLevelHandler().getDefaultBundleStartLevel()
			.ifPresent(sl -> framework.adapt(FrameworkStartLevel.class).setInitialBundleStartLevel(sl));
	}

	private void startFramework(DecorationContext<?> context, Framework framework, Feature feature,
			List<Bundle> bundles, Map<String, Object> configuration, Map<String, Object> variables,
			Map<String, String> frameworkProperties) {
		LOG.info("Starting framework..");
		try {
			framework.start();

			FeatureLauncherConfigurationManager flcm = createConfigurationAdminTracker(
					framework.getBundleContext(), feature, variables);

			startBundles(bundles);

			maybeSetFrameworkStartLevel(context, framework);

			///////////////////////////////////
			// 160.4.3.5: Configuration timeout
			long configurationTimeout = getConfigurationTimeout(configuration);
			
			maybeWaitForConfigurationsToBeCreated(flcm, configurationTimeout);
		} catch (BundleException | InterruptedException e) {
			////////////////////////////////////
			// 160.4.3.6: Cleanup after failure
			cleanup(framework, bundles, frameworkProperties);

			throw new LaunchException("Could not start framework!", e);
		}
	}

	private void startBundles(List<Bundle> installedBundles) throws BundleException, InterruptedException {
		for (Bundle installedBundle : installedBundles) {
			LOG.debug("Starting bundle {}", installedBundle);
			startBundle(installedBundle);
		}
	}

	private void startBundle(Bundle installedBundle) throws BundleException, InterruptedException {
		if (installedBundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
			installedBundle.start();
		}
	}

	private FeatureLauncherConfigurationManager createConfigurationAdminTracker(BundleContext bundleContext, 
			Feature feature, Map<String, Object> variables) {
		if (!feature.getConfigurations().isEmpty()) {
			FeatureLauncherConfigurationManager fcm = new FeatureLauncherConfigurationManager(bundleContext,
					feature.getConfigurations(), variables);

			LOG.info(String.format("Started ConfigurationAdmin service tracker for bundle '%s'",
					bundleContext.getBundle().getSymbolicName()));
			return fcm;
		}
		return null;
	}

	private void maybeWaitForConfigurationAdminTracker(FeatureLauncherConfigurationManager featureConfigurationManager,
			long configurationTimeout) {
		if ((featureConfigurationManager != null) && (configurationTimeout == CONFIGURATION_TIMEOUT_DEFAULT)) {
			try {
				featureConfigurationManager.waitForService(configurationTimeout);

				LOG.info("'ConfigurationAdmin' service is available!");
			} finally {
				maybeStopConfigurationAdminTracker(featureConfigurationManager);
			}
		}
	}

	private void maybeStopConfigurationAdminTracker(FeatureLauncherConfigurationManager featureConfigurationManager) {
		if (featureConfigurationManager != null) {
			featureConfigurationManager.stop();

			LOG.info("Stopped ConfigurationAdmin service tracker");
		}
	}

	private boolean maybeWaitForConfigurationsToBeCreated(FeatureLauncherConfigurationManager featureConfigurationManager,
			long configurationTimeout) throws InterruptedException {

		if(configurationTimeout == -1) {
			LOG.info("The configuration timeout is set to -1, and so we will not wait for configurations to be created");
			return true;
		} else if (featureConfigurationManager != null) {
			
			boolean configurationsCreated = featureConfigurationManager.configurationsCreated();
			
			while(!configurationsCreated && configurationTimeout > 0) {
				long start = System.nanoTime();
				TimeUnit.MILLISECONDS.sleep(10);
				configurationTimeout -= TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
				configurationsCreated = featureConfigurationManager.configurationsCreated();
			}
			
			LOG.debug("Configurations created {} with {} milliseconds to spare", configurationsCreated, configurationTimeout);
			return configurationsCreated;
			
		} else {
			LOG.debug("No configurations to create");
			// Nothing to create
			return true;
		}
	}

	private long getConfigurationTimeout(Map<String, Object> configuration) {
		if (configuration.containsKey(CONFIGURATION_TIMEOUT)) {
			long customConfigurationTimeout = Long
					.parseLong(configuration.get(CONFIGURATION_TIMEOUT).toString());

			if (customConfigurationTimeout < -1) {
				throw new LaunchException("An invalid timeout has been supplied " + customConfigurationTimeout);
			}
			return customConfigurationTimeout;
		} else {
			return 5000L;
		}
	}

	private void addLogListeners(Framework framework) {
		framework.getBundleContext().addFrameworkListener(this::logFrameworkEvent);
		framework.getBundleContext().addBundleListener(this::logBundleEvent);
	}

	private List<Bundle> installBundles(Framework framework, Feature feature,
			List<? extends ArtifactRepository> repositories) {
		List<Bundle> installedBundles = new ArrayList<>();
		if (feature.getBundles() != null && feature.getBundles().size() > 0) {

			LOG.info(String.format("There are %d bundle(s) to install", feature.getBundles().size()));

			for (FeatureBundle featureBundle : feature.getBundles()) {
				installBundle(framework.getBundleContext(), featureBundle,
						repositories, installedBundles);
			}

		} else {
			LOG.warn("There are no bundles to install!");
		}
		return installedBundles;
	}

	private void installBundle(BundleContext bundleContext, FeatureBundle featureBundle,
			List<? extends ArtifactRepository> repositories, List<Bundle> installedBundles) {
		Bundle installedBundle = installBundle(bundleContext, featureBundle.getID(), repositories);

		if (installedBundle != null) {
			maybeSetBundleStartLevel(installedBundle, featureBundle.getMetadata());

			installedBundles.add(installedBundle);
		}
	}

	private Bundle installBundle(BundleContext bundleContext, ID featureBundleID,
			List<? extends ArtifactRepository> repositories) {
		try (InputStream featureBundleIs = getArtifact(featureBundleID, repositories)) {
			if (featureBundleIs.available() != 0) {
				Bundle installedBundle = bundleContext.installBundle(featureBundleID.toString(), featureBundleIs);

				LOG.info(String.format("Installed bundle '%s'", installedBundle.getSymbolicName()));

				return installedBundle;
			}
		} catch (IOException | BundleException e) {
			throw new LaunchException(String.format("Could not install bundle '%s'!", featureBundleID.toString()),
					e);
		}

		return null;
	}

	protected void maybeSetBundleStartLevel(Bundle bundle, Map<String, Object> metadata) {
		if (metadata != null && metadata.containsKey(BUNDLE_START_LEVEL_METADATA)) {
			int startlevel = Integer.valueOf(metadata.get(BUNDLE_START_LEVEL_METADATA).toString()).intValue();

			bundle.adapt(BundleStartLevel.class).setStartLevel(startlevel);
		}
	}

	private InputStream getArtifact(ID featureBundleID, List<? extends ArtifactRepository> repositories) {
		for (ArtifactRepository artifactRepository : repositories) {
			InputStream featureBundleIs = artifactRepository.getArtifact(featureBundleID);
			if (featureBundleIs != null) {
				return featureBundleIs;
			}
		}

		return InputStream.nullInputStream();
	}

	private void logFrameworkEvent(FrameworkEvent frameworkEvent) {
		if (frameworkEvent.getType() == FrameworkEvent.ERROR) {
			LOG.error(String.format("Framework ERROR event %s", frameworkEvent.toString()));
		} else {
			LOG.info(String.format("Framework event type %s: %s",
					FrameworkEventUtil.getFrameworkEventString(frameworkEvent.getType()),
					frameworkEvent.toString()));
		}
	}

	private void logBundleEvent(BundleEvent bundleEvent) {
		LOG.info(String.format("Bundle '%s' event type %s: %s", bundleEvent.getBundle().getSymbolicName(),
				BundleEventUtil.getBundleEventString(bundleEvent.getType()), bundleEvent.toString()));
	}

	private void cleanup(Framework framework, List<Bundle> installedBundles, Map<String, String> frameworkProps) {

		// Stopping the framework will stop all of the bundles
		try {
			framework.stop();
			framework.waitForStop(0);
		} catch (BundleException | InterruptedException e) {
			LOG.error("A problem occurred while cleaning up the framework", e);
		}

		ListIterator<Bundle> it = installedBundles.listIterator(installedBundles.size());

		while (it.hasPrevious()) {
			Bundle installedBundle = it.previous();

			try {
				if (installedBundle.getState() != Bundle.UNINSTALLED) {
					installedBundle.stop();
					installedBundle.uninstall();
					LOG.info(String.format("Uninstalled bundle '%s'", installedBundle.getSymbolicName()));
				}

				it.remove();

			} catch (BundleException exc) {
				LOG.error(String.format("Cannot uninstall bundle '%s'", installedBundle.getSymbolicName()));
			}
		}

		// Cleaning up the storage area will uninstall any bundles left over, but we
		// must
		// only do it if the storage area is allowed to be cleaned at startup
		if (frameworkProps.containsKey(Constants.FRAMEWORK_STORAGE) && Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT
				.equals(frameworkProps.get(Constants.FRAMEWORK_STORAGE_CLEAN))) {
			try {
				FileSystemUtil.recursivelyDelete(
						Paths.get(String.valueOf(frameworkProps.get(Constants.FRAMEWORK_STORAGE))));
			} catch (IOException e) {
				LOG.warn("Could not delete framework storage area!", e);
			}
		}
	}
}
