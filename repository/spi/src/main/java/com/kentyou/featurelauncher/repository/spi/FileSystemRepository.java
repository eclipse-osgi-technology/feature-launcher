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
package com.kentyou.featurelauncher.repository.spi;

import java.nio.file.Path;

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.feature.ID;

/**
 * Defines additional method(s) for file system repository
 * implementations, where artifacts are stored in file system.
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 26, 2024
 */
@ConsumerType
public interface FileSystemRepository extends Repository {

	public Path getArtifactPath(ID id);

	public Path getLocalRepositoryPath();
}
