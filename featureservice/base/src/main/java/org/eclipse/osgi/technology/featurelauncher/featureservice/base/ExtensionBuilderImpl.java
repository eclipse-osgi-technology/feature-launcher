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
import java.util.List;

import org.osgi.service.feature.FeatureArtifact;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.FeatureExtension.Kind;
import org.osgi.service.feature.FeatureExtension.Type;
import org.osgi.service.feature.FeatureExtensionBuilder;

class ExtensionBuilderImpl implements FeatureExtensionBuilder {

	private final String name;
	private final Type type;
	private final Kind kind;

	private String json = null;
	private final List<String> textList = new ArrayList<>();
	private final List<FeatureArtifact> artifacts = new ArrayList<>();

	ExtensionBuilderImpl(String name, Type type, Kind kind) {
		this.name = name;
		this.type = type;
		this.kind = kind;
	}

	@Override
	public FeatureExtensionBuilder addText(String text) {
		if (type != Type.TEXT) {
			throw new IllegalStateException("Cannot add text to extension of type " + type);
		}

		textList.add(text);
		return this;
	}

	@Override
	public FeatureExtensionBuilder setJSON(String json) {
		if (type != Type.JSON) {
			throw new IllegalStateException("Cannot set json to extension of type " + type);
		}

		this.json = json;
		return this;
	}

	@Override
	public FeatureExtensionBuilder addArtifact(FeatureArtifact artifact) {
		if (type != Type.ARTIFACTS) {
			throw new IllegalStateException("Cannot add artifacts to extension of type " + type);
		}

		artifacts.add(artifact);
		return this;
	}

	@Override
	public FeatureExtension build() {
		return new ExtensionImpl(name, type, kind, json, textList, artifacts);
	}

	private static record ExtensionImpl(String name, Type type, Kind kind, String json, List<String> text,
	        List<FeatureArtifact> artifacts) implements FeatureExtension {

		private ExtensionImpl(String name, Type type, Kind kind, String json, List<String> text,
		        List<FeatureArtifact> artifacts) {
			this.name = name;
			this.type = type;
			this.kind = kind;
			this.json = json;
			this.text = List.copyOf(text);
			this.artifacts = List.copyOf(artifacts);
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Type getType() {
			return type;
		}

		@Override
		public Kind getKind() {
			return kind;
		}

		@Override
		public String getJSON() {
			if (type != Type.JSON) {
				throw new IllegalStateException("Extension is not of type JSON " + type);
			}

			if (json.isEmpty()) {
				return null;
			}

			return json;
		}

		@Override
		public List<String> getText() {
			if (type != Type.TEXT) {
				throw new IllegalStateException("Extension is not of type Text " + type);
			}
			return text;
		}

		@Override
		public List<FeatureArtifact> getArtifacts() {
			if (type != Type.ARTIFACTS) {
				throw new IllegalStateException("Extension is not of type Text " + type);
			}
			return artifacts;
		}

	}
}