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

package org.eclipse.osgi.technology.featurelauncher.featureservice.base;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureConfigurationBuilder;

class ConfigurationBuilderImpl implements FeatureConfigurationBuilder {
	private final String p;
	private final String name;

	private final Map<String, Object> values = new LinkedHashMap<>();

	ConfigurationBuilderImpl(String pid) {
		this.p = pid;
		this.name = null;
	}

	ConfigurationBuilderImpl(String factoryPid, String name) {
		this.p = factoryPid;
		this.name = name;
	}

	@Override
	public FeatureConfigurationBuilder addValue(String key, Object value) {
		if (key == null) {
			throw new IllegalArgumentException("ConfigurationProperty key cannot be null");
		}
		this.values.put(key, value);
		return this;
	}

	@Override
	public FeatureConfigurationBuilder addValues(Map<String, Object> properties) {
		properties.forEach((k, v) -> addValue(k, v));
		return this;
	}

	@Override
	public FeatureConfiguration build() {
		if (name == null) {
			return new ConfigurationImpl(p, Optional.empty(), values);
		} else {
			return new ConfigurationImpl(p + "~" + name, Optional.of(p), values);
		}
	}

	private static record ConfigurationImpl(String pid, Optional<String> factoryPid, Map<String, Object> values)
	        implements FeatureConfiguration {

		private ConfigurationImpl(String pid, Optional<String> factoryPid, Map<String, Object> values) {
			this.pid = pid;
			this.factoryPid = factoryPid;
			this.values = Map.copyOf(values);
		}

		@Override
		public String getPid() {
			return pid;
		}

		@Override
		public Optional<String> getFactoryPid() {
			return factoryPid;
		}

		@Override
		public Map<String, Object> getValues() {
			return values;
		}
	}
}