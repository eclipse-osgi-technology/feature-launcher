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
package com.kentyou.featurelauncher.common.decorator.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBuilder;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator.FeatureDecoratorBuilder;

/**
 * Implementation of {@link org.osgi.service.featurelauncher.decorator.FeatureDecorator.FeatureDecoratorBuilder}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 19, 2024
 */
public class FeatureDecoratorBuilderImpl extends AbstractBaseFeatureDecorationBuilder<FeatureDecoratorBuilder>
		implements FeatureDecoratorBuilder {
	private List<FeatureExtension> extensions;
	private Feature built;

	public FeatureDecoratorBuilderImpl(FeatureService featureService, Feature feature) {
		super(featureService, feature);

		this.extensions = new ArrayList<>();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.FeatureDecorator.FeatureDecoratorBuilder#setExtensions(java.util.List)
	 */
	@Override
	public FeatureDecoratorBuilder setExtensions(List<FeatureExtension> extensions) {
		Objects.requireNonNull(extensions, "Extensions cannot be null!");

		ensureNotBuiltYet();

		this.extensions = List.copyOf(extensions);

		return castThis();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.BaseFeatureDecorationBuilder#build()
	 */
	@Override
	public Feature build() {
		ensureNotBuiltYet();

		this.isBuilt = true;

		FeatureBuilder featureBuilder = prebuild();

		if (!extensions.isEmpty()) {
			featureBuilder.addExtensions(extensions.toArray(FeatureExtension[]::new));
		}

		built = featureBuilder.build();
		
		return built;
	}

	@Override
	public Feature getBuilt() {
		return built;
	}
}
