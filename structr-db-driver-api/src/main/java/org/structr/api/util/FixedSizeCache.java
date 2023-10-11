/*
 * Copyright (C) 2010-2023 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.api.util;

import org.apache.commons.collections4.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A map-like storage structure with a fixed maximum size that
 * removes the least recently used entry when the insertion of
 * a new entry causes the map to exceed the specified maximum
 * size.
 *
 * @param <K>
 * @param <V>
 */
public class FixedSizeCache<K, V> {

	private static final Logger logger  = LoggerFactory.getLogger(FixedSizeCache.class);
	private long lastUpdate             = System.currentTimeMillis();
	private MemoryPoolMXBean bean       = null;
	private LRUMap<K, V> cache          = null;
	private String name                 = null;

	public FixedSizeCache(final String name, final int maxSize) {

		this.cache       = new InvalidatingLRUMap<>(maxSize);
		this.bean        = getOldGenerationMXBean();
		this.name        = name;
	}

	public synchronized void put(final K key, final V value) {
		cache.put(key, value);
		checkAvailableMemory();
	}

	public synchronized V get(final K key) {
		return cache.get(key);
	}

	public synchronized void removeAll(final Collection<K> keys) {
		cache.keySet().removeAll(keys);
	}

	public synchronized V remove(final K key) {
		return cache.remove(key);
	}

	public synchronized void clear() {
		cache.clear();
	}

	public synchronized int size() {
		return cache.size();
	}

	public synchronized Map<String, Integer> getCacheInfo() {
		return Map.of("max", cache.maxSize(), "size", size());
	}

	public synchronized boolean isEmpty() {
		return cache.isEmpty();
	}

	public synchronized boolean containsKey(final K key) {
		return cache.containsKey(key);
	}

	// ----- private methods -----
	private MemoryPoolMXBean getOldGenerationMXBean() {

		final List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
		for (final MemoryPoolMXBean bean : beans) {

			if (bean.getName().endsWith(" Old Gen")) {
				return bean;
			}
		}

		logger.warn("Memory management info not available, automatic cache size limitation is DISABLED.");

		return null;
	}

	private void checkAvailableMemory() {

		final long now = System.currentTimeMillis();

		if (bean != null && now > lastUpdate + 1000) {

			final MemoryUsage usage = bean.getCollectionUsage();
			final double maxMemory  = Math.max(1, usage.getMax());
			final double usedMemory = Math.max(1, usage.getUsed());
			final double percentage = (usedMemory / maxMemory) * 100.0;

			lastUpdate = now;

			if (percentage > 98.00) {

				double size = cache.maxSize();

				size *= 0.5;
				size /= 10000;
				size *= 10000;

				// enforce lower bound for cache size
				size = Math.max(1000, size);

				if (size == 1000) {

					logger.warn("JVM is running low on memory and {} size is at its minimum of {}. Please increase JVM heap size.", name, size);

				} else {

					logger.warn("JVM is running low on memory, limiting {} size to {}", name, size);
					logger.warn("If this happens more than once, please increase JVM heap size or reduce cache sizes.");
				}

				cache.clear();

				// replace current cache instance with limited one (should be ok to do since all methods are synchronized)
				cache = new InvalidatingLRUMap<>((int)size);
			}
		}
	}

	// ----- nested classes -----
	private static class InvalidatingLRUMap<K, V> extends LRUMap<K, V> {

		public InvalidatingLRUMap(final int maxSize) {
			super(maxSize, true);
		}

		@Override
		protected boolean removeLRU(final LinkEntry<K, V> entry) {

			final V value = entry.getValue();
			if (value != null && value instanceof Cachable) {

				((Cachable)value).onRemoveFromCache();
			}

			return true;
		}
	}
}
