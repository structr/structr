/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.extraction.function;

import org.structr.core.GraphObjectMap;
import org.structr.core.property.GenericProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts the module's results (nested maps and lists) into GraphObjectMaps for scripts and REST.
 *
 * Function.toGraphObjectMap() is not used: it builds every nested map from its parent map, so each
 * nested object would repeat all keys of the object it is contained in.
 */
public final class ResultMaps {

	private ResultMaps() {
	}

	public static GraphObjectMap toGraphObjectMap(final Map<String, ?> source) {

		final GraphObjectMap result = new GraphObjectMap();

		for (final Map.Entry<String, ?> entry : source.entrySet()) {

			result.put(new GenericProperty<>(entry.getKey()), convert(entry.getValue()));
		}

		return result;
	}

	// ----- private methods -----
	private static Object convert(final Object value) {

		if (value instanceof Map<?, ?> map) {

			final GraphObjectMap nested = new GraphObjectMap();

			for (final Map.Entry<?, ?> entry : map.entrySet()) {

				nested.put(new GenericProperty<>(String.valueOf(entry.getKey())), convert(entry.getValue()));
			}

			return nested;
		}

		if (value instanceof Iterable<?> iterable) {

			final List<Object> list = new ArrayList<>();

			for (final Object element : iterable) {

				list.add(convert(element));
			}

			return list;
		}

		return value;
	}
}
