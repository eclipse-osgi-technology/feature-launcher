/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Stefan Bischof - initial
 */
package org.eclipse.osgi.technology.featurelauncher.feature.generator;

import java.util.List;
import java.util.Optional;

public record FeatureMetadata(String id, Optional<String> name, Optional<String> description, Optional<String> docURL,
        Optional<String> license, Optional<String> scm, Optional<String> vendor,List<String> categories, boolean complete) {

}
