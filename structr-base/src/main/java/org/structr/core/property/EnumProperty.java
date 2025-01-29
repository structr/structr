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
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.ValueToken;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A property that stores and retrieves a simple enum value of the given type.
 *
 *
 */
public class EnumProperty extends AbstractPrimitiveProperty<String> {

	private final Set<String> enumConstants = new LinkedHashSet<>();

	public EnumProperty(final String name, final Class<? extends Enum> enumType) {
		this(name, EnumProperty.trimAndFilterEmptyStrings(EnumProperty.extractConstants(enumType)), null);
	}

	public EnumProperty(final String name, final Set<String> constants) {
		this(name, constants, null);
	}

	public EnumProperty(final String jsonName, final String dbName, final Set<String> constants) {
		this(jsonName, dbName, constants, null);
	}

	public EnumProperty(final String name, final Set<String> constants, final String defaultValue) {
		this(name, name, constants, defaultValue);
	}

	public EnumProperty(final String jsonName, final String dbName, final Set<String> constants, final String defaultValue) {

		super(jsonName, dbName, defaultValue);

		this.enumConstants.addAll(constants);
	}

	@Override
	public Object setProperty(SecurityContext securityContext, GraphObject obj, String value) throws FrameworkException {

		if (StringUtils.isNotBlank(value)) {

			if (!enumConstants.contains(value)) {

				throw new FrameworkException(422, "Cannot parse input for property ‛" + jsonName() + "‛", new ValueToken(declaringTrait.getLabel(), jsonName(), enumConstants));
			}
		}

		return super.setProperty(securityContext, obj, value);
	}

	public Set<String> getEnumConstants() {
		return enumConstants;
	}

	@Override
	public String typeName() {
		return "Enum";
	}

	@Override
	public Class valueType() {
		return String.class;
	}

	@Override
	public PropertyConverter<String, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<String, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, String> inputConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
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
		return false;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return "abc";
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	// ----- OpenAPI -----
	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {

		final Map<String, Object> items = new TreeMap<>();
		final Map<String, Object> map   = new TreeMap<>();

		map.put("type", "array");
		map.put("items", items);

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}

		items.put("type", "string");
		items.put("enum", enumConstants);

		return map;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {

		final Map<String, Object> items = new TreeMap<>();
		final Map<String, Object> map   = new TreeMap<>();

		map.put("type", "array");
		map.put("items", items);

		if (this.isReadOnly()) {
			map.put("readOnly", true);
		}

		items.put("type", "string");
		items.put("enum", enumConstants);

		return map;
	}

	public static String[] extractConstants(final Class<? extends Enum> enumType) {

		final Enum[] sourceConstants = enumType.getEnumConstants();
		final String[] constants     = new String[sourceConstants.length];
		int index                    = 0;

		for (final Enum constant : enumType.getEnumConstants()) {
			constants[index++] = constant.name();
		}

		return constants;
	}

	/**
	 * Takes an array of string values and transforms it into a Set of strings,
	 * trimming the strings and removing empty values.
	 * @param input
	 * @return
	 */
	public static Set<String> trimAndFilterEmptyStrings(final String[] input) {

		final Set<String> normalized = new LinkedHashSet<>();

		for (final String s : input) {

			final String trimmed = s.trim();

			if (StringUtils.isNotBlank(trimmed)) {

				normalized.add(trimmed);
			}
		}

		return normalized;
	}
}
