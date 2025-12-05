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
package org.structr.schema.openapi.example;

import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.Schema;
import org.structr.schema.export.StructrTypeDefinition;

import java.util.List;
import java.util.TreeMap;

public class OpenAPIStructrTypeExample extends TreeMap<String, Object> {

	public OpenAPIStructrTypeExample(final StructrTypeDefinition<?> type, final String viewName) {
		this(type, viewName, 0);
	}

	public OpenAPIStructrTypeExample(final StructrTypeDefinition<?> type, final String viewName, final int level) {

		if (level > 0 && Schema.RestrictedViews.contains(viewName)) {

			handleProperty(type, Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY),   viewName, level);
			handleProperty(type, Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY), viewName, level);
			handleProperty(type, Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), viewName, level);

		} else {

			type.visitProperties(property -> {

				handleProperty(type, property, viewName, level);

			}, viewName);
		}
	}

	// ----- private methods ------
	private void handleProperty(final StructrTypeDefinition<?> type, final PropertyKey property, final String viewName, final int level) {

		if (level > 2) {
			return;
		}

		final Class valueType = property.valueType();

		if (valueType != null && GraphObject.class.isAssignableFrom(valueType)) {

			final JsonSchema schema = type.getSchema();
			final JsonType related  = schema.getType(valueType.getSimpleName(), false);

			if (related != null && related instanceof StructrTypeDefinition) {

				if (property.isCollection()) {

					put(property.jsonName(), List.of(new OpenAPIStructrTypeExample((StructrTypeDefinition)related, viewName, level + 1)));

				} else {

					put(property.jsonName(), new OpenAPIStructrTypeExample((StructrTypeDefinition)related, viewName, level + 1));
				}

			}

		} else {

			final Object exampleValue = property.getExampleValue(type.getName(), viewName);
			if (exampleValue != null) {

				put(property.jsonName(), exampleValue);
			}
		}
	}
}
