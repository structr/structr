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

import java.time.DateTimeException;
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

		ZonedDateTime parsedDate = null;
		DateTimeParseException parseException = null;

		try {

			parsedDate = ZonedDateTime.parse(source, ZonedDateTimeProperty.getDateTimeFormatter(pattern));

		} catch (DateTimeParseException ex) {

			parseException = ex;
		}

		// attempt fallback to default pattern
		if (parsedDate == null) {

			try {

				parsedDate = ZonedDateTime.parse(source, DateTimeFormatter.ISO_ZONED_DATE_TIME);
				// If fallback succeeds, it's safe to clear the previous exception.
				parseException = null;

			} catch (DateTimeParseException ex) {

				// ignore exception for fallback parsing so that the user is presented with the exception for the given pattern (property format or override from settings)
			}
		}

		if (parseException != null) {

			throw new FrameworkException(422, ("Could not parse ZonedDateTime from source " + source + ". Cause: " + parseException.getLocalizedMessage()), parseException.getCause());
		}

		return parsedDate;
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

			return ZonedDateTimeProperty.getDateTimeFormatter(format).format(date);
		}

		return null;
	}

	public static void testPattern(final String pattern) throws FrameworkException {

		// test pattern to see if formatting and parsing can work (not necessarily the same value because of possible precision loss, only technically formatting and parsing)
		if (!StringUtils.isBlank(pattern)) {

			final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

			try {

				final String formatted = ZonedDateTime.now().format(formatter);

				ZonedDateTime.parse(formatted, formatter);

			} catch (DateTimeParseException e) {

				throw new FrameworkException(422, "Unable to save ZonedDateTime pattern '%s'. Parsing a ZonedDateTime formatted via that pattern is not possible.".formatted(pattern), e);

			} catch (DateTimeException e) {

				throw new FrameworkException(422, "Unable to save ZonedDateTime pattern '%s'. Formatting a ZonedDateTime not possible with that pattern.".formatted(pattern), e);
			}
		}
	}
}
