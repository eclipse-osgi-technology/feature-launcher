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
// Verify original feature.json generated
def featureFile = new File(basedir, "target/my-feature.json")
assert featureFile.exists() : "Feature JSON was not generated"

// Verify repo directory with JARs in Maven GAV layout
def repoDir = new File(basedir, "target/repo")
assert repoDir.exists() : "Repo directory was not created"
assert repoDir.isDirectory() : "repo is not a directory"

def jars = []
repoDir.eachFileRecurse(groovy.io.FileType.FILES) { file ->
    if (file.name.endsWith(".jar")) {
        jars << file
    }
}
assert jars.size() > 0 : "No JAR files found in repo directory"

// Verify at least one JAR is in proper GAV layout (org/osgi/...)
def hasGavLayout = jars.any { it.path.contains("org") && it.path.contains("osgi") }
assert hasGavLayout : "JARs not in Maven GAV layout"

// Verify features directory with feature.json copy
def featuresDir = new File(basedir, "target/features")
assert featuresDir.exists() : "Features directory was not created"
def featureCopy = new File(featuresDir, "my-feature.json")
assert featureCopy.exists() : "Feature JSON was not copied to features directory"

// Verify content
def content = featureCopy.text
assert content.contains('"bundles"') : "Feature has no bundles section"
assert content.contains('org.osgi.namespace.service') : "Expected bundle not found"
