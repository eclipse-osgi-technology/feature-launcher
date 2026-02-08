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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureArtifact;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.ID;

public class JsonReaderTest {

	FeatureServiceImpl featureServiceImpl = new FeatureServiceImpl();

	@Test
	void readJson() throws Exception {
		FileReader fileReader = new FileReader(Path.of("src/test/resources/1.json").toFile());
		Feature feature = featureServiceImpl.readFeature(fileReader);
		assertThat(feature).isNotNull();

		StringWriter sw = new StringWriter();
		featureServiceImpl.writeFeature(feature, sw);
		assertThat(sw.toString()).isNotEmpty();
	}

	@Test
	void testReadFeatureVariables() throws Exception {
		Feature feature = readFeature("test-feature.json");

		Map<String, Object> vars = feature.getVariables();
		assertThat(vars).hasSize(4);
		assertThat(vars.get("http.port")).isInstanceOf(BigDecimal.class);
		assertThat(vars.get("http.port")).isEqualTo(BigDecimal.valueOf(8080));
		assertThat(vars.get("db.username")).isEqualTo("scott");
		assertThat(vars).containsEntry("db.password", null);
		assertThat(vars.get("fw.storage.dir")).isEqualTo("/tmp");
	}

	@Test
	void testReadFeatureBundles() throws Exception {
		Feature feature = readFeature("test-feature.json");

		List<FeatureBundle> bundles = feature.getBundles();
		assertThat(bundles).hasSize(4);

		FeatureBundle first = bundles.get(0);
		assertThat(first.getID().getGroupId()).isEqualTo("org.osgi");
		assertThat(first.getID().getArtifactId()).isEqualTo("org.osgi.util.function");
		assertThat(first.getID().getVersion()).isEqualTo("1.1.0");

		FeatureBundle withMeta = bundles.get(2);
		assertThat(withMeta.getID().getArtifactId()).isEqualTo("commons-email");
		assertThat(withMeta.getMetadata()).containsKey("org.acme.javadoc.link");
		assertThat(withMeta.getMetadata().get("org.acme.javadoc.link")).isInstanceOf(String.class);
	}

	@Test
	void testReadFeatureConfigurations() throws Exception {
		Feature feature = readFeature("test-feature.json");

		Map<String, FeatureConfiguration> configs = feature.getConfigurations();
		assertThat(configs).isNotEmpty();

		FeatureConfiguration httpCfg = configs.get("org.apache.felix.http");
		assertThat(httpCfg).isNotNull();
		assertThat(httpCfg.getValues().get("org.osgi.service.http.port")).isEqualTo("${http.port}");

		FeatureConfiguration dbCfg = configs.get("org.acme.db");
		assertThat(dbCfg).isNotNull();
		assertThat(dbCfg.getValues().get("username")).isEqualTo("${db.username}-user");
		assertThat(dbCfg.getValues().get("password")).isEqualTo("${db.password}");
	}

	@Test
	void testReadTypedConfigurations() throws Exception {
		Feature feature = readFeature("test-feature.json");

		Map<String, FeatureConfiguration> configs = feature.getConfigurations();
		FeatureConfiguration typedCfg = configs.get("org.acme.typed");
		assertThat(typedCfg).isNotNull();

		Map<String, Object> values = typedCfg.getValues();
		assertThat(values.get("number")).isInstanceOf(Integer.class);
		assertThat(values.get("number")).isEqualTo(7);

		assertThat(values.get("floatVal")).isInstanceOf(Float.class);

		assertThat(values.get("flag")).isInstanceOf(Boolean.class);
		assertThat(values.get("flag")).isEqualTo(true);

		assertThat(values.get("ports")).isInstanceOf(Integer[].class);
		Integer[] ports = (Integer[]) values.get("ports");
		assertThat(ports).containsExactly(80, 443, 8080);
	}

	@Test
	void testReadFactoryConfiguration() throws Exception {
		Feature feature = readFeature("test-feature.json");

		Map<String, FeatureConfiguration> configs = feature.getConfigurations();
		FeatureConfiguration factoryCfg = configs.get("org.acme.factory~instance1");
		assertThat(factoryCfg).isNotNull();
		assertThat(factoryCfg.getFactoryPid()).isPresent();
		assertThat(factoryCfg.getFactoryPid().get()).isEqualTo("org.acme.factory");
		assertThat(factoryCfg.getValues().get("key1")).isEqualTo("val1");
	}

