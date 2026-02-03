/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.NumberFormatToken;
import org.structr.common.error.PropertyInputParsingException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.docs.DocumentableType;

import java.util.Map;
import java.util.TreeMap;

/**
 * A property that stores and retrieves a simple Long value.
 *
 *
 */
public class LongProperty extends AbstractPrimitiveProperty<Long> implements NumericalPropertyKey<Long> {

	private static final Logger logger = LoggerFactory.getLogger(DoubleProperty.class.getName());

	public LongProperty(final String name) {
		super(name);
	}

	public LongProperty(final String name, final String dbName) {
		super(name, dbName);
	}

	@Override
	public String typeName() {
		return "Long";
	}

	@Override
	public Class valueType() {
		return Long.class;
	}

	@Override
	public SortType getSortType() {
		return SortType.Long;
	}

	@Override
	public PropertyConverter<Long, Long> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<Long, Long> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, Long> inputConverter(SecurityContext securityContext, boolean fromString) {
		return new InputConverter(securityContext);
	}

	@Override
	public Long convertToNumber(Double source) {

		if (source != null) {
			return source.longValue();
		}

		return null;
	}

	protected class InputConverter extends PropertyConverter<Object, Long> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext);
		}

		@Override
		public Object revert(Long source) throws FrameworkException {
			return source;
		}

		@Override
		public Long convert(Object source) throws FrameworkException {

			if (source == null) return null;

			if (source instanceof Number) {

				return ((Number)source).longValue();

			}

			if (source instanceof String && StringUtils.isNotBlank((String) source)) {

				try {
					return Long.valueOf(source.toString());

				} catch (Throwable t) {

					throw new PropertyInputParsingException(
						jsonName(),
						new NumberFormatToken(declaringTrait.getLabel(), jsonName(), source)
					);
				}
			}

			return null;
		}
	}

	@Override
	public Object fixDatabaseProperty(Object value) {

		if (value != null) {

			if (value instanceof Long) {
				return value;
			}

			if (value instanceof Number) {
				return ((Number)value).longValue();
			}

			try {

				return Long.parseLong(value.toString());

			} catch (Throwable t) {

				// no chance, give up..
			}
		}

		return null;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public Object getIndexValue(final Object value) {
		return fixDatabaseProperty(value);
	}

	// ----- interface Documentable -----
	@Override
	public String getShortDescription() {
		return "A property for long integer values.";
	}

	@Override
	public String getLongDescription() {
		return null;
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

	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();

		map.put("type",    "integer");
		map.put("format",  "int64");
		map.put("example", 12467634433L);

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}

		return map;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();

		map.put("type",    "integer");
		map.put("format",  "int64");
		map.put("example", 12467634433L);

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}

		return map;
	}
}
