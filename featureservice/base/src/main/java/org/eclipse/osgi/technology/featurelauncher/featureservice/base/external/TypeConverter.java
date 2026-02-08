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

	public static boolean isKnownType(String typeInfo) {
		if (typeInfo == null) {
			return false;
		}
		return TYPE_MAP.containsKey(typeInfo) || COLLECTION_TYPE_MAP.containsKey(typeInfo)
				|| COLLECTION.equals(typeInfo);
	}

	public static Object toType(final Object value, final String typeInfo) throws IllegalArgumentException {
		if (typeInfo == null) {
			return value;
		}
		final Class<?> typeClass = TYPE_MAP.get(typeInfo);
		if (typeClass != null) {
			if (value == null) {
				return null;
			}
			if (isArray(typeClass)) {
				return convertToArray(value, typeClass.getComponentType(), isPrimitiveArray(typeClass));
			}
			return convertScalar(value, typeClass);
		}
		final Class<?> typeReference = COLLECTION_TYPE_MAP.get(typeInfo);
		if (typeReference != null) {
			if (value == null) {
				return Collections.emptyList();
			}
			return convertToCollection(value, typeReference);
		}

		if (COLLECTION.equals(typeInfo)) {
			if (value == null) {
				return List.of();
			} else if (value instanceof String || value instanceof Boolean || value instanceof Long
					|| value instanceof Double) {
				return List.of(value);
			}
			if (value.getClass().isArray()) {
				final Collection<Object> c = new ArrayList<>();
				for (int i = 0; i < Array.getLength(value); i++) {
					c.add(Array.get(value, i));
				}
				return Collections.unmodifiableCollection(c);
			}
			if (value instanceof Collection<?> coll) {
				return List.copyOf(coll);
			}
			return List.of(value);
		}

		return FAILED;
	}

	private static Object convertScalar(Object value, Class<?> targetType) {
		if (targetType.isInstance(value)) {
			return value;
		}

		if (targetType == String.class) {
			return String.valueOf(value);
		}

		if (targetType == Boolean.class) {
			if (value instanceof String s) {
				return Boolean.valueOf(s);
			}
			if (value instanceof Number n) {
				return n.intValue() != 0;
			}
			return FAILED;
		}

		if (targetType == Character.class) {
			if (value instanceof String s) {
				if (s.length() == 1) {
					return s.charAt(0);
				}
				return FAILED;
			}
			if (value instanceof Number n) {
				return (char) n.intValue();
			}
			return FAILED;
		}

		if (targetType == Integer.class) {
			return toInteger(value);
		}
		if (targetType == Long.class) {
			return toLong(value);
		}
		if (targetType == Double.class) {
			return toDouble(value);
		}
		if (targetType == Float.class) {
			return toFloat(value);
		}
		if (targetType == Byte.class) {
			return toByte(value);
		}
		if (targetType == Short.class) {
			return toShort(value);
		}

		return FAILED;
	}

	private static Object toInteger(Object value) {
		if (value instanceof Number n) {
			return n.intValue();
		}
		if (value instanceof String s) {
			try {
				return Integer.valueOf(s);
			} catch (NumberFormatException e) {
				return FAILED;
			}
		}
		if (value instanceof Boolean b) {
			return b ? 1 : 0;
		}
		return FAILED;
	}

	private static Object toLong(Object value) {
		if (value instanceof Number n) {
			return n.longValue();
		}
		if (value instanceof String s) {
			try {
				return Long.valueOf(s);
			} catch (NumberFormatException e) {
				return FAILED;
			}
		}
		if (value instanceof Boolean b) {
			return b ? 1L : 0L;
		}
		return FAILED;
	}

	private static Object toDouble(Object value) {
		if (value instanceof Number n) {
			return n.doubleValue();
		}
		if (value instanceof String s) {
			try {
				return Double.valueOf(s);
			} catch (NumberFormatException e) {
				return FAILED;
			}
		}
		if (value instanceof Boolean b) {
			return b ? 1.0d : 0.0d;
		}
		return FAILED;
	}

	private static Object toFloat(Object value) {
		if (value instanceof Number n) {
			return n.floatValue();
		}
		if (value instanceof String s) {
			try {
				return Float.valueOf(s);
			} catch (NumberFormatException e) {
				return FAILED;
			}
		}
		if (value instanceof Boolean b) {
			return b ? 1.0f : 0.0f;
		}
		return FAILED;
	}

	private static Object toByte(Object value) {
		if (value instanceof Number n) {
			return n.byteValue();
		}
		if (value instanceof String s) {
			try {
				return Byte.valueOf(s);
			} catch (NumberFormatException e) {
				return FAILED;
			}
		}
		if (value instanceof Boolean b) {
			return b ? (byte) 1 : (byte) 0;
		}
		return FAILED;
	}

	private static Object toShort(Object value) {
		if (value instanceof Number n) {
			return n.shortValue();
		}
		if (value instanceof String s) {
			try {
				return Short.valueOf(s);
			} catch (NumberFormatException e) {
				return FAILED;
			}
		}
		if (value instanceof Boolean b) {
			return b ? (short) 1 : (short) 0;
		}
		return FAILED;
	}

	private static Object convertToArray(Object value, Class<?> componentType, boolean primitive) {
		if (value instanceof List<?> list) {
			if (primitive) {
				Object arr = Array.newInstance(componentType, list.size());
				for (int i = 0; i < list.size(); i++) {
					Object converted = convertScalar(list.get(i), boxType(componentType));
					if (converted == FAILED) {
						return FAILED;
					}
					Array.set(arr, i, converted);
				}
				return arr;
			} else {
				Object arr = Array.newInstance(componentType, list.size());
				for (int i = 0; i < list.size(); i++) {
					Object converted = convertScalar(list.get(i), componentType);
					if (converted == FAILED) {
						return FAILED;
					}
					Array.set(arr, i, converted);
				}
				return arr;
			}
		}
		// Single value — wrap in single-element array
		Class<?> boxed = primitive ? boxType(componentType) : componentType;
		Object converted = convertScalar(value, boxed);
		if (converted == FAILED) {
			return FAILED;
		}
		Object arr = Array.newInstance(componentType, 1);
		Array.set(arr, 0, converted);
		return arr;
	}

	private static Object convertToCollection(Object value, Class<?> componentType) {
		if (value instanceof List<?> list) {
			List<Object> result = new ArrayList<>();
			for (Object item : list) {
				Object converted = convertScalar(item, componentType);
				if (converted == FAILED) {
					return FAILED;
				}
				result.add(converted);
			}
			return Collections.unmodifiableList(result);
		}
		// Single value — wrap in single-element list
		Object converted = convertScalar(value, componentType);
		if (converted == FAILED) {
			return FAILED;
		}
		return List.of(converted);
	}

	private static Class<?> boxType(Class<?> primitiveType) {
		if (primitiveType == boolean.class)
			return Boolean.class;
		if (primitiveType == byte.class)
			return Byte.class;
		if (primitiveType == char.class)
			return Character.class;
		if (primitiveType == double.class)
			return Double.class;
		if (primitiveType == float.class)
			return Float.class;
		if (primitiveType == int.class)
			return Integer.class;
		if (primitiveType == long.class)
			return Long.class;
		if (primitiveType == short.class)
			return Short.class;
		return primitiveType;
	}

	public static String getTypeInfoForValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof String)
			return null;
		if (value instanceof Long)
			return null;
		if (value instanceof Double)
			return null;
		if (value instanceof Boolean)
			return null;

		if (value instanceof Integer)
			return "Integer";
		if (value instanceof Float)
			return "Float";
		if (value instanceof Byte)
			return "Byte";
		if (value instanceof Short)
			return "Short";
		if (value instanceof Character)
			return "Character";

		if (value instanceof int[])
			return "Integer[]";
		if (value instanceof long[])
			return "Long[]";
		if (value instanceof double[])
			return "Double[]";
		if (value instanceof float[])
			return "Float[]";
		if (value instanceof boolean[])
			return "Boolean[]";
		if (value instanceof byte[])
			return "Byte[]";
		if (value instanceof short[])
			return "Short[]";
		if (value instanceof char[])
			return "Character[]";

		if (value instanceof String[])
			return "String[]";
		if (value instanceof Integer[])
			return "Integer[]";
		if (value instanceof Long[])
			return "Long[]";
		if (value instanceof Double[])
			return "Double[]";
		if (value instanceof Float[])
			return "Float[]";
		if (value instanceof Boolean[])
			return "Boolean[]";
		if (value instanceof Byte[])
			return "Byte[]";
		if (value instanceof Short[])
			return "Short[]";
		if (value instanceof Character[])
			return "Character[]";

		if (value instanceof Collection)
			return "Collection";

		return null;
	}

	public static boolean isArray(Class<?> clazz) {
		return clazz.isArray();
	}

	public static boolean isPrimitiveArray(Class<?> clazz) {
		return clazz.isArray() && clazz.getComponentType().isPrimitive();
	}

}
