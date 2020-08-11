/*
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.schema.openapi.schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.schema.openapi.common.OpenAPIReference;

public class OpenAPIPropertySchema extends TreeMap<String, Object> {

	public OpenAPIPropertySchema(final PropertyKey key, final String viewName) {

		final Class type = key.valueType();

		if (type.isArray()) {

			final Map<String, Object> list = new LinkedHashMap<>();

			list.put("type", "array");
			list.put("items", getTypeInfo(type.getComponentType(), viewName));

		} else {

			putAll(getTypeInfo(type, viewName));
		}
	}

	// ----- private methods -----
	private Map<String, Object> getTypeInfo(final Class type, final String viewName) {

		final Map<String, Object> info = new LinkedHashMap<>();

		if (GraphObject.class.isAssignableFrom(type)) {

			return new OpenAPIReference("#/components/schemas/" + type.getSimpleName(), viewName);

		} else {

			info.put("type", type.getSimpleName().toLowerCase());
		}

		return info;
	}
}
