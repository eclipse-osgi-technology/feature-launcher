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
// Verify merged feature file exists
def mergedFile = new File(basedir, "target/merged-feature.json")
assert mergedFile.exists() : "Merged feature file was not created"

def content = mergedFile.text

// Variables: baseVar overridden, mergedVar added
assert content.contains('"mergedVar"') : "Merged variable 'mergedVar' not found"
assert content.contains('"mergedValue"') : "Merged variable value not found"
assert content.contains('"baseVar"') : "Base variable 'baseVar' not found"
assert content.contains('"overridden"') : "baseVar should be overridden by merge source"

// Configurations: both PIDs present
assert content.contains('"com.example.base"') : "Base configuration PID not found"
assert content.contains('"com.example.merged"') : "Merged configuration PID not found"

// Base metadata preserved
assert content.contains('"Base Feature"') || content.contains('Base Feature') : "Base feature name should be preserved"
