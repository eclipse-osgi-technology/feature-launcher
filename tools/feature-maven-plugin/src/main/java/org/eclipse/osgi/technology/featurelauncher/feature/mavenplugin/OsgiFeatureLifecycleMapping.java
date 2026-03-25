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

import org.apache.maven.lifecycle.mapping.DefaultLifecycleMapping;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = org.apache.maven.lifecycle.mapping.LifecycleMapping.class, hint = "osgi-feature")
public class OsgiFeatureLifecycleMapping extends DefaultLifecycleMapping {
	// Optionally override phase mappings, or leave empty to just "register" the
	// packaging type
}
