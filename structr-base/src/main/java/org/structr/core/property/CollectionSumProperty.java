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
package org.structr.core.property;

import org.structr.api.Predicate;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;

import java.util.List;
import java.util.Map;

/**
 *
 *
 */
public class CollectionSumProperty<T extends NodeInterface, S extends Number> extends AbstractReadOnlyProperty<S> {

	private Property<List<T>> collectionKey = null;
	private Property<S> valueKey            = null;
	private Predicate<T> predicate          = null;

	public CollectionSumProperty(String name, Property<List<T>> collectionKey, Property<S> valueKey) {
		super(name);

		this.collectionKey = collectionKey;
		this.valueKey = valueKey;
	}

	public CollectionSumProperty(String name, Property<List<T>> collectionKey, Property<S> valueKey, Predicate<T> predicate) {

		this(name, collectionKey, valueKey);
		this.predicate = predicate;
	}

	@Override
	public String relatedType() {
		return null;
	}

	@Override
	public String valueType() {
		return valueKey.valueType();
	}

	@Override
	public SortType getSortType() {
		return SortType.Integer;
	}

	@Override
	public S getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public S getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final Predicate<GraphObject> predicate) {

		int     intSum    = 0;
		long    longSum   = 0L;
		double  doubleSum = 0.0d;
		float   floatSum  = 0.0f;

		Class cls = Integer.class;

		for (T collectionObj : obj.getProperty(collectionKey)) {

			if (this.predicate != null && !this.predicate.accept(collectionObj)) {
				continue;
			}

			S value = collectionObj.getProperty(valueKey);

			if (value instanceof Integer) {

				intSum += (Integer) value;

			} else if (value instanceof Long) {

				longSum += (Long) value;
				cls = Long.class;

			} else if (value instanceof Double) {

				doubleSum += (Double) value;
				cls = Double.class;

			} else if (value instanceof Float) {

				floatSum += (Float) value;
				cls = Float.class;
			}
		}

		switch (cls.getSimpleName()) {

			case "Integer": return (S) Integer.valueOf(intSum);
			case "Long":    return (S) Long.valueOf(longSum);
			case "Double":  return (S) Double.valueOf(doubleSum);
			case "Float":   return (S) Float.valueOf(floatSum);
		}

		return (S) Integer.valueOf(intSum);
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(java.lang.String type, final String viewName) {
		return 1;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}
}
