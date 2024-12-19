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

import static com.kentyou.featurelauncher.common.util.impl.ConfigurationUtil.CONFIGURATIONS_FILTER;
import static com.kentyou.featurelauncher.common.util.impl.ConfigurationUtil.CONFIGURATION_DEFAULT_LOCATION;
import static com.kentyou.featurelauncher.common.util.impl.ConfigurationUtil.constructConfigurationsFilter;
import static com.kentyou.featurelauncher.common.util.impl.ConfigurationUtil.normalizePid;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.Configuration.ConfigurationAttribute;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.feature.FeatureConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kentyou.prototype.featurelauncher.common.util.impl.VariablesUtil;


/**
 * Manages feature configurations via Configuration Admin Service for
 * {@link com.kentyou.featurelauncher.impl.runtime.FeatureRuntimeImpl}
 * 
 * As defined in the following sections of the "160. Feature Launcher Service
 * Specification": - 160.4.3.4 - 160.4.3.5 - 160.5.2.1.3
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 4, 2024
 */
@Component(service = FeatureRuntimeConfigurationManager.class)
public class FeatureRuntimeConfigurationManager {
	private static final Logger LOG = LoggerFactory.getLogger(FeatureRuntimeConfigurationManager.class);

	@Reference
	ConfigurationAdmin configurationAdmin;

	public void createConfigurations(List<FeatureConfiguration> featureConfigurations,
			Map<String, Object> featureVariables) {
		featureConfigurations.forEach(fc -> createConfiguration(fc, featureVariables));
	}

	public void removeConfigurations(Set<String> featuresConfigurationsPids) {
		try {
			Map<String, Configuration> existingConfigurations = getFeatureLauncherConfigurations();

			if (!existingConfigurations.isEmpty()) {
				for (String featuresConfigurationsPid : featuresConfigurationsPids) {
					if (existingConfigurations.containsKey(featuresConfigurationsPid)) {
						existingConfigurations.remove(featuresConfigurationsPid).delete();
					}
				}
			}

		} catch (IOException | InvalidSyntaxException e) {
			LOG.error("Error removing configurations!", e);
		}
	}

	public List<Configuration> getConfigurations(String filter) {
		try {
			Configuration[] configurations = configurationAdmin.listConfigurations(filter);

			if (configurations != null) {
				return Stream.of(configurations).collect(Collectors.toList());
			}

		} catch (IOException | InvalidSyntaxException e) {
			LOG.error("Error retrieving configurations!", e);
		}

		return Collections.emptyList();
	}

	public Map<String, Configuration> getAllConfigurations() throws IOException, InvalidSyntaxException {
		// @formatter:off
		return Optional.ofNullable(configurationAdmin.listConfigurations(null))
				.map(Arrays::stream)
				.map(s -> s.collect(Collectors.toMap(Configuration::getPid, Function.identity())))
				.orElse(Map.of());
		// @formatter:on
	}

	public void createConfiguration(FeatureConfiguration featureConfiguration, Map<String, Object> featureVariables) {
		if (featureConfiguration.getFactoryPid().isPresent()) {
			createFactoryConfiguration(featureConfiguration, featureVariables);
			return;
		}

		try {
			LOG.info(String.format("Creating configuration %s", featureConfiguration.getPid()));

			Configuration configuration = configurationAdmin.getConfiguration(featureConfiguration.getPid(),
					CONFIGURATION_DEFAULT_LOCATION);

			if (!isReadOnly(configuration)) {
				updateConfigurationProperties(configuration, featureConfiguration, featureVariables);
			} else {
				LOG.warn(String.format("Configuration %s is read only!", featureConfiguration.getPid()));
			}

		} catch (IllegalArgumentException | IOException e) {
			LOG.error(String.format("Error creating configuration %s!", featureConfiguration.getPid()), e);
		}
	}

	private void createFactoryConfiguration(FeatureConfiguration featureConfiguration,
			Map<String, Object> featureVariables) {
		try {
			LOG.info(String.format("Creating factory configuration %s", featureConfiguration.getPid()));

			Configuration configuration = configurationAdmin.getFactoryConfiguration(
					featureConfiguration.getFactoryPid().get(), normalizePid(featureConfiguration.getPid()),
					CONFIGURATION_DEFAULT_LOCATION);

			if (!isReadOnly(configuration)) {
				updateConfigurationProperties(configuration, featureConfiguration, featureVariables);
			} else {
				LOG.warn(String.format("Configuration %s is read only!", featureConfiguration.getPid()));
			}

		} catch (IllegalArgumentException | IOException e) {
			LOG.error(String.format("Error creating configuration %s!", featureConfiguration.getPid()), e);
		}
	}

	private void updateConfigurationProperties(Configuration configuration, FeatureConfiguration featureConfiguration,
			Map<String, Object> featureVariables) {
		Map<String, Object> configurationProperties = VariablesUtil
				.maybeSubstituteVariables(featureConfiguration.getValues(), featureVariables);

		configurationProperties.put(CONFIGURATIONS_FILTER, Boolean.TRUE);

		try {
			configuration.updateIfDifferent(new Hashtable<>(configurationProperties));
		} catch (IOException e) {
			LOG.error(String.format("Error updating configuration properties %s!", featureConfiguration.getPid()), e);
		}
	}

	private Map<String, Configuration> getFeatureLauncherConfigurations() throws IOException, InvalidSyntaxException {
		// @formatter:off
		return Optional.ofNullable(configurationAdmin.listConfigurations(constructConfigurationsFilter()))
				.map(Arrays::stream)
				.map(s -> s.collect(Collectors.toMap(Configuration::getPid, Function.identity())))
				.orElse(Map.of());
		// @formatter:on
	}

	private boolean isReadOnly(Configuration configuration) {
		Set<ConfigurationAttribute> configurationAttributes = configuration.getAttributes();
		return (configurationAttributes != null && configurationAttributes.contains(ConfigurationAttribute.READ_ONLY));
	}
}
