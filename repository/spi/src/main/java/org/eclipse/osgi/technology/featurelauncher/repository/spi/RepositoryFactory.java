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
import java.nio.file.Path;
import java.util.Map;

public interface RepositoryFactory {
	
	/**
	 * Create a {@link Repository} using the local file system
	 * 
	 * @param path a path to the root of a Maven Repository Layout containing
	 *            installable artifacts
	 * @return an {@link ArtifactRepository} using the local file system
	 * @throws IllegalArgumentException if the path does not exist, or exists
	 *             and is not a directory
	 * @throws NullPointerException if the path is <code>null</code>
	 */
	Repository createRepository(Path path);

	/**
	 * Create a {@link Repository} using a potentially remote URI
	 * 
	 * @param uri the URI for the repository. The <code>http</code>,
	 *            <code>https</code> and <code>file</code> schemes must be
	 *            supported by all implementations.
	 * @param props the configuration properties for the remote repository. See
	 *            {@link RepositoryConstants} for standard property names
	 * @return a {@link Repository} using the supplied URI
	 * @throws IllegalArgumentException if the uri scheme is not supported by
	 *             this implementation
	 * @throws NullPointerException if the path is <code>null</code>
	 */
	Repository createRepository(URI uri, Map<String,Object> props);

}
