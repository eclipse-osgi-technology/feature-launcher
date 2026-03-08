/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.osgi.technology.featurelauncher.extras.installer.http.simple;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.runtime.FeatureRuntime;
import org.osgi.service.featurelauncher.runtime.InstalledFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches feature JSON files via HTTP and installs/updates/removes them
 * via FeatureRuntime.
 *
 * <p>
 * Supports two modes:
 * <ul>
 * <li><b>ONCE</b> - Fetch the feature list once and install all features found</li>
 * <li><b>WATCH</b> - Initial fetch + periodic polling for changes</li>
 * </ul>
 *
 * <p>
 * The configured features URL must return a JSON array of objects with feature
 * ID and URL, e.g.:
 * <pre>
 * [
 *   {"id": "org.example:app-feature:1.0.0", "url": "https://example.com/features/app.json"},
 *   {"id": "org.example:db-feature:2.0.0", "url": "https://example.com/features/db.json"}
 * ]
 * </pre>
 *
 * <p>
 * On startup the watcher checks {@link FeatureRuntime#getInstalledFeatures()}
 * to avoid reinstalling features that are already present in the runtime.
 */
class FeatureHttpWatcher {

	private static final Logger LOG = LoggerFactory.getLogger(FeatureHttpWatcher.class);

	enum ScanMode {
		ONCE, WATCH;

		static ScanMode fromString(String value) {
			if (value != null && value.equalsIgnoreCase(WATCH.name())) {
				return WATCH;
			}
			return ONCE;
		}
	}

	record FeatureEntry(String id, URI url) {
	}

	private final FeatureRuntime featureRuntime;
	private final URI featuresUrl;
	private final Path repoDir;
	private final ScanMode scanMode;
	private final long intervalSeconds;
	private final Duration requestTimeout;
	private final HttpClient httpClient;

	private final Map<String, TrackedFeature> trackedFeatures = new HashMap<>();
	private volatile Thread watchThread;

	FeatureHttpWatcher(FeatureRuntime featureRuntime, String featuresUrl, String repoDir,
			String scanMode, long intervalSeconds, long connectTimeoutSeconds,
			long requestTimeoutSeconds) {
		this.featureRuntime = featureRuntime;
		this.featuresUrl = URI.create(featuresUrl);
		this.repoDir = repoDir != null && !repoDir.isEmpty() ? Paths.get(repoDir) : null;
		this.scanMode = ScanMode.fromString(scanMode);
		this.intervalSeconds = intervalSeconds > 0 ? intervalSeconds
				: FeatureHttpInstallerActivator.DEFAULT_SCAN_INTERVAL;
		this.requestTimeout = Duration.ofSeconds(
				requestTimeoutSeconds > 0 ? requestTimeoutSeconds
						: FeatureHttpInstallerActivator.DEFAULT_REQUEST_TIMEOUT);
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(
						connectTimeoutSeconds > 0 ? connectTimeoutSeconds
								: FeatureHttpInstallerActivator.DEFAULT_CONNECT_TIMEOUT))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
	}

	void start() {
		LOG.info("Starting HTTP watcher - url={}, mode={}", featuresUrl, scanMode);

		scan();

		if (ScanMode.WATCH == scanMode) {
			watchThread = Thread.ofVirtual().name("FeatureHttpWatcher").start(() -> {
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
		LOG.info("HTTP watcher stopped");
	}

	private void scan() {
		try {
			List<FeatureEntry> entries = fetchFeatureEntries();
			Map<String, FeatureEntry> currentById = new LinkedHashMap<>();
			for (FeatureEntry entry : entries) {
				currentById.put(entry.id(), entry);
			}

			// On first scan, reconcile with runtime to avoid duplicate installs
			if (trackedFeatures.isEmpty() && !currentById.isEmpty()) {
				reconcileWithRuntime(currentById);
			}

			// Detect removed feature IDs
			Set<String> removed = new HashSet<>(trackedFeatures.keySet());
			removed.removeAll(currentById.keySet());
			for (String idStr : removed) {
				TrackedFeature tracked = trackedFeatures.remove(idStr);
				if (tracked != null && tracked.featureId() != null) {
					LOG.info("Feature removed from list: {}", idStr);
					try {
						featureRuntime.remove(tracked.featureId());
						LOG.info("Successfully removed feature: {}", idStr);
					} catch (Exception e) {
						LOG.error("Failed to remove feature: {}", idStr, e);
					}
				}
			}

			// New or changed
			for (Map.Entry<String, FeatureEntry> e : currentById.entrySet()) {
				String idStr = e.getKey();
				FeatureEntry entry = e.getValue();

				TrackedFeature tracked = trackedFeatures.get(idStr);
				if (tracked != null) {
					// Already tracked - check for content changes
					String content = fetchContent(entry.url());
					if (content == null) {
						continue;
					}
					String contentHash = sha256(content);
					if (!tracked.contentHash().equals(contentHash)) {
						LOG.info("Feature content changed: {}", idStr);
						updateFeature(idStr, tracked, entry.url(), content, contentHash);
					}
				} else {
					// Not tracked - check runtime before installing
					if (isInstalledInRuntime(idStr)) {
						LOG.info("Feature {} already installed in runtime, tracking", idStr);
						ID runtimeId = findIdInRuntime(idStr);
						String content = fetchContent(entry.url());
						String hash = content != null ? sha256(content) : "";
						trackedFeatures.put(idStr, new TrackedFeature(runtimeId, hash, entry.url()));
					} else {
						String content = fetchContent(entry.url());
						if (content == null) {
							continue;
						}
						String contentHash = sha256(content);
						installFeature(idStr, entry.url(), content, contentHash);
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Error during HTTP scan", e);
		}
	}

	private void reconcileWithRuntime(Map<String, FeatureEntry> currentById) {
		for (InstalledFeature installed : featureRuntime.getInstalledFeatures()) {
			String idStr = installed.getFeature().getID().toString();
			FeatureEntry entry = currentById.get(idStr);
			if (entry != null) {
				String content = fetchContent(entry.url());
				String hash = content != null ? sha256(content) : "";
				trackedFeatures.put(idStr, new TrackedFeature(
						installed.getFeature().getID(), hash, entry.url()));
				LOG.info("Reconciled existing feature: {}", idStr);
			}
		}
	}

	private boolean isInstalledInRuntime(String idStr) {
		return featureRuntime.getInstalledFeatures().stream()
				.anyMatch(f -> f.getFeature().getID().toString().equals(idStr));
	}

	private ID findIdInRuntime(String idStr) {
		return featureRuntime.getInstalledFeatures().stream()
				.filter(f -> f.getFeature().getID().toString().equals(idStr))
				.map(f -> f.getFeature().getID())
				.findFirst()
				.orElse(null);
	}

	private List<FeatureEntry> fetchFeatureEntries() {
		LOG.debug("Fetching feature list from: {}", featuresUrl);
		try {
			String body = fetchContent(featuresUrl);
			if (body == null) {
				return List.of();
			}
			return parseFeatureList(body);
		} catch (Exception e) {
			LOG.error("Error fetching feature list from: {}", featuresUrl, e);
			return List.of();
		}
	}

	String fetchContent(URI url) {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(url)
					.timeout(requestTimeout)
					.GET()
					.build();

			HttpResponse<String> response = httpClient.send(request,
					HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				return response.body();
			} else {
				LOG.error("HTTP {} fetching URL: {}", response.statusCode(), url);
				return null;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOG.debug("Interrupted while fetching URL: {}", url);
			return null;
		} catch (Exception e) {
			LOG.error("Error fetching URL: {}", url, e);
			return null;
		}
	}

	List<FeatureEntry> parseFeatureList(String jsonBody) {
		List<FeatureEntry> entries = new ArrayList<>();
		String trimmed = jsonBody.trim();
		if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
			LOG.error("Expected JSON array but got: {}...",
					trimmed.substring(0, Math.min(trimmed.length(), 100)));
			return entries;
		}

		String inner = trimmed.substring(1, trimmed.length() - 1).trim();
		if (inner.isEmpty()) {
			return entries;
		}

		// Split into top-level objects by tracking brace depth
		List<String> objects = new ArrayList<>();
		int braceDepth = 0;
		boolean inQuote = false;
		StringBuilder current = new StringBuilder();
		for (int i = 0; i < inner.length(); i++) {
			char c = inner.charAt(i);
			if (c == '"' && (i == 0 || inner.charAt(i - 1) != '\\')) {
				inQuote = !inQuote;
			}
			if (!inQuote) {
				if (c == '{') {
					braceDepth++;
				} else if (c == '}') {
					braceDepth--;
				} else if (c == ',' && braceDepth == 0) {
					objects.add(current.toString().trim());
					current.setLength(0);
					continue;
				}
			}
			current.append(c);
		}
		String last = current.toString().trim();
		if (!last.isEmpty()) {
			objects.add(last);
		}

		for (String obj : objects) {
			String id = extractJsonValue(obj, "id");
			String url = extractJsonValue(obj, "url");
			if (id != null && url != null) {
				entries.add(new FeatureEntry(id, URI.create(url)));
			} else {
				LOG.warn("Skipping malformed feature entry: {}...",
						obj.substring(0, Math.min(obj.length(), 100)));
			}
		}

		return entries;
	}

	private String extractJsonValue(String json, String key) {
		String searchKey = "\"" + key + "\"";
		int keyIdx = json.indexOf(searchKey);
		if (keyIdx < 0) {
			return null;
		}
		int colonIdx = json.indexOf(':', keyIdx + searchKey.length());
		if (colonIdx < 0) {
			return null;
		}
		// Find opening quote of value
		int openQuote = json.indexOf('"', colonIdx + 1);
		if (openQuote < 0) {
			return null;
		}
		// Find closing quote (not escaped)
		int closeQuote = openQuote + 1;
		while (closeQuote < json.length()) {
			if (json.charAt(closeQuote) == '"' && json.charAt(closeQuote - 1) != '\\') {
				break;
			}
			closeQuote++;
		}
		if (closeQuote >= json.length()) {
			return null;
		}
		return json.substring(openQuote + 1, closeQuote);
	}

	private void installFeature(String idStr, URI featureUrl, String content, String contentHash) {
		LOG.info("Installing feature {} from URL: {}", idStr, featureUrl);

		try (StringReader reader = new StringReader(content)) {
			FeatureRuntime.InstallOperationBuilder builder = featureRuntime.install(reader);

			configureRepositories(builder);

			InstalledFeature installed = builder.install();
			ID featureId = installed.getFeature().getID();

			trackedFeatures.put(idStr, new TrackedFeature(featureId, contentHash, featureUrl));
			LOG.info("Successfully installed feature: {} (ID: {})", idStr, featureId);
		} catch (Exception e) {
			LOG.error("Failed to install feature from URL: {}", featureUrl, e);
		}
	}

	private void updateFeature(String idStr, TrackedFeature tracked, URI featureUrl,
			String content, String contentHash) {
		LOG.info("Updating feature {} from URL: {}", idStr, featureUrl);

		try (StringReader reader = new StringReader(content)) {
			FeatureRuntime.UpdateOperationBuilder builder = featureRuntime.update(
					tracked.featureId(), reader);

			configureRepositories(builder);

			InstalledFeature updated = builder.update();
			ID featureId = updated.getFeature().getID();

			trackedFeatures.put(idStr, new TrackedFeature(featureId, contentHash, featureUrl));
			LOG.info("Successfully updated feature: {} (ID: {})", idStr, featureId);
		} catch (Exception e) {
			LOG.error("Failed to update feature from URL: {}", featureUrl, e);
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

	static String sha256(String content) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (Exception e) {
			throw new RuntimeException("SHA-256 not available", e);
		}
	}

	private record TrackedFeature(ID featureId, String contentHash, URI url) {
	}
}
