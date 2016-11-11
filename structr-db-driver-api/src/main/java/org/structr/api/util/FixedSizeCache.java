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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A map with a fixed maximum size that removes the eldest entry
 * when the insertion of a new entry causes the map to exceed the
 * specified maximum size.
 *
 * @param <K>
 * @param <V>
 */
public class FixedSizeCache<K, V> {

	private int maxSize      = 100000;
	private Map<K, V> cache  = null;
	private int currentSize  = 0;

	public FixedSizeCache(final int maxSize) {

		this.cache   = Collections.synchronizedMap(new LRUMap());
		this.maxSize = maxSize;
	}

	public synchronized void put(final K key, final V value) {
		cache.put(key, value);
		currentSize++;
	}

	public synchronized V get(final K key) {
		return cache.get(key);
	}

	public synchronized void remove(final K key) {
		cache.remove(key);
		currentSize--;
	}

	public synchronized void clear() {
		cache.clear();
		currentSize = 0;
	}

	public synchronized int size() {
		return currentSize;
	}

	private class LRUMap extends LinkedHashMap<K, V> {

		@Override
		protected boolean removeEldestEntry(final Entry<K, V> entry) {

			if (currentSize >= maxSize) {

				currentSize--;
				return true;
			}

			return false;
		}
	}
}
