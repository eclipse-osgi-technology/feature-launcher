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
package com.kentyou.featurelauncher.cli;

/**
 * A FeatureLauncherCliException is thrown by the {@link FeatureLauncherCli}
 * 
 * @author Michael H. Siemaszko (mhs@into.software)
 * @since Oct 10, 2024
 */
public class FeatureLauncherCliException extends RuntimeException {

	private static final long serialVersionUID = -7071269931233244114L;

	/**
	 * Create a FeatureLauncherCliException with the supplied error message
	 * 
	 * @param message
	 */
	public FeatureLauncherCliException(String message) {
		super(message);
	}

	/**
	 * Create a FeatureLauncherCliException with the supplied error message and cause
	 * 
	 * @param message
	 * @param cause
	 */
	public FeatureLauncherCliException(String message, Throwable cause) {
		super(message, cause);
	}
}
