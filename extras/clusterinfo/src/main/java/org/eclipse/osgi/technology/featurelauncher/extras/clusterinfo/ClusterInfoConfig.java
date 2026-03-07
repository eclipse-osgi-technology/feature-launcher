/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.osgi.technology.featurelauncher.extras.clusterinfo;

import org.osgi.annotation.bundle.Capability;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
	pid="$",
	name = "Cluster Info Framework Properties",
	description = "Framework launch properties for the ClusterInfo service"
)
@Capability(
		namespace = "osgi.technology.featurelauncher.framework.properties",
		name = "${@class}"
)
interface ClusterInfoConfig {

	String PREFIX_ = "osgi.clusterinfo.";

	String PROP_PREFIX = PREFIX_;
	String PROP_CLUSTER = PROP_PREFIX + "cluster";
	String PROP_ENDPOINT = PROP_PREFIX + "endpoint";
	String PROP_VENDOR = PROP_PREFIX + "vendor";
	String PROP_VERSION = PROP_PREFIX + "version";
	String PROP_COUNTRY = PROP_PREFIX + "country";
	String PROP_LOCATION = PROP_PREFIX + "location";
	String PROP_REGION = PROP_PREFIX + "region";
	String PROP_ZONE = PROP_PREFIX + "zone";

	String DEFAULT_CLUSTER = "default";

	@AttributeDefinition(name = "Cluster",
		description = "Cluster identifier",
		required = false, defaultValue = DEFAULT_CLUSTER)
	String cluster();

	@AttributeDefinition(name = "Endpoint",
		description = "Endpoint URL",
		required = false)
	String endpoint();

	@AttributeDefinition(name = "Vendor",
		description = "Vendor information",
		required = false)
	String vendor();

	@AttributeDefinition(name = "Version",
		description = "Version information",
		required = false)
	String version();

	@AttributeDefinition(name = "Country",
		description = "Country location (ISO 3166)",
		required = false)
	String country();

	@AttributeDefinition(name = "Location",
		description = "Physical location",
		required = false)
	String location();

	@AttributeDefinition(name = "Region",
		description = "Region identifier",
		required = false)
	String region();

	@AttributeDefinition(name = "Zone",
		description = "Zone identifier",
		required = false)
	String zone();
}
