/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.schema.parser.DatePropertyGenerator;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A property that stores and retrieves an array of Date.
 *
 *
 */
public class DateArrayProperty extends ArrayProperty<Date> {

	private static final Logger logger = LoggerFactory.getLogger(DateArrayProperty.class.getName());

	public DateArrayProperty(final String name) {
		this(name, name);
	}

	public DateArrayProperty(final String jsonName, final String dbName) {

		super(jsonName, Date.class);

		dbName(dbName);

		this.format = getDefaultFormat();
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return value;
	}

	@Override
	public PropertyConverter<Date[], Long[]> databaseConverter(final SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<Date[], Long[]> databaseConverter(final SecurityContext securityContext, final GraphObject entity) {
		return new ArrayDatabaseConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<?, Date[]> inputConverter(final SecurityContext securityContext, final boolean fromString) {
		return new ArrayInputConverter(securityContext, fromString);
	}

	private class ArrayDatabaseConverter extends PropertyConverter<Date[], Long[]> {

		public ArrayDatabaseConverter(SecurityContext securityContext, GraphObject entity) {
			super(securityContext, entity);
		}

		@Override
		public Long[] convert(Date[] source) throws FrameworkException {

			if (source != null) {

				return convertDateArrayToLongArray(source);
			}

			return null;
		}

		@Override
		public Date[] revert(Long[] source) throws FrameworkException {

			if (source != null) {

				return convertLongArrayToDateArray(source);
			}

			return null;
		}
	}

	private class ArrayInputConverter extends PropertyConverter<Object, Date[]> {

		private final boolean fromString;

		public ArrayInputConverter(final SecurityContext securityContext, final boolean fromString) {
			super(securityContext, null);
			this.fromString = fromString;
		}

		@Override
		public Object revert(final Date[] source) throws FrameworkException {

			final ArrayList<String> result = new ArrayList<>();

			if (source != null) {

				for (final Date o : source) {

					if (o != null) {

						result.add(DatePropertyGenerator.format(o, format));
					}
				}
			}

			return result;
		}

		@Override
		public Date[] convert(final Object source) throws FrameworkException {

			if (source == null) {
				return null;
			}

			if (source instanceof List) {
				return DateArrayProperty.this.convert((List)source);
			}

			if (source.getClass().isArray()) {
				return convert(Arrays.asList((Date[])source));
			}

			if (!fromString) {

				throw new ClassCastException();

			} else {

				if (source instanceof String) {

					final String s = (String) source;
					if (s.contains(",")) {

						return DateArrayProperty.this.convert(Arrays.asList(s.split(",")));
					}

					// special handling of empty search attribute
					if (StringUtils.isBlank(s)) {
						return null;
					}

				}

				return new Date[]{DatePropertyGenerator.parse(source.toString(), format)};
			}
		}
	}

	// ----- private methods -----
	private Date[] convertLongArrayToDateArray(final Long[] source) {

		final ArrayList<Date> result = new ArrayList<>();

		for (final Long o : source) {

			if (o == null) {
				continue;
			}

			result.add(new Date(o));
		}

		if (result.isEmpty()) {
			return null;
		}

		return result.toArray(new Date[result.size()]);
	}

	private Long[] convertDateArrayToLongArray(final Date[] source) {

		final ArrayList<Long> result = new ArrayList<>();

		for (final Date o : source) {

			// skip unparseable dates
			if (o == null) {
				continue;
			}

			result.add(o.getTime());
		}

		if (result.isEmpty()) {
			return null;
		}

		return result.toArray(new Long[result.size()]);
	}

	private Date[] convert(final List source) {

		final ArrayList<Date> result = new ArrayList<>();

		for (final Object o : source) {

			// skip unparseable dates
			if (o == null) {
				continue;
			}

			if (o instanceof Date) {

				result.add((Date)o);

			} else if (o != null) {

				result.add(DatePropertyGenerator.parse(o.toString(), format));
			}
		}

		if (result.isEmpty())  {
			return null;
		}

		return result.toArray(new Date[0]);
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return List.of(new SimpleDateFormat(this.format).format(System.currentTimeMillis()));
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	// ----- static methods -----
	public static String getDefaultFormat() {
		return Settings.DefaultDateFormat.getValue();
	}

}
