/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.core.graphql;

import graphql.language.Field;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;

/**
 */
public class GraphQLQuery {

	private static final Set<String> SchemaRequestFieldNames       = new HashSet<>(Arrays.asList("__schema", "__directive", "__directiveLocation", "__type", "__field", "__inputvalue", "__enumvalue", "__typekind", "__typename"));
	private Map<Integer, QueryConfig> configurations = new LinkedHashMap<>();
	private String fieldName                                       = null;
	private Class type                                             = null;

	public GraphQLQuery(final Field field) {

		this.type      = StructrApp.getConfiguration().getNodeEntityClass(field.getName());
		this.fieldName = field.getName();

		if (type != null) {

			init(field, 0);
		}
	}

	public boolean isSchemaQuery() {
		return SchemaRequestFieldNames.contains(this.fieldName);
	}

	public String getFieldName() {
		return fieldName;
	}

	public GraphQLQueryConfiguration getQueryConfiguration(final int depth) {
		return configurations.get(depth);
	}

	public Set<PropertyKey> getPropertyKeys(final int depth) {

		final QueryConfig config = configurations.get(depth);

		return config.getPropertyKeys();
	}

	public Iterable<GraphObject> getEntities(final SecurityContext securityContext) throws FrameworkException {

		final Class type         = StructrApp.getConfiguration().getNodeEntityClass(fieldName);
		final Query query        = StructrApp.getInstance(securityContext).nodeQuery(type);
		final QueryConfig config = getConfig(0);

		config.configureQuery(query);

		return query.getAsList();
	}

	// ----- private methods -----
	private void init(final Field field, final int depth) {

		final QueryConfig config = getConfig(depth);

		config.handleTypeArguments(type, field.getArguments());

		final SelectionSet selectionSet = field.getSelectionSet();
		if (selectionSet != null) {

			for (final Selection selection : selectionSet.getSelections()) {

				if (selection instanceof Field) {

					final Field childField      = (Field)selection;
					final SelectionSet childSet = childField.getSelectionSet();

					// add field to property set
					config.addPropertyKey(StructrApp.getConfiguration().getPropertyKeyForJSONName(type, childField.getName()));
					config.handleFieldArguments(type, field, childField);

					// recurse
					if (childSet != null) {

						init(childField, depth + 1);
					}
				}
			}
		}
	}

	private QueryConfig getConfig(final int depth) {

		QueryConfig config = configurations.get(depth);
		if (config == null) {

			config = new QueryConfig();
			configurations.put(depth, config);
		}

		return config;
	}
}
