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
package com.kentyou.prototype.featurelauncher.launch.launcher;

import static com.kentyou.prototype.featurelauncher.common.decorator.impl.FrameworkLaunchingPropertiesFeatureExtensionHandlerImpl.getDefaultFrameworkProperties;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

import org.osgi.framework.launch.Framework;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.featurelauncher.FeatureLauncher;
import org.osgi.service.featurelauncher.LaunchException;
import org.osgi.service.featurelauncher.decorator.AbandonOperationException;
import org.osgi.service.featurelauncher.decorator.FeatureDecorator;
import org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler;
import org.osgi.service.featurelauncher.repository.ArtifactRepository;
import org.osgi.service.featurelauncher.repository.ArtifactRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kentyou.featurelauncher.repository.spi.Repository;
import com.kentyou.prototype.featurelauncher.common.decorator.impl.DecorationContext;
import com.kentyou.prototype.featurelauncher.common.decorator.impl.LaunchFrameworkFeatureExtensionHandler;
import com.kentyou.prototype.featurelauncher.launch.secondstage.SecondStageLauncherImpl;
import com.kentyou.prototype.featurelauncher.repository.common.osgi.ArtifactRepositoryAdapter;
import com.kentyou.prototype.featurelauncher.repository.common.osgi.RepositoryAdapter;

/**
 * 160.4 The Feature Launcher
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 15, 2024
 */
public class FeatureLauncherImpl implements FeatureLauncher {
	private static final Logger LOG = LoggerFactory.getLogger(FeatureLauncherImpl.class);
	
	private static FeatureService featureService = ServiceLoader.load(FeatureService.class)
			.findFirst().orElseThrow(() -> new NoSuchElementException("No Feature Service available"));

	private ArtifactRepositoryFactory repoFactory = ServiceLoader.load(ArtifactRepositoryFactory.class)
			.findFirst().orElseThrow(() -> new NoSuchElementException("No Repository Factory available"));
	
	@Override
	public ArtifactRepository createRepository(Path path) {
		return repoFactory.createRepository(path);
	}

