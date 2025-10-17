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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.osgi.technology.featurelauncher.featureservice.base.external.CommentRemovingReader;
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
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
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

		JsonObject jsonFeatureObject;
		try {
			CommentRemovingReader cleanReader = new CommentRemovingReader(jsonReader);
			jsonFeatureObject = JsonParser.object().from(cleanReader);
		} catch (JsonParserException e) {
			throw new IOException("Invalid JSON", e);
		}
//        JsonObject json = Json.createReader(Configurations.jsonCommentAwareReader(jsonReader)).readObject();

		var id = jsonFeatureObject.getString("id");
		var builder = builderFactory.newFeatureBuilder(getIDfromMavenCoordinates(id));

		builder.setName(jsonFeatureObject.getString("name", null));
		builder.setDescription(jsonFeatureObject.getString("description", null));
		builder.setDocURL(jsonFeatureObject.getString("docURL", null));
		builder.setLicense(jsonFeatureObject.getString("license", null));
		builder.setSCM(jsonFeatureObject.getString("scm", null));
		builder.setVendor(jsonFeatureObject.getString("vendor", null));
		builder.setComplete(jsonFeatureObject.getBoolean("complete", false));

		builder.addVariables(getVariables(jsonFeatureObject));
		builder.addBundles(getBundles(jsonFeatureObject));
		builder.addCategories(getCategories(jsonFeatureObject));
		builder.addConfigurations(getConfigurations(jsonFeatureObject));
		builder.addExtensions(getExtensions(jsonFeatureObject));

		return builder.build();
	}

	private Map<String, Object> getVariables(JsonObject jsonFeatureObject) {
		Map<String, Object> variables = new LinkedHashMap<>();

		JsonObject josnVariables = jsonFeatureObject.getObject("variables");
		if (josnVariables == null) {
			return Map.of();
		}

		for (Entry<String, Object> entry : josnVariables.entrySet()) {

			Object value = entry.getValue();
			if (value == null || value instanceof Boolean || value instanceof String || value instanceof Number) {
				variables.put(entry.getKey(), value);
			} else {
				throw new IllegalArgumentException(
				        "Variables can only contain singular values, not objects or arrays.");
			}
			;

		}
		return variables;
	}

	private FeatureBundle[] getBundles(JsonObject jsonFeatureObject) {
		JsonArray jsonBundlesArray = jsonFeatureObject.getArray("bundles");
		if (jsonBundlesArray == null) {
			return new FeatureBundle[] {};
		}

		List<FeatureBundle> bundles = new ArrayList<>();

		for (Object jsonBundleObject : jsonBundlesArray) {
			if (jsonBundleObject instanceof JsonObject jsonBundle) {
				var bid = jsonBundle.getString("id");
				var builder = builderFactory.newBundleBuilder(getIDfromMavenCoordinates(bid));

				for (Entry<String, Object> entry : jsonBundle.entrySet()) {
					if (entry.getKey().equals("id")) {
						continue;
					}

					Object value = entry.getValue();
					builder.addMetadata(entry.getKey(), value);

				}
				bundles.add(builder.build());
			}
		}

		return bundles.toArray(new FeatureBundle[0]);
	}

	private String[] getCategories(JsonObject jsonFeatureObject) {
		JsonArray JsonCategoryArray = jsonFeatureObject.getArray("categories");
		if (JsonCategoryArray == null) {
			return new String[] {};
		}

		List<String> cats = new ArrayList<>();
		for (Object categoryObject : JsonCategoryArray) {
			if (categoryObject instanceof String category) {
				cats.add(category);
			} else {
				throw new IllegalArgumentException("Invalid category: " + categoryObject);
			}
		}

		return cats.toArray(new String[] {});
	}

	private FeatureConfiguration[] getConfigurations(JsonObject json) throws IOException {
		JsonObject jo = json.getObject("configurations");
		if (jo == null) {
			return new FeatureConfiguration[] {};
		}
		List<FeatureConfiguration> configs = new ArrayList<>();
		for (Entry<String, Object> entry : jo.entrySet()) {
			String p = entry.getKey();
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

			Map<String, Object> cmap = null;
			Object oJsonCfg = entry.getValue();
			if (oJsonCfg == null) {
				cmap = Map.of();
			} else if (oJsonCfg instanceof JsonObject jsonCfg) {
				cmap = Util.read(jsonCfg);
			} else {
				throw new IllegalArgumentException("Invalid configuration: " + entry);
			}
			builder.addValues(cmap);

			configs.add(builder.build());
		}

		return configs.toArray(new FeatureConfiguration[] {});
	}

	private FeatureExtension[] getExtensions(JsonObject jsonFeatureObject) {

		JsonObject jo = jsonFeatureObject.getObject("extensions");
		if (jo == null) {
			return new FeatureExtension[] {};
		}

		List<FeatureExtension> extensions = new ArrayList<>();

		for (Entry<String, Object> e : jo.entrySet()) {
			Object object = e.getValue();

			JsonObject extensionData = null;
			if (object instanceof JsonObject jso) {
				extensionData = jso;
			} else {
				throw new IllegalArgumentException("Invalid extension: " + e);

			}
			String sTypeFromTypeField = extensionData.getString("type", null);
			FeatureExtension.Type typeFromTypeField = null;
			if ("text".equals(sTypeFromTypeField)) {
				typeFromTypeField = FeatureExtension.Type.TEXT;
			} else if ("artifacts".equals(sTypeFromTypeField)) {
				typeFromTypeField = FeatureExtension.Type.ARTIFACTS;
			} else if ("json".equals(sTypeFromTypeField)) {
				typeFromTypeField = FeatureExtension.Type.JSON;
			} else {
				throw new IllegalStateException("Invalid extension: " + e);
			}

			FeatureExtension.Type typeFromContentField;
			if (extensionData.containsKey("text")) {
				typeFromContentField = FeatureExtension.Type.TEXT;
			} else if (extensionData.containsKey("artifacts")) {
				typeFromContentField = FeatureExtension.Type.ARTIFACTS;
			} else if (extensionData.containsKey("json")) {
				typeFromContentField = FeatureExtension.Type.JSON;
			} else {
				throw new IllegalStateException("Invalid extension: " + e);
			}

			if (typeFromTypeField != typeFromContentField) {
				throw new IllegalStateException("The type of the extension is not consistent to the content: "
				        + typeFromTypeField + " != " + typeFromContentField);
			}
			var k = extensionData.getString("kind", "optional");
			var kind = FeatureExtension.Kind.valueOf(k.toUpperCase());

			FeatureExtensionBuilder builder = builderFactory.newExtensionBuilder(e.getKey(), typeFromContentField,
			        kind);

			switch (typeFromContentField) {
			case TEXT:
				extensionData.getArray("text").stream().filter(String.class::isInstance).map(String.class::cast)
				        .forEach(builder::addText);

				break;
			case ARTIFACTS:
				extensionData.getArray("artifacts").stream().filter(JsonObject.class::isInstance)
				        .map(JsonObject.class::cast).forEach(md -> {
					        Map<String, Object> v = md;
					        String idVal = (String) v.remove("id");

					        ID id = getIDfromMavenCoordinates(idVal);
					        FeatureArtifactBuilder fab = builderFactory.newArtifactBuilder(id);
					        fab.addMetadata(v);

					        builder.addArtifact(fab.build());
				        });

				break;
			case JSON:
				JsonObject jsonObj = extensionData.getObject("json");
				String jsonString = Util.jsonStringOf(jsonObj);
				builder.setJSON(jsonString);
				break;
			}
			extensions.add(builder.build());
		}

		return extensions.toArray(new FeatureExtension[] {});
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

		writeVariables(jaw, feature);

		writeBundles(jaw, feature);
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
				jaw.value(e.getKey(), e.getValue());
			}
			jaw.end();
		}
		jaw.end();

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

				try {
					String json = extVal.getJSON();
					JsonObject jo = JsonParser.object().from(new StringReader(json));
					jaw.key("json");
					jaw.object(jo);

				} catch (JsonParserException e) {
					throw new IllegalArgumentException("Not a Json", e);
				}

				break;
			}
			jaw.end();

		}
		jaw.end();
	}
}