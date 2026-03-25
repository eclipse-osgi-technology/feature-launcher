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

import java.nio.file.Path;
import java.util.Optional;
import java.util.StringJoiner;

import org.eclipse.osgi.technology.featurelauncher.featureservice.base.IDImpl;
import org.osgi.service.feature.ID;

public class GavFromFile {

	/**
	 * Parse a full artifact path such as
	 * /home/alice/.m2/repository/com/example/demo/2.0.1/demo-2.0.1-sources.jar
	 *
	 * @param artifactFile  the absolute path to the artifact file
	 * @param localRepoPath the absolute path to the local Maven repository root
	 * @return the parsed Maven ID
	 * @throws IllegalArgumentException if the path is not inside the local repo or
	 *                                  too short
	 */
	public static ID parseIDFromPath(Path artifactFile, Path localRepoPath) {

		boolean isSubdir = artifactFile.normalize().startsWith(localRepoPath.normalize());
		if (!isSubdir) {
			throw new IllegalArgumentException(
					"Path is not a subdirectory of the local Maven repository: " + artifactFile);
		}
		artifactFile = localRepoPath.relativize(artifactFile);
		int parts = artifactFile.getNameCount();
		if (parts < 4) { // need …/<aid>/<ver>/<file>
			throw new IllegalArgumentException("Path too short for Maven layout: " + artifactFile);
		}

		// last three meaningful segments
		String fileName = artifactFile.getFileName().toString();
		String version = artifactFile.getName(parts - 2).toString();
		String artifactId = artifactFile.getName(parts - 3).toString();

		// groupId = every segment before artifactId, joined with '.'
		StringJoiner group = new StringJoiner(".");
		for (int i = 0; i < parts - 3; i++) {
			group.add(artifactFile.getName(i).toString());
		}
		String groupId = group.toString();

		// ----- classify and type -----
		int dot = fileName.lastIndexOf('.');
		String type = (dot >= 0) ? fileName.substring(dot + 1) : null;
		String base = (dot >= 0) ? fileName.substring(0, dot) : fileName;

		String prefix = artifactId + "-" + version; // mandatory prefix
		String classifier = (base.length() > prefix.length() + 1) ? base.substring(prefix.length() + 1) : null;

		return new IDImpl(groupId, artifactId, version, Optional.ofNullable(type), Optional.ofNullable(classifier));
	}
}
