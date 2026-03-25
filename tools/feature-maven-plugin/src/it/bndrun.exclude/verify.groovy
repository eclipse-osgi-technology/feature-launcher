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

def featureFile = new File(basedir, "target/feature.json")
assert featureFile.exists() : "feature.json was not generated"

def json = new JsonSlurper().parse(featureFile)

// Verify feature-resource-version
assert json."feature-resource-version" == "1.0"

// Verify feature ID contains project coordinates
assert json.id.contains("bndrun.exclude")
assert json.id.contains("1.0.0-SNAPSHOT")

// Verify bundles are present
assert json.bundles != null : "bundles section is missing"
assert json.bundles.size() == 1 : "Expected exactly 1 bundle after exclude, but got ${json.bundles.size()}: ${json.bundles.collect { it.id }}"

// Verify only slf4j-simple remains (slf4j-api was excluded)
def bundleIds = json.bundles.collect { it.id }
def hasSlfSimple = bundleIds.any { it.contains("slf4j-simple") || it.contains("slf4j.simple") }
assert hasSlfSimple : "Expected slf4j-simple bundle not found in: ${bundleIds}"

def hasSlfApi = bundleIds.any { (it.contains("slf4j-api") || it.contains("slf4j.api")) && !it.contains("simple") }
assert !hasSlfApi : "slf4j-api should have been excluded but was found in: ${bundleIds}"
