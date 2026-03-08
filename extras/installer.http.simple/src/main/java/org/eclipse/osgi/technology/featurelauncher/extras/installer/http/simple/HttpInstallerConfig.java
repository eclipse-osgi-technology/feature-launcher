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

import org.osgi.annotation.bundle.Capability;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
	pid = "$",
	name = "HTTP Installer Framework Properties",
	description = "Framework launch properties for the Feature HTTP Installer"
)
@Capability(
		namespace = "osgi.technology.featurelauncher.framework.properties",
		name = "${@class}"
)
interface HttpInstallerConfig {

	String PREFIX_ = "osgi.technology.featurelauncher.extras.installer.http.simple.";

	String PROP_PREFIX = PREFIX_;
	String PROP_FEATURES_URL = PROP_PREFIX + "features.url";
	String PROP_REPO_DIR = PROP_PREFIX + "repo.dir";
	String PROP_SCAN_MODE = PROP_PREFIX + "scan.mode";
	String PROP_SCAN_INTERVAL = PROP_PREFIX + "scan.interval";
	String PROP_CONNECT_TIMEOUT = PROP_PREFIX + "connect.timeout";
	String PROP_REQUEST_TIMEOUT = PROP_PREFIX + "request.timeout";

	long DEFAULT_SCAN_INTERVAL = 60;
	String DEFAULT_SCAN_MODE = "ONCE";
	long DEFAULT_CONNECT_TIMEOUT = 30;
	long DEFAULT_REQUEST_TIMEOUT = 60;

	@AttributeDefinition(name = "Features URL",
		description = "HTTP URL that returns a JSON array of feature objects with id and url, e.g. [{\"id\":\"g:a:v\",\"url\":\"http://...\"}]")
	String features_url();

	@AttributeDefinition(name = "Repository Directory",
		description = "Path to the local artifact repository",
		required = false)
	String repo_dir();

	@AttributeDefinition(name = "Scan Mode",
		description = "How to monitor the features URL",
		required = false, defaultValue = DEFAULT_SCAN_MODE,
		options = {
			@Option(label = "Once", value = "ONCE"),
			@Option(label = "Watch", value = "WATCH")
		})
	String scan_mode();

	@AttributeDefinition(name = "Scan Interval",
		description = "Polling interval in seconds (WATCH mode)",
		required = false, defaultValue = "60",
		type = AttributeType.LONG)
	long scan_interval();

	@AttributeDefinition(name = "Connect Timeout",
		description = "HTTP connect timeout in seconds",
		required = false, defaultValue = "30",
		type = AttributeType.LONG)
	long connect_timeout();

	@AttributeDefinition(name = "Request Timeout",
		description = "HTTP request timeout in seconds",
		required = false, defaultValue = "60",
		type = AttributeType.LONG)
	long request_timeout();
}