	@Test
	void testReadFeatureMetadata() throws Exception {
		Feature feature = readFeature("test-feature2.json");

		assertThat(feature.getID().toString()).isEqualTo("org.acme:fullapp:jar:classifier:1.0.0");
		assertThat(feature.getName()).hasValue("Full Application");
		assertThat(feature.getDescription()).hasValue("A complete application with all metadata");
		assertThat(feature.getDocURL()).hasValue("https://docs.example.com/fullapp");
		assertThat(feature.getLicense()).hasValue("https://opensource.org/licenses/MIT");
		assertThat(feature.getSCM()).hasValue("https://github.com/acme/fullapp");
		assertThat(feature.getVendor()).hasValue("Acme Corp.");
		assertThat(feature.isComplete()).isTrue();
	}

	@Test
	void testReadCategories() throws Exception {
		Feature feature = readFeature("test-feature2.json");

		assertThat(feature.getCategories()).containsExactly("osgi", "enterprise", "testing");
	}

	@Test
	void testReadExtensionText() throws Exception {
		Feature feature = readFeature("test-exfeat1.json");

		Map<String, FeatureExtension> extensions = feature.getExtensions();
		FeatureExtension textExt = extensions.get("my-text-extension");
		assertThat(textExt).isNotNull();
		assertThat(textExt.getType()).isEqualTo(FeatureExtension.Type.TEXT);
		assertThat(textExt.getKind()).isEqualTo(FeatureExtension.Kind.OPTIONAL);
		assertThat(textExt.getText()).containsExactly("Line one of the text extension.",
				"Line two of the text extension.");
	}

	@Test
	void testReadExtensionArtifacts() throws Exception {
		Feature feature = readFeature("test-exfeat1.json");

		Map<String, FeatureExtension> extensions = feature.getExtensions();
		FeatureExtension artExt = extensions.get("my-artifact-extension");
		assertThat(artExt).isNotNull();
		assertThat(artExt.getType()).isEqualTo(FeatureExtension.Type.ARTIFACTS);
		assertThat(artExt.getKind()).isEqualTo(FeatureExtension.Kind.MANDATORY);

		List<FeatureArtifact> artifacts = artExt.getArtifacts();
		assertThat(artifacts).hasSize(2);
		assertThat(artifacts.get(0).getID().getArtifactId()).isEqualTo("ddl");

		FeatureArtifact second = artifacts.get(1);
		assertThat(second.getID().getArtifactId()).isEqualTo("ddl-custom");
		assertThat(second.getMetadata()).containsEntry("org.acme.target", "custom-db");
	}

	@Test
	void testReadExtensionJson() throws Exception {
		Feature feature = readFeature("test-exfeat1.json");

		Map<String, FeatureExtension> extensions = feature.getExtensions();
		FeatureExtension jsonExt = extensions.get("my-json-extension");
		assertThat(jsonExt).isNotNull();
		assertThat(jsonExt.getType()).isEqualTo(FeatureExtension.Type.JSON);
		assertThat(jsonExt.getKind()).isEqualTo(FeatureExtension.Kind.TRANSIENT);
		assertThat(jsonExt.getJSON()).contains("cache-size");
		assertThat(jsonExt.getJSON()).contains("1024");
	}

	@Test
	void testReadComments() throws Exception {
		Feature feature = readFeature("test-comments.json");

		assertThat(feature).isNotNull();
		assertThat(feature.getID().getArtifactId()).isEqualTo("commented");
		assertThat(feature.getName()).hasValue("Commented Feature");
	}

	@Test
	void testReadWriteRoundTrip() throws Exception {
		Feature original = readFeature("test-exfeat1.json");

		StringWriter sw = new StringWriter();
		featureServiceImpl.writeFeature(original, sw);
		String json = sw.toString();

		Feature roundTripped = featureServiceImpl.readFeature(new StringReader(json));

		assertThat(roundTripped.getID().toString()).isEqualTo(original.getID().toString());
		assertThat(roundTripped.getExtensions()).hasSameSizeAs(original.getExtensions());

		for (Map.Entry<String, FeatureExtension> entry : original.getExtensions().entrySet()) {
			FeatureExtension origExt = entry.getValue();
			FeatureExtension rtExt = roundTripped.getExtensions().get(entry.getKey());
			assertThat(rtExt).isNotNull();
			assertThat(rtExt.getType()).isEqualTo(origExt.getType());
			assertThat(rtExt.getKind()).isEqualTo(origExt.getKind());

			switch (origExt.getType()) {
			case TEXT:
				assertThat(rtExt.getText()).isEqualTo(origExt.getText());
				break;
			case ARTIFACTS:
				assertThat(rtExt.getArtifacts()).hasSameSizeAs(origExt.getArtifacts());
				for (int i = 0; i < origExt.getArtifacts().size(); i++) {
					assertThat(rtExt.getArtifacts().get(i).getID().toString())
							.isEqualTo(origExt.getArtifacts().get(i).getID().toString());
				}
				break;
			case JSON:
				assertThat(rtExt.getJSON()).isNotNull();
				break;
			}
		}
	}

