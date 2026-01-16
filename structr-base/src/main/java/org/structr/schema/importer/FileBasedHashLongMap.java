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
package org.structr.schema.importer;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 *
 */
public class FileBasedHashLongMap<K> {

	private final Map<K, LazyFileBasedLongCollection> hashFiles = new LinkedHashMap<>();
	private boolean clearOnOpen                                 = true;
	private String basePath                                     = null;

	public FileBasedHashLongMap(final String basePath) {
		this(basePath, true);
	}

	public FileBasedHashLongMap(final String basePath, final boolean clearOnOpen) {
		this.basePath = basePath;
	}

	public void add(final K key, final Long value) {
		getCollectionForKey(key).add(value);
	}

	public Collection<Long> get(final K key) {
		return getCollectionForKey(key);
	}

	// ----- private methods -----
	private LazyFileBasedLongCollection getCollectionForKey(final K key) {

		LazyFileBasedLongCollection collection = hashFiles.get(key);
		if (collection == null) {

			collection = new LazyFileBasedLongCollection(basePath + File.separator + key.hashCode() + ".lfc", clearOnOpen);
			hashFiles.put(key, collection);
		}

		return collection;
	}
}
