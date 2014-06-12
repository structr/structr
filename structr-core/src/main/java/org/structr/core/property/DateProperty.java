/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.NumericUtils;
import org.neo4j.index.lucene.ValueContext;
import org.structr.common.SecurityContext;
import org.structr.common.error.DateFormatToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.search.DateSearchAttribute;
import org.structr.core.graph.search.SearchAttribute;

/**
* A property that stores and retrieves a simple string-based Date with
* the given date format pattern. This property uses a long value internally
* to provide millisecond precision.
 *
 * @author Christian Morgner
 */
public class DateProperty extends AbstractPrimitiveProperty<Date> {

	public static final String DATE_EMPTY_FIELD_VALUE = NumericUtils.longToPrefixCoded(Long.MIN_VALUE);

	protected String pattern = null;

	public DateProperty(String name, String pattern) {
		super(name);

		this.pattern = pattern;
	}

	public DateProperty(String name, String dbName, String pattern) {
		super(name, dbName);

		this.pattern = pattern;
	}

	public DateProperty(String name, String dbName, Date defaultValue, String pattern) {
		super(name, dbName, defaultValue);

		this.pattern = pattern;
	}

	@Override
	public String typeName() {
		return "Date";
	}

	@Override
	public Integer getSortType() {
		return SortField.LONG;
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
	public PropertyConverter<String, Date> inputConverter(SecurityContext securityContext) {
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

				return Long.parseLong(value.toString());

			} catch (Throwable t) {
			}

			try {

				return new SimpleDateFormat(pattern).parse(value.toString()).getTime();

			} catch (Throwable t) {
			}
		}

		return null;
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

	private class InputConverter extends PropertyConverter<String, Date> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public Date convert(String source) throws FrameworkException {

			if (StringUtils.isNotBlank(source)) {

				try {

					// SimpleDateFormat is not fully ISO8601 compatible, so we replace 'Z' by +0000
					if (StringUtils.contains(source, "Z")) {

						source = StringUtils.replace(source, "Z", "+0000");
					}

					return new SimpleDateFormat(pattern).parse(source);

				} catch (Throwable t) {

					throw new FrameworkException(declaringClass.getSimpleName(), new DateFormatToken(DateProperty.this));

				}

			}

			return null;

		}

		@Override
		public String revert(Date source) throws FrameworkException {

			if (source != null) {
				return new SimpleDateFormat(pattern).format(source);
			}

			return null;
		}

	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, BooleanClause.Occur occur, Date searchValue, boolean exactMatch, Query query) {
		return new DateSearchAttribute(this, searchValue, occur, exactMatch);
	}

	@Override
	public void index(GraphObject entity, Object value) {
		super.index(entity, value != null ? ValueContext.numeric((Number)value) : value);
	}

	@Override
	public String getValueForEmptyFields() {
		return DATE_EMPTY_FIELD_VALUE;
	}

}
