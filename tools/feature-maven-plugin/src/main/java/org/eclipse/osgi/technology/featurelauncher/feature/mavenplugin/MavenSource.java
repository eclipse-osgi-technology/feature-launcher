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
package org.eclipse.osgi.technology.featurelauncher.feature.mavenplugin;

import java.util.Set;

import org.apache.maven.plugins.annotations.Parameter;

import aQute.bnd.maven.lib.resolve.Scope;
import aQute.bnd.unmodifiable.Sets;

public class MavenSource {
	@Parameter(defaultValue = "true")
	boolean mavenDependencies;
	
	@Parameter(property = "dependency.include.dependency.management", defaultValue = "false")
	boolean includeDependencyManagement;

	@Parameter(property = "dependency.scopes", defaultValue = "compile,runtime")
	Set<Scope> scopes = Sets.of(Scope.compile, Scope.runtime);
}
