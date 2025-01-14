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
package org.eclipse.osgi.technology.featurelauncher.repository.lite;

import org.eclipse.osgi.technology.featurelauncher.repository.lite.LiteRepositoryImpl;
import org.eclipse.osgi.technology.featurelauncher.repository.spi.Repository;
import org.eclipse.osgi.technology.featurelauncher.repository.tests.LocalRepositoryTest;

/**
 * Run the default tests for local repositories
 */
public class LocalRepoTest extends LocalRepositoryTest {

	@Override
	protected Class<? extends Repository> getLocalRepoImplType() {
		return LiteRepositoryImpl.class;
	}

}
