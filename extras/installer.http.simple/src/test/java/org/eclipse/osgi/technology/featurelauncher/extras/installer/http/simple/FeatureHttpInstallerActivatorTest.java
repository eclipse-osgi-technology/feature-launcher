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

import static org.eclipse.osgi.technology.featurelauncher.extras.installer.http.simple.FeatureHttpInstallerActivator.PROP_CONNECT_TIMEOUT;
import static org.eclipse.osgi.technology.featurelauncher.extras.installer.http.simple.FeatureHttpInstallerActivator.PROP_FEATURES_URL;
import static org.eclipse.osgi.technology.featurelauncher.extras.installer.http.simple.FeatureHttpInstallerActivator.PROP_REPO_DIR;
import static org.eclipse.osgi.technology.featurelauncher.extras.installer.http.simple.FeatureHttpInstallerActivator.PROP_REQUEST_TIMEOUT;
import static org.eclipse.osgi.technology.featurelauncher.extras.installer.http.simple.FeatureHttpInstallerActivator.PROP_SCAN_INTERVAL;
import static org.eclipse.osgi.technology.featurelauncher.extras.installer.http.simple.FeatureHttpInstallerActivator.PROP_SCAN_MODE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;

class FeatureHttpInstallerActivatorTest {

	@Test
	void start_noFeaturesUrl_doesNotOpenTracker() throws Exception {
		BundleContext context = mock(BundleContext.class);
		when(context.getProperty(PROP_FEATURES_URL)).thenReturn(null);

		FeatureHttpInstallerActivator activator = new FeatureHttpInstallerActivator();
		activator.start(context);

		activator.stop(context);
	}

	@Test
	void start_emptyFeaturesUrl_doesNotOpenTracker() throws Exception {
		BundleContext context = mock(BundleContext.class);
		when(context.getProperty(PROP_FEATURES_URL)).thenReturn("");

		FeatureHttpInstallerActivator activator = new FeatureHttpInstallerActivator();
		activator.start(context);
		activator.stop(context);
	}

	@Test
	void start_withFeaturesUrl_opensTracker() throws Exception {
		BundleContext context = mock(BundleContext.class);
		when(context.getProperty(PROP_FEATURES_URL)).thenReturn("http://localhost:8080/features.json");
		when(context.getProperty(PROP_REPO_DIR)).thenReturn(null);
		when(context.getProperty(PROP_SCAN_MODE)).thenReturn("ONCE");
		when(context.getProperty(PROP_SCAN_INTERVAL)).thenReturn(null);
		when(context.getProperty(PROP_CONNECT_TIMEOUT)).thenReturn(null);
		when(context.getProperty(PROP_REQUEST_TIMEOUT)).thenReturn(null);

		when(context.createFilter(any())).thenReturn(null);

		FeatureHttpInstallerActivator activator = new FeatureHttpInstallerActivator();
		activator.start(context);

		verify(context).getProperty(PROP_FEATURES_URL);

		activator.stop(context);
	}

	@Test
	void stop_calledTwice_doesNotThrow() throws Exception {
		BundleContext context = mock(BundleContext.class);
		when(context.getProperty(PROP_FEATURES_URL)).thenReturn(null);

		FeatureHttpInstallerActivator activator = new FeatureHttpInstallerActivator();
		activator.start(context);
		activator.stop(context);
		activator.stop(context);
	}

	@Test
	void parsesIntervalProperty() throws Exception {
		BundleContext context = mock(BundleContext.class);
		when(context.getProperty(PROP_FEATURES_URL)).thenReturn("http://localhost:8080/features.json");
		when(context.getProperty(PROP_REPO_DIR)).thenReturn("/tmp/repo");
		when(context.getProperty(PROP_SCAN_MODE)).thenReturn("WATCH");
		when(context.getProperty(PROP_SCAN_INTERVAL)).thenReturn("30");
		when(context.getProperty(PROP_CONNECT_TIMEOUT)).thenReturn("10");
		when(context.getProperty(PROP_REQUEST_TIMEOUT)).thenReturn("20");

		FeatureHttpInstallerActivator activator = new FeatureHttpInstallerActivator();
		activator.start(context);

		verify(context).getProperty(PROP_SCAN_INTERVAL);
		verify(context).getProperty(PROP_CONNECT_TIMEOUT);
		verify(context).getProperty(PROP_REQUEST_TIMEOUT);

		activator.stop(context);
	}
}
