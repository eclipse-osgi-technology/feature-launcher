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
package org.eclipse.osgi.technology.featurelauncher.repository.common.osgi;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.osgi.technology.featurelauncher.repository.spi.RepositoryFactory;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory;

public class ArtifactRepositoryFactoryAdapter implements ArtifactRepositoryFactory {

	private final RepositoryFactory factory;
	
	public ArtifactRepositoryFactoryAdapter(RepositoryFactory factory) {
		this.factory = factory;
	}

	@Override
	public ArtifactRepository createRepository(Path path) {
		return new ArtifactRepositoryAdapter(factory.createRepository(path));
	}

	@Override
	public ArtifactRepository createRepository(URI uri, Map<String, Object> props) {
		return new ArtifactRepositoryAdapter(factory.createRepository(uri, props));
	}

}
