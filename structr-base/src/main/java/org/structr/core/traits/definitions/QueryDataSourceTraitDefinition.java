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
package org.structr.core.traits.definitions;

import org.json.JSONObject;
import org.structr.api.util.PagingIterable;
import org.structr.api.util.ResultStream;
import org.structr.common.ChannelInput;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.datasources.QueryBuilder;
import org.structr.core.entity.DataSource;
import org.structr.core.entity.QueryDataSource;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.datasource.DataSourceOperations;
import org.structr.core.traits.wrappers.QueryDataSourceTraitWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.web.datasource.FieldDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QueryDataSourceTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DATA_TYPE_PROPERTY = "dataType";
	public static final String QUERY_PROPERTY     = "query";

	public QueryDataSourceTraitDefinition() {
		super(StructrTraits.QUERY_DATA_SOURCE);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			QueryDataSource.class, (traits, node) -> new QueryDataSourceTraitWrapper(traits, node)
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class, new DataSourceOperations<NodeInterface>() {

				@Override
				public ResultStream<NodeInterface> getValues(final ActionContext actionContext, final DataSource provider, final ChannelInput input) throws FrameworkException {

					final QueryDataSource<NodeInterface> source = provider.as(QueryDataSource.class);
					final String dataType                       = source.getDataType();
					final String queryString                    = source.getQuery();

					if (queryString != null) {

						final App        app             = StructrApp.getInstance(actionContext.getSecurityContext());
						final JSONObject jsonObject      = new JSONObject(queryString);
						final QueryBuilder queryBuilder  = new QueryBuilder(dataType);
						final Query<NodeInterface> query = app.nodeQuery(dataType);

						//if (jsonObject.getJSONArray("operations").length() > 0) {

							queryBuilder.resolveQueryObject(actionContext, jsonObject, query.and());
						//}

						return query.getResultStream();
					}

					// no source => empty result
					return new PagingIterable<>("empty result", List.of());
				}

				@Override
				public Map<String, FieldDefinition> getFields(final ActionContext actionContext, final DataSource provider) throws FrameworkException {

					final Map<String, FieldDefinition> output = new LinkedHashMap<>();
					final QueryDataSource source = provider.as(QueryDataSource.class);
					final Traits          traits = Traits.of(source.getDataType());

					// transform input
					for (final PropertyKey key : traits.getPropertyKeysForView(PropertyView.All)) {

						// hide some internal properties
						if (!SchemaNodeTraitDefinition.PROPERTY_KEY_BLACKLIST_FOR_COMPONENTS.contains(key.jsonName())) {
							output.put(key.jsonName(), key.getFieldDefinition());
						}
					}

					return output;
				}

				@Override
				public String getDataType(final ActionContext actionContext, final DataSource provider) throws FrameworkException {
					return provider.as(QueryDataSource.class).getDataType();
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

		final PropertyKey<String> dataTypeProperty = new StringProperty(DATA_TYPE_PROPERTY);
		final PropertyKey<String> queryProperty    = new StringProperty(QUERY_PROPERTY);

		return newSet(
			dataTypeProperty,
			queryProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(
				DATA_TYPE_PROPERTY,
				QUERY_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				DATA_TYPE_PROPERTY,
				QUERY_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
