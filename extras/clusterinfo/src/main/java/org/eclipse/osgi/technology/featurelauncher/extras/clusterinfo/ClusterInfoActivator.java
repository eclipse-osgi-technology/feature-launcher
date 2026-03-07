/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 * All rights reserved.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.osgi.technology.featurelauncher.extras.clusterinfo;

import java.util.Hashtable;

import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.clusterinfo.FrameworkManager;
import org.osgi.service.clusterinfo.FrameworkNodeStatus;
import org.osgi.service.clusterinfo.NodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator for the OSGi Cluster Information Service (ch148).
 *
 * <p>Registers a {@link FrameworkNodeStatus} service that provides local
 * framework management and JMX-based metrics. Service properties are
 * populated from framework properties following the OSGi Cluster Information
 * specification.
 */
@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class ClusterInfoActivator implements BundleActivator {

	private static final Logger LOG = LoggerFactory.getLogger(ClusterInfoActivator.class);

	private static final String PROP_PREFIX = ClusterInfoConfig.PROP_PREFIX;

	private ServiceRegistration<?> registration;
	private TagAggregator tagAggregator;

	@Override
	public void start(BundleContext context) throws Exception {
		FrameworkNodeStatusImpl nodeStatus = new FrameworkNodeStatusImpl(context);

		Hashtable<String, Object> props = new Hashtable<>();

		// Cluster info properties
		props.put(PROP_PREFIX + "id", getPropertyOrDefault(context,
				Constants.FRAMEWORK_UUID, "unknown"));
		props.put(PROP_PREFIX + "cluster", getPropertyOrDefault(context,
				PROP_PREFIX + "cluster", "default"));

		putIfPresent(props, PROP_PREFIX + "endpoint", context.getProperty(PROP_PREFIX + "endpoint"));
		putIfPresent(props, PROP_PREFIX + "vendor", context.getProperty(PROP_PREFIX + "vendor"));
		putIfPresent(props, PROP_PREFIX + "version", context.getProperty(PROP_PREFIX + "version"));
		putIfPresent(props, PROP_PREFIX + "country", context.getProperty(PROP_PREFIX + "country"));
		putIfPresent(props, PROP_PREFIX + "location", context.getProperty(PROP_PREFIX + "location"));
		putIfPresent(props, PROP_PREFIX + "region", context.getProperty(PROP_PREFIX + "region"));
		putIfPresent(props, PROP_PREFIX + "zone", context.getProperty(PROP_PREFIX + "zone"));

		// Framework properties
		putIfPresent(props, "org.osgi.framework.version",
				context.getProperty("org.osgi.framework.version"));
		putIfPresent(props, "org.osgi.framework.processor",
				context.getProperty("org.osgi.framework.processor"));
		putIfPresent(props, "org.osgi.framework.os.name",
				context.getProperty("org.osgi.framework.os.name"));
		putIfPresent(props, "org.osgi.framework.os.version",
				context.getProperty("org.osgi.framework.os.version"));

		// Java properties
		putIfPresent(props, "java.version", System.getProperty("java.version"));
		putIfPresent(props, "java.vm.version", System.getProperty("java.vm.version"));
		putIfPresent(props, "java.specification.version",
				System.getProperty("java.specification.version"));
		putIfPresent(props, "java.runtime.version",
				System.getProperty("java.runtime.version"));

		String[] interfaces = new String[] {
				FrameworkNodeStatus.class.getName(),
				FrameworkManager.class.getName(),
				NodeStatus.class.getName()
		};

		registration = context.registerService(interfaces, nodeStatus, props);

		tagAggregator = new TagAggregator(context, registration);
		tagAggregator.open();

		LOG.info("Cluster Information Service registered (id={})",
				props.get(PROP_PREFIX + "id"));
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (tagAggregator != null) {
			tagAggregator.close();
			tagAggregator = null;
		}
		if (registration != null) {
			try {
				registration.unregister();
			} catch (IllegalStateException e) {
				LOG.debug("Service already unregistered", e);
			}
			registration = null;
		}
		LOG.info("Cluster Information Service stopped");
	}

	private static String getPropertyOrDefault(BundleContext context, String key,
			String defaultValue) {
		String value = context.getProperty(key);
		return (value != null && !value.isEmpty()) ? value : defaultValue;
	}

	private static void putIfPresent(Hashtable<String, Object> props, String key,
			String value) {
		if (value != null && !value.isEmpty()) {
			props.put(key, value);
		}
	}
}
