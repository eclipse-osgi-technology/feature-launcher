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
package com.kentyou.featurelauncher.repository.maven.osgi;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory;

import com.kentyou.featurelauncher.repository.maven.impl.ArtifactRepositoryFactoryImpl;

@Component
public class ArtifactRepositoryFactoryOSGi extends ArtifactRepositoryFactoryImpl implements ArtifactRepositoryFactory {

}
