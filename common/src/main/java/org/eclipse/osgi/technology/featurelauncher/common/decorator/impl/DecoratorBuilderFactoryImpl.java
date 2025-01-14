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
package org.eclipse.osgi.technology.featurelauncher.common.decorator.impl;

import org.osgi.service.feature.FeatureArtifactBuilder;
import org.osgi.service.feature.FeatureBundleBuilder;
import org.osgi.service.feature.FeatureConfigurationBuilder;
import org.osgi.service.feature.FeatureExtension.Kind;
import org.osgi.service.feature.FeatureExtension.Type;
import org.osgi.service.feature.FeatureExtensionBuilder;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;

/**
 * Implementation of {@link org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
public class DecoratorBuilderFactoryImpl implements DecoratorBuilderFactory {
	private final FeatureService featureService;

	public DecoratorBuilderFactoryImpl(FeatureService featureService) {
		this.featureService = featureService;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory#newArtifactBuilder(org.osgi.service.feature.ID)
	 */
	@Override
	public FeatureArtifactBuilder newArtifactBuilder(ID id) {
		return featureService.getBuilderFactory().newArtifactBuilder(id);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory#newBundleBuilder(org.osgi.service.feature.ID)
	 */
	@Override
	public FeatureBundleBuilder newBundleBuilder(ID id) {
		return featureService.getBuilderFactory().newBundleBuilder(id);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory#newConfigurationBuilder(java.lang.String)
	 */
	@Override
	public FeatureConfigurationBuilder newConfigurationBuilder(String pid) {
		return featureService.getBuilderFactory().newConfigurationBuilder(pid);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory#newConfigurationBuilder(java.lang.String, java.lang.String)
	 */
	@Override
	public FeatureConfigurationBuilder newConfigurationBuilder(String factoryPid, String name) {
		return featureService.getBuilderFactory().newConfigurationBuilder(factoryPid, name);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory#newExtensionBuilder(java.lang.String, org.osgi.service.feature.FeatureExtension.Type, org.osgi.service.feature.FeatureExtension.Kind)
	 */
	@Override
	public FeatureExtensionBuilder newExtensionBuilder(String name, Type type, Kind kind) {
		return featureService.getBuilderFactory().newExtensionBuilder(name, type, kind);
	}
}
