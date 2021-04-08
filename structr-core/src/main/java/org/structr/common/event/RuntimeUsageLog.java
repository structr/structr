/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.common.event;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.map.LRUMap;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;

/**
 */
public class RuntimeUsageLog {

	private static final Map<String, Map<String, Set<Usage>>> data = new LRUMap<>(1000);
	private static final ThreadLocal<Deque<Usage>> stack           = ThreadLocal.withInitial(() -> new ArrayDeque<>());

	public static void log(final GraphObject entity, final PropertyKey key) {

		if (entity != null && key != null) {

			final Deque<Usage> currentStack = stack.get();
			if (currentStack.isEmpty()) {

				return;
			}

			final String type = entity.getClass().getSimpleName();
			final String name = key.jsonName();

			Map<String, Set<Usage>> map = data.get(type);
			if (map == null) {

				map = new LinkedHashMap<>();
				data.put(type, map);
			}

			Set<Usage> entries = map.get(name);
			if (entries == null) {

				entries = new LinkedHashSet<>();
				map.put(name, entries);
			}

			entries.add(currentStack.peek());
		}
	}

	public static Map<String, Map<String, Set<Usage>>> getData() {
		return data;
	}

	public static void enter(final GraphObject entity, final String contextInfo) {

		if (entity != null) {

			if (entity.isFrontendNode()) {

				stack.get().push(new Usage(entity.getUuid(), entity.getType(), contextInfo));

			} else {

				// for data-nodes (custom types) we only need the type, not the individual nodes
				stack.get().push(new Usage(null, entity.getType(), contextInfo));
			}
		}
	}

	public static void leave(final GraphObject entity) {

		if (entity != null) {

			stack.get().pop();
		}
	}
}
