/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.osgi.technology.featurelauncher.extras.installer.directory;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.featurelauncher.runtime.FeatureRuntime;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BundleActivator that tracks the FeatureRuntime service and starts the
 * FeatureDirectoryWatcher when the service becomes available.
 */
@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class FeatureDirectoryInstallerActivator implements BundleActivator {

	private static final Logger LOG = LoggerFactory.getLogger(FeatureDirectoryInstallerActivator.class);

	static final String PROP_PREFIX = DirectoryInstallerConfig.PROP_PREFIX;
	static final String PROP_FEATURES_DIR = DirectoryInstallerConfig.PROP_FEATURES_DIR;
	static final String PROP_REPO_DIR = DirectoryInstallerConfig.PROP_REPO_DIR;
	static final String PROP_SCAN_MODE = DirectoryInstallerConfig.PROP_SCAN_MODE;
	static final String PROP_SCAN_INTERVAL = DirectoryInstallerConfig.PROP_SCAN_INTERVAL;
	static final String PROP_FEATURE_PATTERN = DirectoryInstallerConfig.PROP_FEATURE_PATTERN;
	static final String PROP_SKIP_PATTERNS = DirectoryInstallerConfig.PROP_SKIP_PATTERNS;

	static final long DEFAULT_SCAN_INTERVAL = DirectoryInstallerConfig.DEFAULT_SCAN_INTERVAL;

	private ServiceTracker<FeatureRuntime, FeatureDirectoryWatcher> tracker;

	@Override
	public void start(BundleContext context) throws Exception {

		String featuresDir = context.getProperty(PROP_FEATURES_DIR);
		String repoDir = context.getProperty(PROP_REPO_DIR);
		String scanMode = context.getProperty(PROP_SCAN_MODE);
		long interval = parseLong(context.getProperty(PROP_SCAN_INTERVAL), DEFAULT_SCAN_INTERVAL);
		String featurePattern = context.getProperty(PROP_FEATURE_PATTERN);
		String skipPatterns = context.getProperty(PROP_SKIP_PATTERNS);

		LOG.info("Configuration: {}={}", PROP_FEATURES_DIR, featuresDir);
		LOG.info("Configuration: {}={}", PROP_REPO_DIR, repoDir);
		LOG.info("Configuration: {}={}", PROP_SCAN_MODE, scanMode);
		LOG.info("Configuration: {}={}", PROP_SCAN_INTERVAL, interval);
		LOG.info("Configuration: {}={}", PROP_FEATURE_PATTERN, featurePattern);
		LOG.info("Configuration: {}={}", PROP_SKIP_PATTERNS, skipPatterns);

		if (featuresDir == null || featuresDir.isEmpty()) {
			LOG.warn("No features directory configured ({} is not set)", PROP_FEATURES_DIR);
			return;
		}

		LOG.info("Directory Installer starting - features.dir={}, scan.mode={}", featuresDir,
				scanMode != null ? scanMode : "ONCE");

		tracker = new ServiceTracker<FeatureRuntime, FeatureDirectoryWatcher>(context, FeatureRuntime.class, null) {
			@Override
			public FeatureDirectoryWatcher addingService(ServiceReference<FeatureRuntime> ref) {
				FeatureRuntime runtime = context.getService(ref);
				if (runtime != null) {
					LOG.info("FeatureRuntime service available, starting directory watcher");
					FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(runtime, featuresDir, repoDir,
							scanMode, interval, featurePattern, skipPatterns);
					watcher.start();
					return watcher;
				}
				return null;
			}

			@Override
			public void removedService(ServiceReference<FeatureRuntime> ref, FeatureDirectoryWatcher watcher) {
				LOG.info("FeatureRuntime service removed, stopping directory watcher");
				watcher.stop();
				context.ungetService(ref);
			}
		};
		tracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		LOG.info("Directory Installer stopping");
		if (tracker != null) {
			LOG.debug("Closing FeatureRuntime service tracker");
			tracker.close();
			tracker = null;
		}
		LOG.info("Directory Installer stopped");
	}

	private static long parseLong(String value, long defaultValue) {
		if (value == null || value.isEmpty()) {
			return defaultValue;
		}
		try {
			return Long.parseLong(value.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
