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
package com.kentyou.featurelauncher.repository.maven.impl;

import org.osgi.service.featurelauncher.repository.ArtifactRepository;

import com.kentyou.featurelauncher.repository.tests.LocalArtifactRepositoryTest;

/**
 * Tests
 * {@link com.kentyou.featurelauncher.repository.maven.impl.LocalArtifactRepositoryImpl}
 * and
 * {@link com.kentyou.featurelauncher.repository.maven.impl.ArtifactRepositoryFactoryImpl}
 * 
 * As defined in: "160.2.1.2 Local Repositories"
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 17, 2024
 */
public class LocalArtifactRepositoryImplTest extends LocalArtifactRepositoryTest{

	@Override
	protected Class<? extends ArtifactRepository> getLocalArtifactRepoImplType() {
		return LocalArtifactRepositoryImpl.class;
	}

}
