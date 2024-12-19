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

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * 160.2.1.2 Local Repositories
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
class LocalArtifactRepositoryImpl extends AbstractMavenArtifactRepositoryImpl {
	
	public LocalArtifactRepositoryImpl(Path localPath, Map<String, Object> configurationProperties) {
		super(localPath, () -> String.format("local-%s-%s", localPath, UUID.randomUUID()), 
				configurationProperties);
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "LocalArtifactRepositoryImpl [name=" + getName() + 
				", localRepositoryPath=" + getLocalRepositoryPath();
	}
}
