/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.Collections;
import java.util.Map;
import org.apache.commons.collections4.map.LRUMap;

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

	private Map<K, V> cache  = null;

	public FixedSizeCache(final int maxSize) {
		this.cache   = Collections.synchronizedMap(new InvalidatingLRUMap<>(maxSize));
	}

	public synchronized void put(final K key, final V value) {
		cache.put(key, value);
	}

	public synchronized V get(final K key) {
		return cache.get(key);
	}

	public synchronized void remove(final K key) {
		cache.remove(key);
	}

	public synchronized void clear() {
		cache.clear();
	}

	public synchronized int size() {
		return cache.size();
	}

	public synchronized boolean isEmpty() {
		return cache.isEmpty();
	}

	private static class InvalidatingLRUMap<K, V> extends LRUMap<K, V> {

		public InvalidatingLRUMap(final int maxSize) {
			super(maxSize, true);
		}

		@Override
		protected boolean removeLRU(final LinkEntry<K, V> entry) {

			final V value = entry.getValue();
			if (value != null && value instanceof Cachable) {

				((Cachable)value).invalidate();
			}

			return true;
		}
	}
}
