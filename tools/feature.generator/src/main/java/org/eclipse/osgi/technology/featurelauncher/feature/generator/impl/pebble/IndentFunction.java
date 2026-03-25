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
package org.eclipse.osgi.technology.featurelauncher.feature.generator.impl.pebble;

import java.util.List;
import java.util.Map;

import io.pebbletemplates.pebble.extension.Function;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.PebbleTemplate;

public class IndentFunction implements Function {

	@Override
	public List<String> getArgumentNames() {
		return List.of("object");
	}

	@Override
	public Object execute(Map<String, Object> args, PebbleTemplate self, EvaluationContext context, int lineNumber) {
		Object obj = args.get("object");

		StringBuilder sb = new StringBuilder();
		if (obj instanceof Number count) {

			for (int i = 0; i < count.longValue(); i++) {
				sb.append(" ");
			}
		}
		return sb.toString();
	}
}