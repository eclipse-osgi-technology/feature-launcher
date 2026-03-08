/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.osgi.technology.featurelauncher.extras.installer.http.simple;

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
 * FeatureHttpWatcher when the service becomes available.
 */
@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class FeatureHttpInstallerActivator implements BundleActivator {

	private static final Logger LOG = LoggerFactory.getLogger(FeatureHttpInstallerActivator.class);

	static final String PROP_PREFIX = HttpInstallerConfig.PROP_PREFIX;
	static final String PROP_FEATURES_URL = HttpInstallerConfig.PROP_FEATURES_URL;
	static final String PROP_REPO_DIR = HttpInstallerConfig.PROP_REPO_DIR;
	static final String PROP_SCAN_MODE = HttpInstallerConfig.PROP_SCAN_MODE;
	static final String PROP_SCAN_INTERVAL = HttpInstallerConfig.PROP_SCAN_INTERVAL;
	static final String PROP_CONNECT_TIMEOUT = HttpInstallerConfig.PROP_CONNECT_TIMEOUT;
	static final String PROP_REQUEST_TIMEOUT = HttpInstallerConfig.PROP_REQUEST_TIMEOUT;

	static final long DEFAULT_SCAN_INTERVAL = HttpInstallerConfig.DEFAULT_SCAN_INTERVAL;
	static final long DEFAULT_CONNECT_TIMEOUT = HttpInstallerConfig.DEFAULT_CONNECT_TIMEOUT;
	static final long DEFAULT_REQUEST_TIMEOUT = HttpInstallerConfig.DEFAULT_REQUEST_TIMEOUT;

	private ServiceTracker<FeatureRuntime, FeatureHttpWatcher> tracker;

	@Override
	public void start(BundleContext context) throws Exception {

		String featuresUrl = context.getProperty(PROP_FEATURES_URL);
		String repoDir = context.getProperty(PROP_REPO_DIR);
		String scanMode = context.getProperty(PROP_SCAN_MODE);
		long interval = parseLong(context.getProperty(PROP_SCAN_INTERVAL), DEFAULT_SCAN_INTERVAL);
		long connectTimeout = parseLong(context.getProperty(PROP_CONNECT_TIMEOUT), DEFAULT_CONNECT_TIMEOUT);
		long requestTimeout = parseLong(context.getProperty(PROP_REQUEST_TIMEOUT), DEFAULT_REQUEST_TIMEOUT);

		LOG.info("Configuration: {}={}", PROP_FEATURES_URL, featuresUrl);
		LOG.info("Configuration: {}={}", PROP_REPO_DIR, repoDir);
		LOG.info("Configuration: {}={}", PROP_SCAN_MODE, scanMode);
		LOG.info("Configuration: {}={}", PROP_SCAN_INTERVAL, interval);
		LOG.info("Configuration: {}={}", PROP_CONNECT_TIMEOUT, connectTimeout);
		LOG.info("Configuration: {}={}", PROP_REQUEST_TIMEOUT, requestTimeout);

		if (featuresUrl == null || featuresUrl.isEmpty()) {
			LOG.warn("No features URL configured ({} is not set)", PROP_FEATURES_URL);
			return;
		}

		LOG.info("HTTP Installer starting - features.url={}, scan.mode={}", featuresUrl,
				scanMode != null ? scanMode : "ONCE");

		tracker = new ServiceTracker<FeatureRuntime, FeatureHttpWatcher>(context, FeatureRuntime.class, null) {
			@Override
			public FeatureHttpWatcher addingService(ServiceReference<FeatureRuntime> ref) {
				FeatureRuntime runtime = context.getService(ref);
				if (runtime != null) {
					LOG.info("FeatureRuntime service available, starting HTTP watcher");
					FeatureHttpWatcher watcher = new FeatureHttpWatcher(runtime, featuresUrl, repoDir, scanMode,
							interval, connectTimeout, requestTimeout);
					watcher.start();
					return watcher;
				}
				return null;
			}

			@Override
			public void removedService(ServiceReference<FeatureRuntime> ref, FeatureHttpWatcher watcher) {
				LOG.info("FeatureRuntime service removed, stopping HTTP watcher");
				watcher.stop();
				context.ungetService(ref);
			}
		};
		tracker.open();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		LOG.info("HTTP Installer stopping");
		if (tracker != null) {
			LOG.debug("Closing FeatureRuntime service tracker");
			tracker.close();
			tracker = null;
		}
		LOG.info("HTTP Installer stopped");
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
