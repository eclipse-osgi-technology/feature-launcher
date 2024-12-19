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
package com.kentyou.prototype.featurelauncher.repository.common.osgi;

import java.io.InputStream;

import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;

import com.kentyou.featurelauncher.repository.spi.Repository;

public class RepositoryAdapter implements Repository {

	private final ArtifactRepository delegate;
	private final String name;
	
	public RepositoryAdapter(ArtifactRepository repository) {
		this(repository, repository.toString());
	}

	public RepositoryAdapter(ArtifactRepository repository, String name) {
		this.delegate = repository;
		this.name = name;
	}

	@Override
	public InputStream getArtifactData(ID id) {
		return delegate.getArtifact(id);
	}
	
	@Override
	public String getName() {
		return name;
	}
}
