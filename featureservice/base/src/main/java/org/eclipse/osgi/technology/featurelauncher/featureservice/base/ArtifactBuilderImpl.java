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

import org.osgi.service.feature.FeatureArtifact;
import org.osgi.service.feature.FeatureArtifactBuilder;
import org.osgi.service.feature.ID;

class ArtifactBuilderImpl implements FeatureArtifactBuilder {

	private final ID id;

	private final Map<String, Object> metadata = new LinkedHashMap<>();

	ArtifactBuilderImpl(ID id) {
		this.id = id;
	}

	@Override
	public FeatureArtifactBuilder addMetadata(String key, Object value) {
		if (key == null) {
			throw new IllegalArgumentException("Metadata key cannot be null");
		}

		if (key.length() == 0) {
			throw new IllegalArgumentException("Key must not be empty");
		}

		if ("id".equalsIgnoreCase(key)) {
			throw new IllegalArgumentException("Key cannot be 'id'");
		}

		checkMetadataValueType(value);

		this.metadata.put(key, value);
		return this;
	}

	@Override
	public FeatureArtifactBuilder addMetadata(Map<String, Object> metadata) {

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
	public FeatureArtifact build() {
		return new ArtifactImpl(id, metadata);
	}

	private static record ArtifactImpl(ID id, Map<String, Object> metadata) implements FeatureArtifact {

		private ArtifactImpl(ID id, Map<String, Object> metadata) {
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