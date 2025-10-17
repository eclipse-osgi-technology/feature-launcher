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

import java.util.Objects;
import java.util.Optional;

import org.osgi.service.feature.ID;

public record IDImpl(String groupId, String artifactId, String version, Optional<String> type,
        Optional<String> classifier) implements ID {

	/**
	 * Construct an ID from a Maven ID. Maven IDs have the following syntax:
	 * <p>
	 * {@code group-id ':' artifact-id [ ':' [type] [ ':' classifier ] ] ':' version}
	 *
	 * @param mavenID
	 * @return The ID
	 * @throws IllegalArgumentException if the mavenID does not match the Syntax
	 */
	public static IDImpl fromMavenID(String mavenID) throws IllegalArgumentException {
		var parts = mavenID.split(":");

		if (mavenID.startsWith(":") || mavenID.endsWith(":") || mavenID.contains("::")) {
			throw new IllegalArgumentException("Not a valid maven ID" + mavenID);
		}

		if (parts.length < 3 || parts.length > 5) {
			throw new IllegalArgumentException("Not a valid maven ID" + mavenID);
		}

		var gid = parts[0];
		var aid = parts[1];
		String ver = null;
		Optional<String> t = Optional.empty();
		Optional<String> c = Optional.empty();

		if (parts.length == 3) {
			ver = parts[2];
		} else if (parts.length == 4) {
			t = Optional.of(parts[2]);
			ver = parts[3];
		} else {
			t = Optional.of(parts[2]);
			c = Optional.of(parts[3]);
			ver = parts[4];
		}
		return new IDImpl(gid, aid, ver, t, c);
	}

	/**
	 * Construct an ID
	 * 
	 * @param groupId    The group ID.
	 * @param artifactId The artifact ID.
	 * @param version    The version.
	 * @param type       The type identifier.
	 * @param classifier The classifier.
	 * @throws NullPointerException     if one of the parameters (groupId,
	 *                                  artifactId, version) is null.
	 * @throws IllegalArgumentException if one of the parameters is empty or
	 *                                  contains an colon `:` or if a classifier is
	 *                                  used without a type.
	 */
	public IDImpl(String groupId, String artifactId, String version, Optional<String> type,
	        Optional<String> classifier) {

		Objects.requireNonNull(groupId, "groupId");
		Objects.requireNonNull(artifactId, "artifact");
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(classifier, "classifier");

		if (groupId.isEmpty()) {
			throw new IllegalArgumentException("groupId must not be empty");
		}
		if (artifactId.isEmpty()) {
			throw new IllegalArgumentException("artifactId must not be empty");
		}
		if (version.isEmpty()) {
			throw new IllegalArgumentException("version must not be empty");
		}

		if (type.isPresent() && type.get().isEmpty()) {
			throw new IllegalArgumentException("type must not be empty");
		}

		if (classifier.isPresent() && classifier.get().isEmpty()) {
			throw new IllegalArgumentException("classifier must not be empty");
		}

		if (groupId.contains(":")) {
			throw new IllegalArgumentException("groupId must not contain a colon `:`");
		}
		if (artifactId.contains(":")) {
			throw new IllegalArgumentException("artifactId must not contain a colon `:`");
		}
		if (version.contains(":")) {
			throw new IllegalArgumentException("version must not contain a colon `:`");
		}
		if (type.isPresent() && type.get().contains(":")) {
			throw new IllegalArgumentException("type must not contain a colon `:`");
		}
		if (classifier.isPresent() && classifier.get().contains(":")) {
			throw new IllegalArgumentException("classifier must not contain a colon `:`");
		}
		if (type.isEmpty() && classifier.isPresent()) {
			throw new IllegalArgumentException("type must not be `null` if a classifier is set");
		}
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.type = type;
		this.classifier = classifier;
	}

	/**
	 * Get the group ID.
	 *
	 * @return The group ID.
	 */
	@Override
	public String getGroupId() {
		return groupId;
	}

	/**
	 * Get the artifact ID.
	 *
	 * @return The artifact ID.
	 */
	@Override
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * Get the version.
	 *
	 * @return The version.
	 */
	@Override
	public String getVersion() {
		return version;
	}

	/**
	 * Get the type identifier.
	 *
	 * @return The type identifier.
	 */
	@Override
	public Optional<String> getType() {
		return type;
	}

	/**
	 * Get the classifier.
	 *
	 * @return The classifier.
	 */
	@Override
	public Optional<String> getClassifier() {
		return classifier;
	}

	/**
	 * This method returns the ID using the following syntax:
	 * <p>
	 * {@code groupId ':' artifactId ( ':' type ( ':' classifier )? )? ':' version }
	 *
	 * @return The string representation.
	 */
	@Override
	public String toString() {
		var sb = new StringBuilder(groupId).append(":").append(artifactId);

		if (type.isPresent()) {
			sb = sb.append(":").append(type.get());
			if (classifier.isPresent()) {
				sb = sb.append(":").append(classifier.get());
			}
		}
		return sb.append(":").append(version).toString();
	}
}