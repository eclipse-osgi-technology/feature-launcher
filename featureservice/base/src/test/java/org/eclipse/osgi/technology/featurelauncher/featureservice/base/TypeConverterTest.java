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

import java.util.List;

import org.eclipse.osgi.technology.featurelauncher.featureservice.base.external.TypeConverter;
import org.junit.jupiter.api.Test;

public class TypeConverterTest {

	@Test
	void testNullTypeInfoReturnsValue() {
		assertThat(TypeConverter.toType("hello", null)).isEqualTo("hello");
		assertThat(TypeConverter.toType(42, null)).isEqualTo(42);
		assertThat(TypeConverter.toType(null, null)).isNull();
	}

	@Test
	void testNullValueWithScalarType() {
		assertThat(TypeConverter.toType(null, "String")).isNull();
		assertThat(TypeConverter.toType(null, "Integer")).isNull();
		assertThat(TypeConverter.toType(null, "Boolean")).isNull();
	}

	@Test
	void testStringToInteger() {
		Object result = TypeConverter.toType("42", "Integer");
		assertThat(result).isEqualTo(42);
	}

	@Test
	void testStringToLong() {
		Object result = TypeConverter.toType("100", "Long");
		assertThat(result).isEqualTo(100L);
	}

	@Test
	void testStringToDouble() {
		Object result = TypeConverter.toType("3.14", "Double");
		assertThat(result).isEqualTo(3.14);
	}

	@Test
	void testStringToFloat() {
		Object result = TypeConverter.toType("2.5", "Float");
		assertThat(result).isInstanceOf(Float.class);
		assertThat((Float) result).isEqualTo(2.5f);
	}

	@Test
	void testStringToBoolean() {
		assertThat(TypeConverter.toType("true", "Boolean")).isEqualTo(true);
		assertThat(TypeConverter.toType("false", "Boolean")).isEqualTo(false);
	}

	@Test
	void testStringToByte() {
		Object result = TypeConverter.toType("7", "Byte");
		assertThat(result).isEqualTo((byte) 7);
	}

	@Test
	void testStringToShort() {
		Object result = TypeConverter.toType("256", "Short");
		assertThat(result).isEqualTo((short) 256);
	}

	@Test
	void testStringToCharacter() {
		Object result = TypeConverter.toType("A", "Character");
		assertThat(result).isEqualTo('A');
	}

	@Test
	void testInvalidStringToIntegerFails() {
		Object result = TypeConverter.toType("abc", "Integer");
		assertThat(result).isSameAs(TypeConverter.FAILED);
	}

	@Test
	void testInvalidStringToCharacterFails() {
		Object result = TypeConverter.toType("AB", "Character");
		assertThat(result).isSameAs(TypeConverter.FAILED);
	}

	@Test
	void testNumberToInteger() {
		Object result = TypeConverter.toType(42L, "Integer");
		assertThat(result).isEqualTo(42);
		assertThat(result).isInstanceOf(Integer.class);
	}

	@Test
	void testNumberToLong() {
		Object result = TypeConverter.toType(42, "Long");
		assertThat(result).isEqualTo(42L);
		assertThat(result).isInstanceOf(Long.class);
	}

	@Test
	void testNumberToDouble() {
		Object result = TypeConverter.toType(42, "Double");
		assertThat(result).isEqualTo(42.0);
		assertThat(result).isInstanceOf(Double.class);
	}

	@Test
	void testNumberToFloat() {
		Object result = TypeConverter.toType(42, "Float");
		assertThat(result).isInstanceOf(Float.class);
		assertThat((Float) result).isEqualTo(42.0f);
	}

	@Test
	void testBooleanToInteger() {
		assertThat(TypeConverter.toType(true, "Integer")).isEqualTo(1);
		assertThat(TypeConverter.toType(false, "Integer")).isEqualTo(0);
	}

	@Test
	void testBooleanToLong() {
		assertThat(TypeConverter.toType(true, "Long")).isEqualTo(1L);
		assertThat(TypeConverter.toType(false, "Long")).isEqualTo(0L);
	}

	@Test
	void testIntegerToString() {
		Object result = TypeConverter.toType(42, "String");
		assertThat(result).isEqualTo("42");
	}

	@Test
	void testBooleanToString() {
		assertThat(TypeConverter.toType(true, "String")).isEqualTo("true");
	}

	// Array tests

	@Test
	void testSingleValueToIntArray() {
		Object result = TypeConverter.toType(42, "int[]");
		assertThat(result).isInstanceOf(int[].class);
		assertThat((int[]) result).containsExactly(42);
	}

	@Test
	void testListToIntArray() {
		Object result = TypeConverter.toType(List.of(1, 2, 3), "int[]");
		assertThat(result).isInstanceOf(int[].class);
		assertThat((int[]) result).containsExactly(1, 2, 3);
	}

	@Test
	void testListToIntegerArray() {
		Object result = TypeConverter.toType(List.of(1, 2, 3), "Integer[]");
		assertThat(result).isInstanceOf(Integer[].class);
		assertThat((Integer[]) result).containsExactly(1, 2, 3);
	}

