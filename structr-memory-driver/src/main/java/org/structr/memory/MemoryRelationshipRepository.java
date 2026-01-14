/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.memory.index.filter.Filter;
import org.structr.memory.index.filter.MemoryLabelFilter;
import org.structr.memory.index.filter.SourceNodeFilter;
import org.structr.memory.index.filter.TargetNodeFilter;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 */
public class MemoryRelationshipRepository extends EntityRepository {

	private static final Logger logger = LoggerFactory.getLogger(MemoryRelationshipRepository.class);

	final Map<MemoryIdentity, MemoryRelationship> masterData   = new ConcurrentSkipListMap<>();
	final Map<String, Set<MemoryIdentity>> typeCache           = new ConcurrentSkipListMap<>();
	final Map<MemoryIdentity, Set<MemoryIdentity>> sourceCache = new ConcurrentSkipListMap<>();
	final Map<MemoryIdentity, Set<MemoryIdentity>> targetCache = new ConcurrentSkipListMap<>();
	final Set<String> duplicatesCheckCache                     = new LinkedHashSet<>();
	boolean disableDuplicatesCheck                             = false;

	public MemoryRelationshipRepository() {
		this(false);
	}

	public MemoryRelationshipRepository(final boolean disableDuplicatesCheck) {
		this.disableDuplicatesCheck = disableDuplicatesCheck;
	}

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

					final Set<MemoryIdentity> set = getCacheForType(label, false);
					if (set != null) {

						cache.addAll(set);
					}
				}

				return Iterables.filter(r -> r != null, Iterables.map(i -> masterData.get(i), cache));
			}

			if (filter instanceof SourceNodeFilter) {

				final SourceNodeFilter<MemoryRelationship> s = (SourceNodeFilter<MemoryRelationship>)filter;
				final MemoryIdentity id                      = s.getIdentity();

				final Set<MemoryIdentity> set = getCacheForSource(id, false);
				if (set != null) {

					return Iterables.filter(r -> r != null, Iterables.map(i -> masterData.get(i), new LinkedHashSet<>(set)));
				}

				return Collections.EMPTY_LIST;
			}

			if (filter instanceof TargetNodeFilter) {

				final TargetNodeFilter<MemoryRelationship> s = (TargetNodeFilter<MemoryRelationship>)filter;
				final MemoryIdentity id                      = s.getIdentity();

				final Set<MemoryIdentity> set = getCacheForTarget(id, false);
				if (set != null) {

					return Iterables.filter(r -> r != null, Iterables.map(i -> masterData.get(i), new LinkedHashSet<>(set)));
				}

				return Collections.EMPTY_LIST;
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

		if (!disableDuplicatesCheck) {

			if (duplicatesCheckCache.contains(key)) {
				throw new IllegalStateException("Duplicate relationship: " + key);
			}

			duplicatesCheckCache.add(key);
		}

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

			if (!disableDuplicatesCheck) {

				// clear uniqueness check cache as well
				for (final MemoryRelationship rel : relationships.values()) {

					duplicatesCheckCache.remove(rel.getUniquenessKey());
				}
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
				logger.error(ExceptionUtils.getStackTrace(t));
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
			logger.error(ExceptionUtils.getStackTrace(ex));
		}
	}

	Map<MemoryIdentity, MemoryRelationship> getMasterData() {
		return masterData;
	}

	// ----- private methods -----
	private Set<MemoryIdentity> getCacheForType(final String type) {
		return getCacheForType(type, true);
	}

	private synchronized Set<MemoryIdentity> getCacheForType(final String type, final boolean create) {

		Set<MemoryIdentity> cache = typeCache.get(type);
		if (cache == null && create) {

			cache = new CopyOnWriteArraySet<>();
			typeCache.put(type, cache);
		}

		return cache;
	}

	private Set<MemoryIdentity> getCacheForSource(final MemoryIdentity source) {
		return getCacheForSource(source, true);
	}

	private synchronized Set<MemoryIdentity> getCacheForSource(final MemoryIdentity source, final boolean create) {

		Set<MemoryIdentity> cache = sourceCache.get(source);
		if (cache == null && create) {

			cache = new CopyOnWriteArraySet<>();
			sourceCache.put(source, cache);
		}

		return cache;
	}

	private Set<MemoryIdentity> getCacheForTarget(final MemoryIdentity target) {
		return getCacheForTarget(target, true);
	}

	private synchronized Set<MemoryIdentity> getCacheForTarget(final MemoryIdentity target, final boolean create) {

		Set<MemoryIdentity> cache = targetCache.get(target);
		if (cache == null && create) {

			cache = new CopyOnWriteArraySet<>();
			targetCache.put(target, cache);
		}

		return cache;
	}

	private File getRelationshipStorageFile(final File storageDirectory) {
		return storageDirectory.toPath().resolve("relationships.bin.zip").toFile();
	}
}
