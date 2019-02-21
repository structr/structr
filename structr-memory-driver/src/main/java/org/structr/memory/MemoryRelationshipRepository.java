/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.structr.api.util.Iterables;
import org.structr.memory.index.filter.Filter;
import org.structr.memory.index.filter.MemoryLabelFilter;
import org.structr.memory.index.filter.SourceNodeFilter;
import org.structr.memory.index.filter.TargetNodeFilter;

/**
 */
public class MemoryRelationshipRepository {

	final Map<MemoryIdentity, MemoryRelationship> masterData    = new ConcurrentHashMap<>();
	final Map<String, List<MemoryIdentity>> typeCache           = new ConcurrentHashMap<>();
	final Map<MemoryIdentity, List<MemoryIdentity>> sourceCache = new ConcurrentHashMap<>();
	final Map<MemoryIdentity, List<MemoryIdentity>> targetCache = new ConcurrentHashMap<>();

	MemoryRelationship get(final MemoryIdentity id) {
		return masterData.get(id);
	}

	public void clear() {
		masterData.clear();
		typeCache.clear();
		sourceCache.clear();
		targetCache.clear();
	}

	Iterable<MemoryRelationship> values(final Filter<MemoryRelationship> filter) {

		if (filter != null) {

			if (filter instanceof MemoryLabelFilter) {

				final MemoryLabelFilter<MemoryRelationship> mt = (MemoryLabelFilter<MemoryRelationship>)filter;
				final String label            = mt.getLabel();

				return Iterables.map(i -> masterData.get(i), getCacheForLabel(label));
			}

			if (filter instanceof SourceNodeFilter) {

				final SourceNodeFilter<MemoryRelationship> s = (SourceNodeFilter<MemoryRelationship>)filter;
				final MemoryIdentity id     = s.getIdentity();

				return Iterables.map(i -> masterData.get(i), getCacheForSource(id));
			}

			if (filter instanceof TargetNodeFilter) {

				final TargetNodeFilter<MemoryRelationship> s = (TargetNodeFilter<MemoryRelationship>)filter;
				final MemoryIdentity id     = s.getIdentity();

				return Iterables.map(i -> masterData.get(i), getCacheForTarget(id));
			}
		}

		return masterData.values();
	}

	boolean contains(final MemoryIdentity id) {
		return masterData.containsKey(id);
	}

	void add(final Map<MemoryIdentity, MemoryRelationship> newData) {

		for (final Entry<MemoryIdentity, MemoryRelationship> entry : newData.entrySet()) {

			final MemoryIdentity id  = entry.getKey();
			final MemoryRelationship value            = entry.getValue();

			for (final String label : value.getLabels()) {

				getCacheForLabel(label).add(id);
			}

			getCacheForSource(value.getSourceNodeIdentity()).add(id);
			getCacheForTarget(value.getTargetNodeIdentity()).add(id);

			masterData.put(id, entry.getValue());
		}
	}

	void remove(final Set<MemoryIdentity> ids) {
		masterData.keySet().removeAll(ids);
	}

	// ----- private methods -----
	private synchronized List<MemoryIdentity> getCacheForLabel(final String type) {

		List<MemoryIdentity> cache = typeCache.get(type);
		if (cache == null) {

			cache = new CopyOnWriteArrayList<>();
			typeCache.put(type, cache);
		}

		return cache;
	}

	private synchronized List<MemoryIdentity> getCacheForSource(final MemoryIdentity source) {

		List<MemoryIdentity> cache = sourceCache.get(source);
		if (cache == null) {

			cache = new CopyOnWriteArrayList<>();
			sourceCache.put(source, cache);
		}

		return cache;
	}

	private synchronized List<MemoryIdentity> getCacheForTarget(final MemoryIdentity target) {

		List<MemoryIdentity> cache = targetCache.get(target);
		if (cache == null) {

			cache = new CopyOnWriteArrayList<>();
			targetCache.put(target, cache);
		}

		return cache;
	}
}
