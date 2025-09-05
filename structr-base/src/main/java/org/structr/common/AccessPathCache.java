/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.api.graph.Node;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.graph.Relationship;
import org.structr.core.graph.NodeInterface;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class AccessPathCache {

	private static final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
	private static final Set<String> allRelTypes       = new HashSet<>();
	private static final Set<String> allUuids          = new HashSet<>();

	public static PermissionResolutionMask get(final NodeInterface startNode, final NodeInterface endNode) {

		final String cacheKey  = cacheKey(startNode, endNode);
		final CacheEntry entry = cache.get(cacheKey);

		if (entry != null) {
			return entry.mask;
		}

		return null;
	}

	public static void put(final NodeInterface startNode, final NodeInterface endNode, final PermissionResolutionMask mask) {

		final CacheEntry entry = getOrCreateCacheEntry(startNode, endNode);

		// remember UUIDs
		allUuids.add(startNode.getUuid());
		allUuids.add(endNode.getUuid());

		entry.mask = mask;
	}

	public static void update(final NodeInterface startNode, final NodeInterface endNode, final Node node) {

		final CacheEntry entry = getOrCreateCacheEntry(startNode, endNode);
		final String uuid      = getUuid(node);

		if (uuid != null) {

			entry.uuids.add(uuid);
			allUuids.add(uuid);
		}
	}

	public static void update(final NodeInterface startNode, final NodeInterface endNode, final Relationship rel) {

		final CacheEntry entry = getOrCreateCacheEntry(startNode, endNode);
		final String uuid      = getUuid(rel);

		if (uuid != null) {

			final String relType   = rel.getType().name();

			entry.uuids.add(uuid);
			allUuids.add(uuid);

			entry.relTypes.add(relType);
			allRelTypes.add(relType);

		}
	}

	public static void invalidateForId(final String uuid) {

		if (allUuids.contains(uuid)) {

			for (final CacheEntry entry : cache.values()) {

				if (entry.uuids.contains(uuid)) {
					cache.remove(entry.key);
				}
			}

			allUuids.remove(uuid);
		}
	}

	public static void invalidateForRelType(final String relType) {

		if (allRelTypes.contains(relType)) {

			for (final CacheEntry entry : cache.values()) {

				if (entry.relTypes.contains(relType)) {
					cache.remove(entry.key);
				}
			}

			allRelTypes.remove(relType);
		}
	}

	public static void invalidate() {

		allRelTypes.clear();
		allUuids.clear();
		cache.clear();
	}

	// ----- private methods -----
	private static CacheEntry getOrCreateCacheEntry(final NodeInterface startNode, final NodeInterface endNode) {

		final String cacheKey = cacheKey(startNode, endNode);
		CacheEntry entry      = cache.get(cacheKey);

		if (entry == null) {

			entry = new CacheEntry();
			entry.key = cacheKey;

			cache.put(cacheKey, entry);
		}

		return entry;
	}

	private static String cacheKey(final NodeInterface startNode, final NodeInterface endNode) {
		return startNode.getUuid() + endNode.getUuid();
	}

	private static String getUuid(final PropertyContainer prop) {

		if (prop.hasProperty("id")) {
			return (String)prop.getProperty("id");
		}

		return null;
	}

	// ----- nested classes -----
	private static class CacheEntry {

		protected Set<String> uuids             = new HashSet<>();
		protected Set<String> relTypes          = new HashSet<>();
		protected PermissionResolutionMask mask = null;
		protected String key                    = null;
	}
}
