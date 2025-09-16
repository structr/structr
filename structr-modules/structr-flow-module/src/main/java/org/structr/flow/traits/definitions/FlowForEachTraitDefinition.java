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

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.FlowType;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowBaseNode;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowForEach;
import org.structr.flow.impl.FlowNode;
import org.structr.flow.traits.operations.DataSourceOperations;
import org.structr.flow.traits.operations.GetExportData;
import org.structr.flow.traits.operations.GetFlowType;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FlowForEachTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DATA_TARGET_PROPERTY       = "dataTarget";
	public static final String LOOP_BODY_PROPERTY         = "loopBody";
	public static final String EXCEPTION_HANDLER_PROPERTY = "exceptionHandler";

	public FlowForEachTraitDefinition() {
		super(StructrTraits.FLOW_FOR_EACH);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

				GetFlowType.class,
				new GetFlowType() {

					@Override
					public FlowType getFlowType(FlowNode flowNode) {
						return FlowType.ForEach;
					}
				},

				DataSourceOperations.class,
				new DataSourceOperations() {

					@Override
					public Object get(final Context context, final FlowDataSource dataSource) throws FlowException {
						return context.getData(dataSource.getUuid());
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
			FlowForEach.class, (traits, node) -> new FlowForEach(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> dataTarget = new EndNodes(traitsInstance, DATA_TARGET_PROPERTY, StructrTraits.FLOW_DATA_INPUT);
		final Property<NodeInterface> loopBody             = new EndNode(traitsInstance, LOOP_BODY_PROPERTY, StructrTraits.FLOW_FOR_EACH_BODY);
		final Property<NodeInterface> exceptionHandler     = new EndNode(traitsInstance, EXCEPTION_HANDLER_PROPERTY, StructrTraits.FLOW_EXCEPTION_HANDLER_NODES);

		return newSet(
			dataTarget,
			loopBody,
			exceptionHandler
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY, LOOP_BODY_PROPERTY, FlowNodeTraitDefinition.IS_START_NODE_OF_CONTAINER_PROPERTY, EXCEPTION_HANDLER_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY, LOOP_BODY_PROPERTY, FlowNodeTraitDefinition.IS_START_NODE_OF_CONTAINER_PROPERTY, EXCEPTION_HANDLER_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
