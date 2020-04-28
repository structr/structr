/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.structr.api.util.Iterables;
import org.structr.memory.index.filter.Filter;
import org.structr.memory.index.filter.MemoryLabelFilter;
import org.structr.memory.index.filter.SourceNodeFilter;
import org.structr.memory.index.filter.TargetNodeFilter;

/**
 */
public class MemoryRelationshipRepository extends EntityRepository {

	final Map<MemoryIdentity, MemoryRelationship> masterData   = new ConcurrentHashMap<>();
	final Map<String, Set<MemoryIdentity>> typeCache           = new ConcurrentHashMap<>();
	final Map<MemoryIdentity, Set<MemoryIdentity>> sourceCache = new ConcurrentHashMap<>();
	final Map<MemoryIdentity, Set<MemoryIdentity>> targetCache = new ConcurrentHashMap<>();
	final Set<String> duplicatesCheckCache                     = new LinkedHashSet<>();

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
				final Set<MemoryIdentity> cache                = new LinkedHashSet<>();

				for (final String label : mt.getLabels()) {

					cache.addAll(getCacheForType(label));
				}

				return Iterables.map(i -> masterData.get(i), cache);
			}

			if (filter instanceof SourceNodeFilter) {

				final SourceNodeFilter<MemoryRelationship> s = (SourceNodeFilter<MemoryRelationship>)filter;
				final MemoryIdentity id     = s.getIdentity();

				return Iterables.map(i -> masterData.get(i), new LinkedHashSet<>(getCacheForSource(id)));
			}

			if (filter instanceof TargetNodeFilter) {

				final TargetNodeFilter<MemoryRelationship> s = (TargetNodeFilter<MemoryRelationship>)filter;
				final MemoryIdentity id     = s.getIdentity();

				return Iterables.map(i -> masterData.get(i), new LinkedHashSet<>(getCacheForTarget(id)));
			}
		}

		return masterData.values();
	}

	boolean contains(final MemoryIdentity id) {
		return masterData.containsKey(id);
	}

	void add(final Iterable<MemoryRelationship> newData) {

		for (final MemoryRelationship relationship : newData) {
			add(relationship);
		}
	}

	void add(final MemoryRelationship relationship) {

		final MemoryIdentity id        = relationship.getIdentity();
		final String key               = relationship.getUniquenessKey();

		if (duplicatesCheckCache.contains(key)) {
			throw new IllegalStateException("Duplicate relationship: " + key);
		}

		duplicatesCheckCache.add(key);

		for (final String label : relationship.getLabels()) {

			getCacheForType(label).add(id);
		}

		getCacheForSource(relationship.getSourceNodeIdentity()).add(id);
		getCacheForTarget(relationship.getTargetNodeIdentity()).add(id);

		masterData.put(id, relationship);
	}

	void remove(final Map<MemoryIdentity, MemoryRelationship> relationships) {

		// avoid iteration of caches when there are no IDs to remove..
		if (!relationships.isEmpty()) {

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
	}

	void updateCache(final MemoryRelationship relationship) {
		// relationship type cannot be changed => no-op
	}

	void loadFromStorage(final MemoryDatabaseService db, final File storageDirectory) {

		final File dbFile = getRelationshipStorageFile(storageDirectory);
		if (dbFile.exists()) {

			try (final ObjectInputStream in = new ObjectInputStream(getZipInputStream(dbFile))) {

				final int formatVersion = in.readInt();

				if (STORAGE_FORMAT_VERSION == formatVersion) {

					final int relationshipCount = in.readInt();

					for (int i=0; i<relationshipCount; i++) {

						final MemoryRelationship relationship = MemoryRelationship.createFromStorage(db, in);
						if (relationship != null) {

							add(relationship);
						}
					}

				} else {

					throw new IllegalStateException("Storage format " + formatVersion + " of " + dbFile.getAbsolutePath() + " does not match current format " + STORAGE_FORMAT_VERSION);
				}

			} catch (final Throwable t) {
				t.printStackTrace();
			}
		}
	}

	void writeToStorage(final File storageDirectory) {

		final File relationshipsFile = getRelationshipStorageFile(storageDirectory);

		try (final ObjectOutputStream out = new ObjectOutputStream(getZipOutputStream(relationshipsFile))) {

			final int relationshipCount = masterData.size();

			// first value: data format version
			out.writeInt(STORAGE_FORMAT_VERSION);

			// second value: node count
			out.writeInt(relationshipCount);

			for (final MemoryRelationship relationship : masterData.values()) {

				relationship.writeToStorage(out);
			}

			// flush
			out.flush();

		} catch (final IOException ex) {
			ex.printStackTrace();
		}
	}

	// ----- private methods -----
	private synchronized Set<MemoryIdentity> getCacheForType(final String type) {

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

	private File getRelationshipStorageFile(final File storageDirectory) {
		return storageDirectory.toPath().resolve("relationships.bin.zip").toFile();
	}
}
