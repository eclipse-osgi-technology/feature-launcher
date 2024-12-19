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
package com.kentyou.featurelauncher.common.util.impl;

import java.util.Optional;
import java.util.ServiceLoader;

import org.osgi.service.feature.FeatureService;
import org.osgi.service.featurelauncher.FeatureLauncher;
import org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory;

/**
 * Util for {@link java.util.ServiceLoader<T>} operations.
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 11, 2024
 */
public class ServiceLoaderUtil {

	private ServiceLoaderUtil() {
		// hidden constructor
	}

	public static FeatureService loadFeatureService() {
		return loadService(FeatureService.class);
	}

	public static FeatureLauncher loadFeatureLauncherService() {
		return loadService(FeatureLauncher.class);
	}

	public static ArtifactRepositoryFactory loadArtifactRepositoryFactoryService() {
		return loadService(ArtifactRepositoryFactory.class);
	}

	private static <T> T loadService(Class<T> serviceClass) {
		ServiceLoader<T> loader = ServiceLoader.load(serviceClass);

		Optional<T> serviceOptional = loader.findFirst();
		if (serviceOptional.isPresent()) {
			return serviceOptional.get();
		} else {
			throw new IllegalStateException(String.format("Error loading %s!", serviceClass));
		}
	}
}
