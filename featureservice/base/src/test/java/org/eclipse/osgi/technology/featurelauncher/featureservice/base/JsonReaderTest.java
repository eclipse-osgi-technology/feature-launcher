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

import java.io.FileReader;
import java.io.StringWriter;
import java.nio.file.Path;

import org.eclipse.osgi.technology.featurelauncher.featureservice.base.FeatureServiceImpl;
import org.junit.jupiter.api.Test;
import org.osgi.service.feature.Feature;

public class JsonReaderTest {

	FeatureServiceImpl featureServiceImpl = new FeatureServiceImpl();

	@Test
	void readJson() throws Exception {

		FileReader fileReader = new FileReader(Path.of("src/test/resources/1.json").toFile());
		Feature feature = featureServiceImpl.readFeature(fileReader);
		assertThat(feature).isNotNull();

		StringWriter sw = new StringWriter();
		featureServiceImpl.writeFeature(feature, sw);

		System.out.println(sw);
	}

}
