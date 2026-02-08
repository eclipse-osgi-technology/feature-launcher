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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.osgi.technology.featurelauncher.featureservice.base.external.CommentRemovingReader;
import org.eclipse.osgi.technology.featurelauncher.featureservice.base.external.TypeConverter;
import org.osgi.service.feature.BuilderFactory;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureArtifact;
import org.osgi.service.feature.FeatureArtifactBuilder;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureConfigurationBuilder;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.FeatureExtensionBuilder;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.feature.ID;

import com.grack.nanojson.JsonAppendableWriter;
import com.grack.nanojson.JsonParserException;
import com.grack.nanojson.JsonReader;
import com.grack.nanojson.JsonSink;
import com.grack.nanojson.JsonStringWriter;
import com.grack.nanojson.JsonWriter;

public class FeatureServiceImpl implements FeatureService {
	private final BuilderFactoryImpl builderFactory = new BuilderFactoryImpl();

	@Override
	public BuilderFactory getBuilderFactory() {
		return builderFactory;
	}

	@Override
	public ID getIDfromMavenCoordinates(String mavenID) {
		return IDImpl.fromMavenID(mavenID);
	}

	@Override
	public ID getID(String groupId, String artifactId, String version) {
		return new IDImpl(groupId, artifactId, version, Optional.empty(), Optional.empty());
	}

	@Override
	public ID getID(String groupId, String artifactId, String version, String type) {
		if (type == null) {
			throw new NullPointerException("type must not be null");
		}

		return new IDImpl(groupId, artifactId, version, Optional.of(type), Optional.empty());
	}

	@Override
	public ID getID(String groupId, String artifactId, String version, String type, String classifier) {
		if (type == null) {
			throw new NullPointerException("type must not be null");
		}
		if (classifier == null) {
			throw new NullPointerException("classifier must not be null");
		}

		return new IDImpl(groupId, artifactId, version, Optional.of(type), Optional.of(classifier));
	}

	@Override
	public Feature readFeature(Reader jsonReader) throws IOException {

		try {
			CommentRemovingReader cleanReader = new CommentRemovingReader(jsonReader);
			JsonReader reader = JsonReader.from(cleanReader);
			reader.object();

			// cache here because id must not be first
			String id = null;
			String name = null;
			String description = null;
			String docURL = null;
			String license = null;
			String scm = null;
			String vendor = null;
			boolean complete = false;
			Map<String, Object> variables = Map.of();
			FeatureBundle[] bundles = new FeatureBundle[] {};
			String[] categories = new String[] {};
			FeatureConfiguration[] configurations = new FeatureConfiguration[] {};
			FeatureExtension[] extensions = new FeatureExtension[] {};

			while (reader.next()) {
				switch (reader.key()) {
				case "id" -> id = reader.string();
				case "name" -> name = reader.string();
				case "description" -> description = reader.string();
				case "docURL" -> docURL = reader.string();
				case "license" -> license = reader.string();
				case "scm" -> scm = reader.string();
				case "vendor" -> vendor = reader.string();
				case "complete" -> complete = reader.bool();
				case "variables" -> variables = readVariables(reader);
				case "bundles" -> bundles = readBundles(reader);
				case "categories" -> categories = readCategories(reader);
				case "configurations" -> configurations = readConfigurations(reader);
				case "extensions" -> extensions = readExtensions(reader);
				default -> skipValue(reader);
				}
			}

			if (id == null) {
				throw new IOException("Feature JSON missing required 'id' field");
			}

			// now create builder with id and set cached values
			var builder = builderFactory.newFeatureBuilder(getIDfromMavenCoordinates(id));
			builder.setName(name);
			builder.setDescription(description);
			builder.setDocURL(docURL);
			builder.setLicense(license);
			builder.setSCM(scm);
			builder.setVendor(vendor);
			builder.setComplete(complete);
			builder.addVariables(variables);
			builder.addBundles(bundles);
			builder.addCategories(categories);
			builder.addConfigurations(configurations);
			builder.addExtensions(extensions);

			return builder.build();
		} catch (JsonParserException e) {
			throw new IOException("Invalid JSON", e);
		}
	}

