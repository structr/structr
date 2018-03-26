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

import graphql.language.Argument;
import graphql.language.BooleanValue;
import graphql.language.Field;
import graphql.language.IntValue;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.language.StringValue;
import graphql.language.Value;
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
	private Map<Integer, GraphQLQueryConfiguration> configurations = new LinkedHashMap<>();
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

	public Set<PropertyKey> getPropertyKeys(final int depth) {

		final GraphQLQueryConfiguration config = configurations.get(depth);

		return config.getPropertyKeys();
	}

	public Iterable<GraphObject> getEntities(final SecurityContext securityContext) throws FrameworkException {

		final GraphQLQueryConfiguration config = getConfig(0);
		final Class type                       = StructrApp.getConfiguration().getNodeEntityClass(fieldName);
		final Query query                      = StructrApp.getInstance(securityContext).nodeQuery(type);
		final PropertyKey sortKey              = config.getSortKey();

		query.page(config.getPage());
		query.pageSize(config.getPageSize());

		if (config.sortDescending()) {
			query.sortDescending(sortKey);
		} else {
			query.sortAscending(sortKey);
		}

		return query.getAsList();
	}

	// ----- private methods -----
	private void init(final Field field, final int depth) {

		final GraphQLQueryConfiguration config = getConfig(depth);

		// parse arguments
		for (final Argument argument : field.getArguments()) {

			final String name = argument.getName();
			final Value value = argument.getValue();

			switch (name) {

				case "_page":
					config.setPage(getIntegerValue(value, 1));
					break;

				case "_pageSize":
					config.setPageSize(getIntegerValue(value, Integer.MAX_VALUE));
					break;

				case "_sort":
					config.setSortKey(StructrApp.getConfiguration().getPropertyKeyForJSONName(type, getStringValue(value, "name")));
					break;

				case "_desc":
					config.setSortDescending(getBooleanValue(value, false));
					break;

				default:
					//
					break;
			}
		}

		// recurse
		final SelectionSet selectionSet = field.getSelectionSet();
		if (selectionSet != null) {

			for (final Selection selection : selectionSet.getSelections()) {

				if (selection instanceof Field) {

					final Field childField = (Field)selection;
					final SelectionSet childSet = childField.getSelectionSet();

					// add field to property set
					config.addPropertyKey(StructrApp.getConfiguration().getPropertyKeyForJSONName(type, childField.getName()));

					if (childSet != null) {

						init(childField, depth + 1);
					}
				}
			}
		}
	}

	private boolean getBooleanValue(final Value value, final boolean defaultValue) {

		if (value != null && value instanceof BooleanValue) {

			return ((BooleanValue)value).isValue();
		}

		return defaultValue;
	}

	private int getIntegerValue(final Value value, final int defaultValue) {

		if (value != null && value instanceof IntValue) {

			return ((IntValue)value).getValue().intValue();
		}

		return defaultValue;
	}

	private String getStringValue(final Value value, final String defaultValue) {

		if (value != null && value instanceof StringValue) {

			return ((StringValue)value).getValue();
		}

		return defaultValue;
	}

	private GraphQLQueryConfiguration getConfig(final int depth) {

		GraphQLQueryConfiguration config = configurations.get(depth);
		if (config == null) {

			config = new GraphQLQueryConfiguration();
			configurations.put(depth, config);
		}

		return config;
	}
}
