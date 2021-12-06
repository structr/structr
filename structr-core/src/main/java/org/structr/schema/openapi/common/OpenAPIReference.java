/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import java.util.Set;
import java.util.TreeMap;

import org.structr.core.app.StructrApp;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.export.StructrTypeDefinition;
import org.structr.schema.export.StructrTypeDefinitions;

public class OpenAPIReference extends TreeMap<String, Object> {

	public OpenAPIReference(final String reference) {
		this(reference, null);
	}

	public OpenAPIReference(final Class type, final String viewName) {

		final String base = "#/components/schemas/";
		ConfigurationProvider configuration = StructrApp.getConfiguration();
		Set<String> viewNames = configuration.getPropertyViewsForType(type);

		final String simpleName = type.getSimpleName();
		if (viewName == null || "public".equals(viewName) || !viewNames.contains(viewName)) {

			put("$ref", base + simpleName);

		} else {

			put("$ref", base + simpleName + "." + viewName);
		}

		StructrTypeDefinitions.openApiSerializedSchemaTypes.add((simpleName));
	}

	public OpenAPIReference(final StructrTypeDefinition type, final String viewName) {

		final String base = "#/components/schemas/";

		final String name = type.getName();
		if (!"all".equals(viewName) && (viewName == null || "public".equals(viewName) || !type.getViewNames().contains(viewName))) {

			put("$ref", base + name);

		} else {

			put("$ref", base + name + "." + viewName);
		}

		StructrTypeDefinitions.openApiSerializedSchemaTypes.add((name));
	}

	public OpenAPIReference(final String reference, final String viewName) {

		if (viewName == null || "public".equals(viewName)) {

			put("$ref", reference);

		} else {

			put("$ref", reference + "." + viewName);
		}

		StructrTypeDefinitions.openApiSerializedSchemaTypes.add((reference));
	}
}
