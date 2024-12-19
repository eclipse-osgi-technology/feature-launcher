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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.runtime.InstalledConfiguration;

/**
 * Implementation of {@link org.osgi.service.featurelauncher.runtime.InstalledConfiguration}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
class InstalledConfigurationImpl implements InstalledConfiguration {
	private final String pid;
	private final Optional<String> factoryPid;
	private final Map<String, Object> properties;
	private final List<ID> owningFeatures;

	public InstalledConfigurationImpl(String pid, Optional<String> factoryPid, Map<String, Object> properties,
			List<ID> owningFeatures) {
		this.pid = pid;
		this.factoryPid = factoryPid;
		this.properties = properties;
		this.owningFeatures = owningFeatures;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledConfiguration#getPid()
	 */
	@Override
	public String getPid() {
		return pid;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledConfiguration#getFactoryPid()
	 */
	@Override
	public Optional<String> getFactoryPid() {
		return factoryPid;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledConfiguration#getProperties()
	 */
	@Override
	public Map<String, Object> getProperties() {
		return properties;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledConfiguration#getOwningFeatures()
	 */
	@Override
	public List<ID> getOwningFeatures() {
		return owningFeatures;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(factoryPid, owningFeatures, pid, properties);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InstalledConfigurationImpl other = (InstalledConfigurationImpl) obj;
		return Objects.equals(factoryPid, other.factoryPid) && Objects.equals(owningFeatures, other.owningFeatures)
				&& Objects.equals(pid, other.pid) && Objects.equals(properties, other.properties);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "InstalledConfigurationImpl [pid=" + pid + ", factoryPid=" + factoryPid + ", properties=" + properties
				+ ", owningFeatures=" + owningFeatures + "]";
	}
}
