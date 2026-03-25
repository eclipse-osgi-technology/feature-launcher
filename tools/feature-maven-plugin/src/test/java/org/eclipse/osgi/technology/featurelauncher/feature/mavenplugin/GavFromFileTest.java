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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.osgi.service.feature.ID;

class GavFromFileTest {

	private static final Path REPO = Path.of("/home/alice/.m2/repository");

	@Test
	void simpleJar() {
		Path artifact = REPO.resolve("com/example/demo/2.0.1/demo-2.0.1.jar");

		ID id = GavFromFile.parseIDFromPath(artifact, REPO);

		assertEquals("com.example", id.getGroupId());
		assertEquals("demo", id.getArtifactId());
		assertEquals("2.0.1", id.getVersion());
		assertEquals("jar", id.getType().orElse(null));
		assertTrue(id.getClassifier().isEmpty());
	}

	@Test
	void jarWithClassifier() {
		Path artifact = REPO.resolve("com/example/demo/2.0.1/demo-2.0.1-sources.jar");

		ID id = GavFromFile.parseIDFromPath(artifact, REPO);

		assertEquals("com.example", id.getGroupId());
		assertEquals("demo", id.getArtifactId());
		assertEquals("2.0.1", id.getVersion());
		assertEquals("jar", id.getType().orElse(null));
		assertEquals("sources", id.getClassifier().orElse(null));
	}

	@Test
	void deepGroupId() {
		Path artifact = REPO.resolve("org/apache/felix/org.apache.felix.framework/7.0.5/org.apache.felix.framework-7.0.5.jar");

		ID id = GavFromFile.parseIDFromPath(artifact, REPO);

		assertEquals("org.apache.felix", id.getGroupId());
		assertEquals("org.apache.felix.framework", id.getArtifactId());
		assertEquals("7.0.5", id.getVersion());
		assertEquals("jar", id.getType().orElse(null));
		assertTrue(id.getClassifier().isEmpty());
	}

	@Test
	void snapshotVersion() {
		Path artifact = REPO.resolve("org/osgi/org.osgi.resource/1.0.0-SNAPSHOT/org.osgi.resource-1.0.0-SNAPSHOT.jar");

		ID id = GavFromFile.parseIDFromPath(artifact, REPO);

		assertEquals("org.osgi", id.getGroupId());
		assertEquals("org.osgi.resource", id.getArtifactId());
		assertEquals("1.0.0-SNAPSHOT", id.getVersion());
		assertEquals("jar", id.getType().orElse(null));
		assertTrue(id.getClassifier().isEmpty());
	}

	@Test
	void singleSegmentGroupId() {
		Path artifact = REPO.resolve("commons/commons-lang/3.12.0/commons-lang-3.12.0.jar");

		ID id = GavFromFile.parseIDFromPath(artifact, REPO);

		assertEquals("commons", id.getGroupId());
		assertEquals("commons-lang", id.getArtifactId());
		assertEquals("3.12.0", id.getVersion());
	}

	@Test
	void warType() {
		Path artifact = REPO.resolve("com/example/webapp/1.0/webapp-1.0.war");

		ID id = GavFromFile.parseIDFromPath(artifact, REPO);

		assertEquals("com.example", id.getGroupId());
		assertEquals("webapp", id.getArtifactId());
		assertEquals("1.0", id.getVersion());
		assertEquals("war", id.getType().orElse(null));
		assertTrue(id.getClassifier().isEmpty());
	}

	@Test
	void pathNotInsideRepo() {
		Path artifact = Path.of("/other/path/com/example/demo/1.0/demo-1.0.jar");

		assertThrows(IllegalArgumentException.class, () -> GavFromFile.parseIDFromPath(artifact, REPO));
	}

	@Test
	void pathTooShort() {
		Path artifact = REPO.resolve("too/short.jar");

		assertThrows(IllegalArgumentException.class, () -> GavFromFile.parseIDFromPath(artifact, REPO));
	}
}
