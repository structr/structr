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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

public class EnumArrayProperty<T extends Enum> extends AbstractPrimitiveProperty<T[]> {

	private static final Logger logger = LoggerFactory.getLogger(EnumProperty.class.getName());
	private Class<T> enumType = null;

	public EnumArrayProperty(final String name, final Class<T> enumType) {
		this(name, enumType, null);
	}

	public EnumArrayProperty(final String jsonName, final String dbName, final Class<T> enumType) {
		this(jsonName, dbName, enumType, null);
	}

	public EnumArrayProperty(final String name, final Class<T> enumType, final T[] defaultValue) {
		this(name, name, enumType, defaultValue);
	}

	public EnumArrayProperty(final String jsonName, final String dbName, final Class<T> enumType, final T[] defaultValue) {

		super(jsonName, dbName, defaultValue);

		this.enumType = enumType;
		addEnumValuesToFormat();
	}

	@Override
	public String typeName() {
		return "Enum[]";
	}

	@Override
	public Class valueType() {
		return Enum[].class;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public PropertyConverter<T[], String> databaseConverter(SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<T[], String> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new DatabaseConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<String, T[]> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}

	@Override
	public Object fixDatabaseProperty(Object value) {

		if (value != null) {

			if (value instanceof String) {
				return value;
			}
		}

		return null;
	}

	@Override
	public boolean isArray() {
		return true;
	}

	public Class<T> getEnumType() {
		return enumType;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return "a,b,c";
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	// ----- OpenAPI -----
	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {

		final Map<String, Object> items = new TreeMap<>();
		final Map<String, Object> map = new TreeMap<>();

		map.put("type", "array");
		map.put("items", items);

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}

		items.put("type", "string");
		items.put("enum", Arrays.asList(enumType.getEnumConstants()));

		return map;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {

		final Map<String, Object> items = new TreeMap<>();
		final Map<String, Object> map = new TreeMap<>();

		map.put("type", "array");
		map.put("items", items);

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}

		items.put("type", "string");
		items.put("enum", Arrays.asList(enumType.getEnumConstants()));

		return map;
	}

	protected class DatabaseConverter extends PropertyConverter<T[], String> {

		public DatabaseConverter(SecurityContext securityContext, GraphObject entity) {
			super(securityContext, entity);
		}

		@Override
		public T[] revert(String source) throws FrameworkException {

			if (StringUtils.isNotBlank(source)) {

				try {
					BiFunction<ArrayList<T>, T, ArrayList<T>> accumulator = (l, e) -> {
						l.add(e);
						return l;
					};
					BinaryOperator<ArrayList<T>> combiner = (acc, cur) -> {
						acc.addAll(cur);
						return acc;
					};
					return (T[]) Arrays.stream(source.split(",")).map(s -> (T) Enum.valueOf(enumType, s)).reduce(new ArrayList<T>(), accumulator, combiner).toArray(Enum[]::new);
				} catch (Throwable t) {

					logger.warn("Cannot convert database value '{}' on object {} to enum of type '{}', ignoring.", new Object[]{source, this.currentObject.getUuid(), enumType.getSimpleName()});
				}
			}

			return null;

		}

		@Override
		public String convert(T[] source) throws FrameworkException {

			if (source != null) {

				return String.join(",", Arrays.stream(source).map(Enum::toString).toList());
			}

			return null;
		}

	}

	protected class InputConverter extends PropertyConverter<String, T[]> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public String revert(T[] source) throws FrameworkException {

			if (source != null) {

				return String.join(",", Arrays.stream(source).map(Enum::toString).toList());
			}

			return null;
		}

		@Override
		public T[] convert(String source) throws FrameworkException {

			if (StringUtils.isNotBlank(source)) {

				try {
					BiFunction<ArrayList<T>, T, ArrayList<T>> accumulator = (l, e) -> {
						l.add(e);
						return l;
					};
					BinaryOperator<ArrayList<T>> combiner = (acc, cur) -> {
						acc.addAll(cur);
						return acc;
					};
					return (T[]) Arrays.stream(source.split(",")).map(s -> (T) Enum.valueOf(enumType, s)).reduce(new ArrayList<T>(), accumulator, combiner).toArray(Enum[]::new);

				} catch (Throwable t) {

					//throw new FrameworkException(422, "Cannot parse input for property " + jsonName(), new ValueToken(declaringTrait.getName(), EnumArrayProperty.this.dbName, enumType.getEnumConstants()));

					// FIXME
				}
			}

			return null;

		}

	}

	private void addEnumValuesToFormat() {

		this.format = "";

		for (T enumConst : enumType.getEnumConstants()) {
			this.format += (enumConst.toString()) + ",";
		}

		this.format = this.format.substring(0, this.format.length() - 1);
	}
}