	@Test
	void testListToStringArray() {
		Object result = TypeConverter.toType(List.of("a", "b"), "String[]");
		assertThat(result).isInstanceOf(String[].class);
		assertThat((String[]) result).containsExactly("a", "b");
	}

	@Test
	void testSingleStringToStringArray() {
		Object result = TypeConverter.toType("hello", "String[]");
		assertThat(result).isInstanceOf(String[].class);
		assertThat((String[]) result).containsExactly("hello");
	}

	@Test
	void testListToBooleanArray() {
		Object result = TypeConverter.toType(List.of(true, false), "Boolean[]");
		assertThat(result).isInstanceOf(Boolean[].class);
		assertThat((Boolean[]) result).containsExactly(true, false);
	}

	// Collection tests

	@Test
	void testNullToCollection() {
		Object result = TypeConverter.toType(null, "Collection<Integer>");
		assertThat(result).isInstanceOf(List.class);
		assertThat((List<?>) result).isEmpty();
	}

	@Test
	void testSingleValueToCollection() {
		Object result = TypeConverter.toType(42, "Collection<Integer>");
		assertThat(result).isInstanceOf(List.class);
		@SuppressWarnings("unchecked")
		List<Integer> list = (List<Integer>) result;
		assertThat(list).containsExactly(42);
	}

	@Test
	void testListToCollection() {
		Object result = TypeConverter.toType(List.of(1, 2, 3), "Collection<Integer>");
		assertThat(result).isInstanceOf(List.class);
		@SuppressWarnings("unchecked")
		List<Integer> list = (List<Integer>) result;
		assertThat(list).containsExactly(1, 2, 3);
	}

	@Test
	void testNullToGenericCollection() {
		Object result = TypeConverter.toType(null, "Collection");
		assertThat(result).isInstanceOf(List.class);
		assertThat((List<?>) result).isEmpty();
	}

	@Test
	void testStringToGenericCollection() {
		Object result = TypeConverter.toType("hello", "Collection");
		assertThat(result).isInstanceOf(List.class);
		@SuppressWarnings("unchecked")
		List<String> list = (List<String>) result;
		assertThat(list).containsExactly("hello");
	}

	// Unknown type
	@Test
	void testUnknownTypeFails() {
		Object result = TypeConverter.toType("hello", "UnknownType");
		assertThat(result).isSameAs(TypeConverter.FAILED);
	}

	// isKnownType tests
	@Test
	void testIsKnownType() {
		assertThat(TypeConverter.isKnownType("Integer")).isTrue();
		assertThat(TypeConverter.isKnownType("String")).isTrue();
		assertThat(TypeConverter.isKnownType("Boolean")).isTrue();
		assertThat(TypeConverter.isKnownType("Integer[]")).isTrue();
		assertThat(TypeConverter.isKnownType("Collection<Integer>")).isTrue();
		assertThat(TypeConverter.isKnownType("Collection")).isTrue();
		assertThat(TypeConverter.isKnownType("UnknownType")).isFalse();
		assertThat(TypeConverter.isKnownType(null)).isFalse();
	}

	// getTypeInfoForValue tests
	@Test
	void testGetTypeInfoForValue() {
		assertThat(TypeConverter.getTypeInfoForValue(null)).isNull();
		assertThat(TypeConverter.getTypeInfoForValue("hello")).isNull();
		assertThat(TypeConverter.getTypeInfoForValue(42L)).isNull();
		assertThat(TypeConverter.getTypeInfoForValue(3.14)).isNull();
		assertThat(TypeConverter.getTypeInfoForValue(true)).isNull();

		assertThat(TypeConverter.getTypeInfoForValue(42)).isEqualTo("Integer");
		assertThat(TypeConverter.getTypeInfoForValue(3.14f)).isEqualTo("Float");
		assertThat(TypeConverter.getTypeInfoForValue((byte) 1)).isEqualTo("Byte");
		assertThat(TypeConverter.getTypeInfoForValue((short) 1)).isEqualTo("Short");
		assertThat(TypeConverter.getTypeInfoForValue('A')).isEqualTo("Character");
	}

	@Test
	void testGetTypeInfoForArrays() {
		assertThat(TypeConverter.getTypeInfoForValue(new int[] { 1, 2 })).isEqualTo("Integer[]");
		assertThat(TypeConverter.getTypeInfoForValue(new String[] { "a" })).isEqualTo("String[]");
		assertThat(TypeConverter.getTypeInfoForValue(new Integer[] { 1 })).isEqualTo("Integer[]");
		assertThat(TypeConverter.getTypeInfoForValue(new double[] { 1.0 })).isEqualTo("Double[]");
	}

	@Test
	void testGetTypeInfoForCollection() {
		assertThat(TypeConverter.getTypeInfoForValue(List.of(1, 2))).isEqualTo("Collection");
	}
}
