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
package org.structr.core.property;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.search.Occurrence;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.search.ArraySearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;

/**
* A property that stores and retrieves an array of the given type.
 *
 *
 */
public class ArrayProperty<T> extends AbstractPrimitiveProperty<T[]> {

	private static final Logger logger = LoggerFactory.getLogger(ArrayProperty.class.getName());

	private Class<T> componentType = null;

	public ArrayProperty(String name, Class<T> componentType) {

		super(name);

		this.componentType = componentType;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {

		// We can only try to fix a String and convert it into a String[]
		if (value != null && value instanceof String) {

			String[] fixedValue = null;

			final String stringValue = (String) value;

			if (stringValue.contains(",")) {
				fixedValue = stringValue.split(",");
			}

			if (stringValue.contains(" ")) {
				fixedValue = stringValue.split(" ");
			}

			if (securityContext != null && entity != null) {

				try {
					setProperty(securityContext, entity, (T[]) fixedValue);
				} catch (FrameworkException ex) {
					logger.warn("", ex);
				}

			}

			return fixedValue;

		}

		return value;
	}

	@Override
	public String typeName() {
		return componentType.getSimpleName().concat("[]");
	}

	@Override
	public Class valueType() {
		return componentType;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public PropertyConverter<T[], ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<T[], ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		this.securityContext = securityContext;
		this.entity = entity;
		return null;
	}

	@Override
	public PropertyConverter<?, T[]> inputConverter(SecurityContext securityContext) {
		return new ArrayInputConverter(securityContext);
	}

	private class ArrayInputConverter extends PropertyConverter<Object, T[]> {

		public ArrayInputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public Object revert(Object[] source) throws FrameworkException {
			return source != null ? Arrays.asList(source) : null;
		}

		@Override
		public T[] convert(Object source) throws FrameworkException {

			if (source == null) {
				return null;
			}

			if (source instanceof List) {

				T[] dummyValue = (T[])Array.newInstance(componentType, 0);
				return (T[])((List<T>)source).toArray(dummyValue);
			}

			if (source.getClass().isArray()) {
				return (T[])source;
			}

			if (source instanceof String) {

				final String s = (String) source;

				if (s.contains(",")) {
					return (T[]) s.split(",");
				}
			}

			return (T[]) new Object[] { source };
		}

	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Occurrence occur, T[] searchValue, boolean exactMatch, Query query) {

		// early exit, return empty search attribute
		if (searchValue == null) {
			return new ArraySearchAttribute(this, "", exactMatch ? occur : Occurrence.OPTIONAL, exactMatch);
		}

		final SearchAttributeGroup group = new SearchAttributeGroup(occur);

		for (T value : searchValue) {

			group.add(new ArraySearchAttribute(this, value, exactMatch ? occur : Occurrence.OPTIONAL, exactMatch));

		}

		return group;
	}


}
