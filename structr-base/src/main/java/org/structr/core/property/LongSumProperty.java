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

import java.util.Map;

/**
 *
 *
 */
public class LongSumProperty extends AbstractReadOnlyProperty<Long> {

	private EndNodes collectionProperty  = null;
	private Property<Long> valueProperty = null;

	public LongSumProperty(String name, EndNodes collectionProperty, Property<Long> valueProperty, Long defaultValue) {

		super(name, defaultValue);

		this.collectionProperty = collectionProperty;
		this.valueProperty = valueProperty;
		this.defaultValue = defaultValue;
	}

	@Override
	public String relatedType() {
		return null;
	}

	@Override
	public Class valueType() {
		return Long.class;
	}

	@Override
	public Long getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public Long getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate) {

		final Iterable<? extends GraphObject> collection = obj.getProperty(collectionProperty);
		if (collection != null) {

			long sum = 0L;

			for (GraphObject element : collection) {

				Long value = element.getProperty(valueProperty);
				if (value != null) {

					sum += value.longValue();
				}

			}

			return Long.valueOf(sum);
		}

		return defaultValue();
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public SortType getSortType() {
		return SortType.Long;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return 1L;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}
}
