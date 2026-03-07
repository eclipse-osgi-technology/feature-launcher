/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.osgi.technology.featurelauncher.extras.installer.directory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.runtime.FeatureRuntime;
import org.osgi.service.featurelauncher.runtime.InstalledFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches a directory for feature JSON files and installs/updates/removes them
 * via FeatureRuntime.
 *
 */
class FeatureDirectoryWatcher {

	private static final Logger LOG = LoggerFactory.getLogger(FeatureDirectoryWatcher.class);

	enum ScanMode {
		ONCE, WATCH;

		static ScanMode fromString(String value) {
			if (value != null && value.equalsIgnoreCase(WATCH.name())) {
				return WATCH;
			}
			return ONCE;
		}
	}

	private static final String DEFAULT_PATTERN = "*.json";
	private static final String DEFAULT_SKIP_PATTERNS = "";

	private final FeatureRuntime featureRuntime;
	private final Path featuresDir;
	private final Path repoDir;
	private final ScanMode scanMode;
	private final long intervalSeconds;
	private final String featurePattern;
	private final String[] skipPatterns;

	private final Map<Path, TrackedFeature> trackedFeatures = new HashMap<>();
	private volatile Thread watchThread;

	FeatureDirectoryWatcher(FeatureRuntime featureRuntime, String featuresDir, String repoDir, String scanMode,
			long intervalSeconds, String featurePattern, String skipPatternsStr) {
		this.featureRuntime = featureRuntime;
		this.featuresDir = Paths.get(featuresDir);
		this.repoDir = repoDir != null && !repoDir.isEmpty() ? Paths.get(repoDir) : null;
		this.scanMode = ScanMode.fromString(scanMode);
		this.intervalSeconds = intervalSeconds > 0 ? intervalSeconds
				: FeatureDirectoryInstallerActivator.DEFAULT_SCAN_INTERVAL;
		this.featurePattern = featurePattern != null && !featurePattern.isEmpty() ? featurePattern : DEFAULT_PATTERN;

		String effectiveSkip = skipPatternsStr != null && !skipPatternsStr.isEmpty() ? skipPatternsStr
				: DEFAULT_SKIP_PATTERNS;
		this.skipPatterns = effectiveSkip.split(",");
		for (int i = 0; i < this.skipPatterns.length; i++) {
			this.skipPatterns[i] = this.skipPatterns[i].trim();
		}
	}

