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

import java.io.File;

import org.apache.maven.plugins.annotations.Parameter;

import aQute.bnd.maven.lib.configuration.Bndruns;
import aQute.bnd.maven.lib.configuration.Bundles;

/**
 * Defines sources for bundle resolution. Used by both {@link Include} and
 * {@link Exclude} to specify where bundles come from.
 */
public class Source {

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	File targetDir;

	/** Resolve bundles from Maven project dependencies. */
	@Parameter
	MavenSource maven;

	/** Resolve bundles from bndrun files. */
	@Parameter
	Bndruns bndruns = new Bndruns();

	@Parameter
	Bundles bundles = new Bundles();

	/** Resolve bundles from existing OSGi feature JSON files. */
	@Parameter
	Features features = new Features();
}
