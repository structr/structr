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
package org.structr.common;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Christian Morgner
 */
public class FixedSizeCache<K, V> {

	private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>(10000);
	private final Queue<K> keyQueue             = new ConcurrentLinkedQueue<>();
	private long maxSize                        = 10000;

	public FixedSizeCache(final long maxSize) {
		this.maxSize = maxSize;
	}

	public void put(final K key, final V value) {

		// only check size restriction if the
		// put operation does not replace an
		// existing element
		if (cache.put(key, value) == null) {

			// check size
			if (cache.size() > maxSize) {

				// remove eldest entry (head of queue)
				final K keyToRemove = keyQueue.poll();
				if (keyToRemove != null) {

					cache.remove(keyToRemove);
				}
			}

			keyQueue.add(key);
		}
	}

	public V get(final K key) {
		return cache.get(key);
	}

	public void clear() {
		cache.clear();
		keyQueue.clear();
	}
}
