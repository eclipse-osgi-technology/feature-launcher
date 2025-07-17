package org.eclipse.osgi.technology.featurelauncher.feature.generator;

import java.io.File;

public record Setting(File outputDirectory, String fileName, FeatureBundleSetting featureBundleSetting,
        ConfigSetting configSetting) {
}
