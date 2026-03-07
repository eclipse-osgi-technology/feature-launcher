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

import org.osgi.annotation.bundle.Capability;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
	pid="$",
	name = "Directory Installer Framework Properties",
	description = "Framework launch properties for the Feature Directory Installer"
)
@Capability(
		namespace = "osgi.technology.featurelauncher.framework.properties",
		name = "${@class}"
)
interface DirectoryInstallerConfig {

	String PREFIX_ = "osgi.technology.featurelauncher.extras.installer.directory.";

	String PROP_PREFIX = PREFIX_;
	String PROP_FEATURES_DIR = PROP_PREFIX + "features.dir";
	String PROP_REPO_DIR = PROP_PREFIX + "repo.dir";
	String PROP_SCAN_MODE = PROP_PREFIX + "scan.mode";
	String PROP_SCAN_INTERVAL = PROP_PREFIX + "scan.interval";
	String PROP_FEATURE_PATTERN = PROP_PREFIX + "feature.pattern";
	String PROP_SKIP_PATTERNS = PROP_PREFIX + "skip.patterns";

	long DEFAULT_SCAN_INTERVAL = 10;
	String DEFAULT_SCAN_MODE = "ONCE";
	String DEFAULT_FEATURE_PATTERN = "*.json";
	String DEFAULT_SKIP_PATTERNS = "00-*.json,bootstrap.json";

	@AttributeDefinition(name = "Features Directory",
		description = "Path to the directory containing feature JSON files")
	String features_dir();

	@AttributeDefinition(name = "Repository Directory",
		description = "Path to the local artifact repository",
		required = false)
	String repo_dir();

	@AttributeDefinition(name = "Scan Mode",
		description = "How to monitor the features directory",
		required = false, defaultValue = DEFAULT_SCAN_MODE,
		options = {
			@Option(label = "Once", value = "ONCE"),
			@Option(label = "Watch", value = "WATCH")
		})
	String scan_mode();

	@AttributeDefinition(name = "Scan Interval",
		description = "Polling interval in seconds (WATCH mode)",
		required = false, defaultValue = "10",
		type = AttributeType.LONG)
	long scan_interval();

	@AttributeDefinition(name = "Feature Pattern",
		description = "Glob pattern for feature files",
		required = false, defaultValue = DEFAULT_FEATURE_PATTERN)
	String feature_pattern();

	@AttributeDefinition(name = "Skip Patterns",
		description = "Comma-separated patterns to skip",
		required = false, defaultValue = DEFAULT_SKIP_PATTERNS)
	String skip_patterns();
}
