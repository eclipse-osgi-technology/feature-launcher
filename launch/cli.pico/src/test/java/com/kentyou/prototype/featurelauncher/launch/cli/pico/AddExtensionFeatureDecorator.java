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
package com.kentyou.prototype.featurelauncher.launch.cli.pico;

import java.util.List;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureExtension.Kind;
import org.osgi.service.feature.FeatureExtension.Type;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;

public class AddExtensionFeatureDecorator implements FeatureDecorator {

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.FeatureDecorator#decorate(org.osgi.service.feature.Feature, org.osgi.service.featurelauncher.decorator.FeatureDecorator.FeatureDecoratorBuilder, org.osgi.service.featurelauncher.decorator.DecoratorBuilderFactory)
	 */
	@Override
	public Feature decorate(Feature feature, FeatureDecoratorBuilder decoratedFeatureBuilder,
			DecoratorBuilderFactory factory) throws AbandonOperationException {
		return decoratedFeatureBuilder
				.setClassifier(feature.getID().getClassifier().orElse(null))
				.setExtensions(
					List.of(factory.newExtensionBuilder("testExtension", Type.TEXT, Kind.OPTIONAL)
						.addText("foobar").build()))
				.build();
	}
}