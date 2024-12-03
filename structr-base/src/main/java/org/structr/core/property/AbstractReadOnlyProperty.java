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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Abstract base class for read-only properties.
 *
 *
 */
public abstract class AbstractReadOnlyProperty<T> extends Property<T> {

	public AbstractReadOnlyProperty(final String name) {
		this(name, name);
	}

	public AbstractReadOnlyProperty(final String name, final T defaultValue) {
		this(name, name, defaultValue);
	}

	public AbstractReadOnlyProperty(final String jsonName, final String dbName) {
		this(jsonName, dbName, null);
	}

	public AbstractReadOnlyProperty(final String jsonName, final String dbName, final T defaultValue) {
		super(jsonName, dbName, defaultValue);
	}

	@Override
	public Property<T> indexed() {

		// related node properties are always passively indexed
		// (because they can change without setProperty())
		super.passivelyIndexed();
		return this;
	}

	@Override
	public String typeName() {
		return ""; // read-only
	}

	@Override
	public Object fixDatabaseProperty(final Object value) {
		return value;
	}

	@Override
	public Object setProperty(SecurityContext securityContext, GraphObject obj, final T value) throws FrameworkException {
		throw new FrameworkException(422, "Property ‛" + jsonName() + "‛ is read-only", new ReadOnlyPropertyToken(obj.getClass().getSimpleName(), jsonName));
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(final SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(final SecurityContext securityContext, final GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, T> inputConverter(final SecurityContext securityContext) {
		return null;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	// ----- OpenAPI -----
	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();
		final String valueType        = valueType();

		final Map<String, String> openApiTypeMap = new HashMap<>();
		openApiTypeMap.put("image", "object");
		openApiTypeMap.put("double", "number");

		if (valueType != null) {
			String simpleName = valueType.toLowerCase();

			if (openApiTypeMap.containsKey(simpleName)) {
				simpleName = openApiTypeMap.get(simpleName);
			}

			map.put("type", simpleName);
			map.put("example", getExampleValue(type, viewName));

			if (this.isReadOnly()) {
				map.put("readOnly", true);
			}
		}

		return map;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {

		final Map<String, Object> map = new TreeMap<>();
		final String valueType        = valueType();

		if (valueType != null) {

			map.put("type", valueType.toLowerCase());
			map.put("example", getExampleValue(type, viewName));

			if (this.isReadOnly()) {
				map.put("readOnly", true);
			}
		}

		return map;
	}
}
