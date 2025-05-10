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

import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureBundleBuilder;
import org.osgi.service.feature.ID;

class BundleBuilderImpl implements FeatureBundleBuilder {

	private final ID id;

	private final Map<String, Object> metadata = new LinkedHashMap<>();

	BundleBuilderImpl(ID id) {
		this.id = id;
	}

	@Override
	public FeatureBundleBuilder addMetadata(String key, Object value) {
		if (key == null) {
			throw new IllegalArgumentException("Metadata key cannot be null");
		}

		if (value == null) {
			throw new IllegalArgumentException("Metadata value cannot be null");
		}

		if ("id".equalsIgnoreCase(key)) {
			throw new IllegalArgumentException("Key cannot be 'id'");
		}

		checkMetadataValueType(value);

		this.metadata.put(key, value);
		return this;
	}

	@Override
	public FeatureBundleBuilder addMetadata(Map<String, Object> metadata) {
		metadata.forEach((k, v) -> addMetadata(k, v));
		return this;
	}

	private void checkMetadataValueType(Object value) {
		if ((value instanceof String) || (value instanceof Boolean) || (value instanceof Number)) {
			return;
		}

		throw new IllegalArgumentException("Illegal metadata value: " + value);
	}

	@Override
	public FeatureBundle build() {
		return new BundleImpl(id, metadata);
	}

	private static record BundleImpl(ID id, Map<String, Object> metadata) implements FeatureBundle {

		private BundleImpl(ID id, Map<String, Object> metadata) {
			this.id = id;
			this.metadata = Map.copyOf(metadata);
		}

		@Override
		public ID getID() {
			return id;
		}

		@Override
		public Map<String, Object> getMetadata() {
			return metadata;
		}

	}
}