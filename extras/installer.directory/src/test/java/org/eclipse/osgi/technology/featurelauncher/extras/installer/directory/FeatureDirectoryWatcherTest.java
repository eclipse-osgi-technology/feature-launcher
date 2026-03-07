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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.ID;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.runtime.FeatureRuntime;
import org.osgi.service.featurelauncher.runtime.InstalledFeature;

class FeatureDirectoryWatcherTest {

	@TempDir
	Path tempDir;

	private FeatureRuntime featureRuntime;
	private FeatureRuntime.InstallOperationBuilder installBuilder;
	private FeatureRuntime.UpdateOperationBuilder updateBuilder;
	private InstalledFeature installedFeature;
	private Feature feature;
	private ID featureId;

	@BeforeEach
	void setUp() {
		featureRuntime = mock(FeatureRuntime.class);
		installBuilder = mock(FeatureRuntime.InstallOperationBuilder.class);
		updateBuilder = mock(FeatureRuntime.UpdateOperationBuilder.class);
		installedFeature = mock(InstalledFeature.class);
		feature = mock(Feature.class);
		featureId = mock(ID.class);

		when(featureRuntime.install(any(Reader.class))).thenReturn(installBuilder);
		when(featureRuntime.update(any(ID.class), any(Reader.class))).thenReturn(updateBuilder);

		when(installBuilder.useDefaultRepositories(anyBoolean())).thenReturn(installBuilder);
		when(installBuilder.addRepository(anyString(), any(ArtifactRepository.class))).thenReturn(installBuilder);
		when(installBuilder.install()).thenReturn(installedFeature);

		when(updateBuilder.useDefaultRepositories(anyBoolean())).thenReturn(updateBuilder);
		when(updateBuilder.addRepository(anyString(), any(ArtifactRepository.class))).thenReturn(updateBuilder);
		when(updateBuilder.update()).thenReturn(installedFeature);

		when(installedFeature.getFeature()).thenReturn(feature);
		when(feature.getID()).thenReturn(featureId);
		when(featureId.toString()).thenReturn("org.example:test-feature:1.0.0");
	}

	@Test
	void onceMode_installsSingleFeature() throws Exception {
		Path featureFile = tempDir.resolve("10-app.json");
		Files.writeString(featureFile, "{}");

		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(), null, "ONCE",
				30, null, null);
		watcher.start();

