/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import org.structr.core.property.PropertyKey;
import java.util.Collections;
import org.structr.core.GraphObject;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.ViewTransformation;
import org.structr.core.entity.AbstractNode;

//~--- classes ----------------------------------------------------------------

/**
 * A comparator for structr entities that uses a given property key and sort
 * order for comparison.
 * 
 * Properties with null values (not existing properties) are always handled
 * as "lower than", so that any not-null value ranks higher.
 * 
 *
 */
public class GraphObjectComparator extends ViewTransformation<GraphObject> implements Comparator<GraphObject> {

	public static final String ASCENDING  = "asc";
	public static final String DESCENDING = "desc";
	private static final Logger logger    = Logger.getLogger(GraphObjectComparator.class.getName());

	//~--- fields ---------------------------------------------------------

	private PropertyKey sortKey;
	private String sortOrder;

	//~--- constructors ---------------------------------------------------

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

	//~--- methods --------------------------------------------------------

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
			
			t.printStackTrace();
			
			logger.log(Level.WARNING, "Cannot compare properties {0} of type {1} to {2} of type {3}, property {4} error.",
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

	@Override
	public void apply(SecurityContext securityContext, List<GraphObject> obj) throws FrameworkException {
		Collections.sort(obj, this);
	}

	@Override
	public int getOrder() {
		return 998;
	}

	@Override
	public boolean evaluateWrappedResource() {
		return true;
	}
}