	@Override
	public ArtifactRepository createRepository(URI uri, Map<String, Object> props) {
		return repoFactory.createRepository(uri, props);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.FeatureLauncher#launch(org.osgi.service.feature.Feature)
	 */
	@Override
	public LaunchBuilder launch(Feature feature) {
		Objects.requireNonNull(feature, "Feature cannot be null!");

		return new LaunchBuilderImpl(feature);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.osgi.service.featurelauncher.FeatureLauncher#launch(java.io.Reader)
	 */
	@Override
	public LaunchBuilder launch(Reader jsonReader) {
		Objects.requireNonNull(jsonReader, "Feature JSON cannot be null!");

		try {
			Feature feature = featureService.readFeature(jsonReader);

			return launch(feature);

		} catch (IOException e) {
			throw new LaunchException("Error reading feature!", e);
		}
	}

	class LaunchBuilderImpl implements LaunchBuilder {
		private final Feature originalFeature;
		private boolean isLaunched;
		private List<ArtifactRepository> artifactRepositories;
		private Map<String, Object> configuration;
		private Map<String, Object> variables;
		private Map<String, String> frameworkProps;
		private List<FeatureDecorator> decorators;
		private Map<String, FeatureExtensionHandler> extensionHandlers;

		LaunchBuilderImpl(Feature feature) {
			Objects.requireNonNull(feature, "Feature cannot be null!");

			this.originalFeature = feature;
			this.isLaunched = false;
			this.artifactRepositories = new ArrayList<>();
			this.configuration = new HashMap<>();
			this.variables = new HashMap<>();
			this.frameworkProps = new HashMap<>();
			this.decorators = new ArrayList<>();
			this.extensionHandlers = new HashMap<>();
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.FeatureLauncher.LaunchBuilder#withRepository(org.osgi.service.featurelauncher.repository.ArtifactRepository)
		 */
		@Override
		public LaunchBuilder withRepository(ArtifactRepository repository) {
			Objects.requireNonNull(repository, "Artifact Repository cannot be null!");

			ensureNotLaunchedYet();

			this.artifactRepositories.add(repository);

			return this;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.FeatureLauncher.LaunchBuilder#withConfiguration(java.util.Map)
		 */
		@Override
		public LaunchBuilder withConfiguration(Map<String, Object> configuration) {
			Objects.requireNonNull(configuration, "Configuration cannot be null!");

			ensureNotLaunchedYet();

			this.configuration = Map.copyOf(configuration);

			return this;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.FeatureLauncher.LaunchBuilder#withVariables(java.util.Map)
		 */
		@Override
		public LaunchBuilder withVariables(Map<String, Object> variables) {
			Objects.requireNonNull(variables, "Variables cannot be null!");

			ensureNotLaunchedYet();

			this.variables = Map.copyOf(variables);

			return this;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.FeatureLauncher.LaunchBuilder#withFrameworkProperties(java.util.Map)
		 */
		@Override
		public LaunchBuilder withFrameworkProperties(Map<String, String> frameworkProps) {
			Objects.requireNonNull(frameworkProps, "Framework launch properties cannot be null!");

			ensureNotLaunchedYet();

			this.frameworkProps = Map.copyOf(frameworkProps);

			return this;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.FeatureLauncher.LaunchBuilder#withDecorator(org.osgi.service.featurelauncher.decorator.FeatureDecorator)
		 */
		@Override
		public LaunchBuilder withDecorator(FeatureDecorator decorator) {
			Objects.requireNonNull(decorator, "Feature Decorator cannot be null!");

			ensureNotLaunchedYet();

			this.decorators.add(decorator);

			return this;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.FeatureLauncher.LaunchBuilder#withExtensionHandler(java.lang.String, org.osgi.service.featurelauncher.decorator.FeatureExtensionHandler)
		 */
		@Override
		public LaunchBuilder withExtensionHandler(String extensionName, FeatureExtensionHandler extensionHandler) {
			Objects.requireNonNull(extensionName, "Feature extension name cannot be null!");
			Objects.requireNonNull(extensionHandler, "Feature extension handler cannot be null!");

			ensureNotLaunchedYet();

			this.extensionHandlers.put(extensionName, extensionHandler);

			return this;
		}

		/* 
		 * (non-Javadoc)
		 * @see org.osgi.service.featurelauncher.FeatureLauncher.LaunchBuilder#launchFramework()
		 */
		@Override
		public Framework launchFramework() {

			ensureNotLaunchedYet();

			this.isLaunched = true;
			
			Objects.requireNonNull(originalFeature, "Feature is required!");

			// TODO default repos
			if (this.artifactRepositories.isEmpty()) {
				throw new NullPointerException("At least one Artifact Repository is required!");
			}

			Path defaultFrameworkStorageDir;
			try {
				defaultFrameworkStorageDir = createDefaultFrameworkStorageDir();
			} catch (IOException e) {
				throw new LaunchException("Could not create default framework storage directory!", e);
			}

			Map<String, String> fwkProperties = new HashMap<String, String>();
					
			fwkProperties.putAll(getDefaultFrameworkProperties(defaultFrameworkStorageDir));
			fwkProperties.putAll(frameworkProps);
			fwkProperties = Map.copyOf(fwkProperties);

			LOG.info("Launching feature {}", originalFeature.getID());

			if(LOG.isDebugEnabled()) {
				LOG.debug("Using artifact repositories: {}", artifactRepositories);
				LOG.debug("Using framework properties: {}", fwkProperties);
				LOG.debug("Using configuration properties: {}", configuration);
				LOG.debug("Using variable overrides: {}", variables);
				LOG.debug("Using decorators {}", decorators);
				LOG.debug("Using extension handlers {}", extensionHandlers);
			}

			List<Repository> repositories = artifactRepositories.stream().map(ar -> {
				if(ar instanceof ArtifactRepositoryAdapter) {
					return ((ArtifactRepositoryAdapter) ar).unwrap();
				} else {
					return new RepositoryAdapter(ar);
				}
			}).collect(toList());
			LaunchFrameworkFeatureExtensionHandler lffehi = new LaunchFrameworkFeatureExtensionHandler(
					repositories);
			DecorationContext<LaunchFrameworkFeatureExtensionHandler> context = new DecorationContext<>(lffehi, repositories);
			
			Feature feature = originalFeature;
			try {
				//////////////////////////////////////
				// 160.4.3.1: Feature Decoration
				feature = context.executeFeatureDecorators(featureService, feature, decorators);
				feature = context.executeFeatureExtensionHandlers(featureService, feature, extensionHandlers);
			} catch (AbandonOperationException aoe) {
				throw new LaunchException("Feature Decoration failed", aoe);
			}
			
			/////////////////////////////////////////////////
			// 160.4.3.2: Locating a framework implementation
			Optional<Object> locatedFrameworkFactory = lffehi.getLocatedFrameworkFactory();
			
			if(locatedFrameworkFactory.isEmpty()) {
				LOG.info("The feature {} does not include a launch framework. A framework must be available on the current classpath", feature.getID());
			} else {
				LOG.warn("The feature {} includes a launch framework. This will be used.", feature.getID());
			}
			
			// Use instance directly
			SecondStageLauncherImpl secondStage = new SecondStageLauncherImpl();

			return secondStage.launchFramework(feature, context, artifactRepositories, locatedFrameworkFactory, 
					variables, configuration, frameworkProps);
		}

		private void ensureNotLaunchedYet() {
			if (this.isLaunched == true) {
				throw new IllegalStateException("Framework already launched!");
			}
		}
		
		private Path createDefaultFrameworkStorageDir() throws IOException {
			return Files.createTempDirectory("osgi_");
		}
	}
}
