/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNodes;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.FlowType;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowExceptionHandler;
import org.structr.flow.impl.FlowNode;
import org.structr.flow.traits.operations.DataSourceOperations;
import org.structr.flow.traits.operations.GetFlowType;

import java.util.Map;
import java.util.Set;

public class FlowExceptionHandlerTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String HANDLED_NODES_PROPERTY = "handledNodes";
	public static final String DATA_TARGET_PROPERTY   = "dataTarget";

	public FlowExceptionHandlerTraitDefinition() {
		super(StructrTraits.FLOW_EXCEPTION_HANDLER);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(
			GetFlowType.class,
			new GetFlowType() {

				@Override
				public FlowType getFlowType(FlowNode flowNode) {
					return FlowType.Exception;
				}
			},

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource node) throws FlowException {
					return context.getData(node.getUuid());
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowExceptionHandler.class, (traits, node) -> new FlowExceptionHandler(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> handledNodes = new StartNodes(HANDLED_NODES_PROPERTY, StructrTraits.FLOW_EXCEPTION_HANDLER_NODES);
		final Property<Iterable<NodeInterface>> dataTarget   = new EndNodes(DATA_TARGET_PROPERTY, StructrTraits.FLOW_DATA_INPUT);

		return newSet(
			handledNodes,
			dataTarget
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					FlowNodeTraitDefinition.NEXT_PROPERTY, HANDLED_NODES_PROPERTY, DATA_TARGET_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					FlowNodeTraitDefinition.NEXT_PROPERTY, HANDLED_NODES_PROPERTY, DATA_TARGET_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
