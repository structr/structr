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
import org.structr.api.config.Settings;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.DateFormatToken;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.PropertyInputParsingException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.converter.TemporalDateConverter;
import org.structr.schema.parser.DatePropertyGenerator;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
* A property that stores and retrieves a simple string-based Date with
* the given date format pattern. This property uses a long value internally
* to provide millisecond precision.
 *
 *
 */
public class DateProperty extends AbstractPrimitiveProperty<Date> {

	public DateProperty(final String name) {
		super(name);
		this.format = getDefaultFormat();
	}

	public DateProperty(final String jsonName, final String dbName) {
		super(jsonName, dbName);
		this.format = getDefaultFormat();
	}

	public DateProperty(final String jsonName, final String dbName, final String format) {
		super(jsonName);

		if (StringUtils.isNotBlank(format)) {
			this.format = format;
		} else {
			this.format = getDefaultFormat();
		}
	}

	@Override
	public String typeName() {
		return "Date";
	}

	@Override
	public Class valueType() {
		return Date.class;
	}

	@Override
	public SortType getSortType() {
		return SortType.Long;
	}

	@Override
	public PropertyConverter<Date, Long> databaseConverter(SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<Date, Long> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new DatabaseConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<?, Date> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
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

				return Long.valueOf(value.toString());

			} catch (Throwable t) {
			}

			try {

				return DatePropertyGenerator.parse(value.toString(), format).getTime();

			} catch (Throwable t) {
			}
		}

		return null;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	private class DatabaseConverter extends PropertyConverter<Date, Long> {

		public DatabaseConverter(SecurityContext securityContext, GraphObject entity) {
			super(securityContext, entity);
		}

		@Override
		public Long convert(Date source) throws FrameworkException {

			if (source != null) {

				return source.getTime();
			}

			return null;
		}

		@Override
		public Date revert(Long source) throws FrameworkException {

			if (source != null) {

				return new Date(source);
			}

			return null;

		}
	}

	private class InputConverter extends PropertyConverter<Object, Date> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public Date convert(Object source) throws FrameworkException {

			if (source != null) {

				final Date convertedDate = TemporalDateConverter.convert(source);

				if (convertedDate != null) {

					return convertedDate;
				} else if (source instanceof Long l) {

					return Date.from(Instant.ofEpochMilli(l));
				} else if (source instanceof String) {

					if (StringUtils.isNotBlank((String)source)) {

						Date result = DatePropertyGenerator.parse((String)source, format);

						if (result != null) {
							return result;
						}

						throw new PropertyInputParsingException(
							jsonName(),
							new DateFormatToken(declaringTrait.getLabel(), jsonName()).withDetail(source)
						);

					}

				} else {

					throw new FrameworkException(422, "Unknown input type for date property ‛" + jsonName() + "‛: " + (source.getClass().getName()), new DateFormatToken(declaringTrait.getLabel(), jsonName()).withDetail(source));

				}
			}

			return null;
		}

		@Override
		public String revert(Date source) throws FrameworkException {

			return DatePropertyGenerator.format(source, format);
		}

	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return new SimpleDateFormat(this.format).format(System.currentTimeMillis());
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	// ----- OpenAPI -----
	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();

		map.put("type",   "string");
		map.put("format", "date-time");

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}

		return map;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();

		map.put("type",   "string");
		map.put("format", "date-time");

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}

		return map;
	}

	// ----- static methods -----
	public static String getDefaultFormat() {
		return Settings.DefaultDateFormat.getValue();
	}
}
