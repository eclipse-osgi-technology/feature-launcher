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
package org.eclipse.osgi.technology.featurelauncher.repository.spi;

import java.io.InputStream;

import org.osgi.annotation.versioning.ConsumerType;
import org.osgi.service.feature.ID;

/**
 * Provides a repository interface which is free from OSGi dependencies
 * 
 * @author Timothy Ward (timothyjward@apache.org)
 * @since Dec 18, 2024
 */
@ConsumerType
public interface Repository {

	public InputStream getArtifactData(ID id);

	public String getName();
}
