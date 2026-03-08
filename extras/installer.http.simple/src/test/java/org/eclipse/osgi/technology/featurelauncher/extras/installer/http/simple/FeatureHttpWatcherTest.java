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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.runtime.FeatureRuntime;
import org.osgi.service.featurelauncher.runtime.InstalledFeature;

import com.sun.net.httpserver.HttpServer;

class FeatureHttpWatcherTest {

	@TempDir
	Path tempDir;

	private FeatureRuntime featureRuntime;
	private FeatureRuntime.InstallOperationBuilder installBuilder;
	private FeatureRuntime.UpdateOperationBuilder updateBuilder;
	private InstalledFeature installedFeature;
	private Feature feature;
	private ID featureId;
	private HttpServer server;
	private String baseUrl;

	@BeforeEach
	void setUp() throws IOException {
		featureRuntime = mock(FeatureRuntime.class);
		installBuilder = mock(FeatureRuntime.InstallOperationBuilder.class);
		updateBuilder = mock(FeatureRuntime.UpdateOperationBuilder.class);
		installedFeature = mock(InstalledFeature.class);
		feature = mock(Feature.class);
		featureId = mock(ID.class);

		when(featureRuntime.install(any(Reader.class))).thenReturn(installBuilder);
		when(featureRuntime.update(any(ID.class), any(Reader.class))).thenReturn(updateBuilder);
		when(featureRuntime.getInstalledFeatures()).thenReturn(List.of());

		when(installBuilder.useDefaultRepositories(anyBoolean())).thenReturn(installBuilder);
		when(installBuilder.addRepository(anyString(), any(ArtifactRepository.class))).thenReturn(installBuilder);
		when(installBuilder.install()).thenReturn(installedFeature);

		when(updateBuilder.useDefaultRepositories(anyBoolean())).thenReturn(updateBuilder);
		when(updateBuilder.addRepository(anyString(), any(ArtifactRepository.class))).thenReturn(updateBuilder);
		when(updateBuilder.update()).thenReturn(installedFeature);

		when(installedFeature.getFeature()).thenReturn(feature);
		when(feature.getID()).thenReturn(featureId);
		when(featureId.toString()).thenReturn("org.example:test-feature:1.0.0");

		server = HttpServer.create(new InetSocketAddress(0), 0);
		server.start();
		baseUrl = "http://localhost:" + server.getAddress().getPort();
	}

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void onceMode_installsSingleFeature() {
		serveJson("/feature1.json", "{}");
		serveJson("/features.json", featureList(
				entry("org.example:test-feature:1.0.0", baseUrl + "/feature1.json")));

		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "ONCE");
		watcher.start();

