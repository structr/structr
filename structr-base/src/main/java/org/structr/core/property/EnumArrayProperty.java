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
import org.structr.common.error.ValueToken;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

public class EnumArrayProperty extends AbstractPrimitiveProperty<String[]> {

	private static final Logger logger = LoggerFactory.getLogger(EnumArrayProperty.class);
	private final Set<String> enumConstants = new LinkedHashSet<>();

	public EnumArrayProperty(final String name, final Class<? extends Enum> enumType) {
		this(name, EnumProperty.trimAndFilterEmptyStrings(EnumProperty.extractConstants(enumType)), null);
	}

	public EnumArrayProperty(final String name, final Set<String> constants) {
		this(name, constants, null);
	}

	public EnumArrayProperty(final String jsonName, final String dbName, final Set<String> constants) {
		this(jsonName, dbName, constants, null);
	}

	public EnumArrayProperty(final String name, final Set<String> constants, final String[] defaultValue) {
		this(name, name, constants, defaultValue);
	}

	public EnumArrayProperty(final String jsonName, final String dbName, final Set<String> constants, final String[] defaultValue) {

		super(jsonName, dbName, defaultValue);

		this.enumConstants.addAll(constants);
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
	public Object setProperty(SecurityContext securityContext, GraphObject obj, String[] values) throws FrameworkException {

		for (final String value : values) {

			if (StringUtils.isNotBlank(value)) {

				if (!enumConstants.contains(value)) {

					throw new FrameworkException(422, "Cannot parse input for property ‛" + jsonName() + "‛", new ValueToken(declaringTrait.getName(), jsonName(), enumConstants));
				}
			}
		}

		return super.setProperty(securityContext, obj, values);
	}

	@Override
	public PropertyConverter<String[], String> databaseConverter(SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<String[], String> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new DatabaseConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<String, String[]> inputConverter(SecurityContext securityContext) {
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

	public Set<String> getEnumConstants() {
		return enumConstants;
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
		items.put("enum", getEnumConstants());

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
		items.put("enum", getEnumConstants());

		return map;
	}

	protected class DatabaseConverter extends PropertyConverter<String[], String> {

		public DatabaseConverter(SecurityContext securityContext, GraphObject entity) {
			super(securityContext, entity);
		}

		@Override
		public String[] revert(String source) throws FrameworkException {

			if (StringUtils.isNotBlank(source)) {

				try {
					BiFunction<ArrayList<String>, String, ArrayList<String>> accumulator = (l, e) -> {
						l.add(e);
						return l;
					};
					BinaryOperator<ArrayList<String>> combiner = (acc, cur) -> {
						acc.addAll(cur);
						return acc;
					};
					return Arrays.stream(source.split(",")).reduce(new ArrayList<>(), accumulator, combiner).toArray(String[]::new);

				} catch (Throwable t) {

					logger.warn("Cannot convert database value '{}' on object {} to enum, ignoring.", source, this.currentObject.getUuid());
				}
			}

			return null;

		}

		@Override
		public String convert(String[] source) throws FrameworkException {

			if (source != null) {

				return String.join(",", Arrays.stream(source).toList());
			}

			return null;
		}

	}

	protected class InputConverter extends PropertyConverter<String, String[]> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public String revert(String[] source) throws FrameworkException {

			if (source != null) {

				return String.join(",", Arrays.stream(source).toList());
			}

			return null;
		}

		@Override
		public String[] convert(String source) throws FrameworkException {

			if (StringUtils.isNotBlank(source)) {

				try {
					BiFunction<ArrayList<String>, String, ArrayList<String>> accumulator = (l, e) -> {
						l.add(e);
						return l;
					};
					BinaryOperator<ArrayList<String>> combiner = (acc, cur) -> {
						acc.addAll(cur);
						return acc;
					};
					return Arrays.stream(source.split(",")).reduce(new ArrayList<>(), accumulator, combiner).toArray(String[]::new);

				} catch (Throwable t) {

					//throw new FrameworkException(422, "Cannot parse input for property " + jsonName(), new ValueToken(declaringTrait.getName(), EnumArrayProperty.this.dbName, enumType.getEnumConstants()));

					// FIXME
				}
			}

			return null;

		}

	}
}
