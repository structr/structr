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
import org.structr.api.config.Settings;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.ZonedDateTimeFormatToken;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.schema.parser.ZonedDateTimePropertyGenerator;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

public class ZonedDateTimeProperty extends AbstractPrimitiveProperty<ZonedDateTime> {

	public ZonedDateTimeProperty(final String name) {
		super(name);

		this.format = getFormatOverride();
	}

	public ZonedDateTimeProperty(final String jsonName, final String dbName) {
		super(jsonName, dbName);

		this.format = getFormatOverride();
	}

	public ZonedDateTimeProperty(final String jsonName, final String dbName, final String format) {
		super(jsonName);

		if (StringUtils.isNotBlank(format)) {
			this.format = format;
		} else {
			this.format = getFormatOverride();
		}
	}

	@Override
	public String typeName() {
		return "ZonedDateTime";
	}

	@Override
	public Class valueType() {
		return ZonedDateTime.class;
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
	public PropertyConverter<?, ZonedDateTime> inputConverter(final SecurityContext securityContext, boolean fromString) {
		return new InputConverter(securityContext);
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		if (value != null) {

			try {

				if (value instanceof String) {

					return ZonedDateTimePropertyGenerator.parse(value.toString(), format);

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
	public boolean isArray() {
		return false;
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

						return ZonedDateTimePropertyGenerator.parse(sourceString, format);
					}

				} else {

					throw new FrameworkException(422, "Incompatible input type for ZonedDateTime property " + ZonedDateTimeProperty.this.jsonName() + ": " + (source.getClass().getName()), new ZonedDateTimeFormatToken(declaringTrait.getLabel(), ZonedDateTimeProperty.this));
				}
			}

			return null;
		}

		@Override
		public String revert(ZonedDateTime source) throws FrameworkException {

			return source.format(getDateTimeFormatter(format));
		}
	}


	public static String getFormatOverride() {
		return Settings.ZonedDateTimeFormatOverride.getValue();
	}

	public static DateTimeFormatter getDateTimeFormatter(final String customPattern) {

		if (StringUtils.isBlank(customPattern)) {

			final String settingsPatternOverride = Settings.ZonedDateTimeFormatOverride.getValue();

			if (StringUtils.isBlank(settingsPatternOverride)) {

				return DateTimeFormatter.ISO_ZONED_DATE_TIME;

			} else {

				return DateTimeFormatter.ofPattern(settingsPatternOverride);
			}

		} else {

			return DateTimeFormatter.ofPattern(customPattern);
		}
	}


	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final int index) {
		return getDateTimeFormatter(format).format(ZonedDateTime.now());
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();

		map.put("type",   "string");
		map.put("format", "zoned-date-time");

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}

		final String defaultDisplayFormat = "yyyy-MM-dd'T'HH:mm:ss\\[.SSS\\]\\(Z|±HH:mm\\)[Zone/Region]";		// technically not 100% correct - the S from [.SSS] can occur 0-9 times.
		final String formatOverride       = Settings.ZonedDateTimeFormatOverride.getValue();
		final String formatString         = StringUtils.isBlank(format) ? (StringUtils.isBlank(formatOverride) ? defaultDisplayFormat : formatOverride) : format;

		map.put("description", """
			ISO-8601 date-time with UTC/offset and IANA time-zone region,
			as produced by java.time.ZonedDateTime.

			Format: %s
			""".formatted(formatString));
		map.put("example", getExampleValue(0));

		if (defaultValue != null) {
			map.put("default", getDateTimeFormatter(format).format(defaultValue));
		}

		return map;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();

		map.put("type",   "string");
		map.put("format", "zoned-date-time");

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}

		return map;
	}

	// ----- interface Documentable -----
	@Override
	public String getShortDescription() {
		return "A property type for zoned date-time values.";
	}

	@Override
	public String getLongDescription() {
		return null;
	}
}
