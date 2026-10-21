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
package org.structr.core.function;

import org.structr.common.SortKeysComparator;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The sort keys of the sort() function and of the sort predicate of find(), which accept the same
 * arguments in any combination: the name of a key, optionally followed by its order, or an object
 * with the options key, descending (or desc), natural and nulls. An order is true or false, or asc
 * or desc, in any case.
 *
 * Null in place of a name is the default key, and so is an order without a name before it, so
 * sort(list, null, true) and sort(list, 'desc') both sort by name in descending order.
 */
public class SortKeyArguments {

	private static final String DEFAULT_SORT_KEY = "name";
	private static final String KEY              = "key";
	private static final String DESCENDING       = "descending";
	private static final String DESC             = "desc";
	private static final String NATURAL          = "natural";
	private static final String NULLS            = "nulls";

	private static final Set<String> OPTIONS = Set.of(KEY, DESCENDING, DESC, NATURAL, NULLS);

	public record SortKey(String key, boolean descending, boolean natural, SortKeysComparator.Nulls nulls) {

		/**
		 * Whether the database can sort by this key: a property of the objects themselves, in the order of
		 * the database, with missing values where the database puts them.
		 */
		public boolean isSortableByDatabase() {

			return !key.contains(".") && !natural && nulls == null;
		}
	}

	/**
	 * Parses the arguments into sort keys, in the order they are given. Without any, the result is the
	 * default key in ascending order.
	 *
	 * @param function the name of the function, for error messages
	 */
	public static List<SortKey> parse(final String function, final List<?> arguments) throws FrameworkException {

		final List<SortKey> keys = new ArrayList<>();

		for (int i = 0; i < arguments.size(); i++) {

			final Object argument         = arguments.get(i);
			final Map<String, Object> map = toMap(argument);

			if (map != null) {

				keys.add(fromOptions(function, map));
				continue;
			}

			if (isOrder(argument)) {

				// an order without a name before it, like sort(list, true)
				keys.add(new SortKey(DEFAULT_SORT_KEY, isDescending(argument), false, null));
				continue;
			}

			// a name, or null for the default key, optionally followed by its order
			final String name  = argument != null ? argument.toString() : DEFAULT_SORT_KEY;
			boolean descending = false;

			if (i + 1 < arguments.size() && (isOrder(arguments.get(i + 1)) || arguments.get(i + 1) == null)) {

				descending = isDescending(arguments.get(++i));
			}

			keys.add(new SortKey(name, descending, false, null));
		}

		if (keys.isEmpty()) {

			keys.add(new SortKey(DEFAULT_SORT_KEY, false, false, null));
		}

		return keys;
	}

	public static Map<String, Object> toMap(final Object value) {

		if (value instanceof Map map) {

			return map;
		}

		if (value instanceof GraphObjectMap map) {

			return map.toMap();
		}

		return null;
	}

	// ----- private methods -----
	private static SortKey fromOptions(final String function, final Map<String, Object> options) throws FrameworkException {

		for (final String name : options.keySet()) {

			if (!OPTIONS.contains(name)) {

				throw new FrameworkException(422, function + "(): unknown option '" + name + "' of a sort key, supported are " + String.join(", ", new TreeSet<>(OPTIONS)) + ".");
			}
		}

		final Object key = options.get(KEY);
		if (key != null && !(key instanceof String)) {

			throw new FrameworkException(422, function + "(): the key of a sort key must be a string, got " + key + ".");
		}

		final Boolean descending = getBoolean(function, options, DESCENDING);
		final Boolean desc       = getBoolean(function, options, DESC);

		if (descending != null && desc != null && !descending.equals(desc)) {

			throw new FrameworkException(422, function + "(): " + DESCENDING + " and " + DESC + " of a sort key contradict each other.");
		}

		final Boolean natural = getBoolean(function, options, NATURAL);

		return new SortKey(
			key != null ? (String)key : DEFAULT_SORT_KEY,
			Boolean.TRUE.equals(descending != null ? descending : desc),
			Boolean.TRUE.equals(natural),
			getNulls(function, options)
		);
	}

	private static Boolean getBoolean(final String function, final Map<String, Object> options, final String name) throws FrameworkException {

		final Object value = options.get(name);
		if (value == null) {

			return null;
		}

		if (value instanceof Boolean bool) {

			return bool;
		}

		switch (value.toString()) {

			case "true":
				return true;

			case "false":
				return false;
		}

		throw new FrameworkException(422, function + "(): invalid value '" + value + "' for " + name + ", supported values are true and false.");
	}

	private static SortKeysComparator.Nulls getNulls(final String function, final Map<String, Object> options) throws FrameworkException {

		final Object nulls = options.get(NULLS);
		if (nulls == null) {

			return null;
		}

		switch (nulls.toString()) {

			case "first":
				return SortKeysComparator.Nulls.First;

			case "last":
				return SortKeysComparator.Nulls.Last;
		}

		throw new FrameworkException(422, function + "(): invalid value '" + nulls + "' for " + NULLS + ", supported values are first and last.");
	}

	// true or false, also as text, which is how an order often arrives from StructrScript or a request, or asc or desc
	private static boolean isOrder(final Object value) {

		if (value instanceof Boolean) {

			return true;
		}

		final String text = String.valueOf(value);

		return "true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text) || "asc".equalsIgnoreCase(text) || "desc".equalsIgnoreCase(text);
	}

	private static boolean isDescending(final Object value) {

		return value != null && ("true".equalsIgnoreCase(value.toString()) || "desc".equalsIgnoreCase(value.toString()));
	}
}
