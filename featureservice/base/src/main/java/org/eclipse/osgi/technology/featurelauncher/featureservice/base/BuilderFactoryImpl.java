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

import org.osgi.service.feature.BuilderFactory;
import org.osgi.service.feature.FeatureArtifactBuilder;
import org.osgi.service.feature.FeatureBuilder;
import org.osgi.service.feature.FeatureBundleBuilder;
import org.osgi.service.feature.FeatureConfigurationBuilder;
import org.osgi.service.feature.FeatureExtension.Kind;
import org.osgi.service.feature.FeatureExtension.Type;
import org.osgi.service.feature.FeatureExtensionBuilder;
import org.osgi.service.feature.ID;

class BuilderFactoryImpl implements BuilderFactory {
	@Override
	public FeatureArtifactBuilder newArtifactBuilder(ID id) {
		return new ArtifactBuilderImpl(id);
	}

	@Override
	public FeatureBundleBuilder newBundleBuilder(ID id) {
		return new BundleBuilderImpl(id);
	}

	@Override
	public FeatureConfigurationBuilder newConfigurationBuilder(String pid) {
		return new ConfigurationBuilderImpl(pid);
	}

	@Override
	public FeatureConfigurationBuilder newConfigurationBuilder(String factoryPid, String name) {
		return new ConfigurationBuilderImpl(factoryPid, name);
	}

	@Override
	public FeatureBuilder newFeatureBuilder(ID id) {
		return new FeatureBuilderImpl(id);
	}

	@Override
	public FeatureExtensionBuilder newExtensionBuilder(String name, Type type, Kind kind) {
		return new ExtensionBuilderImpl(name, type, kind);
	}
}