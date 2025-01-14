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
package org.eclipse.osgi.technology.featurelauncher.shading;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.AppendingTransformer;

/**
 * This class is used to relocate class names in resources such as
 * META-INF/sisu
 */
public class RelocatingAppendingTransformer extends AppendingTransformer {

	@Override
	public void processResource(String resource, InputStream is, List<Relocator> relocators, long time)
			throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (PrintStream ps = new PrintStream(out, false, StandardCharsets.UTF_8);
			Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
			while (scanner.hasNextLine()) {
				String relContent = scanner.nextLine();
				for (Relocator relocator : relocators) {
					if (relocator.canRelocateClass(relContent)) {
						relContent = relocator.applyToSourceContent(relContent);
					}
				}
				ps.println(relContent);
			}
		}
		super.processResource(resource, new ByteArrayInputStream(out.toByteArray()),
				relocators, time);
	}

	
	
}
