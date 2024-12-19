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
package com.kentyou.prototype.featurelauncher.common.decorator.impl;

import static com.kentyou.prototype.featurelauncher.common.decorator.impl.FeatureDecorationConstants.DEFAULT_DECORATED_TYPE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBuilder;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.decorator.BaseFeatureDecorationBuilder;

/**
 * Implementation of {@link org.osgi.service.featurelauncher.decorator.BaseFeatureDecorationBuilder<T>}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 18, 2024
 */
public abstract class AbstractBaseFeatureDecorationBuilder<T extends BaseFeatureDecorationBuilder<T>>
		implements BaseFeatureDecorationBuilder<T> {
	protected final FeatureService featureService;
	protected final Feature originalFeature;
	protected boolean isBuilt;
	protected List<FeatureBundle> bundles;
	protected List<FeatureConfiguration> configs;
	protected Map<String, Object> variables;
	protected String classifier;

	public AbstractBaseFeatureDecorationBuilder(FeatureService featureService, Feature feature) {
		Objects.requireNonNull(featureService, "Feature Service cannot be null!");
		Objects.requireNonNull(feature, "Feature cannot be null!");

		this.featureService = featureService;
		this.originalFeature = feature;
		this.isBuilt = false;
		this.bundles = new ArrayList<>();
		this.configs = new ArrayList<>();
		this.variables = new HashMap<>();
		this.classifier = DEFAULT_DECORATED_CLASSIFIER;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.BaseFeatureDecorationBuilder#setBundles(java.util.List)
	 */
	@Override
	public T setBundles(List<FeatureBundle> bundles) {
		Objects.requireNonNull(bundles, "Bundles cannot be null!");

		ensureNotBuiltYet();

		this.bundles = List.copyOf(bundles);

		return castThis();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.BaseFeatureDecorationBuilder#setConfigurations(java.util.List)
	 */
	@Override
	public T setConfigurations(List<FeatureConfiguration> configs) {
		Objects.requireNonNull(bundles, "Configurations cannot be null!");

		ensureNotBuiltYet();

		this.configs = List.copyOf(configs);

		return castThis();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.BaseFeatureDecorationBuilder#setVariable(java.lang.String, java.lang.Object)
	 */
	@Override
	public T setVariable(String key, Object defaultValue) {
		Objects.requireNonNull(key, "Variable key cannot be null!");
		Objects.requireNonNull(defaultValue, "Variable default value cannot be null!");

		ensureNotBuiltYet();

		this.variables.put(key, defaultValue);

		return castThis();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.BaseFeatureDecorationBuilder#setVariables(java.util.Map)
	 */
	@Override
	public T setVariables(Map<String, Object> variables) {
		Objects.requireNonNull(variables, "Variables cannot be null!");

		ensureNotBuiltYet();

		this.variables = new HashMap<>(variables);

		return castThis();
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.decorator.BaseFeatureDecorationBuilder#setClassifier(java.lang.String)
	 */
	@Override
	public T setClassifier(String classifier) {
		// Classifier can be null
		ensureNotBuiltYet();

		this.classifier = classifier;

		return castThis();
	}

	protected FeatureBuilder prebuild() {
		ID originalFeatureID = originalFeature.getID();

		ID decoratedFeatureID;
		if(classifier == null) {
			decoratedFeatureID = originalFeatureID.getType()
					.map(t -> featureService.getID(originalFeatureID.getGroupId(), 
							originalFeatureID.getArtifactId(), originalFeatureID.getVersion(), t))
					.orElseGet(() -> featureService.getID(originalFeatureID.getGroupId(), 
							originalFeatureID.getArtifactId(), originalFeatureID.getVersion()));
		} else {
			decoratedFeatureID = featureService.getID(originalFeatureID.getGroupId(), 
					originalFeatureID.getArtifactId(), originalFeatureID.getVersion(),
					originalFeatureID.getType().orElse(DEFAULT_DECORATED_TYPE), classifier);
		}

		FeatureBuilder featureBuilder = featureService.getBuilderFactory().newFeatureBuilder(decoratedFeatureID);

		if (!bundles.isEmpty()) {
			featureBuilder.addBundles(bundles.toArray(FeatureBundle[]::new));
		} else if (originalFeature.getBundles() != null) {
			featureBuilder.addBundles(originalFeature.getBundles().toArray(FeatureBundle[]::new));
		}

		if (!configs.isEmpty()) {
			featureBuilder.addConfigurations(configs.toArray(FeatureConfiguration[]::new));
		} else if (originalFeature.getConfigurations() != null) {
			featureBuilder.addConfigurations(
					originalFeature.getConfigurations().values().toArray(FeatureConfiguration[]::new));
		}

		if (!variables.isEmpty()) {
			featureBuilder.addVariables(variables);
		} else if (originalFeature.getVariables() != null) {
			featureBuilder.addVariables(originalFeature.getVariables());
		}

		featureBuilder.setComplete(originalFeature.isComplete());

		if (originalFeature.getDescription().isPresent()) {
			featureBuilder.setDescription(originalFeature.getDescription().get());
		}

		if (originalFeature.getDocURL().isPresent()) {
			featureBuilder.setDocURL(originalFeature.getDocURL().get());
		}

		if (originalFeature.getName().isPresent()) {
			featureBuilder.setName(originalFeature.getName().get());
		}

		if (originalFeature.getLicense().isPresent()) {
			featureBuilder.setLicense(originalFeature.getLicense().get());
		}

		if (originalFeature.getSCM().isPresent()) {
			featureBuilder.setSCM(originalFeature.getSCM().get());
		}

		if (originalFeature.getVendor().isPresent()) {
			featureBuilder.setVendor(originalFeature.getVendor().get());
		}

		if (originalFeature.getCategories() != null && !originalFeature.getCategories().isEmpty()) {
			featureBuilder.addCategories(originalFeature.getCategories().toArray(String[]::new));
		}

		if (originalFeature.getExtensions() != null & !originalFeature.getExtensions().isEmpty()) {
			featureBuilder.addExtensions(originalFeature.getExtensions().values().toArray(FeatureExtension[]::new));
		}

		return featureBuilder;
	}

	@SuppressWarnings("unchecked")
	protected T castThis() {
		return (T) this;
	}

	protected void ensureNotBuiltYet() {
		if (this.isBuilt == true) {
			throw new IllegalStateException("Feature already built!");
		}
	}
	
	public abstract Feature getBuilt();
}
