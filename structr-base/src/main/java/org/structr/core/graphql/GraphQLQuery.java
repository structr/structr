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
package org.structr.core.graphql;

import graphql.language.Field;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.QueryGroup;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;

import java.util.*;

/**
 */
public class GraphQLQuery {

	private static final Set<String> SchemaRequestFieldNames = new HashSet<>(Arrays.asList("__schema", "__directive", "__directiveLocation", "__type", "__field", "__inputvalue", "__enumvalue", "__typekind", "__typename"));
	private final Map<String, QueryConfig> configurations    = new LinkedHashMap<>();
	private SecurityContext securityContext                  = null;
	private String fieldName                                 = null;

	public GraphQLQuery(final SecurityContext securityContex, final Field field) throws FrameworkException {

		this.securityContext = securityContex;
		this.fieldName       = field.getName();

		if (Traits.exists(fieldName)) {

			final Traits traits = Traits.of(fieldName);

			init(securityContex, traits, field, "/" + fieldName);
		}
	}

	public boolean isSchemaQuery() {
		return SchemaRequestFieldNames.contains(this.fieldName);
	}

	public String getRootPath() {
		return "/" + getFieldName();
	}

	public String getFieldName() {
		return fieldName;
	}

	public GraphQLQueryConfiguration getQueryConfiguration(final String path) {
		return configurations.get(path);
	}

	public Set<PropertyKey> getPropertyKeys(final String path) {

		final QueryConfig config = configurations.get(path);

		return config.getPropertyKeys();
	}

	public Iterable<GraphObject> getEntities(final SecurityContext securityContext) throws FrameworkException {

		final QueryGroup query   = StructrApp.getInstance(securityContext).nodeQuery(fieldName);
		final QueryConfig config = getConfig(getRootPath());

		config.configureQuery(query);

		return query.getResultStream();
	}

	// ----- private methods -----
	private void init(final SecurityContext securityContext, final Traits type, final Field field, final String path) throws FrameworkException {

		final QueryConfig config = getConfig(path);

		config.handleTypeArguments(securityContext, type, field.getArguments());

		final SelectionSet selectionSet = field.getSelectionSet();
		if (selectionSet != null) {

			for (final Selection selection : selectionSet.getSelections()) {

				if (selection instanceof Field) {

					final Field childField      = (Field)selection;
					final SelectionSet childSet = childField.getSelectionSet();
					final PropertyKey key       = type.key(childField.getName());

					// add field to property set
					config.addPropertyKey(key);
					config.handleFieldArguments(securityContext, type, field, childField);

					// recurse
					if (childSet != null) {

						final Traits childType = key.relatedType() != null ? Traits.of(key.relatedType()) : type;

						init(securityContext, childType, childField, path + "/" + childField.getName());
					}
				}
			}
		}
	}

	private QueryConfig getConfig(final String path) {

		QueryConfig config = configurations.get(path);
		if (config == null) {

			config = new QueryConfig(new ActionContext(securityContext));
			configurations.put(path, config);
		}

		return config;
	}
}
