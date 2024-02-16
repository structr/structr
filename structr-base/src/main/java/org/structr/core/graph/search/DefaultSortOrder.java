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
package org.structr.core.graph.search;

import org.structr.api.search.SortOrder;
import org.structr.api.search.SortSpec;
import org.structr.api.search.SortType;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.ConfigurationProvider;

import java.util.ArrayList;
import java.util.List;

public class DefaultSortOrder implements SortOrder {

	private final List<PropertySortSpec> specs = new ArrayList<>();

	public DefaultSortOrder() {
	}

	public DefaultSortOrder(final Class type, final String[] sortKeyNames, final String[] sortOrders) {

		if (sortKeyNames != null) {

			final ConfigurationProvider config = StructrApp.getConfiguration();
			final int length                   = sortKeyNames.length;

			for (int i=0; i<length; i++) {

				final PropertyKey key = config.getPropertyKeyForJSONName(type, sortKeyNames[i]);
				if (key != null) {

					specs.add(new PropertySortSpec(key, sortOrders != null && sortOrders.length > i && "desc".equals(sortOrders[i])));
				}
			}
		}
	}

	public DefaultSortOrder(final PropertyKey key, final boolean descending) {
		specs.add(new PropertySortSpec(key, descending));
	}

	public void addElement(final PropertyKey key, final boolean descending) {
		specs.add(new PropertySortSpec(key, descending));
	}

	@Override
	public List<SortSpec> getSortElements() {
		return (List)specs;
	}

	@Override
	public int compare(final Object o1, final Object o2) {

		if (o1 == null || o2 == null) {
			throw new NullPointerException("Cannot compare null objects.");
		}

		if (o1 instanceof GraphObject && o2 instanceof GraphObject) {

			final GraphObject g1 = (GraphObject)o1;
			final GraphObject g2 = (GraphObject)o2;

			for (final PropertySortSpec spec : specs) {

				final PropertyKey key = spec.getSortProperty();
				final boolean desc    = spec.sortDescending();
				Comparable c1         = g1.getComparableProperty(key);
				Comparable c2         = g2.getComparableProperty(key);

				if (c1 == null || c2 == null) {

					if (c1 == null && c2 == null) {

						return 0;

					} else if (c1 == null) {

						return desc ? -1 : 1;

					} else {

						return desc ? 1 : -1;

					}

				}

				final int result = desc ? c2.compareTo(c1) : c1.compareTo(c2);
				if (result != 0) {

					// return result if values are different, stay in loop if values are equal
					return result;
				}
			}

			// if we arrive here, the values for all the keys are equal
			return 0;
		}

		throw new IllegalArgumentException("Cannot compare non-GraphObject objects");
	}

	public boolean isEmpty() {
		return specs.isEmpty();
	}


	// ----- nested classes -----
	private class PropertySortSpec implements SortSpec {

		private boolean descending = false;
		private PropertyKey key    = null;

		public PropertySortSpec(final PropertyKey key, final boolean desending) {

			this.descending = desending;
			this.key        = key;
		}

		public PropertyKey getSortProperty() {
			return key;
		}

		@Override
		public SortType getSortType() {
			return key.getSortType();
		}

		@Override
		public String getSortKey() {
			return key.jsonName();
		}

		@Override
		public boolean sortDescending() {
			return descending;
		}
	}
}
