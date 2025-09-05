/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import java.util.TreeMap;
import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.FlowType;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowBaseNode;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowFilter;
import org.structr.flow.impl.FlowNode;
import org.structr.flow.traits.operations.DataSourceOperations;
import org.structr.flow.traits.operations.GetExportData;
import org.structr.flow.traits.operations.GetFlowType;

import java.util.Map;
import java.util.Set;

public class FlowFilterTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DATA_TARGET_PROPERTY = "dataTarget";
	public static final String CONDITION_PROPERTY   = "condition";

	public FlowFilterTraitDefinition() {
		super(StructrTraits.FLOW_FILTER);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

				GetFlowType.class,
				new GetFlowType() {

					@Override
					public FlowType getFlowType(FlowNode flowNode) {
						return FlowType.Filter;
					}
				},

				DataSourceOperations.class,
				new DataSourceOperations() {

					@Override
					public Object get(final Context context, final FlowDataSource node) throws FlowException {

						final FlowFilter filter = node.as(FlowFilter.class);
						final String uuid       = node.getUuid();

						Object data = context.getData(uuid);
						if (data == null) {

							filter.filter(context);
							data = context.getData(uuid);
						}

						return data;
					}
				},

				GetExportData.class,
				new GetExportData() {

					@Override
					public Map<String, Object> getExportData(final FlowBaseNode flowBaseNode) {

						final Map<String, Object> result = new TreeMap<>();

						result.put(GraphObjectTraitDefinition.ID_PROPERTY,                             flowBaseNode.getUuid());
						result.put(GraphObjectTraitDefinition.TYPE_PROPERTY,                           flowBaseNode.getType());
						result.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        flowBaseNode.isVisibleToPublicUsers());
						result.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, flowBaseNode.isVisibleToAuthenticatedUsers());

						return result;
					}
				}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowFilter.class, (traits, node) -> new FlowFilter(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> dataTarget = new EndNodes(DATA_TARGET_PROPERTY, StructrTraits.FLOW_DATA_INPUT);
		final Property<NodeInterface> condition            = new StartNode(CONDITION_PROPERTY, StructrTraits.FLOW_CONDITION_BASE_NODE);

		return newSet(
			dataTarget,
			condition
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				DATA_TARGET_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY, CONDITION_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				DATA_TARGET_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY, CONDITION_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
