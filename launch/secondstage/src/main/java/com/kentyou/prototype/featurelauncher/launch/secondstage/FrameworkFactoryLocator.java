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
package com.kentyou.prototype.featurelauncher.launch.secondstage;

import java.lang.StackWalker.StackFrame;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.featurelauncher.LaunchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 160.4.3.2: Locating a framework implementation
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 18, 2024
 */
class FrameworkFactoryLocator {
	static final int FIND_FRAMEWORK_CALLER_CLASS_SKIP = 4;
	private static final Logger LOG = LoggerFactory.getLogger(FrameworkFactoryLocator.class);

	public static FrameworkFactory locateFrameworkFactory(Map<String, Object> configuration,
			Optional<?> fromFeature) {
		/*
		 * TODO: "160.4.3.2: #1. If any provider specific configuration has been given
		 * to the Feature Launcher implementation then this should be used to identify
		 * the framework"
		 */

		/*
		 * "160.4.3.2: #2. If the Feature declares an Extension LAUNCH_FRAMEWORK then
		 * the Feature Launcher implementation must use the first listed artifact that
		 * can be found in any configured Artifact Repositories, as described in
		 * Selecting a framework implementation on page 99"
		 */
		if (fromFeature.isPresent()) {
			LOG.debug("Using a framework declared in the Feature Extensions");
			Object factory = fromFeature.get();
			if(factory instanceof FrameworkFactory) {
				return (FrameworkFactory) factory;
			} else {
				LOG.error("The FrameworkFactory {} is not an instance of FrameworkFactory. Is there a class space issue?",
						factory);
			}
		}

		/*
		 * "160.4.3.2: #3. If no framework implementation is found in the previous steps
		 * then the Feature Launcher implementation must search the classpath using the
		 * Thread Context Class Loader, or, if the Thread Context Class Loader is not
		 * set, the Class Loader which loaded the caller of the Feature Launcher's
		 * launch method. The first suitable framework instance located is the instance
		 * that will be used."
		 */
		Optional<FrameworkFactory> findFrameworkFactoryOptional = findFrameworkFactory();
		if (findFrameworkFactoryOptional.isPresent()) {
			LOG.debug("Using a framework found on the classpath");
			return findFrameworkFactoryOptional.get();
		}

		/*
		 * 160.4.3.2: #4. In the event that no suitable OSGi framework can be found by
		 * any of the previous steps then the Feature Launcher implementation may
		 * provide a default framework implementation to be used.
		 */
		Optional<FrameworkFactory> defaultFrameworkFactoryOptional = loadDefaultFrameworkFactory();
		if (defaultFrameworkFactoryOptional.isPresent()) {
			return defaultFrameworkFactoryOptional.get();
		} else {
			throw new LaunchException("No framework factory available!");
		}
	}

	private static Optional<FrameworkFactory> findFrameworkFactory() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader == null) {
			Optional<Class<?>> callerClassOptional = getCallerClass();
			if (callerClassOptional.isPresent()) {
				classLoader = callerClassOptional.get().getClassLoader();
			} else {
				classLoader = ClassLoader.getSystemClassLoader();
			}
		}

		ServiceLoader<FrameworkFactory> serviceLoader = ServiceLoader.load(FrameworkFactory.class, classLoader);

		return serviceLoader.findFirst();
	}

	private static Optional<FrameworkFactory> loadDefaultFrameworkFactory() {
		ServiceLoader<FrameworkFactory> loader = ServiceLoader.load(FrameworkFactory.class);
		return loader.findFirst();
	}

	private static Optional<Class<?>> getCallerClass() {
		// @formatter:off
		Object raw = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
				.walk(s -> s.map(StackFrame::getDeclaringClass)
							.skip(FIND_FRAMEWORK_CALLER_CLASS_SKIP)
							.findFirst());
		// @formatter:on

		@SuppressWarnings("unchecked")
		Optional<Class<?>> callerClass = (Optional<Class<?>>) raw;

		return callerClass;
	}

	private FrameworkFactoryLocator() {
		// hidden constructor
	}
}
