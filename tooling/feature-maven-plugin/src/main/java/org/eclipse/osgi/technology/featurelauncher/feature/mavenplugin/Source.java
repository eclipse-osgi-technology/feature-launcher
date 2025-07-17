package org.eclipse.osgi.technology.featurelauncher.feature.mavenplugin;

import java.io.File;

import org.apache.maven.plugins.annotations.Parameter;

import aQute.bnd.maven.lib.configuration.Bndruns;
import aQute.bnd.maven.lib.configuration.Bundles;

public class Source {

	@Parameter(defaultValue = "${project.build.directory}", readonly = true)
	File targetDir;
	
	@Parameter
	MavenSource maven;
	
	
	//in bndRunSource
	@Parameter
	Bndruns bndruns = new Bndruns();

	@Parameter
	Bundles bundles = new Bundles();

	@Parameter
	Bndruns features = new Bndruns();





}
