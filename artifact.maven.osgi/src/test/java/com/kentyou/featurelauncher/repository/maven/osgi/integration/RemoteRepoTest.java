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
package com.kentyou.featurelauncher.repository.maven.osgi.integration;

import org.osgi.service.feature.FeatureService;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory;
import org.osgi.test.common.annotation.InjectService;

import com.kentyou.featurelauncher.repository.tests.RemoteArtifactRepositoryTest;

/**
 * Run the default tests for local repositories
 */
public class RemoteRepoTest extends RemoteArtifactRepositoryTest {

	@InjectService
	ArtifactRepositoryFactory arf;
	
	@InjectService
	FeatureService fs;
	
	@SuppressWarnings("unchecked")
	@Override
	protected Class<? extends ArtifactRepository> getRemoteArtifactRepoImplType() throws ClassNotFoundException {
		return (Class<? extends ArtifactRepository>) getClass().getClassLoader()
				.loadClass("com.kentyou.featurelauncher.repository.maven.impl.RemoteArtifactRepositoryImpl");
	}

	@Override
	protected ArtifactRepositoryFactory getArtifactRepositoryFactory() throws Exception {
		return arf;
	}

	@Override
	protected FeatureService getFeatureService() throws Exception {
		return fs;
	}

}
