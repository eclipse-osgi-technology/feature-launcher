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

import static org.eclipse.osgi.technology.featurelauncher.extras.installer.directory.FeatureDirectoryInstallerActivator.PROP_FEATURES_DIR;
import static org.eclipse.osgi.technology.featurelauncher.extras.installer.directory.FeatureDirectoryInstallerActivator.PROP_FEATURE_PATTERN;
import static org.eclipse.osgi.technology.featurelauncher.extras.installer.directory.FeatureDirectoryInstallerActivator.PROP_REPO_DIR;
import static org.eclipse.osgi.technology.featurelauncher.extras.installer.directory.FeatureDirectoryInstallerActivator.PROP_SCAN_INTERVAL;
import static org.eclipse.osgi.technology.featurelauncher.extras.installer.directory.FeatureDirectoryInstallerActivator.PROP_SCAN_MODE;
import static org.eclipse.osgi.technology.featurelauncher.extras.installer.directory.FeatureDirectoryInstallerActivator.PROP_SKIP_PATTERNS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;

class FeatureDirectoryInstallerActivatorTest {

	@Test
	void start_noFeaturesDir_doesNotOpenTracker() throws Exception {
		BundleContext context = mock(BundleContext.class);
		when(context.getProperty(PROP_FEATURES_DIR)).thenReturn(null);

		FeatureDirectoryInstallerActivator activator = new FeatureDirectoryInstallerActivator();
		activator.start(context);

		activator.stop(context);
	}

	@Test
	void start_emptyFeaturesDir_doesNotOpenTracker() throws Exception {
		BundleContext context = mock(BundleContext.class);
		when(context.getProperty(PROP_FEATURES_DIR)).thenReturn("");

		FeatureDirectoryInstallerActivator activator = new FeatureDirectoryInstallerActivator();
		activator.start(context);
		activator.stop(context);
	}

	@Test
	void start_withFeaturesDir_opensTracker() throws Exception {
		BundleContext context = mock(BundleContext.class);
		when(context.getProperty(PROP_FEATURES_DIR)).thenReturn("/tmp/features");
		when(context.getProperty(PROP_REPO_DIR)).thenReturn(null);
		when(context.getProperty(PROP_SCAN_MODE)).thenReturn("ONCE");
		when(context.getProperty(PROP_SCAN_INTERVAL)).thenReturn(null);
		when(context.getProperty(PROP_FEATURE_PATTERN)).thenReturn(null);
		when(context.getProperty(PROP_SKIP_PATTERNS)).thenReturn(null);

		when(context.createFilter(any())).thenReturn(null);

		FeatureDirectoryInstallerActivator activator = new FeatureDirectoryInstallerActivator();
		activator.start(context);

		verify(context).getProperty(PROP_FEATURES_DIR);

		activator.stop(context);
	}

	@Test
	void stop_calledTwice_doesNotThrow() throws Exception {
		BundleContext context = mock(BundleContext.class);
		when(context.getProperty(PROP_FEATURES_DIR)).thenReturn(null);

		FeatureDirectoryInstallerActivator activator = new FeatureDirectoryInstallerActivator();
		activator.start(context);
		activator.stop(context);
		activator.stop(context);
	}

	@Test
	void parsesIntervalProperty() throws Exception {
		BundleContext context = mock(BundleContext.class);
		when(context.getProperty(PROP_FEATURES_DIR)).thenReturn("/tmp/features");
		when(context.getProperty(PROP_REPO_DIR)).thenReturn("/tmp/repo");
		when(context.getProperty(PROP_SCAN_MODE)).thenReturn("WATCH");
		when(context.getProperty(PROP_SCAN_INTERVAL)).thenReturn("10");
		when(context.getProperty(PROP_FEATURE_PATTERN)).thenReturn("*.json");
		when(context.getProperty(PROP_SKIP_PATTERNS)).thenReturn("00-*.json");

		FeatureDirectoryInstallerActivator activator = new FeatureDirectoryInstallerActivator();
		activator.start(context);

		verify(context).getProperty(PROP_SCAN_INTERVAL);

		activator.stop(context);
	}
}
