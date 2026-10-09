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
package org.structr.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.docs.Documentation;
import org.structr.docs.ontology.ConceptType;
import org.structr.schema.action.ActionContext;

import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A comparator for structr entities that uses a dot-notation path
 * through the graph for comparison.
 *
 * A path through a collection, like projects.name, resolves to several values
 * per object: ascending order uses the smallest of them, descending order the
 * largest, so each object is placed by the value that comes first.
 *
 * Objects without a value, including those with an empty collection on the
 * path, are handled as "higher than" any value, so they come last in ascending
 * and first in descending order.
 */
@Documentation(name="Transitive sorting", type=ConceptType.Feature, shortDescription="Sort a list of nodes by property of related nodes.", parent="Advanced find")
public class PathResolvingComparator implements Comparator<GraphObject> {

	private static final Logger logger    = LoggerFactory.getLogger(PathResolvingComparator.class.getName());

	private final Map<GraphObject, Comparable> resolvedValues = new IdentityHashMap<>();
	private ActionContext actionContext                       = null;
	private boolean sortDescending                            = false;
	private boolean warned                                    = false;
	private String[] parts                                    = null;
	private String sortKey                                    = null;

	/**
	 * Creates a new PathResolvingComparator with the given sort key and order.
	 * @param actionContext
	 * @param sortKey
	 * @param sortDescending
	 */
	public PathResolvingComparator(final ActionContext actionContext, final String sortKey, final boolean sortDescending) {

		this.sortDescending = sortDescending;
		this.actionContext  = actionContext;
		this.sortKey        = sortKey;
		this.parts          = sortKey.split("[\\.]+");
	}

	@Override
	public int compare(final GraphObject n1, final GraphObject n2) {

		if (n1 == null || n2 == null) {

			throw new NullPointerException();
		}

		final Comparable c1 = getValue(n1);
		final Comparable c2 = getValue(n2);

		if (c1 == null || c2 == null) {

			if (c1 == null && c2 == null) {

				return 0;

			} else if (c1 == null) {

				return sortDescending ? -1 : 1;

			} else {

				return sortDescending ? 1 : -1;
			}

		}

		if (sortDescending) {

			return c2.compareTo(c1);

		} else {

			return c1.compareTo(c2);

		}
	}

	// ----- private methods -----
	private Comparable getValue(final GraphObject obj) {

		// a sort compares each object many times, and resolving the path means traversing relationships
		if (resolvedValues.containsKey(obj)) {

			return resolvedValues.get(obj);
		}

		final Comparable value = resolve(obj, obj, 0, null);

		resolvedValues.put(obj, value);

		return value;
	}

	/**
	 * Resolves the path from the given position on and returns the value that comes first in the sort order,
	 * either the given one or one found on the path.
	 */
	private Comparable resolve(final GraphObject current, final GraphObject obj, final int pos, final Comparable first) {

		final String part     = parts[pos];
		final Traits type     = current.getTraits();
		final PropertyKey key = type.key(part);

		if (key == null) {

			warnOnce("Unknown key {} while resolving path {} for sorting.", part, sortKey);

			return first;
		}

		try {

			return collect(current.evaluate(actionContext, part, null, obj, 1, 1), obj, pos, first);

		} catch (FrameworkException fex) {

			warnOnce("Exception while evaluating sort path {}: {}", sortKey, fex.getMessage());

			return first;
		}
	}

	private Comparable collect(final Object value, final GraphObject obj, final int pos, final Comparable first) {

		// no value at this point of the path: the object keeps the value found so far, if any
		if (value == null) {

			return first;
		}

		// a collection on the path (or at its end, like an array property) contributes all of its values
		if (value instanceof Iterable<?> iterable) {

			Comparable result = first;

			for (final Object element : iterable) {

				result = collect(element, obj, pos, result);
			}

			return result;
		}

		if (value instanceof Object[] array) {

			return collect(Arrays.asList(array), obj, pos, first);
		}

		// last part of path?
		if (pos == parts.length - 1) {

			if (value instanceof Comparable c) {

				return comesFirst(c, first) ? c : first;
			}

			warnOnce("Path evaluation result of component {} of type {} in {} cannot be used for sorting.", parts[pos], value.getClass().getSimpleName(), sortKey);

			return first;
		}

		if (value instanceof GraphObject o) {

			return resolve(o, obj, pos + 1, first);
		}

		warnOnce("Path component {} of type {} in {} cannot be evaluated further.", parts[pos], value.getClass().getSimpleName(), sortKey);

		return first;
	}

	private boolean comesFirst(final Comparable value, final Comparable first) {

		if (first == null) {

			return true;
		}

		return sortDescending ? value.compareTo(first) > 0 : value.compareTo(first) < 0;
	}

	// the same problem would otherwise be logged for every comparison of the sort
	private void warnOnce(final String message, final Object... arguments) {

		if (!warned) {

			warned = true;

			logger.warn(message, arguments);
		}
	}
}
