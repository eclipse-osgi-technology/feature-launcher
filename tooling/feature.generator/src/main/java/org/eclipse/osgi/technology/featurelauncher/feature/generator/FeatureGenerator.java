package org.eclipse.osgi.technology.featurelauncher.feature.generator;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface FeatureGenerator {

	File generate(Setting setting, FeatureMetadata metadata, List<FeatureBundleMetadata> bundles,
	        Map<String, Object> variables) throws Exception;
}