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

public class ArtifactRepositoryAdapter implements ArtifactRepository {

	private final Repository repository;
	
	public ArtifactRepositoryAdapter(Repository repository) {
		this.repository = repository;
	}

	@Override
	public InputStream getArtifact(ID id) {
		return repository.getArtifactData(id);
	}

	public Repository unwrap() {
		return repository;
	}
}
