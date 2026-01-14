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
import org.structr.core.property.DateProperty;
import org.structr.core.property.Property;
import org.structr.schema.SchemaHelper.Type;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 *
 *
 */
public class DatePropertyGenerator extends PropertyGenerator<Date> {

	private static final SimpleDateFormat[] SupportedISOFormats = new SimpleDateFormat[] {
		new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"),
		new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"),
		new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"),
		new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
	};

	private String pattern = null;

	public DatePropertyGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getValueType() {
		return Date.class.getName();
	}

	@Override
	public Type getPropertyType() {
		return Type.Date;
	}

	@Override
	protected Property newInstance() throws FrameworkException {
		return new DateProperty(source.getPropertyName());
	}

	@Override
	public Date getDefaultValue() {

		final String pattern = source.getFormat();

		return DatePropertyGenerator.parse(source.getDefaultValue(), pattern != null ? pattern  : null);
	}

	/**
	 * Static method to catch parse exception
	 *
	 * @param input
	 * @param pattern optional SimpleDateFormat pattern
	 * @return
	 */
	public static Date parse(final String input, final String pattern) {

		if (StringUtils.isBlank(pattern)) {

			return parseISO8601DateString(input);

		} else {

			String source = input;

			try {
				// SimpleDateFormat is not fully ISO8601 compatible, so we replace 'Z' by +0000
				if (StringUtils.contains(source, "Z")) {

					source = StringUtils.replace(source, "Z", "+0000");
				}

				return new SimpleDateFormat(pattern).parse(source);

			} catch (ParseException ignore) { }

			// try to parse as ISO8601 date (supports multiple formats)
			return parseISO8601DateString(source);

		}

	}

	/**
	 * Try to parse source string as a ISO8601 date.
	 *
	 * @param input
	 * @return null if unable to parse
	 */
	public static Date parseISO8601DateString(final String input) {

		String source = input;

		// SimpleDateFormat is not fully ISO8601 compatible, so we replace 'Z' by +0000
		if (StringUtils.contains(source, "Z")) {

			source = StringUtils.replace(source, "Z", "+0000");
		}

		Date parsedDate = null;

		for (final SimpleDateFormat format : SupportedISOFormats) {

			try {

				synchronized (format) {
					parsedDate = format.parse(source);
				}

			} catch (ParseException pe) {}

			if (parsedDate != null) {
				return parsedDate;
			}
		}

		return null;

	}

	/**
	 * Central method to format a date into a string.
	 *
	 * If no format is given, use the (old) default format.
	 *
	 * @param date
	 * @param format optional SimpleDateFormat pattern
	 * @return
	 */
	public static String format(final Date date, String format) {

		if (date != null) {

			if (StringUtils.isBlank(format)) {

				format = DateProperty.getDefaultFormat();

			}

			return new SimpleDateFormat(format).format(date);
		}

		return null;

	}

	/**
	 * Central method to format a date into a string.
	 *
	 * If no format is given, use the (old) default format.
	 *
	 * @param date
	 * @param format optional SimpleDateFormat pattern
	 * @return
	 */
	public static String format(final ZonedDateTime date, String format) {

		if (date != null) {

			if (StringUtils.isBlank(format)) {

				format = DateProperty.getDefaultFormat();

			}

			return DateTimeFormatter.ofPattern(format).format(date);
		}

		return null;

	}
}
