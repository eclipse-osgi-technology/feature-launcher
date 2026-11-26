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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;


/**
 * Implementation of {@link FeatureExtensionHandler}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 19, 2024
 */
public class FrameworkLaunchingPropertiesFeatureExtensionHandlerImpl
		implements FeatureExtensionHandler {
	private final Map<String, Object> frameworkProperties;

	private final Map<String, Object> customProperties; // properties starting with a single underscore

	public FrameworkLaunchingPropertiesFeatureExtensionHandlerImpl() {
		this.frameworkProperties = new HashMap<>();
		this.customProperties = new HashMap<>();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler#handle(org.osgi.service.feature.Feature, org.osgi.service.feature.FeatureExtension, org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler.FeatureExtensionHandlerBuilder, org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory)
	 */
	@Override
	public Feature handle(Feature feature, FeatureExtension extension,
			List<ArtifactRepository> repositories,
			FeatureExtensionHandlerBuilder decoratedFeatureBuilder, DecoratorBuilderFactory factory)
			throws AbandonOperationException {

		// Variables are not substituted here: overrides are only known when the
		// framework is launched, where the merged variables are applied (160.3.2)
		Map<String, Object> properties = DecorationContext.readFeatureExtensionJSON(extension.getJSON());

		for (Map.Entry<String, Object> propertyEntry : properties.entrySet()) {
			String propertyName = propertyEntry.getKey();
			Object propertyValue = propertyEntry.getValue();

			if (propertyName.startsWith("__")) {
				frameworkProperties.put(propertyName.replaceFirst("_", ""), propertyValue);
			} else if (propertyName.startsWith("_")) {
				customProperties.put(propertyName, propertyValue);
			} else {
				frameworkProperties.put(propertyName, propertyValue);
			}
		}

		return feature;
	}

	public Map<String, Object> getFrameworkProperties() {
		return frameworkProperties;
	}

	public Map<String, Object> getCustomProperties() {
		return customProperties;
	}
	
	public static Map<String, String> getDefaultFrameworkProperties(Path defaultFrameworkStorageDir) {
		return Map.of("org.osgi.framework.storage", defaultFrameworkStorageDir.toString(),
				"org.osgi.framework.storage.clean", "onFirstInit");
	}
}
