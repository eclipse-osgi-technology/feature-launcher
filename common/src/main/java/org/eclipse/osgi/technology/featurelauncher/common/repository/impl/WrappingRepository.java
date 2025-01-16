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
package org.eclipse.osgi.technology.featurelauncher.common.repository.impl;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.eclipse.osgi.technology.featurelauncher.common.util.impl.FileSystemUtil;
import org.osgi.service.feature.ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.osgi.technology.featurelauncher.repository.spi.FileSystemRepository;
import org.eclipse.osgi.technology.featurelauncher.repository.spi.Repository;

/**
 * Converts a {@link Repository} with no File backing into a file-backed {@link Repository}
 */
public class WrappingRepository implements FileSystemRepository {

	private static final Logger LOG = LoggerFactory.getLogger(WrappingRepository.class);
	
	private final Repository wrapped;
	
	private final Path localRepoPath;
	
	public WrappingRepository(Repository toWrap, String name) {
		Objects.requireNonNull(toWrap, "A repository must be supplied for wrapping");
		this.wrapped = toWrap;

		if(this.wrapped instanceof FileSystemRepository) {
			localRepoPath = null;
		} else {
			localRepoPath = createTemporaryFolder();
		}
	}
	
	private Path createTemporaryFolder() {
		try {
			Path localRepositoryPath = Files.createTempDirectory("featurelauncherM2repo_");

			deleteOnShutdown(localRepositoryPath);

			return localRepositoryPath;

		} catch (IOException e) {
			throw new IllegalStateException("Could not create temporary local artifact repository!", e);
		}
	}
	
	private void deleteOnShutdown(Path localRepositoryPath) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					FileSystemUtil.recursivelyDelete(localRepositoryPath);
				} catch (IOException e) {
					LOG.warn("Could not delete temporary local artifact repository!");
				}
			}
		});
	}
	
	@Override
	public InputStream getArtifactData(ID id) {
		if(localRepoPath == null) {
			return wrapped.getArtifactData(id);
		} else {
			try {
				return Files.newInputStream(getArtifactPath(id), READ);
			} catch (IOException e) {
				return null;
			}
		}
	}

	@Override
	public Path getArtifactPath(ID id) {
		if(localRepoPath == null) {
			return ((FileSystemRepository)wrapped).getArtifactPath(id);
		} else {
			Path filePath = getFilePath(id);
			
			if(Files.exists(filePath)) {
				return filePath;
			}
			
			InputStream is = getArtifactData(id);
			if(is == null) {
				return null;
			}
			try(is) {
				Files.createDirectories(filePath.getParent());
				try(OutputStream os = Files.newOutputStream(filePath, CREATE_NEW)) {
					is.transferTo(os);
				}
				return filePath;
			} catch(IOException ioe) {
				LOG.error("Failed caching artifact {}", id);
			}
		}
		return null;
	}

	private Path getFilePath(ID id) {
		Path p = localRepoPath.resolve(id.getGroupId())
				.resolve(id.getArtifactId())
				.resolve(id.getVersion());
		String fileName = "file" + 
				id.getClassifier().map(c -> "-" + c).orElse("") +
				id.getType().orElse("jar");

		Path filePath = p.resolve(fileName);
		return filePath;
	}

	@Override
	public Path getLocalRepositoryPath() {
		if(localRepoPath != null) {
			return ((FileSystemRepository)wrapped).getLocalRepositoryPath();
		} else {
			return localRepoPath;
		}
	}

	@Override
	public String getName() {
		return wrapped.getName();
	}

	@Override
	public String toString() {
		return "Wrapped Artifact repository: Name = " + getName() + " repo = " + wrapped;
	}
}
