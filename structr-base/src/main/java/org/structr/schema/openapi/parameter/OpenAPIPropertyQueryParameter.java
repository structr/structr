/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.schema.openapi.parameter;

import org.structr.core.property.PropertyKey;

import java.util.TreeMap;

public class OpenAPIPropertyQueryParameter extends TreeMap<String, Object> {

	public OpenAPIPropertyQueryParameter(final String type, final PropertyKey property, final String viewName, final int level) {

		put("name",            property.jsonName());
		put("in",              "query");
		put("description",     "Filter by " + property.jsonName());
		put("allowEmptyValue", property.isIndexedWhenEmpty());
		put("schema",          property.describeOpenAPIInputType(type, viewName, level + 1));
	}
}
