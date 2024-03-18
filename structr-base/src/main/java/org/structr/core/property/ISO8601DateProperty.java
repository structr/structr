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
import org.structr.common.SecurityContext;
import org.structr.common.error.DateFormatToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.converter.TemporalDateConverter;
import org.structr.schema.parser.DatePropertyParser;

import java.time.Instant;
import java.util.Date;

/**
 * A property that stores and retrieves a Date string in ISO8601 format. This property
 * uses a long value internally to provide millisecond precision.
 */
public class ISO8601DateProperty extends DateProperty {

	public ISO8601DateProperty(final String name) {
		super(name);
	}

	@Override
	public PropertyConverter<Date, Long> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new DatabaseConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<Object, Date> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
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

					return DatePropertyParser.parseISO8601DateString(Date.from(Instant.ofEpochMilli(l)).toString());
				} else if (source instanceof String sourceString) {
					if (StringUtils.isNotBlank(sourceString)) {

						Date result = DatePropertyParser.parseISO8601DateString(sourceString);
						if (result != null) {

							return result;

						} else {

							throw new FrameworkException(422, "Cannot parse input for property " + jsonName(), new DateFormatToken(declaringClass.getSimpleName(), ISO8601DateProperty.this));

						}
					}
				}
			}

			return null;
		}

		@Override
		public String revert(Date source) throws FrameworkException {

			return DatePropertyParser.format(source, null);
		}
	}
}
