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

import java.util.TreeMap;
import org.structr.schema.export.StructrTypeDefinition;

public class OpenAPIReference extends TreeMap<String, Object> {

	public OpenAPIReference(final String reference) {
		this(reference, null);
	}

	public OpenAPIReference(final StructrTypeDefinition type, final String viewName) {

		final String base = "#/components/schemas/";

		if (viewName == null || "public".equals(viewName) || !type.getViewNames().contains(viewName)) {

			put("$ref", base + type.getName());

		} else {

			put("$ref", base + type.getName() + "." + viewName);
		}
	}

	public OpenAPIReference(final String reference, final String viewName) {

		if (viewName == null || "public".equals(viewName)) {

			put("$ref", reference);

		} else {

			put("$ref", reference + "." + viewName);
		}
	}
}
