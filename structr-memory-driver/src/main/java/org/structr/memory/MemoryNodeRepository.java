/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.memory;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.structr.api.util.Iterables;
import org.structr.memory.index.filter.Filter;
import org.structr.memory.index.filter.MemoryLabelFilter;
import org.structr.memory.index.filter.MemoryTypeFilter;

/**
 */
public class MemoryNodeRepository {

	final Map<MemoryIdentity, MemoryNode> masterData  = new ConcurrentHashMap<>();
	final Map<String, Set<MemoryIdentity>> labelCache = new ConcurrentHashMap<>();
	final Map<String, Set<MemoryIdentity>> typeCache  = new ConcurrentHashMap<>();

	MemoryNode get(final MemoryIdentity id) {
		return masterData.get(id);
	}

	public void clear() {
		masterData.clear();
		labelCache.clear();
		typeCache.clear();
	}

	Iterable<MemoryNode> values(final Filter<MemoryNode> filter) {

		if (filter != null) {

			if (filter instanceof MemoryLabelFilter) {

				final MemoryLabelFilter<MemoryNode> mt = (MemoryLabelFilter<MemoryNode>)filter;
				final String label                     = mt.getLabel();

				return Iterables.map(i -> masterData.get(i), new LinkedHashSet<>(getCacheForLabel(label)));
			}

			if (filter instanceof MemoryTypeFilter) {

				final MemoryTypeFilter<MemoryNode> mt = (MemoryTypeFilter<MemoryNode>)filter;
				final String type                     = mt.getType();

				return Iterables.map(i -> masterData.get(i), new LinkedHashSet<>(getCacheForType(type)));
			}
		}

		return masterData.values();
	}

	boolean contains(final MemoryIdentity id) {
		return masterData.containsKey(id);
	}

	void add(final Map<MemoryIdentity, MemoryNode> newData) {

		for (final Entry<MemoryIdentity, MemoryNode> entry : newData.entrySet()) {

			final MemoryIdentity id = entry.getKey();
			final MemoryNode value  = entry.getValue();
			final String type       = id.getType();

			for (final String label : value.getLabels()) {

				getCacheForLabel(label).add(id);
			}

			getCacheForType(type).add(id);

			masterData.put(id, entry.getValue());
		}
	}

	void remove(final Set<MemoryIdentity> ids) {

		// avoid iteration of caches when there are no IDs to remove..
		if (!ids.isEmpty()) {

			masterData.keySet().removeAll(ids);

			for (final Set<MemoryIdentity> cache : labelCache.values()) {
				cache.removeAll(ids);
			}

			for (final Set<MemoryIdentity> cache : typeCache.values()) {
				cache.removeAll(ids);
			}
		}
	}

	void updateCache(final MemoryNode node) {

		final MemoryIdentity id = node.getIdentity();
		final String type       = (String)node.getProperty("type");


		// remove identity from all caches
		for (final Set<MemoryIdentity> cache : labelCache.values()) {
			cache.remove(id);
		}

		for (final Set<MemoryIdentity> cache : typeCache.values()) {
			cache.remove(id);
		}

		// add identity to type cache again
		getCacheForLabel(type).add(id);
		getCacheForType(type).add(id);
	}

	// ----- private methods -----
	private synchronized Set<MemoryIdentity> getCacheForLabel(final String type) {

		Set<MemoryIdentity> cache = labelCache.get(type);
		if (cache == null) {

			cache = new CopyOnWriteArraySet<>();
			labelCache.put(type, cache);
		}

		return cache;
	}

	private synchronized Set<MemoryIdentity> getCacheForType(final String type) {

		Set<MemoryIdentity> cache = typeCache.get(type);
		if (cache == null) {

			cache = new CopyOnWriteArraySet<>();
			typeCache.put(type, cache);
		}

		return cache;
	}
}
