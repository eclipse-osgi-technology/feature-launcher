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
package org.eclipse.osgi.technology.featurelauncher.common.osgi.util.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.FrameworkEvent;

/**
 * Translate type of event into readable representation.
 * 
 * Based on: {@link org.ops4j.pax.exam.FrameworkEventUtils}
 * 
 * @since Sep 19, 2024
 */
public class FrameworkEventUtil {
	private static final Map<Integer, String> EVENT_STRINGS;

	static {
		EVENT_STRINGS = new HashMap<>();
		for (Field field : FrameworkEvent.class.getDeclaredFields()) {
			if (Modifier.isPublic(field.getModifiers())) {
				Integer value;
				try {
					value = (Integer) field.get(null);
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Failed to obtain value of FrameworkEvent." + field.getName());
				}
				EVENT_STRINGS.put(value, field.getName());
			}
		}
	}

	private FrameworkEventUtil() {
		// hidden constructor
	}

	/**
	 * Return a readable representation of the type of a {@link FrameworkEvent}.
	 * 
	 * @param frameworkEventType a value from {@link FrameworkEvent#getType()}.
	 * @return the name of the field that corresponds to this type.
	 */
	public static String getFrameworkEventString(int frameworkEventType) {
		return EVENT_STRINGS.get(frameworkEventType);
	}
}
