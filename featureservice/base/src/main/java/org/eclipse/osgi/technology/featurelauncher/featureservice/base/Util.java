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

import java.io.StringWriter;
import java.util.Hashtable;
import java.util.Map;

import com.grack.nanojson.JsonAppendableWriter;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonWriter;

public class Util {

	static String jsonStringOf(JsonObject jsonObj) {
		StringWriter sw = new StringWriter();
		JsonAppendableWriter jaw = JsonWriter.indent("  ").on(sw);
		jaw.value(jsonObj);
		jaw.done();
		String jsonString = sw.toString();
		return jsonString;
	}

	static Map<String, Object> read(JsonObject json) {
		Hashtable<String, Object> config = new Hashtable<>();
		for (String key : json.keySet()) {
			Object value = json.get(key);
			if (value instanceof JsonObject js) {
				value = jsonStringOf(js);
			}
			config.put(key, value);
		}
		return config;
	}

}