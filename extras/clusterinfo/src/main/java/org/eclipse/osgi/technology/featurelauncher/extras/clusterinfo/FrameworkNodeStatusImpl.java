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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.FrameworkDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.startlevel.dto.BundleStartLevelDTO;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;
import org.osgi.service.clusterinfo.FrameworkNodeStatus;

/**
 * Implementation of {@link FrameworkNodeStatus} that manages the local OSGi
 * framework.
 */
final class FrameworkNodeStatusImpl implements FrameworkNodeStatus {

	private final BundleContext context;
	private final JmxMetricsProvider metricsProvider;

	FrameworkNodeStatusImpl(BundleContext context) {
		this.context = context;
		this.metricsProvider = new JmxMetricsProvider();
	}

	@Override
	public Map<String, Object> getMetrics(String... names) {
		return metricsProvider.getMetrics(names);
	}

	@Override
	public BundleDTO getBundle(long id) throws Exception {
		Bundle bundle = context.getBundle(id);
		if (bundle == null) {
			throw new IllegalArgumentException("No bundle with id " + id);
		}
		return bundle.adapt(BundleDTO.class);
	}

	@Override
	public Map<String, String> getBundleHeaders(long id) throws Exception {
		Bundle bundle = context.getBundle(id);
		if (bundle == null) {
			throw new IllegalArgumentException("No bundle with id " + id);
		}
		Dictionary<String, String> headers = bundle.getHeaders();
		Map<String, String> result = new LinkedHashMap<>();
		var keys = headers.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			result.put(key, headers.get(key));
		}
		return result;
	}

	@Override
	public Collection<BundleDTO> getBundles() throws Exception {
		FrameworkDTO fwkDTO = context.getBundle(0).adapt(FrameworkDTO.class);
		return fwkDTO.bundles;
	}

	@Override
	public BundleStartLevelDTO getBundleStartLevel(long id) throws Exception {
		Bundle bundle = context.getBundle(id);
		if (bundle == null) {
			throw new IllegalArgumentException("No bundle with id " + id);
		}
		return bundle.adapt(BundleStartLevelDTO.class);
	}

	@Override
	public int getBundleState(long id) throws Exception {
		Bundle bundle = context.getBundle(id);
		if (bundle == null) {
			throw new IllegalArgumentException("No bundle with id " + id);
		}
		return bundle.getState();
	}

	@Override
	public FrameworkStartLevelDTO getFrameworkStartLevel() throws Exception {
		return context.getBundle(0).adapt(FrameworkStartLevelDTO.class);
	}

	@Override
	public ServiceReferenceDTO getServiceReference(long id) throws Exception {
		FrameworkDTO fwkDTO = context.getBundle(0).adapt(FrameworkDTO.class);
		for (ServiceReferenceDTO sref : fwkDTO.services) {
			if (sref.id == id) {
				return sref;
			}
		}
		return null;
	}

	@Override
	public Collection<ServiceReferenceDTO> getServiceReferences() throws Exception {
		FrameworkDTO fwkDTO = context.getBundle(0).adapt(FrameworkDTO.class);
		return fwkDTO.services;
	}

	@Override
	public Collection<ServiceReferenceDTO> getServiceReferences(String filter) throws Exception {
		if (filter == null || filter.isEmpty()) {
			return getServiceReferences();
		}
		org.osgi.framework.ServiceReference<?>[] refs = context.getAllServiceReferences(null, filter);
		Collection<ServiceReferenceDTO> result = new ArrayList<>();
		if (refs != null) {
			FrameworkDTO fwkDTO = context.getBundle(0).adapt(FrameworkDTO.class);
			for (org.osgi.framework.ServiceReference<?> ref : refs) {
				long svcId = (Long) ref.getProperty("service.id");
				for (ServiceReferenceDTO dto : fwkDTO.services) {
					if (dto.id == svcId) {
						result.add(dto);
						break;
					}
				}
			}
		}
		return result;
	}

	@Override
	public BundleDTO installBundle(String location) throws Exception {
		Bundle bundle = context.installBundle(location);
		return bundle.adapt(BundleDTO.class);
	}

	@Override
	public void setBundleStartLevel(long id, int startLevel) throws Exception {
		Bundle bundle = context.getBundle(id);
		if (bundle == null) {
			throw new IllegalArgumentException("No bundle with id " + id);
		}
		bundle.adapt(BundleStartLevel.class).setStartLevel(startLevel);
	}

	@Override
	public void setFrameworkStartLevel(FrameworkStartLevelDTO startLevel) throws Exception {
		context.getBundle(0).adapt(FrameworkStartLevel.class).setStartLevel(startLevel.startLevel);
	}

	@Override
	public void startBundle(long id) throws Exception {
		startBundle(id, 0);
	}

	@Override
	public void startBundle(long id, int options) throws Exception {
		Bundle bundle = context.getBundle(id);
		if (bundle == null) {
			throw new IllegalArgumentException("No bundle with id " + id);
		}
		bundle.start(options);
	}

	@Override
	public void stopBundle(long id) throws Exception {
		stopBundle(id, 0);
	}

	@Override
	public void stopBundle(long id, int options) throws Exception {
		Bundle bundle = context.getBundle(id);
		if (bundle == null) {
			throw new IllegalArgumentException("No bundle with id " + id);
		}
		bundle.stop(options);
	}

	@Override
	public BundleDTO uninstallBundle(long id) throws Exception {
		Bundle bundle = context.getBundle(id);
		if (bundle == null) {
			throw new IllegalArgumentException("No bundle with id " + id);
		}
		BundleDTO dto = bundle.adapt(BundleDTO.class);
		bundle.uninstall();
		return dto;
	}

	@Override
	public BundleDTO updateBundle(long id) throws Exception {
		Bundle bundle = context.getBundle(id);
		if (bundle == null) {
			throw new IllegalArgumentException("No bundle with id " + id);
		}
		bundle.update();
		return bundle.adapt(BundleDTO.class);
	}

	@Override
	public BundleDTO updateBundle(long id, String url) throws Exception {
		Bundle bundle = context.getBundle(id);
		if (bundle == null) {
			throw new IllegalArgumentException("No bundle with id " + id);
		}
		bundle.update(new URI(url).toURL().openStream());
		return bundle.adapt(BundleDTO.class);
	}
}
