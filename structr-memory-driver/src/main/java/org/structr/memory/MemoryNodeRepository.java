/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.memory.index.filter.MemoryTypeFilter;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 */
public class MemoryNodeRepository extends EntityRepository {

	private static final Logger logger = LoggerFactory.getLogger(MemoryNodeRepository.class);

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
				final Set<MemoryIdentity> cache        = new LinkedHashSet<>();

				// multiple labels result in OR not AND query
				for (final String label : mt.getLabels()) {

					cache.addAll(getCacheForLabel(label));
				}

				return Iterables.map(i -> masterData.get(i), cache);
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

	void add(final Iterable<MemoryNode> newData) {

		for (final MemoryNode node : newData) {
			add(node);
		}
	}

	void add(final MemoryNode node) {

		final MemoryIdentity id = node.getIdentity();
		final String type       = id.getType();

		for (final String label : node.getLabels()) {

			getCacheForLabel(label).add(id);
		}

		getCacheForType(type).add(id);

		masterData.put(id, node);
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

	void loadFromStorage(final MemoryDatabaseService db, final File storageDirectory) {

		final File nodesFile = getNodeStorageFile(storageDirectory);

		if (nodesFile.exists()) {

			try (final ObjectInputStream in = new ObjectInputStream(getZipInputStream(nodesFile))) {

				final int formatVersion = in.readInt();

				if (STORAGE_FORMAT_VERSION == formatVersion) {

					final int nodeCount = in.readInt();

					for (int i=0; i<nodeCount; i++) {

						final MemoryNode node = MemoryNode.createFromStorage(db, in);
						if (node != null) {

							add(node);
						}
					}

				} else {

					throw new IllegalStateException("Storage format " + formatVersion + " of " + nodesFile.getAbsolutePath() + " does not match current format " + STORAGE_FORMAT_VERSION);
				}

			} catch (final Throwable t) {
				logger.error(ExceptionUtils.getStackTrace(t));
			}
		}
	}

	void writeToStorage(final File storageDirectory) {

		final File nodesFile = getNodeStorageFile(storageDirectory);

		try (final ObjectOutputStream out = new ObjectOutputStream(getZipOutputStream(nodesFile))) {

			final int nodeCount = masterData.size();

			// first value: data format version
			out.writeInt(STORAGE_FORMAT_VERSION);

			// second value: node count
			out.writeInt(nodeCount);

			for (final MemoryNode node : masterData.values()) {

				node.writeToStorage(out);
			}

			// flush
			out.flush();

		} catch (final IOException ex) {
			logger.error(ExceptionUtils.getStackTrace(ex));
		}
	}

	Map<MemoryIdentity, MemoryNode> getMasterData() {
		return masterData;
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

	private File getNodeStorageFile(final File storageDirectory) {
		return storageDirectory.toPath().resolve("nodes.bin.zip").toFile();
	}
}
