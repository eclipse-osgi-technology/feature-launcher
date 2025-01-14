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
package org.eclipse.osgi.technology.featurelauncher.repository.artifact.lite.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.osgi.technology.featurelauncher.repository.artifact.lite.LiteArtifactRepositoryFactory;
import org.junit.jupiter.api.Test;
import org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory;
import org.osgi.test.common.annotation.InjectService;

/**
 * Run the basic tests for local repositories
 */
public class OSGiServiceTest {
	@Test
	public void testArtifactRepoImplType(@InjectService ArtifactRepositoryFactory arf) {
		assertTrue(LiteArtifactRepositoryFactory.class.isInstance(arf));
	}
}