		verify(featureRuntime, times(1)).install(any(Reader.class));
		verify(installBuilder).install();
	}

	@Test
	void onceMode_installsMultipleFeatures() {
		serveJson("/feature1.json", "{}");
		serveJson("/feature2.json", "{}");
		serveJson("/feature3.json", "{}");

		// Each install returns a different feature ID
		ID id1 = mock(ID.class);
		ID id2 = mock(ID.class);
		ID id3 = mock(ID.class);
		when(id1.toString()).thenReturn("org.example:feature1:1.0.0");
		when(id2.toString()).thenReturn("org.example:feature2:1.0.0");
		when(id3.toString()).thenReturn("org.example:feature3:1.0.0");

		Feature f1 = mock(Feature.class);
		Feature f2 = mock(Feature.class);
		Feature f3 = mock(Feature.class);
		when(f1.getID()).thenReturn(id1);
		when(f2.getID()).thenReturn(id2);
		when(f3.getID()).thenReturn(id3);

		InstalledFeature if1 = mock(InstalledFeature.class);
		InstalledFeature if2 = mock(InstalledFeature.class);
		InstalledFeature if3 = mock(InstalledFeature.class);
		when(if1.getFeature()).thenReturn(f1);
		when(if2.getFeature()).thenReturn(f2);
		when(if3.getFeature()).thenReturn(f3);

		when(installBuilder.install()).thenReturn(if1, if2, if3);

		serveJson("/features.json", featureList(
				entry("org.example:feature1:1.0.0", baseUrl + "/feature1.json"),
				entry("org.example:feature2:1.0.0", baseUrl + "/feature2.json"),
				entry("org.example:feature3:1.0.0", baseUrl + "/feature3.json")));

		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "ONCE");
		watcher.start();

		verify(featureRuntime, times(3)).install(any(Reader.class));
	}

	@Test
	void emptyList_noInstalls() {
		serveJson("/features.json", "[]");

		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "ONCE");
		watcher.start();

		verify(featureRuntime, never()).install(any(Reader.class));
	}

	@Test
	void unreachableUrl_noInstalls() {
		FeatureHttpWatcher watcher = createWatcher("http://localhost:1/features.json", "ONCE");
		watcher.start();

		verify(featureRuntime, never()).install(any(Reader.class));
	}

	@Test
	void httpError_skipsFeature() {
		serveJson("/feature1.json", "{}");
		serveError("/feature2.json", 404);
		serveJson("/features.json", featureList(
				entry("org.example:feature1:1.0.0", baseUrl + "/feature1.json"),
				entry("org.example:feature2:1.0.0", baseUrl + "/feature2.json")));

		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "ONCE");
		watcher.start();

		verify(featureRuntime, times(1)).install(any(Reader.class));
	}

	@Test
	void watchMode_detectsNewUrl() {
		serveJson("/feature1.json", "{}");
		serveJson("/feature2.json", "{}");

		ID id2 = mock(ID.class);
		when(id2.toString()).thenReturn("org.example:feature2:1.0.0");
		Feature f2 = mock(Feature.class);
		when(f2.getID()).thenReturn(id2);
		InstalledFeature if2 = mock(InstalledFeature.class);
		when(if2.getFeature()).thenReturn(f2);
		when(installBuilder.install()).thenReturn(installedFeature, if2);

		AtomicReference<String> listBody = new AtomicReference<>(featureList(
				entry("org.example:test-feature:1.0.0", baseUrl + "/feature1.json")));
		serveDynamic("/features.json", listBody);

		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "WATCH", 1);
		watcher.start();

		verify(featureRuntime, times(1)).install(any(Reader.class));

		listBody.set(featureList(
				entry("org.example:test-feature:1.0.0", baseUrl + "/feature1.json"),
				entry("org.example:feature2:1.0.0", baseUrl + "/feature2.json")));

		verify(featureRuntime, timeout(3000).times(2)).install(any(Reader.class));

		watcher.stop();
	}

	@Test
	void watchMode_detectsRemovedUrl() {
		serveJson("/feature1.json", "{}");

		AtomicReference<String> listBody = new AtomicReference<>(featureList(
				entry("org.example:test-feature:1.0.0", baseUrl + "/feature1.json")));
		serveDynamic("/features.json", listBody);

		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "WATCH", 1);
		watcher.start();

		verify(featureRuntime, times(1)).install(any(Reader.class));

		listBody.set("[]");

		verify(featureRuntime, timeout(3000)).remove(any(ID.class));

		watcher.stop();
	}

	@Test
	void watchMode_detectsChangedContent() {
		AtomicReference<String> featureBody = new AtomicReference<>("{}");
		serveDynamic("/feature1.json", featureBody);
		serveJson("/features.json", featureList(
				entry("org.example:test-feature:1.0.0", baseUrl + "/feature1.json")));

		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "WATCH", 1);
		watcher.start();

		verify(featureRuntime, times(1)).install(any(Reader.class));

		featureBody.set("{\"changed\": true}");

		verify(featureRuntime, timeout(3000)).update(any(ID.class), any(Reader.class));

		watcher.stop();
	}

	@Test
	void stopShutdownsPolling() throws Exception {
		serveJson("/feature1.json", "{}");
		serveJson("/features.json", featureList(
				entry("org.example:test-feature:1.0.0", baseUrl + "/feature1.json")));

		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "WATCH", 1);
		watcher.start();
		watcher.stop();

		int countBefore = countInstallInvocations();
		Thread.sleep(2000);
		int countAfter = countInstallInvocations();
		assertEquals(countBefore, countAfter, "No new installs should happen after stop");
	}

	@Test
	void configuresLocalRepository() {
		Path repoDir = tempDir.resolve("repo");
		try {
			Files.createDirectory(repoDir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		serveJson("/feature1.json", "{}");
		serveJson("/features.json", featureList(
				entry("org.example:test-feature:1.0.0", baseUrl + "/feature1.json")));

		ArtifactRepository mockRepo = mock(ArtifactRepository.class);
		when(featureRuntime.createRepository(repoDir)).thenReturn(mockRepo);

		FeatureHttpWatcher watcher = new FeatureHttpWatcher(featureRuntime,
				baseUrl + "/features.json", repoDir.toString(),
				"ONCE", 60, 5, 5);
		watcher.start();

		verify(featureRuntime).createRepository(repoDir);
		verify(installBuilder).addRepository(anyString(), any(ArtifactRepository.class));
	}

	@Test
	void noRepoDir_skipsRepositoryConfiguration() {
		serveJson("/feature1.json", "{}");
		serveJson("/features.json", featureList(
				entry("org.example:test-feature:1.0.0", baseUrl + "/feature1.json")));

		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "ONCE");
		watcher.start();

		verify(featureRuntime, never()).createRepository(any(Path.class));
		verify(installBuilder, never()).addRepository(anyString(), any(ArtifactRepository.class));
	}

	@Test
	void nullScanModeDefaultsToOnce() throws Exception {
		serveJson("/feature1.json", "{}");
		serveJson("/features.json", featureList(
				entry("org.example:test-feature:1.0.0", baseUrl + "/feature1.json")));

		FeatureHttpWatcher watcher = new FeatureHttpWatcher(featureRuntime,
				baseUrl + "/features.json", null,
				null, 60, 5, 5);
		watcher.start();

		verify(featureRuntime, times(1)).install(any(Reader.class));

		Thread.sleep(500);
		verify(featureRuntime, times(1)).install(any(Reader.class));
	}

	@Test
	void installFailure_doesNotStopOtherInstalls() {
		ID id2 = mock(ID.class);
		when(id2.toString()).thenReturn("org.example:feature2:1.0.0");
		Feature f2 = mock(Feature.class);
		when(f2.getID()).thenReturn(id2);
		InstalledFeature if2 = mock(InstalledFeature.class);
		when(if2.getFeature()).thenReturn(f2);

		when(installBuilder.install())
				.thenThrow(new RuntimeException("test failure"))
				.thenReturn(if2);

		serveJson("/feature1.json", "{}");
		serveJson("/feature2.json", "{}");
		serveJson("/features.json", featureList(
				entry("org.example:feature1:1.0.0", baseUrl + "/feature1.json"),
				entry("org.example:feature2:1.0.0", baseUrl + "/feature2.json")));

		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "ONCE");
		watcher.start();

		verify(featureRuntime, times(2)).install(any(Reader.class));
	}

	@Test
	void alreadyInstalledFeature_skipsInstall() {
		serveJson("/feature1.json", "{}");
		serveJson("/features.json", featureList(
				entry("org.example:test-feature:1.0.0", baseUrl + "/feature1.json")));

		// Feature already exists in runtime
		InstalledFeature existing = mock(InstalledFeature.class);
		Feature existingFeature = mock(Feature.class);
		ID existingId = mock(ID.class);
		when(existingId.toString()).thenReturn("org.example:test-feature:1.0.0");
		when(existingFeature.getID()).thenReturn(existingId);
		when(existing.getFeature()).thenReturn(existingFeature);
		when(featureRuntime.getInstalledFeatures()).thenReturn(List.of(existing));

		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "ONCE");
		watcher.start();

		verify(featureRuntime, never()).install(any(Reader.class));
	}

	@Test
	void afterRestart_reconcileWithRuntime_skipsReinstall() {
		serveJson("/feature1.json", "{}");
		serveJson("/features.json", featureList(
				entry("org.example:test-feature:1.0.0", baseUrl + "/feature1.json")));

		// Simulate runtime already has the feature installed
		InstalledFeature existing = mock(InstalledFeature.class);
		Feature existingFeature = mock(Feature.class);
		ID existingId = mock(ID.class);
		when(existingId.toString()).thenReturn("org.example:test-feature:1.0.0");
		when(existingFeature.getID()).thenReturn(existingId);
		when(existing.getFeature()).thenReturn(existingFeature);
		when(featureRuntime.getInstalledFeatures()).thenReturn(List.of(existing));

		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "ONCE");
		watcher.start();

		// Should NOT call install since it's already in runtime
		verify(featureRuntime, never()).install(any(Reader.class));
		// Should NOT call update since content hasn't changed
		verify(featureRuntime, never()).update(any(ID.class), any(Reader.class));
	}

	@Test
	void parseFeatureList_validJsonArray() {
		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "ONCE");
		List<FeatureHttpWatcher.FeatureEntry> entries = watcher.parseFeatureList(
				"[{\"id\":\"org.example:app:1.0.0\",\"url\":\"http://a.com/f1.json\"},"
						+ "{\"id\":\"org.example:db:2.0.0\",\"url\":\"http://b.com/f2.json\"}]");
		assertEquals(2, entries.size());
		assertEquals("org.example:app:1.0.0", entries.get(0).id());
		assertEquals(URI.create("http://a.com/f1.json"), entries.get(0).url());
		assertEquals("org.example:db:2.0.0", entries.get(1).id());
		assertEquals(URI.create("http://b.com/f2.json"), entries.get(1).url());
	}

	@Test
	void parseFeatureList_emptyArray() {
		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "ONCE");
		List<FeatureHttpWatcher.FeatureEntry> entries = watcher.parseFeatureList("[]");
		assertTrue(entries.isEmpty());
	}

	@Test
	void parseFeatureList_invalidFormat() {
		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "ONCE");
		List<FeatureHttpWatcher.FeatureEntry> entries = watcher.parseFeatureList("not a json array");
		assertTrue(entries.isEmpty());
	}

	@Test
	void parseFeatureList_withWhitespace() {
		FeatureHttpWatcher watcher = createWatcher(baseUrl + "/features.json", "ONCE");
		List<FeatureHttpWatcher.FeatureEntry> entries = watcher.parseFeatureList(
				"  [\n  {\"id\": \"org.example:app:1.0.0\", \"url\": \"http://a.com/f1.json\"} ,\n"
						+ "  {\"id\": \"org.example:db:2.0.0\", \"url\": \"http://b.com/f2.json\"}\n]  ");
		assertEquals(2, entries.size());
		assertEquals("org.example:app:1.0.0", entries.get(0).id());
		assertEquals("org.example:db:2.0.0", entries.get(1).id());
	}

	@Test
	void sha256_producesConsistentHash() {
		String hash1 = FeatureHttpWatcher.sha256("test content");
		String hash2 = FeatureHttpWatcher.sha256("test content");
		assertEquals(hash1, hash2);
	}

	@Test
	void sha256_differentContentProducesDifferentHash() {
		String hash1 = FeatureHttpWatcher.sha256("content A");
		String hash2 = FeatureHttpWatcher.sha256("content B");
		assertTrue(!hash1.equals(hash2));
	}

	// --- helpers ---

	private FeatureHttpWatcher createWatcher(String featuresUrl, String scanMode) {
		return createWatcher(featuresUrl, scanMode, 60);
	}

	private FeatureHttpWatcher createWatcher(String featuresUrl, String scanMode, long interval) {
		return new FeatureHttpWatcher(featureRuntime, featuresUrl, null,
				scanMode, interval, 5, 5);
	}

	private String featureList(String... entries) {
		return "[" + String.join(",", entries) + "]";
	}

	private String entry(String id, String url) {
		return "{\"id\":\"" + id + "\",\"url\":\"" + url + "\"}";
	}

	private void serveJson(String path, String body) {
		server.createContext(path, exchange -> {
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(bytes);
			}
		});
	}

	private void serveError(String path, int statusCode) {
		server.createContext(path, exchange -> {
			exchange.sendResponseHeaders(statusCode, -1);
			exchange.close();
		});
	}

	private void serveDynamic(String path, AtomicReference<String> bodyRef) {
		server.createContext(path, exchange -> {
			byte[] bytes = bodyRef.get().getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(bytes);
			}
		});
	}

	private int countInstallInvocations() {
		try {
			verify(featureRuntime, times(0)).install(any(Reader.class));
			return 0;
		} catch (AssertionError e) {
			return -1;
		}
	}
}
