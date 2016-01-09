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
package org.structr.rest.filter.comparator;

import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.PropertyKey;
import org.structr.core.GraphObject;

/**
 *
 *
 */
public class PropertyValueComparator implements Comparator<GraphObject> {

	public enum Order {
		Ascending, Descending
	}

	private static final Logger logger = Logger.getLogger(PropertyValueComparator.class.getName());
	private PropertyKey propertyKey = null;
	private Order order = null;

	public PropertyValueComparator(PropertyKey propertyKey) {
		this(propertyKey, Order.Ascending);
	}

	public PropertyValueComparator(PropertyKey propertyKey, Order order) {
		this.propertyKey = propertyKey;
		this.order = order;
	}

	@Override
	public int compare(GraphObject o1, GraphObject o2) {

		Object value1 = o1.getProperty(propertyKey);
		Object value2 = o2.getProperty(propertyKey);

		if(value1 != null && value2 != null) {
			if(value1 instanceof Comparable && value2 instanceof Comparable) {

				Comparable comp1 = (Comparable)value1;
				Comparable comp2 = (Comparable)value2;

				if(order.equals(Order.Ascending)) {
					
					return comp1.compareTo(comp2);

				} else if(order.equals(Order.Descending)){

					return comp2.compareTo(comp1);
				}

			} else {

				logger.log(Level.WARNING, "Cannot compare {0} to {1}!", new Object[] { value1.getClass().getName(), value2.getClass().getName() } );
			}

		} else {

			logger.log(Level.WARNING, "Cannot compare null values!");
		}

		return 0;
	}
}
