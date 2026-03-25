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
// Verify feature file exists
def featureFile = new File(basedir, "target/my-feature.json")
assert featureFile.exists() : "Feature JSON was not generated"

def content = featureFile.text

// Verify bundles are present
assert content.contains('"bundles"') : "Feature JSON has no bundles section"
assert content.contains('org.osgi.namespace.service') : "Expected bundle not found"

// Verify hash entries exist (SHA-256 enabled)
assert content.contains('"hash"') || content.contains('"sha-256"') || content.contains('"SHA-256"') :
    "No hash entries found in feature JSON (bundleHashSha256 was enabled)"
