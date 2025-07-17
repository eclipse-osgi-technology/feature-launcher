package org.eclipse.osgi.technology.featurelauncher.feature.mavenplugin;

import org.apache.maven.lifecycle.mapping.DefaultLifecycleMapping;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = org.apache.maven.lifecycle.mapping.LifecycleMapping.class, hint = "osgi-feature")
public class OsgiFeatureLifecycleMapping extends DefaultLifecycleMapping {
	// Optionally override phase mappings, or leave empty to just "register" the
	// packaging type
}