	void start() {
		if (!Files.isDirectory(featuresDir)) {
			LOG.error("Features directory does not exist or is not a directory: {}", featuresDir);
			return;
		}

		LOG.info("Starting directory watcher - dir={}, mode={}, pattern={}", featuresDir, scanMode, featurePattern);

		scan();

		if (ScanMode.WATCH == scanMode) {
			watchThread = Thread.ofVirtual().name("FeatureDirectoryWatcher").start(() -> {
				LOG.info("WATCH mode active - polling every {} seconds", intervalSeconds);
				Duration interval = Duration.ofSeconds(intervalSeconds);
				while (!Thread.currentThread().isInterrupted()) {
					try {
						Thread.sleep(interval);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
					scan();
				}
			});
		}
	}

	void stop() {
		Thread t = watchThread;
		if (t != null) {
			t.interrupt();
			try {
				t.join(5000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			watchThread = null;
		}
		LOG.info("Directory watcher stopped");
	}

	private void scan() {
		try {
			List<Path> currentFiles = collectFeatureFiles();
			Set<Path> currentSet = new HashSet<>(currentFiles);

			Set<Path> removed = new HashSet<>(trackedFeatures.keySet());
			removed.removeAll(currentSet);
			for (Path path : removed) {
				TrackedFeature tracked = trackedFeatures.remove(path);
				if (tracked != null && tracked.featureId != null) {
					LOG.info("Feature file removed: {}", path.getFileName());
					try {
						featureRuntime.remove(tracked.featureId);
						LOG.info("Successfully removed feature: {}", tracked.featureId);
					} catch (Exception e) {
						LOG.error("Failed to remove feature: {}", tracked.featureId, e);
					}
				}
			}

			for (Path path : currentFiles) {
				long lastModified = Files.getLastModifiedTime(path).toMillis();
				long size = Files.size(path);

				TrackedFeature tracked = trackedFeatures.get(path);
				if (tracked == null) {
					installFeature(path, lastModified, size);
				} else if (tracked.lastModified != lastModified || tracked.size != size) {
					LOG.info("Feature file changed: {}", path.getFileName());
					updateFeature(path, tracked, lastModified, size);
				}
			}
		} catch (Exception e) {
			LOG.error("Error during directory scan", e);
		}
	}

	private List<Path> collectFeatureFiles() {
		try (var stream = Files.list(featuresDir)) {
			return stream.filter(Files::isRegularFile)
					.filter(p -> matchesGlob(p.getFileName().toString(), featurePattern))
					.filter(p -> !shouldSkip(p.getFileName().toString()))
					.sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
		} catch (IOException e) {
			LOG.error("Error reading features directory: {}", featuresDir, e);
			return List.of();
		}
	}

	private boolean shouldSkip(String filename) {
		for (String pattern : skipPatterns) {
			if (matchesGlob(filename, pattern)) {
				return true;
			}
		}
		return false;
	}

	private boolean matchesGlob(String filename, String pattern) {
		if (pattern.startsWith("*")) {
			return filename.endsWith(pattern.substring(1));
		} else if (pattern.endsWith("*")) {
			return filename.startsWith(pattern.substring(0, pattern.length() - 1));
		} else if (pattern.contains("*")) {
			String[] parts = pattern.split("\\*", 2);
			return filename.startsWith(parts[0]) && filename.endsWith(parts[1]);
		} else {
			return filename.equals(pattern);
		}
	}

	private void installFeature(Path featureFile, long lastModified, long size) {
		String filename = featureFile.getFileName().toString();
		LOG.info("Installing feature: {}", filename);

		try (Reader reader = Files.newBufferedReader(featureFile)) {
			FeatureRuntime.InstallOperationBuilder builder = featureRuntime.install(reader);

			configureRepositories(builder);

			InstalledFeature installed = builder.install();
			ID featureId = installed.getFeature().getID();

			trackedFeatures.put(featureFile, new TrackedFeature(featureId, lastModified, size));
			LOG.info("Successfully installed feature: {} (ID: {})", filename, featureId);
		} catch (IOException e) {
			LOG.error("Failed to read feature file: {}", filename, e);
		} catch (Exception e) {
			LOG.error("Failed to install feature: {}", filename, e);
		}
	}

	private void updateFeature(Path featureFile, TrackedFeature tracked, long lastModified, long size) {
		String filename = featureFile.getFileName().toString();
		LOG.info("Updating feature: {}", filename);

		try (Reader reader = Files.newBufferedReader(featureFile)) {
			FeatureRuntime.UpdateOperationBuilder builder = featureRuntime.update(tracked.featureId, reader);

			configureRepositories(builder);

			InstalledFeature updated = builder.update();
			ID featureId = updated.getFeature().getID();

			trackedFeatures.put(featureFile, new TrackedFeature(featureId, lastModified, size));
			LOG.info("Successfully updated feature: {} (ID: {})", filename, featureId);
		} catch (IOException e) {
			LOG.error("Failed to read feature file: {}", filename, e);
		} catch (Exception e) {
			LOG.error("Failed to update feature: {}", filename, e);
		}
	}

	private void configureRepositories(FeatureRuntime.OperationBuilder<?> builder) {
		builder.useDefaultRepositories(true);
		addLocalRepository(builder);
	}

	private void addLocalRepository(FeatureRuntime.OperationBuilder<?> builder) {
		if (repoDir != null && Files.isDirectory(repoDir)) {
			builder.addRepository("local:" + repoDir, featureRuntime.createRepository(repoDir));
		}
	}

	private record TrackedFeature(ID featureId, long lastModified, long size) {
	}

}
