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
package com.kentyou.featurelauncher.repository.lite.impl.integration;

import org.osgi.service.feature.FeatureService;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory;
import org.osgi.test.common.annotation.InjectService;

import com.kentyou.featurelauncher.repository.tests.LocalArtifactRepositoryTest;

/**
 * Run the default tests for local repositories
 */
public class LocalRepoTest extends LocalArtifactRepositoryTest {

	@InjectService
	ArtifactRepositoryFactory arf;
	
	@InjectService
	FeatureService fs;
	
	@SuppressWarnings("unchecked")
	@Override
	protected Class<? extends ArtifactRepository> getLocalArtifactRepoImplType() throws ClassNotFoundException {
		return (Class<? extends ArtifactRepository>) getClass().getClassLoader()
				.loadClass("com.kentyou.featurelauncher.repository.lite.impl.LocalArtifactRepositoryImpl");
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
