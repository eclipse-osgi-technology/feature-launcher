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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Provides JMX-based metrics for the local node.
 */
final class JmxMetricsProvider {

	static final String AVAILABLE_PROCESSORS = "availableProcessors";
	static final String SYSTEM_LOAD_AVERAGE = "systemLoadAverage";
	static final String HEAP_MEMORY_USED = "heapMemoryUsed";
	static final String HEAP_MEMORY_MAX = "heapMemoryMax";
	static final String NON_HEAP_MEMORY_USED = "nonHeapMemoryUsed";
	static final String NON_HEAP_MEMORY_MAX = "nonHeapMemoryMax";

	private static final Set<String> ALL_METRICS = Set.of(AVAILABLE_PROCESSORS, SYSTEM_LOAD_AVERAGE, HEAP_MEMORY_USED,
			HEAP_MEMORY_MAX, NON_HEAP_MEMORY_USED, NON_HEAP_MEMORY_MAX);

	Map<String, Object> getMetrics(String... names) {
		Set<String> requested = (names == null || names.length == 0) ? ALL_METRICS : Set.of(names);

		Map<String, Object> result = new LinkedHashMap<>();

		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
		MemoryUsage heap = mem.getHeapMemoryUsage();
		MemoryUsage nonHeap = mem.getNonHeapMemoryUsage();

		if (requested.contains(AVAILABLE_PROCESSORS)) {
			result.put(AVAILABLE_PROCESSORS, os.getAvailableProcessors());
		}
		if (requested.contains(SYSTEM_LOAD_AVERAGE)) {
			result.put(SYSTEM_LOAD_AVERAGE, os.getSystemLoadAverage());
		}
		if (requested.contains(HEAP_MEMORY_USED)) {
			result.put(HEAP_MEMORY_USED, heap.getUsed());
		}
		if (requested.contains(HEAP_MEMORY_MAX)) {
			result.put(HEAP_MEMORY_MAX, heap.getMax());
		}
		if (requested.contains(NON_HEAP_MEMORY_USED)) {
			result.put(NON_HEAP_MEMORY_USED, nonHeap.getUsed());
		}
		if (requested.contains(NON_HEAP_MEMORY_MAX)) {
			result.put(NON_HEAP_MEMORY_MAX, nonHeap.getMax());
		}

		return result;
	}
}
