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
