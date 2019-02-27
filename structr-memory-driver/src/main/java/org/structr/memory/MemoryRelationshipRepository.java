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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.structr.api.util.Iterables;
import org.structr.memory.index.filter.Filter;
import org.structr.memory.index.filter.MemoryLabelFilter;
import org.structr.memory.index.filter.SourceNodeFilter;
import org.structr.memory.index.filter.TargetNodeFilter;

/**
 */
public class MemoryRelationshipRepository {

	final Map<MemoryIdentity, MemoryRelationship> masterData    = new LinkedHashMap<>();
	final Map<String, Set<MemoryIdentity>> typeCache           = new LinkedHashMap<>();
	final Map<MemoryIdentity, Set<MemoryIdentity>> sourceCache = new LinkedHashMap<>();
	final Map<MemoryIdentity, Set<MemoryIdentity>> targetCache = new LinkedHashMap<>();
	final Set<String> duplicatesCheckCache                      = new LinkedHashSet<>();

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

	synchronized void add(final Map<MemoryIdentity, MemoryRelationship> newData) {

		for (final Entry<MemoryIdentity, MemoryRelationship> entry : newData.entrySet()) {

			final MemoryIdentity id        = entry.getKey();
			final MemoryRelationship value = entry.getValue();
			final String key               = value.getUniquenessKey();

			if (duplicatesCheckCache.contains(key)) {
				throw new IllegalStateException("Duplicate relationship: " + key);
			}

			duplicatesCheckCache.add(key);

			for (final String label : value.getLabels()) {

				getCacheForLabel(label).add(id);
			}

			getCacheForSource(value.getSourceNodeIdentity()).add(id);
			getCacheForTarget(value.getTargetNodeIdentity()).add(id);

			masterData.put(id, entry.getValue());
		}
	}

	void remove(final Map<MemoryIdentity, MemoryRelationship> relationships) {

		final Set<MemoryIdentity> ids = relationships.keySet();

		masterData.keySet().removeAll(ids);

		// clear uniqueness check cache as well
		for (final MemoryRelationship rel : relationships.values()) {

			duplicatesCheckCache.remove(rel.getUniquenessKey());
		}

		for (final Set<MemoryIdentity> cache : typeCache.values()) {
			cache.removeAll(ids);
		}

		for (final Set<MemoryIdentity> cache : sourceCache.values()) {
			cache.removeAll(ids);
		}

		for (final Set<MemoryIdentity> cache : targetCache.values()) {
			cache.removeAll(ids);
		}
	}

	synchronized void updateCache(final MemoryRelationship relationship) {
		// relationship type cannot be changed => no-op
	}

	// ----- private methods -----
	private synchronized Set<MemoryIdentity> getCacheForLabel(final String type) {

		Set<MemoryIdentity> cache = typeCache.get(type);
		if (cache == null) {

			cache = new CopyOnWriteArraySet<>();
			typeCache.put(type, cache);
		}

		return cache;
	}

	private synchronized Set<MemoryIdentity> getCacheForSource(final MemoryIdentity source) {

		Set<MemoryIdentity> cache = sourceCache.get(source);
		if (cache == null) {

			cache = new CopyOnWriteArraySet<>();
			sourceCache.put(source, cache);
		}

		return cache;
	}

	private synchronized Set<MemoryIdentity> getCacheForTarget(final MemoryIdentity target) {

		Set<MemoryIdentity> cache = targetCache.get(target);
		if (cache == null) {

			cache = new CopyOnWriteArraySet<>();
			targetCache.put(target, cache);
		}

		return cache;
	}
}
