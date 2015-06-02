package org.structr.schema.importer;

import gnu.trove.map.hash.THashMap;
import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author Christian Morgner
 */
public class FileBasedHashLongMap<K> {

	private final Map<K, LazyFileBasedLongCollection> hashFiles = new THashMap<>();
	private String basePath                                     = null;


	public FileBasedHashLongMap(final String basePath) {
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

			collection = new LazyFileBasedLongCollection(basePath + File.separator + key.hashCode() + ".lfc");
			hashFiles.put(key, collection);
		}

		return collection;
	}
}