	private Map<String, Object> readVariables(JsonReader reader) throws JsonParserException {
		Map<String, Object> variables = new LinkedHashMap<>();
		reader.object();
		while (reader.next()) {
			String key = reader.key();
			switch (reader.current()) {
			case STRING -> variables.put(key, reader.string());
			case NUMBER -> variables.put(key, toBigDecimal(reader.number()));
			case BOOLEAN -> variables.put(key, reader.bool());
			case NULL -> {
				reader.nul();
				variables.put(key, null);
			}
			default -> throw new IllegalArgumentException(
					"Variables can only contain singular values, not objects or arrays.");
			}
		}
		return variables;
	}

	private BigDecimal toBigDecimal(Number n) {
		if (n instanceof BigDecimal bd) {
			return bd;
		}
		if (n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte) {
			return BigDecimal.valueOf(n.longValue());
		}
		if (n instanceof Double || n instanceof Float) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		return new BigDecimal(n.toString());
	}

	private FeatureBundle[] readBundles(JsonReader reader) throws JsonParserException {
		List<FeatureBundle> bundles = new ArrayList<>();
		reader.array();
		while (reader.next()) {
			reader.object();
			String bid = null;
			Map<String, Object> metadata = new LinkedHashMap<>();
			while (reader.next()) {
				String key = reader.key();
				if ("id".equals(key)) {
					bid = reader.string();
				} else {
					metadata.put(key, reader.value());
				}
			}
			if (bid != null) {
				var builder = builderFactory.newBundleBuilder(getIDfromMavenCoordinates(bid));
				for (Entry<String, Object> entry : metadata.entrySet()) {
					builder.addMetadata(entry.getKey(), entry.getValue());
				}
				bundles.add(builder.build());
			}
		}
		return bundles.toArray(new FeatureBundle[0]);
	}

	private String[] readCategories(JsonReader reader) throws JsonParserException {
		List<String> cats = new ArrayList<>();
		reader.array();
		while (reader.next()) {
			String category = reader.string();
			if (category != null) {
				cats.add(category);
			} else {
				throw new IllegalArgumentException("Invalid category: null");
			}
		}
		return cats.toArray(new String[] {});
	}

	private FeatureConfiguration[] readConfigurations(JsonReader reader) throws JsonParserException {
		List<FeatureConfiguration> configs = new ArrayList<>();
		reader.object();
		while (reader.next()) {
			String p = reader.key();
			String factoryPid = null;
			var idx = p.indexOf('~');
			if (idx > 0) {
				factoryPid = p.substring(0, idx);
				p = p.substring(idx + 1);
			}

			FeatureConfigurationBuilder builder;
			if (factoryPid == null) {
				builder = builderFactory.newConfigurationBuilder(p);
			} else {
				builder = builderFactory.newConfigurationBuilder(factoryPid, p);
			}

			Map<String, Object> cmap = readConfigValues(reader);
			builder.addValues(cmap);
			configs.add(builder.build());
		}
		return configs.toArray(new FeatureConfiguration[] {});
	}

