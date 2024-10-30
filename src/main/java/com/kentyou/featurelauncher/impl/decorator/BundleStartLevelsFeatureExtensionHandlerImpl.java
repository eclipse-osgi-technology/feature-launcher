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
package com.kentyou.featurelauncher.impl.decorator;

import static com.kentyou.featurelauncher.impl.decorator.FeatureDecorationConstants.BUNDLE_START_LEVELS_DEFAULT;
import static com.kentyou.featurelauncher.impl.decorator.FeatureDecorationConstants.BUNDLE_START_LEVELS_MINIMUM;

import java.util.Map;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;

import com.kentyou.featurelauncher.impl.util.FeatureDecorationUtil;

/**
 * Implementation of
 * {@link com.kentyou.featurelauncher.impl.decorator.BundleStartLevelsFeatureExtensionHandler}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 19, 2024
 */
public class BundleStartLevelsFeatureExtensionHandlerImpl implements BundleStartLevelsFeatureExtensionHandler {

	private Integer defaultBundleStartLevel;

	private Integer minimumFrameworkStartLevel;

	public BundleStartLevelsFeatureExtensionHandlerImpl() {
		this.defaultBundleStartLevel = null;
		this.minimumFrameworkStartLevel = null;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler#handle(org.osgi.service.feature.Feature, org.osgi.service.feature.FeatureExtension, org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler.FeatureExtensionHandlerBuilder, org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory)
	 */
	@Override
	public Feature handle(Feature feature, FeatureExtension extension,
			FeatureExtensionHandlerBuilder decoratedFeatureBuilder, DecoratorBuilderFactory factory)
			throws AbandonOperationException {

		Map<String, String> properties = FeatureDecorationUtil.readFeatureExtensionJSON(extension.getJSON());

		// The minimum required framework start level after installing this feature
		if (properties.containsKey(BUNDLE_START_LEVELS_MINIMUM)) {
			minimumFrameworkStartLevel = Integer.valueOf(properties.get(BUNDLE_START_LEVELS_MINIMUM));

			if (minimumFrameworkStartLevel.intValue() < 1 || minimumFrameworkStartLevel > Integer.MAX_VALUE) {
				throw new AbandonOperationException(String.format(
						"Minimum required framework start level must be an integer greater than zero and less than Integer.MAX_VALUE, not %d",
						minimumFrameworkStartLevel));
			}
		}

		// The default start level to use for bundles in this feature
		if (properties.containsKey(BUNDLE_START_LEVELS_DEFAULT)) {
			defaultBundleStartLevel = Integer.valueOf(properties.get(BUNDLE_START_LEVELS_DEFAULT));

			if (defaultBundleStartLevel.intValue() < 1 || defaultBundleStartLevel > Integer.MAX_VALUE) {
				throw new AbandonOperationException(String.format(
						"Default start level to use for bundles must be an integer greater than zero and less than Integer.MAX_VALUE, not %d",
						defaultBundleStartLevel));
			}
		}

		return feature;
	}

	/* 
	 * (non-Javadoc)
	 * @see com.kentyou.featurelauncher.impl.decorator.BundleStartLevelsFeatureExtensionHandler#hasDefaultBundleStartLevel()
	 */
	@Override
	public boolean hasDefaultBundleStartLevel() {
		return (defaultBundleStartLevel != null);
	}

	/* 
	 * (non-Javadoc)
	 * @see com.kentyou.featurelauncher.impl.decorator.BundleStartLevelsFeatureExtensionHandler#getDefaultBundleStartLevel()
	 */
	@Override
	public Integer getDefaultBundleStartLevel() {
		return defaultBundleStartLevel;
	}

	/* 
	 * (non-Javadoc)
	 * @see com.kentyou.featurelauncher.impl.decorator.BundleStartLevelsFeatureExtensionHandler#hasMinimumFrameworkStartLevel()
	 */
	@Override
	public boolean hasMinimumFrameworkStartLevel() {
		return (minimumFrameworkStartLevel != null);
	}

	/* 
	 * (non-Javadoc)
	 * @see com.kentyou.featurelauncher.impl.decorator.BundleStartLevelsFeatureExtensionHandler#getMinimumFrameworkStartLevel()
	 */
	@Override
	public Integer getMinimumFrameworkStartLevel() {
		return minimumFrameworkStartLevel;
	}
}