	@Test
	void testReadWriteRoundTripMetadata() throws Exception {
		Feature original = readFeature("test-feature2.json");

		StringWriter sw = new StringWriter();
		featureServiceImpl.writeFeature(original, sw);
		String json = sw.toString();

		Feature roundTripped = featureServiceImpl.readFeature(new StringReader(json));

		assertThat(roundTripped.getID().toString()).isEqualTo(original.getID().toString());
		assertThat(roundTripped.getName()).isEqualTo(original.getName());
		assertThat(roundTripped.getDescription()).isEqualTo(original.getDescription());
		assertThat(roundTripped.getDocURL()).isEqualTo(original.getDocURL());
		assertThat(roundTripped.getLicense()).isEqualTo(original.getLicense());
		assertThat(roundTripped.getSCM()).isEqualTo(original.getSCM());
		assertThat(roundTripped.getVendor()).isEqualTo(original.getVendor());
		assertThat(roundTripped.isComplete()).isEqualTo(original.isComplete());
		assertThat(roundTripped.getCategories()).isEqualTo(original.getCategories());
	}

	@Test
	void testWriteFeature() throws Exception {
		Feature feature = readFeature("test-feature2.json");

		StringWriter sw = new StringWriter();
		featureServiceImpl.writeFeature(feature, sw);
		String json = sw.toString();

		assertThat(json).contains("\"id\"");
		assertThat(json).contains("\"name\"");
		assertThat(json).contains("\"complete\"");
		assertThat(json).contains("\"categories\"");
		assertThat(json).contains("\"osgi\"");
		assertThat(json).contains("\"enterprise\"");
		assertThat(json).contains("\"testing\"");
		assertThat(json).contains("\"vendor\"");
	}

	@Test
	void testWriteExtensionsIncludesType() throws Exception {
		Feature feature = readFeature("test-exfeat1.json");

		StringWriter sw = new StringWriter();
		featureServiceImpl.writeFeature(feature, sw);
		String json = sw.toString();

		assertThat(json).contains("\"type\"");
		assertThat(json).contains("\"text\"");
		assertThat(json).contains("\"artifacts\"");
		assertThat(json).contains("\"json\"");
	}

	@Test
	void testIDParsing3Parts() throws Exception {
		ID id = featureServiceImpl.getIDfromMavenCoordinates("org.acme:app:1.0.0");
		assertThat(id.getGroupId()).isEqualTo("org.acme");
		assertThat(id.getArtifactId()).isEqualTo("app");
		assertThat(id.getVersion()).isEqualTo("1.0.0");
		assertThat(id.getType()).isEmpty();
		assertThat(id.getClassifier()).isEmpty();
	}

	@Test
	void testIDParsing4Parts() throws Exception {
		ID id = featureServiceImpl.getIDfromMavenCoordinates("org.acme:app:jar:1.0.0");
		assertThat(id.getGroupId()).isEqualTo("org.acme");
		assertThat(id.getArtifactId()).isEqualTo("app");
		assertThat(id.getVersion()).isEqualTo("1.0.0");
		assertThat(id.getType()).hasValue("jar");
		assertThat(id.getClassifier()).isEmpty();
	}

	@Test
	void testIDParsing5Parts() throws Exception {
		ID id = featureServiceImpl.getIDfromMavenCoordinates("org.acme:app:jar:sources:1.0.0");
		assertThat(id.getGroupId()).isEqualTo("org.acme");
		assertThat(id.getArtifactId()).isEqualTo("app");
		assertThat(id.getVersion()).isEqualTo("1.0.0");
		assertThat(id.getType()).hasValue("jar");
		assertThat(id.getClassifier()).hasValue("sources");
	}

	@Test
	void testIDParsingInvalid() {
		assertThatThrownBy(() -> featureServiceImpl.getIDfromMavenCoordinates("invalid"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testReadSingleLineTextExtension() throws Exception {
		Feature feature = readFeature("test-exfeat2.json");

		Map<String, FeatureExtension> extensions = feature.getExtensions();
		FeatureExtension textExt = extensions.get("simple-text");
		assertThat(textExt).isNotNull();
		assertThat(textExt.getType()).isEqualTo(FeatureExtension.Type.TEXT);
		assertThat(textExt.getText()).containsExactly("Single line text.");
	}

	private Feature readFeature(String resourceName) throws Exception {
		FileReader fileReader = new FileReader(Path.of("src/test/resources/" + resourceName).toFile());
		return featureServiceImpl.readFeature(fileReader);
	}
}
