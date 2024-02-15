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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.search.Occurrence;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.search.ArraySearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;
import org.structr.schema.parser.DatePropertyParser;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A property that stores and retrieves an array of Date.
 *
 *
 */
public class DateArrayProperty extends AbstractPrimitiveProperty<List<Date>> {

	private static final Logger logger = LoggerFactory.getLogger(DateArrayProperty.class.getName());

	public DateArrayProperty(final String name) {
		super(name);
		this.format = getDefaultFormat();
	}

	public DateArrayProperty(final String jsonName, final String dbName) {
		super(jsonName, dbName);
		this.format = getDefaultFormat();
	}

	public DateArrayProperty(final String jsonName, final String dbName, final String format) {
		super(jsonName);

		if (StringUtils.isNotBlank(format)) {
			this.format = format;
		} else {
			this.format = getDefaultFormat();
		}
	}

	@Override
	public Object fixDatabaseProperty(Object value) {

		return value;
	}

	@Override
	public String typeName() {
		return "List<Date>";
	}

	@Override
	public Class valueType() {
		return Date.class;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public PropertyConverter<List<Date>, List<Long>> databaseConverter(SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<List<Date>, List<Long>> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new ArrayDatabaseConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<?, List<Date>> inputConverter(SecurityContext securityContext) {
		return new ArrayInputConverter(securityContext);
	}

	private class ArrayDatabaseConverter extends PropertyConverter<List<Date>, List<Long>> {

		public ArrayDatabaseConverter(SecurityContext securityContext, GraphObject entity) {
			super(securityContext, entity);
		}

		@Override
		public List<Long> convert(List<Date> source) throws FrameworkException {

			if (source != null) {

				return convertDateArrayToLongArray(source);
			}

			return null;
		}

		@Override
		public List<Date> revert(List<Long> source) throws FrameworkException {

			if (source != null) {

				return convertLongArrayToDateArray(source);
			}

			return null;

		}
	}

	private class ArrayInputConverter extends PropertyConverter<Object, List<Date>> {

		public ArrayInputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public Object revert(List<Date> source) throws FrameworkException {

			final ArrayList<String> result = new ArrayList<>();
			for (final Date o : source) {
				result.add(DatePropertyParser.format(o, format));
			}

			return result;
		}

		@Override
		public List<Date> convert(Object source) throws FrameworkException {

			if (source == null) {
				return null;
			}

			if (source instanceof List) {
				return DateArrayProperty.this.convert((List)source);
			}

			if (source.getClass().isArray()) {
				return convert(Arrays.asList((Date[])source));
			}

			if (source instanceof String) {

				final String s = (String)source;
				if (s.contains(",")) {

					return DateArrayProperty.this.convert(Arrays.asList(s.split(",")));
				}

				// special handling of empty search attribute
				if (StringUtils.isBlank(s)) {
					return null;
				}

			}

			return List.of(DatePropertyParser.parse(source.toString(), format));
		}

	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Occurrence occur, List<Date> searchValue, boolean exactMatch, Query query) {

		// early exit, return empty search attribute
		if (searchValue == null) {
			return new ArraySearchAttribute(this, "", exactMatch ? occur : Occurrence.OPTIONAL, exactMatch);
		}

		final SearchAttributeGroup group = new SearchAttributeGroup(occur);

		for (Date value : searchValue) {

			group.add(new ArraySearchAttribute(this, value, exactMatch ? occur : Occurrence.OPTIONAL, exactMatch));
		}

		return group;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	// ----- private methods -----
	private List<Date> convertLongArrayToDateArray(final List<Long> source) {

		final ArrayList<Date> result = new ArrayList<>();

		for (final Long o : source) {

			result.add(new Date(o));
		}

		return result;
	}

	private List<Long> convertDateArrayToLongArray(final List<Date> source) {

		final ArrayList<Long> result = new ArrayList<>();

		for (final Date o : source) {

			result.add(o.getTime());
		}

		return result;
	}

	private List<Date> convert(final List source) {

		final ArrayList<Date> result = new ArrayList<>();

		for (final Object o : source) {

			if (o instanceof Date) {

				result.add((Date)o);

			} else if (o != null) {

				result.add(DatePropertyParser.parse(o.toString(), format));

			} else {

				// dont know
				throw new IllegalStateException("Conversion of array type failed.");
			}
		}

		return result;
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
