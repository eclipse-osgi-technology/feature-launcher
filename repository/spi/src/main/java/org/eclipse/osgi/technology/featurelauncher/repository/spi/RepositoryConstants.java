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
package org.eclipse.osgi.technology.featurelauncher.repository.spi;

import java.net.URI;

/**
 * Defines constants for repositories, supplementing or restating those
 * defined in the main API
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 25, 2024
 */
public class RepositoryConstants {
	
	/**
	 * Non-instantiable constants class
	 */
	private RepositoryConstants() {}
	
	/**
	 * Duplicates the name property from the real API
	 */
	public static final String ARTIFACT_REPOSITORY_NAME = "name";
	
	public static final String REMOTE_ARTIFACT_REPOSITORY_TYPE = "type";

	public static final String DEFAULT_REMOTE_ARTIFACT_REPOSITORY_TYPE = "default";

	public static final URI REMOTE_ARTIFACT_REPOSITORY_URI = URI.create("https://repo1.maven.org/maven2/");

	public static final String LOCAL_ARTIFACT_REPOSITORY_PATH = "localRepositoryPath";

	public static final String DEFAULT_LOCAL_ARTIFACT_REPOSITORY_NAME = "local";

	public static final String DEFAULT_REMOTE_ARTIFACT_REPOSITORY_NAME = "central";
}
