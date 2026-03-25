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
assert json.id.contains("var")
assert json.id.contains("1.0.0-SNAPSHOT")

// Verify variables section exists
assert json.variables != null : "variables section is missing"

// Non-typed variables default to String
assert json.variables.NonTypeInt == "1" : "Expected NonTypeInt='1' (String) but got '${json.variables.NonTypeInt}'"
assert json.variables.NonTypeDouble == "1.111" : "Expected NonTypeDouble='1.111' (String) but got '${json.variables.NonTypeDouble}'"

// String-typed variables
assert json.variables.StringTypeInt == "1" : "Expected StringTypeInt='1' but got '${json.variables.StringTypeInt}'"
assert json.variables.StringTypeDouble == "1.111" : "Expected StringTypeDouble='1.111' but got '${json.variables.StringTypeDouble}'"

// Integer-typed variable
assert json.variables.IntegerTypeInt == 1 : "Expected IntegerTypeInt=1 (Integer) but got '${json.variables.IntegerTypeInt}'"

// Double-typed variables
assert json.variables.DoubleTypeInt instanceof Number : "Expected DoubleTypeInt to be a Number"
assert json.variables.DoubleTypeDouble instanceof Number : "Expected DoubleTypeDouble to be a Number"

// Boolean-typed variable
assert json.variables.BooleanTypeInt == true : "Expected BooleanTypeInt=true but got '${json.variables.BooleanTypeInt}'"
