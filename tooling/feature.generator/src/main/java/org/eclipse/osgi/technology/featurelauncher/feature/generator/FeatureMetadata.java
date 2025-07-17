package org.eclipse.osgi.technology.featurelauncher.feature.generator;

import java.util.List;
import java.util.Optional;

public record FeatureMetadata(String id, Optional<String> name, Optional<String> description, Optional<String> docURL,
        Optional<String> license, Optional<String> scm, Optional<String> vendor,List<String> categories, boolean complete) {

}
