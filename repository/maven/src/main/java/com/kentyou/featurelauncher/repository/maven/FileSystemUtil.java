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
package com.kentyou.featurelauncher.repository.maven;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Util for file system operations.
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 30, 2024
 */
class FileSystemUtil {
	private FileSystemUtil() {
		// hidden constructor
	}

	public static void validateDirectory(Path path) {
		if (!path.toFile().exists()) {
			throw new IllegalArgumentException(String.format("Path '%s' does not exist!", path.toString()));
		}

		if (!path.toFile().isDirectory()) {
			throw new IllegalArgumentException(String.format("Path '%s' is not a directory!", path.toString()));
		}
	}

	/**
	 * Based on:
	 * {@link aQute.bnd.test.jupiter.TemporaryDirectoryExtension.delete(Path)}
	 **/
	public static void recursivelyDelete(Path path) throws IOException {
		path = path.toAbsolutePath();
		if (Files.notExists(path) && !Files.isSymbolicLink(path)) {
			return;
		}
		if (path.equals(path.getRoot()))
			throw new IllegalArgumentException("Cannot recursively delete root for safety reasons");

		Files.walkFileTree(path, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				try {
					Files.delete(file);
				} catch (IOException e) {
					throw exc;
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc != null) { // directory iteration failed
					throw exc;
				}
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

}
