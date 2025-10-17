/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Stefan Bischof - initial implementation
 */

package org.eclipse.osgi.technology.featurelauncher.featureservice.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureBuilder;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.ID;

class FeatureBuilderImpl implements FeatureBuilder {

	private final ID id;

	private String name;
	private String description;
	private String docURL;
	private String license;
	private String scm;
	private String vendor;
	private boolean complete;

	private final List<FeatureBundle> bundles = new ArrayList<>();
	private final List<String> categories = new ArrayList<>();
	private final Map<String, FeatureConfiguration> configurations = new LinkedHashMap<>();
	private final Map<String, FeatureExtension> extensions = new LinkedHashMap<>();
	private final Map<String, Object> variables = new LinkedHashMap<>();

	FeatureBuilderImpl(ID id) {
		this.id = id;
	}

	@Override
	public FeatureBuilder setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public FeatureBuilder setDocURL(String url) {
		this.docURL = url;
		return this;
	}

	@Override
	public FeatureBuilder setVendor(String vendor) {
		this.vendor = vendor;
		return this;
	}

	@Override
	public FeatureBuilder setLicense(String license) {
		this.license = license;
		return this;
	}

	@Override
	public FeatureBuilder setComplete(boolean complete) {
		this.complete = complete;
		return this;
	}

	@Override
	public FeatureBuilder setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public FeatureBuilder setSCM(String scm) {
		this.scm = scm;
		return this;
	}

	@Override
	public FeatureBuilder addBundles(FeatureBundle... bundles) {
		this.bundles.addAll(Arrays.asList(bundles));
		return this;
	}

	@Override
	public FeatureBuilder addCategories(String... categories) {
		this.categories.addAll(Arrays.asList(categories));
		return this;
	}

	@Override
	public FeatureBuilder addConfigurations(FeatureConfiguration... configs) {
		for (FeatureConfiguration cfg : configs) {
			this.configurations.put(cfg.getPid(), cfg);
		}
		return this;
	}

	@Override
	public FeatureBuilder addExtensions(FeatureExtension... extensions) {
		for (FeatureExtension ex : extensions) {
			this.extensions.put(ex.getName(), ex);
		}
		return this;
	}

	@Override
	public FeatureBuilder addVariable(String key, Object value) {
		this.variables.put(key, value);
		return this;
	}

	@Override
	public FeatureBuilder addVariables(Map<String, Object> variables) {
		this.variables.putAll(variables);
		return this;
	}

	@Override
	public Feature build() {
		return new FeatureImpl(id, Optional.ofNullable(name), Optional.ofNullable(description),
		        Optional.ofNullable(docURL), Optional.ofNullable(license), Optional.ofNullable(scm),
		        Optional.ofNullable(vendor), complete, bundles, categories, configurations, extensions, variables);
	}

	private static record FeatureImpl(ID id, Optional<String> name, Optional<String> description,
	        Optional<String> docURL, Optional<String> license, Optional<String> scm, Optional<String> vendor,
	        boolean complete, List<FeatureBundle> bundles, List<String> categories,
	        Map<String, FeatureConfiguration> configurations, Map<String, FeatureExtension> extensions,
	        Map<String, Object> variables) implements Feature {

		private FeatureImpl(ID id, Optional<String> name, Optional<String> description, Optional<String> docURL,
		        Optional<String> license, Optional<String> scm, Optional<String> vendor, boolean complete,
		        List<FeatureBundle> bundles, List<String> categories, Map<String, FeatureConfiguration> configurations,
		        Map<String, FeatureExtension> extensions, Map<String, Object> variables) {
			this.id = id;
			this.name = name;
			this.description = description;
			this.docURL = docURL;
			this.license = license;
			this.scm = scm;
			this.vendor = vendor;
			this.complete = complete;

			this.bundles = List.copyOf(bundles);
			this.categories = List.copyOf(categories);
			this.configurations = Map.copyOf(configurations);
			this.extensions = Map.copyOf(extensions);
			this.variables = Collections.unmodifiableMap(variables);// null value allowed
		}

		@Override
		public ID getID() {
			return id;
		}

		@Override
		public Optional<String> getName() {
			return name;
		}

		@Override
		public Optional<String> getDescription() {
			return description;
		}

		@Override
		public Optional<String> getVendor() {
			return vendor;
		}

		@Override
		public Optional<String> getLicense() {
			return license;
		}

		@Override
		public Optional<String> getDocURL() {
			return docURL;
		}

		@Override
		public Optional<String> getSCM() {
			return scm;
		}

		@Override
		public boolean isComplete() {
			return complete;
		}

		@Override
		public List<FeatureBundle> getBundles() {
			return bundles;
		}

		@Override
		public List<String> getCategories() {
			return categories;
		}

		@Override
		public Map<String, FeatureConfiguration> getConfigurations() {
			return configurations;
		}

		@Override
		public Map<String, FeatureExtension> getExtensions() {
			return extensions;
		}

		@Override
		public Map<String, Object> getVariables() {
			return variables;
		}

	}
}