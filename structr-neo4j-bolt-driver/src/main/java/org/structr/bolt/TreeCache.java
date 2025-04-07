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
package org.structr.bolt;

import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * A tree-based hierarchical cache implementation.
 * @param <T>
 */
public class TreeCache<T extends Comparable> {

	private final TreeCacheNode root               = new TreeCacheNode("root");
	private final Map<String, String[]> splitCache = new HashMap<>();

	private final String keyPartSeparator;
	private final long nodeId;

	public TreeCache(final long nodeId, final String keyPartSeparator) {

		this.keyPartSeparator = keyPartSeparator;
		this.nodeId           = nodeId;
	}

	public void insert(final String key, final T value) {
		getNode(key, true).add(value);
	}

	public List<T> get(final String key) {

		final TreeCacheNode<T> node = getNode(key, false);
		if (node != null) {

			final List<T> list = node.getDataRecursively();

			// Sort list data because it might have the wrong ordering when the
			// data is concatenated from more than one tree node.
			Collections.sort(list);

			return list;
		}

		return null;
	}

	public boolean contains(final String key) {
		return getNode(key, false) != null;
	}

	public void clear() {
		root.clear();
	}

	// ----- private methods -----
	private String[] split(final String key) {

		String[] cached = splitCache.get(key);
		if (cached == null) {

			cached = StringUtils.split(key, keyPartSeparator);
			splitCache.put(key, cached);
		}

		return cached;
	}

	private String serialize() {
		return "TreeCache(" + nodeId + "): " + root.serialize();
	}

	private TreeCacheNode<T> getNode(final String key, final boolean create) {

		TreeCacheNode<T> current = root;

		// empty key => use root
		if (StringUtils.isNotBlank(key)) {

			// insert will always store data in the leaves
			for (final String part : split(key)) {

				if (current != null) {

					current = current.getOrCreateChild(part, create);

				} else {

					return null;
				}
			}
		}

		return current;
	}

	// ----- nested classes -----
	private class TreeCacheNode<T extends Comparable> {

		private final Map<String, TreeCacheNode> children = new HashMap<>();
		private final Set<T> data                         = new TreeSet<>();
		private final String key;

		public TreeCacheNode(final String key) {
			this.key = key;
		}

		public List<T> getDataRecursively() {

			final List<T> allData = new LinkedList<>();

			allData.addAll(data);

			for (final TreeCacheNode<T> child : children.values()) {
				allData.addAll(child.getDataRecursively());
			}

			return allData;
		}

		@Override
		public String toString() {
			return key;
		}

		public void add(final T value) {
			data.add(value);
		}

		public void clear() {
			children.clear();
			data.clear();
		}

		public TreeCacheNode<T> getOrCreateChild(final String key, final boolean create) {

			TreeCacheNode<T> node = children.get(key);
			if (node == null && create) {

				node = new TreeCacheNode<>(key);
				children.put(key, node);
			}

			return node;
		}

		public String serialize() {
			return serialize(0);
		}

		// ----- private methods -----
		public String serialize(final int level) {

			final StringBuilder buf = new StringBuilder();

			buf.append(StringUtils.repeat("    ", level));
			buf.append(key);
			buf.append(": ");
			buf.append(data);
			buf.append("\n");

			for (final TreeCacheNode<T> child : children.values()) {

				buf.append(child.serialize(level + 1));
			}

			return buf.toString();
		}
	}
}