	private Map<String, Object> readConfigValues(JsonReader reader) throws JsonParserException {
		Hashtable<String, Object> config = new Hashtable<>();
		reader.object();
		while (reader.next()) {
			String key = reader.key();
			Object value;

			switch (reader.current()) {
			case OBJECT -> {
				value = captureJsonObject(reader);
			}
			case ARRAY -> {
				List<Object> list = new ArrayList<>();
				reader.array();
				while (reader.next()) {
					list.add(reader.value());
				}
				value = list;
			}
			default -> value = reader.value();
			}

			String propertyKey = key;
			String typeInfo = null;
			int colonIdx = key.lastIndexOf(':');
			if (colonIdx > 0 && colonIdx < key.length() - 1) {
				String candidateType = key.substring(colonIdx + 1);
				if (TypeConverter.isKnownType(candidateType)) {
					propertyKey = key.substring(0, colonIdx);
					typeInfo = candidateType;
				}
			}

			if (typeInfo != null) {
				Object converted = TypeConverter.toType(value, typeInfo);
				if (converted != TypeConverter.FAILED) {
					value = converted;
				}
			}

			if (value != null) {
				config.put(propertyKey, value);
			}
		}
		return config;
	}

	private FeatureExtension[] readExtensions(JsonReader reader) throws JsonParserException {
		List<FeatureExtension> extensions = new ArrayList<>();
		reader.object();
		while (reader.next()) {
			String extensionName = reader.key();
			extensions.add(readSingleExtension(reader, extensionName));
		}
		return extensions.toArray(new FeatureExtension[] {});
	}

	private FeatureExtension readSingleExtension(JsonReader reader, String name) throws JsonParserException {
		reader.object();

		String sType = null;
		String sKind = "optional";
		FeatureExtension.Type typeFromContentField = null;
		List<String> textLines = null;
		List<ArtifactData> artifactDataList = null;
		String jsonString = null;

		while (reader.next()) {
			switch (reader.key()) {
			case "type" -> sType = reader.string();
			case "kind" -> sKind = reader.string();
			case "text" -> {
				typeFromContentField = FeatureExtension.Type.TEXT;
				textLines = new ArrayList<>();
				reader.array();
				while (reader.next()) {
					textLines.add(reader.string());
				}
			}
			case "artifacts" -> {
				typeFromContentField = FeatureExtension.Type.ARTIFACTS;
				artifactDataList = new ArrayList<>();
				reader.array();
				while (reader.next()) {
					reader.object();
					String artId = null;
					Map<String, Object> metadata = new LinkedHashMap<>();
					while (reader.next()) {
						String key = reader.key();
						if ("id".equals(key)) {
							artId = reader.string();
						} else {
							metadata.put(key, reader.value());
						}
					}
					artifactDataList.add(new ArtifactData(artId, metadata));
				}
			}
			case "json" -> {
				typeFromContentField = FeatureExtension.Type.JSON;
				jsonString = captureJsonObject(reader);
			}
			default -> skipValue(reader);
			}
		}

		FeatureExtension.Type typeFromTypeField;
		if ("text".equals(sType)) {
			typeFromTypeField = FeatureExtension.Type.TEXT;
		} else if ("artifacts".equals(sType)) {
			typeFromTypeField = FeatureExtension.Type.ARTIFACTS;
		} else if ("json".equals(sType)) {
			typeFromTypeField = FeatureExtension.Type.JSON;
		} else {
			throw new IllegalStateException("Invalid extension type: " + sType);
		}

		if (typeFromContentField == null) {
			throw new IllegalStateException("Extension has no content field: " + name);
		}

		if (typeFromTypeField != typeFromContentField) {
			throw new IllegalStateException("The type of the extension is not consistent to the content: "
					+ typeFromTypeField + " != " + typeFromContentField);
		}

		var kind = FeatureExtension.Kind.valueOf(sKind.toUpperCase());
		FeatureExtensionBuilder builder = builderFactory.newExtensionBuilder(name, typeFromContentField, kind);

		switch (typeFromContentField) {
		case TEXT -> textLines.forEach(builder::addText);
		case ARTIFACTS -> {
			for (ArtifactData ad : artifactDataList) {
				ID id = getIDfromMavenCoordinates(ad.id());
				FeatureArtifactBuilder fab = builderFactory.newArtifactBuilder(id);
				fab.addMetadata(ad.metadata());
				builder.addArtifact(fab.build());
			}
		}
		case JSON -> builder.setJSON(jsonString);
		}

		return builder.build();
	}

