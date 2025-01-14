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
package org.eclipse.osgi.technology.featurelauncher.impl.runtime;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.osgi.framework.Bundle;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.runtime.InstalledBundle;

/**
 * Implementation of {@link org.osgi.service.featurelauncher.runtime.InstalledBundle}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
class InstalledBundleImpl implements InstalledBundle {
	private final ID bundleId;
	private final Collection<ID> aliases;
	private final Bundle bundle;
	private final int startLevel;
	private final List<ID> owningFeatures;

	public InstalledBundleImpl(ID bundleId, Collection<ID> aliases, Bundle bundle, int startLevel,
			List<ID> owningFeatures) {
		this.bundleId = bundleId;
		this.aliases = aliases;
		this.bundle = bundle;
		this.startLevel = startLevel;
		this.owningFeatures = owningFeatures;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledBundle#getBundleId()
	 */
	@Override
	public ID getBundleId() {
		return bundleId;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledBundle#getAliases()
	 */
	@Override
	public Collection<ID> getAliases() {
		return aliases;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledBundle#getBundle()
	 */
	@Override
	public Bundle getBundle() {
		return bundle;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledBundle#getStartLevel()
	 */
	@Override
	public int getStartLevel() {
		return startLevel;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.runtime.InstalledBundle#getOwningFeatures()
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
		return Objects.hash(aliases, bundle, bundleId, owningFeatures, startLevel);
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
		InstalledBundleImpl other = (InstalledBundleImpl) obj;
		return Objects.equals(aliases, other.aliases) && Objects.equals(bundle, other.bundle)
				&& Objects.equals(bundleId, other.bundleId) && Objects.equals(owningFeatures, other.owningFeatures)
				&& startLevel == other.startLevel;
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "InstalledBundleImpl [bundleId=" + bundleId + ", aliases=" + aliases + ", bundle=" + bundle
				+ ", startLevel=" + startLevel + ", owningFeatures=" + owningFeatures + "]";
	}
}
