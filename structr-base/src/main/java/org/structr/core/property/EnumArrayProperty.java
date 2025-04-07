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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.ValueToken;
import org.structr.core.GraphObject;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class EnumArrayProperty extends ArrayProperty<String> {

	private static final Logger logger = LoggerFactory.getLogger(EnumArrayProperty.class);
	private final Set<String> enumConstants = new LinkedHashSet<>();

	public EnumArrayProperty(final String name, final Class<? extends Enum> enumType) {
		this(name, EnumProperty.trimAndFilterEmptyStrings(EnumProperty.extractConstants(enumType)), null);
	}

	public EnumArrayProperty(final String name, final Set<String> constants) {
		this(name, constants, null);
	}

	public EnumArrayProperty(final String name, final Set<String> constants, final String[] defaultValue) {
		this(name, name, constants, defaultValue);
	}

	public EnumArrayProperty(final String jsonName, final String dbName, final Set<String> constants, final String[] defaultValue) {

		super(jsonName, String.class);

		this.dbName(dbName);
		this.defaultValue(defaultValue);

		this.enumConstants.addAll(constants);
	}

	@Override
	public Object setProperty(SecurityContext securityContext, GraphObject obj, String[] values) throws FrameworkException {

		for (final String value : values) {

			if (StringUtils.isNotBlank(value)) {

				if (!enumConstants.contains(value)) {

					throw new FrameworkException(422, "Cannot parse input for property ‛" + jsonName() + "‛", new ValueToken(declaringTrait.getLabel(), jsonName(), enumConstants));
				}
			}
		}

		return super.setProperty(securityContext, obj, values);
	}

	@Override
	public Object fixDatabaseProperty(final Object value) {

		if (value != null) {

			if (value instanceof String) {
				return value;
			}
		}

		return null;
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
}
