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
package org.eclipse.osgi.technology.featurelauncher.common.osgi.util.impl;

/**
 * Util for {@link org.osgi.service.cm.ConfigurationAdmin}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 5, 2024
 */
public class ConfigurationUtil {
	public static final String CONFIGURATIONS_FILTER = ".featurelauncher.config";
	public static final String CONFIGURATION_DEFAULT_LOCATION = "?";

	private ConfigurationUtil() {
		// hidden constructor
	}

	public static String constructConfigurationsFilter() {
		StringBuilder sb = new StringBuilder();

		sb.append("(");
		sb.append(CONFIGURATIONS_FILTER);
		sb.append("=");
		sb.append(Boolean.TRUE);
		sb.append(")");

		return sb.toString();
	}

	public static String normalizePid(String rawPid) {
		return rawPid.substring(rawPid.indexOf('~') + 1);
	}
}
