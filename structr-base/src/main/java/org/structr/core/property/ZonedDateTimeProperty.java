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
import org.structr.common.error.FrameworkException;
import org.structr.common.error.ZonedDateTimeFormatToken;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.schema.parser.ZonedDateTimePropertyParser;

import java.time.ZonedDateTime;
import java.util.Map;

public class ZonedDateTimeProperty extends AbstractPrimitiveProperty<ZonedDateTime> {

	public ZonedDateTimeProperty(final String name) {
		super(name);
	}

	public ZonedDateTimeProperty(final String jsonName, final String dbName) {
		super(jsonName, dbName);
	}

	public ZonedDateTimeProperty(final String jsonName, final String dbName, final String format) {
		super(jsonName);
	}

	@Override
	public String typeName() {
		return "ZonedDateTime";
	}

	@Override
	public String valueType() {
		return "ZonedDateTime";
	}

	@Override
	public PropertyConverter<ZonedDateTime, ?> databaseConverter(final SecurityContext securityContext) {
		return new DatabaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<ZonedDateTime, ?> databaseConverter(final SecurityContext securityContext, final GraphObject entity) {
		return new DatabaseConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<?, ZonedDateTime> inputConverter(final SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		if (value != null) {

			try {

				if (value instanceof String) {

					return ZonedDateTimePropertyParser.parse(value.toString(), format);
				} else if (value instanceof ZonedDateTime) {

					return value;
				}

			} catch (Throwable t) {
			}

			return value.toString();
		}

		return null;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}


	// Converters
	private class DatabaseConverter extends PropertyConverter<ZonedDateTime, ZonedDateTime> {

		public DatabaseConverter(SecurityContext securityContext, GraphObject entity) {
			super(securityContext, entity);
		}

		@Override
		public ZonedDateTime convert(ZonedDateTime source) throws FrameworkException {

			if (source != null) {

				return source;
			}

			return null;
		}

		@Override
		public ZonedDateTime revert(ZonedDateTime source) throws FrameworkException {

			if (source != null) {

				return source;
			}

			return null;

		}
	}

	private class InputConverter extends PropertyConverter<Object, ZonedDateTime> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public ZonedDateTime convert(Object source) throws FrameworkException {

			if (source != null) {

				if (source instanceof ZonedDateTime) {

					return (ZonedDateTime) source;

				} else if (source instanceof String sourceString) {

					if (StringUtils.isNotBlank(sourceString)) {

						return ZonedDateTimePropertyParser.parse(sourceString);

					}

				} else {

					throw new FrameworkException(422, "Incompatible input type for zoneddatetime property " + jsonName() + ": " + (source.getClass().getName()), new ZonedDateTimeFormatToken(declaringTrait.getName(), ZonedDateTimeProperty.this));

				}
			}

			return null;
		}

		@Override
		public String revert(ZonedDateTime source) throws FrameworkException {

			return source.toString();
		}

	}

	// Open API
	@Override
	public Object getExampleValue(String type, String viewName) {
		return ZonedDateTime.now();
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	public static String getDefaultFormat() {
		return Settings.DefaultZonedDateTimeFormat.getValue();
	}
}
