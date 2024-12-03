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
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.NumberFormatToken;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

import java.util.Map;
import java.util.TreeMap;
import org.structr.common.error.PropertyInputParsingException;

/**
* A property that stores and retrieves a simple Integer value.
 *
 *
 */
public class IntProperty extends AbstractPrimitiveProperty<Integer> implements NumericalPropertyKey<Integer> {

	public IntProperty(final String name) {
		super(name);
	}

	public IntProperty(final String jsonName, final String dbName) {
		super(jsonName, dbName);
	}

	@Override
	public String typeName() {
		return "Integer";
	}

	@Override
	public String valueType() {
		return "Integer";
	}

	@Override
	public SortType getSortType() {
		return SortType.Integer;
	}

	@Override
	public PropertyConverter<Integer, Integer> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<Integer, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new DatabaseConverter(securityContext);
	}

	@Override
	public PropertyConverter<?, Integer> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}

	@Override
	public Integer convertToNumber(Double source) {

		if (source != null) {
			return source.intValue();
		}

		return null;
	}

	protected class DatabaseConverter extends PropertyConverter<Integer, Object> {

		public DatabaseConverter(final SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public Integer revert(final Object source) throws FrameworkException {

			if (source instanceof Number) {

				return ((Number)source).intValue();
			}

			if (source instanceof String && StringUtils.isNotBlank((String) source)) {

				try {
					return Double.valueOf(source.toString()).intValue();

				} catch (Throwable t) {

					throw new PropertyInputParsingException(
						jsonName(),
						new NumberFormatToken(declaringTrait.getName(), jsonName(), source)
					);
				}
			}

			return null;
		}

		@Override
		public Object convert(final Integer source) throws FrameworkException {
			return source;
		}

	}

	protected class InputConverter extends PropertyConverter<Object, Integer> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public Object revert(Integer source) throws FrameworkException {
			return source;
		}

		@Override
		public Integer convert(Object source) throws FrameworkException {

			if (source == null) return null;

			if (source instanceof Number) {

				return ((Number)source).intValue();
			}

			if (source instanceof String && StringUtils.isNotBlank((String) source)) {

				try {
					return Double.valueOf(source.toString()).intValue();

				} catch (Throwable t) {

					throw new PropertyInputParsingException(
						jsonName(),
						new NumberFormatToken(declaringTrait.getName(), jsonName(), source)
					);
				}
			}

			return null;

		}
	}

	@Override
	public Object fixDatabaseProperty(Object value) {

		if (value != null) {

			if (value instanceof Integer) {
				return value;
			}

			if (value instanceof Number) {
				return ((Number)value).intValue();
			}

			try {

				return Double.valueOf(value.toString()).intValue();

			} catch (Throwable t) {

				// no chance, give up..
			}
		}

		return null;
	}

	@Override
	public Object getIndexValue(final Object value) {
		return fixDatabaseProperty(value);
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return 1;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();

		map.put("type",   "integer");
		map.put("format", "int32");
		map.put("example", 1);

		if (this.readOnly) {
			map.put("readOnly", true);
		}

		return map;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();

		map.put("type",   "integer");
		map.put("format", "int32");
		map.put("example", 1);

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}

		return map;
	}
}
