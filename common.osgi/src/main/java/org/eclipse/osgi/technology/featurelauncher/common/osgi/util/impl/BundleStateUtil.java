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

import org.osgi.framework.Bundle;

/**
 * Translate bundle state into readable representation.
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Sep 19, 2024
 */
public class BundleStateUtil {
	private BundleStateUtil() {
		// hidden constructor
	}

	/**
	 * Return a readable representation of a Bundle state.
	 * 
	 * A bundle can be in one of six states:
	 * <ul>
	 * <li>{@link #UNINSTALLED}</li>
	 * <li>{@link #INSTALLED}</li>
	 * <li>{@link #RESOLVED}</li>
	 * <li>{@link #STARTING}</li>
	 * <li>{@link #STOPPING}</li>
	 * <li>{@link #ACTIVE}</li>
	 * </ul>
	 */
	public static String getBundleStateString(int bundleState) {
		switch (bundleState) {
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.STARTING:
			return "STARTING";
		case Bundle.STOPPING:
			return "STOPPING";
		case Bundle.ACTIVE:
			return "ACTIVE";
		default:
			throw new RuntimeException("Unrecognized Bundle state!");
		}
	}
}
