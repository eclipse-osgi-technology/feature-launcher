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
package com.kentyou.featurelauncher.repository.spi;

import java.net.URI;

/**
 * Defines additional constants for artifact repositories, supplementing those
 * defined in {@link org.osgi.service.featurelauncher.FeatureLauncherConstants}
 * and {@link org.osgi.service.featurelauncher.runtime.FeatureRuntimeConstants}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 25, 2024
 */
public class ArtifactRepositoryConstants {
	
	/**
	 * Non-instantiable constants class
	 */
	private ArtifactRepositoryConstants() {}
	
	public static final String REMOTE_ARTIFACT_REPOSITORY_TYPE = "type";

	public static final String DEFAULT_REMOTE_ARTIFACT_REPOSITORY_TYPE = "default";

	public static final URI REMOTE_ARTIFACT_REPOSITORY_URI = URI.create("https://repo1.maven.org/maven2/");

	public static final String LOCAL_ARTIFACT_REPOSITORY_PATH = "localRepositoryPath";

	public static final String DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME = "local";

	public static final String DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME = "central";
}