		verify(featureRuntime, times(1)).install(any(Reader.class));
		verify(installBuilder).install();
	}

	@Test
	void onceMode_installsMultipleFeaturesSorted() throws Exception {
		Files.writeString(tempDir.resolve("20-second.json"), "{}");
		Files.writeString(tempDir.resolve("10-first.json"), "{}");
		Files.writeString(tempDir.resolve("30-third.json"), "{}");

		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(), null, "ONCE",
				30, null, null);
		watcher.start();

		verify(featureRuntime, times(3)).install(any(Reader.class));
	}

	@Test
	void noDefaultSkipPatterns_installsAll() throws Exception {
		Files.writeString(tempDir.resolve("00-bootstrap.json"), "{}");
		Files.writeString(tempDir.resolve("bootstrap.json"), "{}");
		Files.writeString(tempDir.resolve("10-app.json"), "{}");

		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(), null, "ONCE",
				30, null, null);
		watcher.start();

		verify(featureRuntime, times(3)).install(any(Reader.class));
	}

	@Test
	void skipsCustomPatterns() throws Exception {
		Files.writeString(tempDir.resolve("skip-me.json"), "{}");
		Files.writeString(tempDir.resolve("install-me.json"), "{}");

		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(), null, "ONCE",
				30, null, "skip-me.json");
		watcher.start();

		verify(featureRuntime, times(1)).install(any(Reader.class));
	}

	@Test
	void respectsFeaturePattern() throws Exception {
		Files.writeString(tempDir.resolve("feature.json"), "{}");
		Files.writeString(tempDir.resolve("feature.xml"), "<xml/>");
		Files.writeString(tempDir.resolve("data.txt"), "text");

		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(), null, "ONCE",
				30, "*.json", null);
		watcher.start();

		verify(featureRuntime, times(1)).install(any(Reader.class));
	}

	@Test
	void emptyDirectory_noInstalls() throws Exception {
		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(), null, "ONCE",
				30, null, null);
		watcher.start();

		verify(featureRuntime, never()).install(any(Reader.class));
	}

	@Test
	void nonExistentDirectory_doesNotThrow() {
		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, "/nonexistent/path", null, "ONCE",
				30, null, null);
		watcher.start();

		verify(featureRuntime, never()).install(any(Reader.class));
	}

	@Test
	void watchMode_detectsNewFile() throws Exception {
		Files.writeString(tempDir.resolve("10-first.json"), "{}");

		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(), null, "WATCH",
				1, null, null);
		watcher.start();

		verify(featureRuntime, times(1)).install(any(Reader.class));

		Files.writeString(tempDir.resolve("20-second.json"), "{}");

		verify(featureRuntime, timeout(3000).times(2)).install(any(Reader.class));

		watcher.stop();
	}

	@Test
	void watchMode_detectsRemovedFile() throws Exception {
		Path featureFile = tempDir.resolve("10-app.json");
		Files.writeString(featureFile, "{}");

		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(), null, "WATCH",
				1, null, null);
		watcher.start();

		verify(featureRuntime, times(1)).install(any(Reader.class));

		Files.delete(featureFile);

		verify(featureRuntime, timeout(3000)).remove(any(ID.class));

		watcher.stop();
	}

	@Test
	void watchMode_detectsChangedFile() throws Exception {
		Path featureFile = tempDir.resolve("10-app.json");
		Files.writeString(featureFile, "{}");

		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(), null, "WATCH",
				1, null, null);
		watcher.start();

		verify(featureRuntime, times(1)).install(any(Reader.class));

		Thread.sleep(1100);
		Files.writeString(featureFile, "{\"changed\": true}");

		verify(featureRuntime, timeout(3000)).update(any(ID.class), any(Reader.class));

		watcher.stop();
	}

	@Test
	void stopShutdownsExecutor() throws Exception {
		Files.writeString(tempDir.resolve("10-app.json"), "{}");

		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(), null, "WATCH",
				1, null, null);
		watcher.start();
		watcher.stop();

		int countBefore = countInstallInvocations();
		Thread.sleep(2000);
		int countAfter = countInstallInvocations();
		assertEquals(countBefore, countAfter, "No new installs should happen after stop");
	}

	@Test
	void configuresLocalRepository() throws Exception {
		Path repoDir = tempDir.resolve("repo");
		Files.createDirectory(repoDir);
		Files.writeString(tempDir.resolve("10-app.json"), "{}");

		ArtifactRepository mockRepo = mock(ArtifactRepository.class);
		when(featureRuntime.createRepository(repoDir)).thenReturn(mockRepo);

		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(),
				repoDir.toString(), "ONCE", 30, null, null);
		watcher.start();

		verify(featureRuntime).createRepository(repoDir);
		verify(installBuilder).addRepository(anyString(), any(ArtifactRepository.class));
	}

	@Test
	void nullScanModeDefaultsToOnce() throws Exception {
		Files.writeString(tempDir.resolve("10-app.json"), "{}");

		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(), null, null,
				30, null, null);
		watcher.start();

		verify(featureRuntime, times(1)).install(any(Reader.class));

		Thread.sleep(500);
		verify(featureRuntime, times(1)).install(any(Reader.class));
	}

	@Test
	void noDefaultSkipPatterns_installsAllFormats() throws Exception {
		Files.writeString(tempDir.resolve("00-anything.json"), "{}");
		Files.writeString(tempDir.resolve("bootstrap.json"), "{}");
		Files.writeString(tempDir.resolve("01-real-feature.json"), "{}");

		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(), null, "ONCE",
				30, null, null);
		watcher.start();

		verify(featureRuntime, times(3)).install(any(Reader.class));
	}

	@Test
	void installFailure_doesNotStopOtherInstalls() throws Exception {
		when(installBuilder.install()).thenThrow(new RuntimeException("test failure")).thenReturn(installedFeature);

		Files.writeString(tempDir.resolve("10-first.json"), "{}");
		Files.writeString(tempDir.resolve("20-second.json"), "{}");

		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(), null, "ONCE",
				30, null, null);
		watcher.start();

		verify(featureRuntime, times(2)).install(any(Reader.class));
	}

	@Test
	void noRepoDir_skipsRepositoryConfiguration() throws Exception {
		Files.writeString(tempDir.resolve("10-app.json"), "{}");

		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(), null, "ONCE",
				30, null, null);
		watcher.start();

		verify(featureRuntime, never()).createRepository(any(Path.class));
		verify(installBuilder, never()).addRepository(anyString(), any(ArtifactRepository.class));
	}

	@Test
	void onlyRegularFilesAreProcessed() throws Exception {
		Files.writeString(tempDir.resolve("10-app.json"), "{}");
		Files.createDirectory(tempDir.resolve("subdir.json")); // directory with .json suffix

		FeatureDirectoryWatcher watcher = new FeatureDirectoryWatcher(featureRuntime, tempDir.toString(), null, "ONCE",
				30, null, null);
		watcher.start();

		verify(featureRuntime, times(1)).install(any(Reader.class));
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
