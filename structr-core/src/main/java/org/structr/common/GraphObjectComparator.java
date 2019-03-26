/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyKey;

/**
 * A comparator for structr entities that uses a given property key and sort
 * order for comparison.
 *
 * Properties with null values (not existing properties) are always handled
 * as "lower than", so that any not-null value ranks higher.
 */
public class GraphObjectComparator implements Comparator<GraphObject> {

	public static final String ASCENDING  = "asc";
	public static final String DESCENDING = "desc";
	private static final Logger logger    = LoggerFactory.getLogger(GraphObjectComparator.class.getName());

	private PropertyKey sortKey;
	private String sortOrder;

	/**
	 * Creates a new GraphObjectComparator with the given sort key and order.
	 * @param sortKey
	 * @param sortDescending
	 */
	public GraphObjectComparator(final PropertyKey sortKey, final boolean sortDescending) {
		this(sortKey, sortDescending ? DESCENDING : ASCENDING);
	}

	public GraphObjectComparator(final PropertyKey sortKey, final String sortOrder) {

		this.sortKey   = sortKey;
		this.sortOrder = sortOrder;
	}

	@Override
	public int compare(GraphObject n1, GraphObject n2) {

		if (n1 == null || n2 == null) {

			throw new NullPointerException();

		}

		try {
			boolean desc = DESCENDING.equalsIgnoreCase(sortOrder);

			Comparable c1 = n1.getComparableProperty(sortKey);
			Comparable c2 = n2.getComparableProperty(sortKey);

			if (c1 == null || c2 == null) {

				if (c1 == null && c2 == null) {

					return 0;

				} else if (c1 == null) {

					return desc ? -1 : 1;

				} else {

					return desc ? 1 : -1;

				}

			}

			if (desc) {

				return c2.compareTo(c1);

			} else {

				return c1.compareTo(c2);

			}

		} catch (Throwable t) {

			logger.warn("Cannot compare properties {} of type {} to {} of type {}, property {} error.",
				new Object[] {
					n1.getProperty(GraphObject.id),
					n1.getProperty(AbstractNode.type),
					n2.getProperty(GraphObject.id),
					n2.getProperty(AbstractNode.type),
					sortKey
				});
		}

		return 0;
	}
}
