/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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
@Capability(namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE, name = FeatureLauncherConstants.FEATURE_LAUNCHER_IMPLEMENTATION, version = FeatureLauncherConstants.FEATURE_LAUNCHER_SPECIFICATION_VERSION,uses = org.osgi.service.featurelauncher.FeatureLauncher.class)
package org.eclipse.osgi.technology.featurelauncher.impl.runtime;

import org.osgi.annotation.bundle.Capability;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.service.featurelauncher.FeatureLauncherConstants;
