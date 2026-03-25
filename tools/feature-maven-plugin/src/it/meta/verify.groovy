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
assert json.id.contains("meta")
assert json.id.contains("1.0.0-SNAPSHOT")

// Verify metadata from pom.xml
assert json.name == "TheName" : "Expected name 'TheName' but got '${json.name}'"
assert json.description == "TheDescription" : "Expected description 'TheDescription' but got '${json.description}'"
assert json.scm == "theScmUrl" : "Expected scm 'theScmUrl' but got '${json.scm}'"
assert json.vendor == "TheOrganisation" : "Expected vendor 'TheOrganisation' but got '${json.vendor}'"
assert json.docURL == "TheIssueManagement" : "Expected docURL 'TheIssueManagement' but got '${json.docURL}'"
assert json.license == "TheLicense1,TheLicense2" : "Expected license 'TheLicense1,TheLicense2' but got '${json.license}'"

// Verify categories
assert json.categories != null : "categories section is missing"
assert json.categories.contains("Foo") : "Expected category 'Foo' not found"
assert json.categories.contains("bar") : "Expected category 'bar' not found"
assert json.categories.contains("buzz") : "Expected category 'buzz' not found"
assert json.categories.size() == 3 : "Expected 3 categories but got ${json.categories.size()}"
