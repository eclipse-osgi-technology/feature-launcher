/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Stefan Bischof - initial
 */
import groovy.json.JsonSlurper

def featureFile = new File(basedir, "target/my-feature.json")
assert featureFile.exists() : "feature.json was not generated"

def json = new JsonSlurper().parse(featureFile)

// Verify feature-resource-version
assert json."feature-resource-version" == "1.0"

// Verify feature ID contains project coordinates
assert json.id.contains("configByFeature")
assert json.id.contains("1.0.0-SNAPSHOT")

// Verify bundles are present
assert json.bundles != null : "bundles section is missing"
assert json.bundles.size() > 0 : "no bundles found"

// Verify the declared dependency is included
def bundleIds = json.bundles.collect { it.id }
def hasHealthcheck = bundleIds.any { it.contains("org.apache.felix.healthcheck.generalchecks") }
assert hasHealthcheck : "Expected bundle org.apache.felix.healthcheck.generalchecks not found in: ${bundleIds}"

// Verify configuration section exists (configByFeature=true)
assert json.configuration != null : "configuration section is missing (configByFeature=true was set)"
