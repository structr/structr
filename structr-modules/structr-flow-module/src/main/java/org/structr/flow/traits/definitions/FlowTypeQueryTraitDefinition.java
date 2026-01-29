/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow.traits.definitions;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowBaseNode;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowTypeQuery;
import org.structr.flow.traits.operations.DataSourceOperations;
import org.structr.flow.traits.operations.GetExportData;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FlowTypeQueryTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DATA_TARGET_PROPERTY = "dataTarget";
	public static final String DATA_TYPE_PROPERTY   = "dataType";


	private static final Logger logger = LoggerFactory.getLogger(FlowTypeQueryTraitDefinition.class);

	public FlowTypeQueryTraitDefinition() {
		super(StructrTraits.FLOW_TYPE_QUERY);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

				DataSourceOperations.class,
				new DataSourceOperations() {

					@Override
					public Object get(final Context context, final FlowDataSource node) throws FlowException {

						final SecurityContext securityContext = node.getSecurityContext();
						final App app                         = StructrApp.getInstance(securityContext);
						final FlowTypeQuery dataSource        = node.as(FlowTypeQuery.class);

						try (Tx tx = app.tx()) {

							final String type = dataSource.getDataType();

							JSONObject jsonObject = null;

							final String queryString = dataSource.getQuery();
							if (queryString != null) {
								jsonObject = new JSONObject(queryString);
							}

							final Query query = app.nodeQuery(type);

							if (jsonObject != null && jsonObject.getJSONArray("operations").length() > 0) {
								dataSource.resolveQueryObject(context, jsonObject, query);
							}

							final List list = query.getAsList();

							tx.success();

							return list;

						} catch (FrameworkException ex) {

							logger.error("Exception in FlowTypeQuery: " + ex.getMessage());
						}

						return null;
					}
				},

				GetExportData.class,
				new GetExportData() {

					@Override
					public Map<String, Object> getExportData(final FlowBaseNode flowBaseNode) {

						final FlowTypeQuery flowTypeQuery = flowBaseNode.as(FlowTypeQuery.class);

						final Map<String, Object> result = new TreeMap<>();

						result.put(GraphObjectTraitDefinition.ID_PROPERTY,                             flowTypeQuery.getUuid());
						result.put(GraphObjectTraitDefinition.TYPE_PROPERTY,                           flowTypeQuery.getType());
						result.put(FlowTypeQueryTraitDefinition.DATA_TYPE_PROPERTY,                    flowTypeQuery.getDataType());
						result.put(FlowDataSourceTraitDefinition.QUERY_PROPERTY,                       flowTypeQuery.getQuery());
						result.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        flowTypeQuery.isVisibleToPublicUsers());
						result.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, flowTypeQuery.isVisibleToAuthenticatedUsers());

						return result;
					}
				}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowTypeQuery.class, (traits, node) -> new FlowTypeQuery(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> dataTarget = new EndNodes(traitsInstance, DATA_TARGET_PROPERTY, StructrTraits.FLOW_DATA_INPUT);
		final Property<String> dataType                    = new StringProperty(DATA_TYPE_PROPERTY);

		return newSet(
			dataTarget,
			dataType
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				DATA_TARGET_PROPERTY, DATA_TYPE_PROPERTY, FlowDataSourceTraitDefinition.QUERY_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				DATA_TARGET_PROPERTY, DATA_TYPE_PROPERTY, FlowDataSourceTraitDefinition.QUERY_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
