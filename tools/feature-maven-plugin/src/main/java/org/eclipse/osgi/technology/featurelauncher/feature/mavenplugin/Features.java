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

import aQute.bnd.maven.lib.configuration.FileTree;

public class Features extends FileTree {
	public Features() {
	}

	/**
	 * Add a Features file.
	 *
	 * @param feature A Features file. A relative path is relative to the project
	 *                base directory.
	 */
	public void setFeatures(File feature) {
		addFile(feature);
	}
}
