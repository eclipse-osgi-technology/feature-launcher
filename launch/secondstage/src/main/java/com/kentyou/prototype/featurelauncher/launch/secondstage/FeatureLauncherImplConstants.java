/**
 * Copyright (c) 2024 Kentyou and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Kentyou - initial implementation
 */
package com.kentyou.prototype.featurelauncher.launch.secondstage;

/**
 * Additional constants, supplementing those defined in
 * {@link org.osgi.service.featurelauncher.FeatureLauncherConstants} and
 * {@link org.osgi.service.featurelauncher.runtime.FeatureRuntimeConstants}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 3, 2024
 */
public interface FeatureLauncherImplConstants {
	String FRAMEWORK_STORAGE_CLEAN_TESTONLY = "testOnly";
	
	String CONFIGURATION_ADMIN_IMPL_DEFAULT = "org.apache.felix:org.apache.felix.configadmin:1.9.26";
}
