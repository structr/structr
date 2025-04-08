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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.search.Operation;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.NumberFormatToken;
import org.structr.common.error.PropertyInputParsingException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.search.ArraySearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

/**
 * A property that stores and retrieves an array of the given type.
 *
 *
 */
public class ArrayProperty<T> extends AbstractPrimitiveProperty<T[]> {

	private static final Logger logger = LoggerFactory.getLogger(ArrayProperty.class.getName());

	private Class<T> componentType = null;
	private Method valueOfMethod = null;

	public ArrayProperty(String name, Class<T> componentType) {

		super(name);

		this.componentType = componentType;
		this.valueOfMethod = methodOrNull();
	}

	@Override
	public Object fixDatabaseProperty(Object value) {

		// We can only try to fix a String and convert it into a String[]
		if (value != null && value instanceof String) {

			String[] fixedValue = null;

			final String stringValue = (String)value;

			if (stringValue.contains(",")) {
				fixedValue = stringValue.split(",");
			}

			if (stringValue.contains(" ")) {
				fixedValue = stringValue.split(" ");
			}

			if (securityContext != null && entity != null) {

				try {
					setProperty(securityContext, entity, (T[])fixedValue);
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

		try {
			// This trick results in returning the proper array class for array properties.
			// Neccessary because of and since commit 1db80071543018a0766efa2dc895b7bc3e9a0e34
			return Class.forName("[L" + componentType.getName() + ";");

		} catch (ClassNotFoundException e) {}

		return null;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public PropertyConverter<T[], ?> databaseConverter(SecurityContext securityContext) {
		return new ArrayDatabaseConverter(securityContext);
	}

	@Override
	public PropertyConverter<T[], ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		this.securityContext = securityContext;
		this.entity = entity;
		return databaseConverter(securityContext);
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
				return ArrayProperty.this.convert((List)source);
			}

			if (source.getClass().isArray()) {
				return convert(Arrays.asList((T[])source));
			}

			if (source instanceof String) {

				final String s = (String)source;
				if (s.contains(",")) {

					return ArrayProperty.this.convert(Arrays.asList(s.split(",")));
				}
			}

			// create array of componentTypes
			final T[] result = (T[])Array.newInstance(componentType, 1);
			final T value    = ArrayProperty.this.fromString(source.toString());

			if (value != null) {
				result[0] = value;
			}

			return result;
		}

	}

	private class ArrayDatabaseConverter extends PropertyConverter<T[], Object> {

		public ArrayDatabaseConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public T[] revert(Object source) throws FrameworkException {

			if (source == null) {
				return null;
			}

			if (source instanceof List) {
				return ArrayProperty.this.convert((List)source);
			}

			if (source.getClass().isArray()) {
				return revert(Arrays.asList((T[])source));
			}

			if (source instanceof String) {

				final String s = (String)source;
				if (s.contains(",")) {

					return ArrayProperty.this.convert(Arrays.asList(s.split(",")));
				}
			}

			// create array of componentTypes
			final T[] result = (T[])Array.newInstance(componentType, 1);
			final T value    = ArrayProperty.this.fromString(source.toString());

			if (value != null) {
				result[0] = value;
			}

			return result;

		}

		@Override
		public Object[] convert(T[] source) throws FrameworkException {
			return source;
		}
	}

	@Override
	protected void determineSearchType(final SecurityContext securityContext, final String requestParameter, final boolean exactMatch, final Query query) throws FrameworkException {

		if (requestParameter.contains(",") || requestParameter.contains(";")) {

			if (requestParameter.contains(";")) {

				if (exactMatch) {

					query.and();

					for (final Object part : trimFilterAndConvert(securityContext, requestParameter.split(";"))) {

						query.or(this, part, false);
					}

					query.parent();

				} else {

					query.and();

					for (final Object part : trimFilterAndConvert(securityContext, requestParameter.split(";"))) {

						query.or(this, part, false);
					}

					query.parent();
				}

			} else {

				query.and(this, convertSearchValue(securityContext, requestParameter), exactMatch);
			}

			return;
		}

		if (StringUtils.isEmpty(requestParameter)) {

			query.and(this, null);

			return;
		}

		// use default implementation
		super.determineSearchType(securityContext, requestParameter, exactMatch, query);
	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Operation operation, T[] valueInput, boolean exactMatch, Query query) {

		T[] searchValue = null;

		// we need to apply the database converter (at least for Date properties)
		final PropertyConverter conv = databaseConverter(securityContext);
		if (conv != null) {

			try {
				searchValue = (T[])conv.convert(valueInput);

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}

		} else {

			searchValue = valueInput;
		}

		// early exit, return empty search attribute
		if (searchValue == null) {
			return new ArraySearchAttribute(this, "", exactMatch ? operation : Operation.OR, exactMatch);
		}

		if (!exactMatch) {

			final SearchAttributeGroup group = new SearchAttributeGroup(operation);
			for (T value : searchValue) {

				group.add(new ArraySearchAttribute(this, value, Operation.AND, false));
			}

			return group;
		}

		return new ArraySearchAttribute(this, searchValue, Operation.AND, exactMatch);
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public boolean isArray() {
		return true;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {

		final Map<String, Object> items = new TreeMap<>();
		final Map<String, Object> map   = new TreeMap<>();

		items.put("type", componentType.getSimpleName().toLowerCase());

		map.put("type", "array");
		map.put("items", items);

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}

		return map;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {

		final Map<String, Object> items = new TreeMap<>();
		final Map<String, Object> map   = new TreeMap<>();

		items.put("type", componentType.getSimpleName().toLowerCase());

		map.put("type", "array");
		map.put("items", items);

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}

		return map;
	}

	// ----- private methods -----
	private T[] convert(final List source) throws FrameworkException {

		final ArrayList<T> result = new ArrayList<>();

		for (final Object o : source) {

			if (componentType.isInstance(o)) {

				result.add((T)o);

			} else if (componentType.equals(Integer.class) && o instanceof Double) {

				result.add((T)(Integer)((Double)o).intValue());

			} else if (o != null) {

				final T value = fromString(o.toString());

				if (value == null) {

					throw new PropertyInputParsingException(
						jsonName(),
						new NumberFormatToken(declaringTrait.getLabel(), jsonName(), source)
					);
				}

				result.add(value);

			} else {

				// dont know
				throw new IllegalStateException("Conversion of array type failed.");
			}
		}

		return (T[])result.toArray((Object[])Array.newInstance(componentType, 0));
	}

	private T fromString(final String source) {

		if (valueOfMethod != null) {

			try {
				return (T)valueOfMethod.invoke(null, source);

			} catch (Throwable t) {
				return null;
			}
		}

		return (T)source;
	}

	private Method methodOrNull() {

		try {

			return componentType.getDeclaredMethod("valueOf", String.class);

		} catch (Throwable t) {}

		return null;
	}

	private Object[] trimFilterAndConvert(final SecurityContext securityContext, final String[] input) throws FrameworkException {

		final ArrayList trimmed = new ArrayList<>();

		for (final String part : input) {

			final String trimmedString = part.trim();

			if (StringUtils.isNotBlank(trimmedString)) {

				trimmed.add(convertSearchValue(securityContext, trimmedString));
			}
		}

		return trimmed.toArray();
	}
}