	private record ArtifactData(String id, Map<String, Object> metadata) {
	}

	private String captureJsonObject(JsonReader reader) throws JsonParserException {
		JsonStringWriter jsw = JsonWriter.string();
		jsw.object();
		reader.object();
		copyObjectContents(reader, jsw);
		jsw.end();
		return jsw.done();
	}

	private <T extends JsonSink<T>> void copyObjectContents(JsonReader reader, JsonSink<T> sink)
			throws JsonParserException {
		while (reader.next()) {
			String key = reader.key();
			switch (reader.current()) {
			case OBJECT -> {
				sink.object(key);
				reader.object();
				copyObjectContents(reader, sink);
				sink.end();
			}
			case ARRAY -> {
				sink.array(key);
				reader.array();
				copyArrayContents(reader, sink);
				sink.end();
			}
			case STRING -> sink.value(key, reader.string());
			case NUMBER -> sink.value(key, reader.number());
			case BOOLEAN -> sink.value(key, reader.bool());
			case NULL -> {
				reader.nul();
				sink.nul(key);
			}
			}
		}
	}

	private <T extends JsonSink<T>> void copyArrayContents(JsonReader reader, JsonSink<T> sink)
			throws JsonParserException {
		while (reader.next()) {
			switch (reader.current()) {
			case OBJECT -> {
				sink.object();
				reader.object();
				copyObjectContents(reader, sink);
				sink.end();
			}
			case ARRAY -> {
				sink.array();
				reader.array();
				copyArrayContents(reader, sink);
				sink.end();
			}
			case STRING -> sink.value(reader.string());
			case NUMBER -> sink.value(reader.number());
			case BOOLEAN -> sink.value(reader.bool());
			case NULL -> {
				reader.nul();
				sink.nul();
			}
			}
		}
	}

	private void skipValue(JsonReader reader) throws JsonParserException {
		switch (reader.current()) {
		case OBJECT -> {
			reader.object();
			while (reader.next()) {
				skipValue(reader);
			}
		}
		case ARRAY -> {
			reader.array();
			while (reader.next()) {
				skipValue(reader);
			}
		}
		case STRING -> reader.string();
		case NUMBER -> reader.number();
		case BOOLEAN -> reader.bool();
		case NULL -> reader.nul();
		}
	}

	@Override
	public void writeFeature(Feature feature, Writer jsonWriter) throws IOException {
		JsonAppendableWriter jaw = JsonWriter.indent("  ").on(jsonWriter);
		jaw.object();
		jaw.value("id", feature.getID().toString());
		feature.getName().ifPresent(n -> jaw.value("name", n));
		feature.getDescription().ifPresent(d -> jaw.value("description", d));
		feature.getDocURL().ifPresent(d -> jaw.value("docURL", d));
		feature.getLicense().ifPresent(l -> jaw.value("license", l));
		feature.getSCM().ifPresent(s -> jaw.value("scm", s));
		feature.getVendor().ifPresent(v -> jaw.value("vendor", v));
		if (feature.isComplete()) {
			jaw.value("complete", true);
		}

		writeVariables(jaw, feature);

		writeBundles(jaw, feature);
		writeCategories(jaw, feature);
		writeConfigurations(jaw, feature);

		writeExtensions(jaw, feature);

		jaw.end();
		jaw.done();

	}

	private void writeVariables(JsonAppendableWriter jaw, Feature feature) {
		var vars = feature.getVariables();

		if (vars == null || vars.size() == 0) {
			return;
		}

		jaw.key("variables");

		jaw.object();
		for (Entry<String, Object> e : vars.entrySet()) {
			jaw.value(e.getKey(), e.getValue());
		}
		jaw.end();
	}

