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
package org.structr.schema.openapi.common;

import org.apache.commons.lang3.StringUtils;
import org.structr.core.traits.Traits;
import org.structr.schema.export.StructrTypeDefinition;
import org.structr.schema.export.StructrTypeDefinitions;

import java.util.Set;
import java.util.TreeMap;

public class OpenAPISchemaReference extends TreeMap<String, Object> {

	final String base = "#/components/schemas/";

	public OpenAPISchemaReference(final String reference) {
		this(reference, null);
	}

	public OpenAPISchemaReference(final Traits type, final String viewName) {

		final Set<String> viewNames = type.getViewNames();
		final String simpleName     = type.getName();

		if (viewName == null || "public".equals(viewName) || !viewNames.contains(viewName)) {

			put("$ref", base + simpleName);

		} else {

			put("$ref", base + simpleName + "." + viewName);
		}

		StructrTypeDefinitions.openApiSerializedSchemaTypes.add((simpleName));
	}

	public OpenAPISchemaReference(final StructrTypeDefinition type, final String viewName) {

		final String name = type.getName();
		if (!"all".equals(viewName) && (viewName == null || "public".equals(viewName) || !type.getViewNames().contains(viewName))) {

			put("$ref", base + name);

		} else {

			put("$ref", base + name + "." + viewName);
		}

		StructrTypeDefinitions.openApiSerializedSchemaTypes.add((name));
	}

	public OpenAPISchemaReference(final String input, final String viewName) {

		String reference = input;

		if (!StringUtils.startsWith(reference, "#/")) {
			reference = base + reference;
		}

		if (viewName == null || "public".equals(viewName)) {

			put("$ref", reference);

		} else {

			put("$ref", reference + "." + viewName);
		}

		StructrTypeDefinitions.openApiSerializedSchemaTypes.add((reference));
	}
}
