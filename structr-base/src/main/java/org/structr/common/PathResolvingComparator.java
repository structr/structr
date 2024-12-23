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
package org.structr.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.Comparator;

/**
 * A comparator for structr entities that uses a dot-notation path
 * through the graph for comparison.
 *
 * Properties with null values (not existing properties) are always handled
 * as "lower than", so that any not-null value ranks higher.
 */
public class PathResolvingComparator implements Comparator<GraphObject> {

	private static final Logger logger    = LoggerFactory.getLogger(PathResolvingComparator.class.getName());

	private ActionContext actionContext = null;
	private boolean sortDescending      = false;
	private String sortKey              = null;

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
	}

	@Override
	public int compare(final GraphObject n1, final GraphObject n2) {

		if (n1 == null || n2 == null) {

			throw new NullPointerException();
		}

		final Comparable c1 = resolve(n1, sortKey);
		final Comparable c2 = resolve(n2, sortKey);

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
	private Comparable resolve(final GraphObject obj, final String path) {

		final ConfigurationProvider config = StructrApp.getConfiguration();
		final String[] parts               = path.split("[\\.]+");
		GraphObject current                = obj;
		int pos                            = 0;

		for (final String part : parts) {

			final Traits type     = current.getTraits();
			final PropertyKey key = type.key(part);

			if (key == null) {

				logger.warn("Unknown key {} while resolving path {} for sorting.", part, path);
				return null;
			}

			try {
				final Object value = current.evaluate(actionContext, part, null, new EvaluationHints(), 1, 1);
				if (value != null) {

					// last part of path?
					if (++pos == parts.length) {

						if (value instanceof Comparable) {

							return (Comparable)value;
						}

						logger.warn("Path evaluation result of component {} of type {} in {} cannot be used for sorting.", part, value.getClass().getSimpleName(), path);
						return null;
					}

					if (value instanceof GraphObject) {

						current = (GraphObject)value;

					} else {

						logger.warn("Path component {} of type {} in {} cannot be evaluated further.", part, value.getClass().getSimpleName(), path);
						return null;
					}

				} else {

					// value needs to be sorted as null if getProperty() returns null
					return null;
				}

			} catch (FrameworkException fex) {

				logger.warn("Exception while evaluating sort path {}: {}", path, fex.getMessage());
				return null;
			}
		}

		return null;
	}
}