	private void writeBundles(JsonAppendableWriter jaw, Feature feature) {
		var bundles = feature.getBundles();
		if (bundles == null || bundles.size() == 0) {
			return;
		}

		jaw.key("bundles");
		jaw.array();
		for (FeatureBundle bundle : bundles) {
			jaw.object();
			jaw.value("id", bundle.getID().toString());

			for (Entry<String, Object> e : bundle.getMetadata().entrySet()) {
				jaw.value(e.getKey(), e.getValue());
			}
			jaw.end();

		}

		jaw.end();

	}

	private void writeCategories(JsonAppendableWriter jaw, Feature feature) {
		var categories = feature.getCategories();
		if (categories == null || categories.isEmpty()) {
			return;
		}

		jaw.key("categories");
		jaw.array(categories);
	}

	private void writeConfigurations(JsonAppendableWriter jaw, Feature feature) throws IOException {
		var configs = feature.getConfigurations();
		if (configs == null || configs.size() == 0) {
			return;
		}
		jaw.key("configurations");
		jaw.object();

		for (Map.Entry<String, FeatureConfiguration> cfgs : configs.entrySet()) {
			jaw.key(cfgs.getKey());
			jaw.object();
			FeatureConfiguration cfg = cfgs.getValue();

			for (Entry<String, Object> e : cfg.getValues().entrySet()) {
				String key = e.getKey();
				Object value = e.getValue();
				String typeInfo = TypeConverter.getTypeInfoForValue(value);
				if (typeInfo != null) {
					key = key + ":" + typeInfo;
				}
				writeConfigValue(jaw, key, value);
			}
			jaw.end();
		}
		jaw.end();

	}

	private void writeConfigValue(JsonAppendableWriter jaw, String key, Object value) {
		if (value == null) {
			jaw.value(key, (Object) null);
		} else if (value.getClass().isArray()) {
			jaw.key(key);
			jaw.array();
			int len = java.lang.reflect.Array.getLength(value);
			for (int i = 0; i < len; i++) {
				jaw.value(java.lang.reflect.Array.get(value, i));
			}
			jaw.end();
		} else if (value instanceof Collection<?> coll) {
			jaw.key(key);
			jaw.array();
			for (Object item : coll) {
				jaw.value(item);
			}
			jaw.end();
		} else {
			jaw.value(key, value);
		}
	}

	private void writeExtensions(JsonAppendableWriter jaw, Feature feature) {
		var extensions = feature.getExtensions();
		if (extensions == null || extensions.size() == 0) {
			return;
		}
		jaw.key("extensions");
		jaw.object();

		for (Map.Entry<String, FeatureExtension> entry : extensions.entrySet()) {
			var extVal = entry.getValue();
			jaw.key(extVal.getName());
			jaw.object();

			jaw.value("kind", extVal.getKind().toString().toLowerCase());
			jaw.value("type", extVal.getType().toString().toLowerCase());

			switch (extVal.getType()) {
			case TEXT:
				jaw.key("text");
				jaw.array(extVal.getText());
				break;
			case ARTIFACTS:
				jaw.key("artifacts");
				jaw.array();
				for (FeatureArtifact art : extVal.getArtifacts()) {
					jaw.object();
					jaw.value("id", art.getID().toString());

					for (Entry<String, Object> e : art.getMetadata().entrySet()) {
						jaw.value(e.getKey(), e.getValue());
					}
					jaw.end();
				}
				jaw.end();
				break;
			case JSON:
				jaw.key("json");
				streamJsonObjectToWriter(extVal.getJSON(), jaw);
				break;
			}
			jaw.end();

		}
		jaw.end();
	}

	private void streamJsonObjectToWriter(String json, JsonAppendableWriter jaw) {
		try {
			JsonReader reader = JsonReader.from(new StringReader(json));
			reader.object();
			jaw.object();
			copyObjectContents(reader, jaw);
			jaw.end();
		} catch (JsonParserException e) {
			throw new IllegalArgumentException("Not a Json", e);
		}
	}
}
