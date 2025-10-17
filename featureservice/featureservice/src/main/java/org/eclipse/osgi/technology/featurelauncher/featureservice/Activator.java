/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Stefan Bischof - initial implementation
 */

package org.eclipse.osgi.technology.featurelauncher.featureservice;

import java.util.Hashtable;

import org.eclipse.osgi.technology.featurelauncher.featureservice.base.FeatureServiceImpl;
import org.osgi.annotation.bundle.Capability;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.feature.FeatureService;

@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
@Capability(namespace = "osgi.service", attribute = "objectClass:List<String>=\"org.osgi.service.feature.FeatureService\"", uses = FeatureService.class)
public class Activator implements BundleActivator {

	@Override
	public void start(BundleContext context) throws Exception {
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		properties.put(Constants.SERVICE_VENDOR, "eclipse.osgi.technology");
		context.registerService(FeatureService.class, new FeatureServiceImpl(), properties);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
	}
}