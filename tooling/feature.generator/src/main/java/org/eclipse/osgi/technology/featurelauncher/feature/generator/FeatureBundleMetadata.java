package org.eclipse.osgi.technology.featurelauncher.feature.generator;

import java.io.File;
import java.util.Map;

import org.osgi.service.feature.ID;

public record FeatureBundleMetadata(ID id, File file, Map<String, Object> properties) {

}
