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
package com.kentyou.featurelauncher.impl;

import static com.kentyou.featurelauncher.common.util.impl.ConfigurationUtil.CONFIGURATION_DEFAULT_LOCATION;
import static com.kentyou.featurelauncher.common.util.impl.ConfigurationUtil.normalizePid;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.featurelauncher.LaunchException;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kentyou.featurelauncher.common.util.impl.VariablesUtil;

/**
 * Manages feature configurations via Configuration Admin Service for {@link com.kentyou.featurelauncher.impl.FeatureLauncherImpl}
 * 
 * As defined in the following sections of the "160. Feature Launcher Service Specification":
 *  - 160.4.3.4
 *  - 160.4.3.5
 *  - 160.5.2.1.3
 *  
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 25, 2024
 */
public class FeatureLauncherConfigurationManager implements ServiceTrackerCustomizer<Object, Object> {
	private static final Logger LOG = LoggerFactory.getLogger(FeatureLauncherConfigurationManager.class);

	private static final String CONFIGURATION_ADMIN_CLASS_NAME = "org.osgi.service.cm.ConfigurationAdmin";
	private static final String CONFIGURATION_CLASS_NAME = "org.osgi.service.cm.Configuration";
	public static final long CONFIGURATION_TIMEOUT_DEFAULT = 5000;

	private final BundleContext bundleContext;
	private final Map<String, FeatureConfiguration> featureConfigurations;
	private final Map<String, Object> featureVariables;

	private final ServiceTracker<Object, Object> serviceTracker;

	private Class<?> configurationAdminClass;
	private Class<?> configurationClass;

	private Method listConfigurationsMethod;
	private Method getFactoryConfigurationMethod;
	private Method getConfigurationMethod;
	private Method getConfigurationPropertiesMethod;
	private Method updateConfigurationPropertiesMethod;

	private boolean configurationsCreated;

	public FeatureLauncherConfigurationManager(BundleContext bundleContext,
			Map<String, FeatureConfiguration> featureConfigurations, Map<String, Object> featureVariables) {
		this.bundleContext = bundleContext;
		this.featureConfigurations = featureConfigurations;
		this.featureVariables = featureVariables;

		this.configurationsCreated = false;

		this.serviceTracker = new ServiceTracker<>(this.bundleContext, CONFIGURATION_ADMIN_CLASS_NAME, this);
		this.serviceTracker.open(true);
	}

	public void waitForService(long timeout) {
		try {
			if (serviceTracker.waitForService(timeout) == null) {
				throw new LaunchException("'ConfigurationAdmin' service is not available!");
			}
		} catch (InterruptedException e) {
			throw new LaunchException("Error awaiting 'ConfigurationAdmin' service!", e);
		}
	}

	public boolean serviceAdded() {
		return (serviceTracker.getTrackingCount() > 0);
	}

	public boolean configurationsCreated() {
		return configurationsCreated;
	}

	public void stop() {
		serviceTracker.close();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
	 */
	@Override
	public Object addingService(ServiceReference<Object> reference) {
		LOG.info("Added ConfigurationAdmin service"); // TODO: change to debug level

		createConfigurationsIfNeeded(reference);

		return bundleContext.getService(reference);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void modifiedService(ServiceReference<Object> reference, Object service) {
		// NOP
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void removedService(ServiceReference<Object> reference, Object service) {
		bundleContext.ungetService(reference);

		LOG.info("Removed ConfigurationAdmin service");
	}

	private void createConfigurationsIfNeeded(ServiceReference<Object> reference) {
		if (!featureConfigurations.isEmpty()) {
			LOG.info(String.format("There are %d feature configuration(s) to create", featureConfigurations.size()));

			try {
				Object configurationAdminService = bundleContext.getService(reference);

				this.configurationAdminClass = reference.getBundle().loadClass(CONFIGURATION_ADMIN_CLASS_NAME);
				this.configurationClass = reference.getBundle().loadClass(CONFIGURATION_CLASS_NAME);

				this.listConfigurationsMethod = configurationAdminClass.getMethod("listConfigurations", String.class);
				this.getFactoryConfigurationMethod = configurationAdminClass.getMethod("getFactoryConfiguration",
						String.class, String.class, String.class);
				this.getConfigurationMethod = configurationAdminClass.getMethod("getConfiguration", String.class,
						String.class);
				this.getConfigurationPropertiesMethod = configurationClass.getMethod("getProperties");
				this.updateConfigurationPropertiesMethod = configurationClass.getMethod("update", Dictionary.class);

				featureConfigurations.forEach((featureConfigurationPid, featureConfiguration) -> createConfiguration(
						featureConfigurationPid, featureConfiguration, configurationAdminService));

				this.configurationsCreated = true;

			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
				LOG.error("Error creating configurations!", e);
			}

		} else {
			LOG.info("Feature has no configurations!");
		}
	}

	private void createConfiguration(String featureConfigurationPid, FeatureConfiguration featureConfiguration,
			Object configurationAdminService) {
		if (featureConfiguration.getFactoryPid().isPresent()) {
			createFactoryConfiguration(featureConfigurationPid, featureConfiguration, configurationAdminService);
			return;
		}

		try {
			LOG.info(String.format("Creating configuration %s", featureConfigurationPid));

			Object configurationObject = getConfigurationMethod.invoke(configurationAdminService,
					featureConfiguration.getPid(), CONFIGURATION_DEFAULT_LOCATION);

			updateConfigurationProperties(configurationObject, featureConfigurationPid, featureConfiguration);

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			LOG.error(String.format("Error creating configuration %s!", featureConfigurationPid), e);
		}
	}

	private void createFactoryConfiguration(String featureConfigurationPid, FeatureConfiguration featureConfiguration,
			Object configurationAdminService) {
		try {
			LOG.info(String.format("Creating factory configuration %s", featureConfigurationPid));

			Object configurationObject = getFactoryConfigurationMethod.invoke(configurationAdminService,
					featureConfiguration.getFactoryPid().get(), normalizePid(featureConfiguration.getPid()),
					CONFIGURATION_DEFAULT_LOCATION);

			updateConfigurationProperties(configurationObject, featureConfigurationPid, featureConfiguration);

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			LOG.error(String.format("Error creating configuration %s!", featureConfigurationPid), e);
		}
	}

	private void updateConfigurationProperties(Object configurationObject, String featureConfigurationPid,
			FeatureConfiguration featureConfiguration) {

		Map<String, Object> configurationProperties = VariablesUtil
				.maybeSubstituteVariables(featureConfiguration.getValues(), featureVariables);

		try {
			updateConfigurationPropertiesMethod.invoke(configurationObject,
					FrameworkUtil.asDictionary(configurationProperties));
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			LOG.error(String.format("Error updating configuration properties %s!", featureConfigurationPid), e);
		}
	}

	@SuppressWarnings("unused")
	private List<Map<String, Object>> listConfigurations(Object configurationAdminService, String filter) {
		try {
			Object result = listConfigurationsMethod.invoke(configurationAdminService, filter);
			if (result != null) {
				List<Map<String, Object>> configurations = new ArrayList<>();
				for (Object configObj : (Object[]) result) {
					configurations.add(getConfigurationProperties(configObj));
				}
				return configurations;
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			LOG.error("Error listing configurations!", e);
		}

		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getConfigurationProperties(Object configurationObject)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return FrameworkUtil
				.asMap((Dictionary<String, Object>) getConfigurationPropertiesMethod.invoke(configurationObject));
	}
}
