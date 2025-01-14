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
package org.eclipse.osgi.technology.featurelauncher.common.util.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Util for variables substitution operations.
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Nov 2, 2024
 */
public class VariablesUtil {

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");
	
	public static Map<String, Object> maybeSubstituteVariables(Map<String, Object> properties, Map<String, Object> variables) throws IllegalArgumentException {
		if (!properties.isEmpty() && !variables.isEmpty()) {
			Map<String, Object> substituted = new HashMap<>();

			for (Map.Entry<String, Object> propertyEntry : properties.entrySet()) {
				String propertyName = propertyEntry.getKey();

				Object rawPropertyValue = propertyEntry.getValue();
				
				if (rawPropertyValue instanceof String s) {
					Matcher matcher = PLACEHOLDER_PATTERN.matcher(s);
					StringBuilder sb = new StringBuilder();
					int from = 0;
					while(matcher.find()) {
						sb.append(s, from, matcher.start());
						String name = matcher.group(1);
						if(!variables.containsKey(name)) {
							throw new IllegalArgumentException("There is no variable defined for name " + name);
						}
						sb.append(variables.get(name));
						from = matcher.end();
					}
					if(s.length() > from) {
						sb.append(s, from, s.length());
					}
					substituted.put(propertyName, sb.toString());
				} else {
					substituted.put(propertyName, rawPropertyValue);
				}

			}

			return substituted;
		}

		return new HashMap<>(properties);
	}
}
