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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks services with the {@code osgi.clusterinfo.tags} property and
 * aggregates all tags onto the FrameworkNodeStatus service registration.
 */
final class TagAggregator implements ServiceTrackerCustomizer<Object, Object> {

	private static final Logger LOG = LoggerFactory.getLogger(TagAggregator.class);
	private static final String TAGS_PROPERTY = "osgi.clusterinfo.tags";

	private final BundleContext context;
	private final ServiceRegistration<?> nodeRegistration;
	private final ServiceTracker<Object, Object> tracker;
	private final Set<String> aggregatedTags = new LinkedHashSet<>();

	TagAggregator(BundleContext context, ServiceRegistration<?> nodeRegistration) throws InvalidSyntaxException {
		this.context = context;
		this.nodeRegistration = nodeRegistration;
		this.tracker = new ServiceTracker<>(context, context.createFilter("(" + TAGS_PROPERTY + "=*)"), this);
	}

	void open() {
		tracker.open();
	}

	void close() {
		tracker.close();
	}

	@Override
	public Object addingService(ServiceReference<Object> reference) {
		Object service = context.getService(reference);
		updateTags();
		return service;
	}

	@Override
	public void modifiedService(ServiceReference<Object> reference, Object service) {
		updateTags();
	}

	@Override
	public void removedService(ServiceReference<Object> reference, Object service) {
		context.ungetService(reference);
		updateTags();
	}

	private synchronized void updateTags() {
		aggregatedTags.clear();
		ServiceReference<Object>[] refs = tracker.getServiceReferences();
		if (refs != null) {
			for (ServiceReference<Object> ref : refs) {
				Object tagsValue = ref.getProperty(TAGS_PROPERTY);
				if (tagsValue instanceof String[]) {
					aggregatedTags.addAll(Arrays.asList((String[]) tagsValue));
				} else if (tagsValue instanceof String) {
					aggregatedTags.add((String) tagsValue);
				}
			}
		}

		try {
			nodeRegistration.setProperties(createUpdatedProperties(nodeRegistration.getReference()));
		} catch (IllegalStateException e) {
			LOG.debug("Service already unregistered", e);
		}
	}

	private java.util.Hashtable<String, Object> createUpdatedProperties(ServiceReference<?> ref) {
		java.util.Hashtable<String, Object> props = new java.util.Hashtable<>();
		for (String key : ref.getPropertyKeys()) {
			if (!"objectClass".equals(key) && !"service.id".equals(key) && !"service.bundleid".equals(key)
					&& !"service.scope".equals(key)) {
				props.put(key, ref.getProperty(key));
			}
		}
		props.put(TAGS_PROPERTY, aggregatedTags.toArray(new String[0]));
		return props;
	}
}
