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

import org.structr.core.traits.Traits;
import org.structr.schema.export.StructrTypeDefinition;

import java.util.Set;
import java.util.TreeMap;

public class OpenAPIResponseReference extends TreeMap<String, Object> {

	public OpenAPIResponseReference(final String reference) {
		this(reference, null);
	}

	public OpenAPIResponseReference(final Traits type, final String viewName, final Boolean isMultipleResponse) {

		final String base           = "#/components/responses/";
		final Set<String> viewNames = type.getViewNames();
		final String simpleName     = type.getName();

		String reference;

		if (viewName == null || "public".equals(viewName) || !viewNames.contains(viewName)) {

			reference = base + simpleName;

		} else {

			reference = base + simpleName + "." + viewName;
		}

		if (isMultipleResponse) {

			reference += "MultipleResponse";

		} else {

			reference += "SingleResponse";
		}

		put("$ref", reference);
	}

	public OpenAPIResponseReference(final StructrTypeDefinition type, final String viewName, final Boolean isMultipleResponse) {

		final String base = "#/components/responses/";

		final String name = type.getName();
		String reference;
		if (!"all".equals(viewName) && (viewName == null || "public".equals(viewName) || !type.getViewNames().contains(viewName))) {

			reference = base + name;


		} else {

			reference = base + name + "." + viewName;
		}

		if (isMultipleResponse) {

			reference += "MultipleResponse";

		} else {

			reference += "SingleResponse";
		}

		put("$ref", reference);
	}

	public OpenAPIResponseReference(final String reference, final String viewName) {

		if (viewName == null || "public".equals(viewName)) {

			put("$ref", reference);

		} else {

			put("$ref", reference + "." + viewName);
		}
	}
}
