/**
 * Copyright (c) 2012 - 2024 Data In Motion and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Data In Motion - initial API and implementation
 */
package com.kentyou.featurelauncher.impl.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleEvent;

/**
 * Translate type of bundle lifecycle change into readable representation.
 * 
 * Based on: {@link org.ops4j.pax.exam.FrameworkEventUtils}
 * 
 * @since Sep 19, 2024
 */
public class BundleEventUtil {
	private static final Map<Integer, String> EVENT_STRINGS;

	static {
		EVENT_STRINGS = new HashMap<>();
		for (Field field : BundleEvent.class.getDeclaredFields()) {
			if (Modifier.isPublic(field.getModifiers())) {
				Integer value;
				try {
					value = (Integer) field.get(null);
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Failed to obtain value of BundleEvent." + field.getName());
				}
				EVENT_STRINGS.put(value, field.getName());
			}
		}
	}

	private BundleEventUtil() {
		// hidden constructor
	}

	/**
	 * Return a readable representation of the type of a {@link BundleEvent}.
	 * 
	 * @param bundleEventType a value from {@link BundleEvent#getType()}.
	 * @return the name of the field that corresponds to this type.
	 */
	public static String getBundleEventString(int bundleEventType) {
		return EVENT_STRINGS.get(bundleEventType);
	}
}
