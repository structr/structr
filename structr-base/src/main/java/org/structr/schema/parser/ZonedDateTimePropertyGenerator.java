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
package org.structr.schema.parser;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.Property;
import org.structr.core.property.ZonedDateTimeProperty;
import org.structr.schema.SchemaHelper;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ZonedDateTimePropertyGenerator extends PropertyGenerator<ZonedDateTime> {

	private String pattern = null;

	public ZonedDateTimePropertyGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition propertyDefinition) {
		super(errorBuffer, className, propertyDefinition);
	}

	@Override
	public SchemaHelper.Type getPropertyType() {
		return SchemaHelper.Type.ZonedDateTime;
	}

	@Override
	public String getValueType() {
		return ZonedDateTime.class.getName();
	}

	@Override
	protected Property newInstance() throws FrameworkException {

		final String name       = source.getPropertyName();
		final String expression = source.getFormat();

		if (expression != null && !expression.isEmpty()) {

			pattern = expression;
		}

		return new ZonedDateTimeProperty(name);
	}

	@Override
	public ZonedDateTime getDefaultValue() {

		final String pattern      = source.getFormat();
		final String defaultValue = source.getDefaultValue();

		try {

			return ZonedDateTimePropertyGenerator.parse(defaultValue, (pattern != null ? pattern : null));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return null;
	}

	/**
	 * Static method to catch parse exception
	 *
	 * @param source
	 * @param pattern optional SimpleDateFormat pattern
	 * @return
	 */
	public static ZonedDateTime parse(final String source, final String pattern) throws FrameworkException {

		if (StringUtils.isBlank(pattern)) {

			ZonedDateTime parsedDate = null;

			DateTimeParseException parseException = null;

			try {

				parsedDate = ZonedDateTime.parse(source, DateTimeFormatter.ofPattern(ZonedDateTimeProperty.getDefaultFormat()));
			} catch (DateTimeParseException ex) {
				parseException = ex;
			}

			if (parsedDate == null) {

				try {
					parsedDate = ZonedDateTime.parse(source, DateTimeFormatter.ISO_DATE_TIME);
					// If fallback succeeds, it's safe to clear the previous exception.
					parseException = null;
				} catch (DateTimeParseException ex) {
					parseException = ex;
				}
			}

			if (parseException != null) {

				throw new FrameworkException(422, ("Could not parse ZonedDateTime from source " + source + ". Cause: " + parseException.getCause()), parseException.getCause());
			}

			return parsedDate;

		} else {

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
			return ZonedDateTime.parse(source, formatter);
		}

	}

	public static ZonedDateTime parse(final String source) throws FrameworkException {
		return parse(source, null);
	}

	/**
	 * Central method to format a date into a string.
	 * <p>
	 * If no format is given, use the (old) default format.
	 *
	 * @param date
	 * @param format optional SimpleDateFormat pattern
	 * @return
	 */
	public static String format(final ZonedDateTime date, String format) {

		if (date != null) {

			if (format == null || StringUtils.isBlank(format)) {

				format = ZonedDateTimeProperty.getDefaultFormat();

			}

			return DateTimeFormatter.ofPattern(format).format(date);
		}

		return null;

	}
}
