package org.eclipse.osgi.technology.featurelauncher.launch.spi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.osgi.technology.featurelauncher.common.decorator.impl.DecorationContext;
import org.eclipse.osgi.technology.featurelauncher.repository.spi.Repository;
import org.osgi.service.feature.Feature;

/**
 * Used to separate the first stage of the launch (no framework on the classpath) from the second stage
 * where we do have access to the framework API 
 */
public interface SecondStageLauncher {
	
	/**
	 * Launch a framework for the supplied configuration
	 * @param feature - a decorated feature
	 * @param context - the decoration context
	 * @param repositories - the configured repositories
	 * @param frameworkFactory - the framework factory discovered by the extension handlers
	 * @param variableOverrides - the variable overrides for this launch
	 */
	public LaunchResult launch(Feature feature, DecorationContext<?> context, List<? extends Repository> repositories,
			Optional<Object> frameworkFactory, Map<String, Object> variableOverrides,
			Map<String, Object> configurationProperties, Map<String, String> frameworkProperties);

	public interface LaunchResult {
		void waitForStop(long time) throws InterruptedException;
	}
}
