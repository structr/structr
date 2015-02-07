/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.DateFormatToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

//~--- classes ----------------------------------------------------------------

/**
 * A property that stores and retrieves a Date string in ISO8601 format. This property
 * uses a long value internally to provide millisecond precision.
 *
 * Note: Java's SimpleDateFormat doesn't accept 'Z' as indicator for general time zone (UTC),
 * which breaks ISO8601. This class replaces the 'Z' by '+0000' before parsing
 * as a workaround.
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class ISO8601DateProperty extends DateProperty {
	
	public static final String PATTERN = "yyyy-MM-dd'T'HH:mm:ssZ";

	public ISO8601DateProperty(final String name) {
		super(name);
		this.format = PATTERN;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public PropertyConverter<Date, Long> databaseConverter(SecurityContext securityContext, GraphObject entity) {

		return new DatabaseConverter(securityContext, entity);

	}

	@Override
	public PropertyConverter<String, Date> inputConverter(SecurityContext securityContext) {

		return new InputConverter(securityContext);

	}

	//~--- inner classes --------------------------------------------------

	private class DatabaseConverter extends PropertyConverter<Date, Long> {

		public DatabaseConverter(SecurityContext securityContext, GraphObject entity) {

			super(securityContext, entity);

		}

		//~--- methods ------------------------------------------------

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


	private class InputConverter extends PropertyConverter<String, Date> {

		public InputConverter(SecurityContext securityContext) {

			super(securityContext, null);

		}

		//~--- methods ------------------------------------------------

		@Override
		public Date convert(String source) throws FrameworkException {

			if (StringUtils.isNotBlank(source)) {

				try {

					// SimpleDateFormat is not fully ISO8601 compatible, so we replace 'Z' by +0000
					if (StringUtils.contains(source, "Z")) {

						source = StringUtils.replace(source, "Z", "+0000");
					}

					return new SimpleDateFormat(format).parse(source);
					
				} catch (Throwable t) {

					throw new FrameworkException(declaringClass.getSimpleName(), new DateFormatToken(ISO8601DateProperty.this));

				}

			}

			return null;

		}

		@Override
		public String revert(Date source) throws FrameworkException {

			if (source != null) {

				return new SimpleDateFormat(format).format(source);
			}

			return null;

		}
	}
}
