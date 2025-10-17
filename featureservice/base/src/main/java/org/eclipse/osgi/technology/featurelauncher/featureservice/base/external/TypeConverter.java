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

package org.eclipse.osgi.technology.featurelauncher.featureservice.base.external;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TypeConverter {

	public static final String BINARY = "binary";
	public static final String BINARIES = "binary[]";
	private static final String COLLECTION = "Collection";

	public static final Object FAILED = new Object();

	private static final Map<String, Class<?>> TYPE_MAP = new LinkedHashMap<>();
	private static final Map<String, Class<?>> COLLECTION_TYPE_MAP = new LinkedHashMap<>();
	static {
		TYPE_MAP.put("boolean", Boolean.class);
		TYPE_MAP.put("boolean[]", boolean[].class);
		TYPE_MAP.put("Boolean", Boolean.class);
		TYPE_MAP.put("Boolean[]", Boolean[].class);
		TYPE_MAP.put("byte", Byte.class);
		TYPE_MAP.put("byte[]", byte[].class);
		TYPE_MAP.put("Byte", Byte.class);
		TYPE_MAP.put("Byte[]", Byte[].class);
		TYPE_MAP.put("char", Character.class);
		TYPE_MAP.put("char[]", char[].class);
		TYPE_MAP.put("Character", Character.class);
		TYPE_MAP.put("Character[]", Character[].class);
		TYPE_MAP.put("double", Double.class);
		TYPE_MAP.put("double[]", double[].class);
		TYPE_MAP.put("Double", Double.class);
		TYPE_MAP.put("Double[]", Double[].class);
		TYPE_MAP.put("float", Float.class);
		TYPE_MAP.put("float[]", float[].class);
		TYPE_MAP.put("Float", Float.class);
		TYPE_MAP.put("Float[]", Float[].class);
		TYPE_MAP.put("int", Integer.class);
		TYPE_MAP.put("int[]", int[].class);
		TYPE_MAP.put("Integer", Integer.class);
		TYPE_MAP.put("Integer[]", Integer[].class);
		TYPE_MAP.put("long", Long.class);
		TYPE_MAP.put("long[]", long[].class);
		TYPE_MAP.put("Long", Long.class);
		TYPE_MAP.put("Long[]", Long[].class);
		TYPE_MAP.put("short", Short.class);
		TYPE_MAP.put("short[]", short[].class);
		TYPE_MAP.put("Short", Short.class);
		TYPE_MAP.put("Short[]", Short[].class);
		TYPE_MAP.put("String", String.class);
		TYPE_MAP.put("String[]", String[].class);
		TYPE_MAP.put(BINARY, String.class);
		TYPE_MAP.put(BINARIES, String[].class);

		COLLECTION_TYPE_MAP.put("Collection<Boolean>", Boolean.class);
		COLLECTION_TYPE_MAP.put("Collection<Byte>", Byte.class);
		COLLECTION_TYPE_MAP.put("Collection<Character>", Character.class);
		COLLECTION_TYPE_MAP.put("Collection<Double>", Double.class);
		COLLECTION_TYPE_MAP.put("Collection<Float>", Float.class);
		COLLECTION_TYPE_MAP.put("Collection<Integer>", Integer.class);
		COLLECTION_TYPE_MAP.put("Collection<Long>", Long.class);
		COLLECTION_TYPE_MAP.put("Collection<Short>", Short.class);
		COLLECTION_TYPE_MAP.put("Collection<String>", String.class);

	}

	public static Object toType(final Object value, final String typeInfo) throws IllegalArgumentException {
		if (typeInfo == null) {
			return value;
		}
		final Class<?> typeClass = TYPE_MAP.get(typeInfo);
		if (typeClass != null) {
			Object result = null;

			// TODO:
			if (result == null && value != null) {
				result = FAILED;
			}
			return result;
		}
		final Class<?> typeReference = COLLECTION_TYPE_MAP.get(typeInfo);
		if (typeReference != null) {
			if (value == null) {
				return Collections.EMPTY_LIST;
			}
			Object result = null;
			// TODO:
			if (result == null) {
				result = FAILED;
			}
			return result;
		}

		if (COLLECTION.equals(typeInfo)) {
			if (value == null) {
				return List.of();
			} else if (value instanceof String || value instanceof Boolean || value instanceof Long
			        || value instanceof Double) {
				return List.of(value);
			}
			final Collection<Object> c = new ArrayList<>();
			for (int i = 0; i < Array.getLength(value); i++) {
				c.add(Array.get(value, i));
			}
			return Collections.unmodifiableCollection(c);
		}

		return FAILED;
	}

	public static boolean isArray(Class<?> clazz) {
		return clazz.isArray();
	}

	public static boolean isPrimitiveArray(Class<?> clazz) {
		return clazz.isArray() && clazz.getComponentType().isPrimitive();
	}

}
