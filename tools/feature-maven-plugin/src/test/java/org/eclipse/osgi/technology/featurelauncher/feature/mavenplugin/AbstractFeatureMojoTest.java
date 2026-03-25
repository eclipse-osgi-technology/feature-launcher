/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Stefan Bischof - initial
 */
package org.eclipse.osgi.technology.featurelauncher.feature.mavenplugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Organization;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the utility methods in AbstractFeatureMojo.
 */
class AbstractFeatureMojoTest {

	private TestMojo mojo;
	private MavenProject project;

	@BeforeEach
	void setUp() {
		project = new MavenProject();
		project.setGroupId("org.example");
		project.setArtifactId("test-project");
		project.setVersion("1.0.0");
		project.setName("Test Project");

		mojo = new TestMojo();
		mojo.project = project;
	}

	// --- extractProjectVariables ---

	@Test
	void extractProjectVariables_noType_usesStringDefault() {
		// Format: osgi.feature.var._name (no type before underscore = empty string = String)
		project.getProperties().put("osgi.feature.var._myVar", "hello");

		Map<String, Object> vars = mojo.extractProjectVariables();

		assertEquals(1, vars.size());
		assertEquals("hello", vars.get("myVar"));
	}

	@Test
	void extractProjectVariables_stringTyped() {
		project.getProperties().put("osgi.feature.var.String_myStr", "world");

		Map<String, Object> vars = mojo.extractProjectVariables();

		assertEquals("world", vars.get("myStr"));
	}

	@Test
	void extractProjectVariables_integer() {
		project.getProperties().put("osgi.feature.var.Integer_count", "42");

		Map<String, Object> vars = mojo.extractProjectVariables();

		assertEquals(42, vars.get("count"));
		assertInstanceOf(Integer.class, vars.get("count"));
	}

	@Test
	void extractProjectVariables_double() {
		project.getProperties().put("osgi.feature.var.Double_ratio", "3.14");

		Map<String, Object> vars = mojo.extractProjectVariables();

		assertEquals(3.14, vars.get("ratio"));
		assertInstanceOf(Double.class, vars.get("ratio"));
	}

	@Test
	void extractProjectVariables_boolean() {
		project.getProperties().put("osgi.feature.var.Boolean_enabled", "true");

		Map<String, Object> vars = mojo.extractProjectVariables();

		assertEquals(true, vars.get("enabled"));
		assertInstanceOf(Boolean.class, vars.get("enabled"));
	}

	@Test
	void extractProjectVariables_noPrefix() {
		project.getProperties().put("some.other.property", "ignored");

		Map<String, Object> vars = mojo.extractProjectVariables();

		assertTrue(vars.isEmpty());
	}

	@Test
	void extractProjectVariables_unknownType() {
		project.getProperties().put("osgi.feature.var.Unknown_foo", "bar");

		assertThrows(IllegalArgumentException.class, () -> mojo.extractProjectVariables());
	}

	// --- extractProjectCategories ---

	@Test
	void extractProjectCategories() {
		project.getProperties().put("osgi.feature.category.1", "web");
		project.getProperties().put("osgi.feature.category.2", "persistence");

		List<String> categories = mojo.extractProjectCategories();

		assertEquals(2, categories.size());
		assertTrue(categories.contains("web"));
		assertTrue(categories.contains("persistence"));
	}

	@Test
	void extractProjectCategories_empty() {
		List<String> categories = mojo.extractProjectCategories();

		assertTrue(categories.isEmpty());
	}

	// --- buildProjectGav ---

	@Test
	void buildProjectGav() {
		assertEquals("org.example:test-project:1.0.0", mojo.buildProjectGav());
	}

	// --- getProjectVendor ---

	@Test
	void getProjectVendor() {
		Organization org = new Organization();
		org.setName("Eclipse Foundation");
		project.setOrganization(org);

		assertEquals("Eclipse Foundation", mojo.getProjectVendor());
	}

	@Test
	void getProjectVendor_null() {
		assertEquals(null, mojo.getProjectVendor());
	}

	/**
	 * Minimal concrete subclass to test protected methods.
	 */
	static class TestMojo extends AbstractFeatureMojo {
		@Override
		public void execute() {}
	}
}
