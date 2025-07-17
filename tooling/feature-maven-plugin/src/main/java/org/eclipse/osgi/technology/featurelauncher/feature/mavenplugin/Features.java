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
