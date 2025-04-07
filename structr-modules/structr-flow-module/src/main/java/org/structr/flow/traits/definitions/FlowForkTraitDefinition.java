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
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.FlowType;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowFork;
import org.structr.flow.impl.FlowNode;
import org.structr.flow.traits.operations.DataSourceOperations;
import org.structr.flow.traits.operations.GetFlowType;

import java.util.Map;
import java.util.Set;

public class FlowForkTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DATA_TARGET_PROPERTY       = "dataTarget";
	public static final String LOOP_BODY_PROPERTY         = "loopBody";
	public static final String EXCEPTION_HANDLER_PROPERTY = "exceptionHandler";

	public FlowForkTraitDefinition() {
		super(StructrTraits.FLOW_FORK);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			GetFlowType.class,
			new GetFlowType() {

				@Override
				public FlowType getFlowType(final FlowNode flowNode) {
					return FlowType.Fork;
				}
			},

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource dataSource) throws FlowException {

					final String uuid = dataSource.getUuid();
					Object data       = context.getData(uuid);

					if (data == null) {

						FlowDataSource _ds = dataSource.getDataSource();
						if (_ds != null) {

							data = _ds.get(context);
							context.setData(uuid, data);
						}

					}

					return data;

				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowFork.class, (traits, node) -> new FlowFork(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> dataTarget = new EndNodes(DATA_TARGET_PROPERTY, StructrTraits.FLOW_DATA_INPUT);
		final Property<NodeInterface> loopBody             = new EndNode(LOOP_BODY_PROPERTY, StructrTraits.FLOW_FORK_BODY);
		final Property<NodeInterface> exceptionHandler     = new EndNode(EXCEPTION_HANDLER_PROPERTY, StructrTraits.FLOW_EXCEPTION_HANDLER_NODES);

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
				EXCEPTION_HANDLER_PROPERTY, FlowNodeTraitDefinition.IS_START_NODE_OF_CONTAINER_PROPERTY, LOOP_BODY_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY, DATA_TARGET_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				EXCEPTION_HANDLER_PROPERTY, FlowNodeTraitDefinition.IS_START_NODE_OF_CONTAINER_PROPERTY, LOOP_BODY_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY, DATA_TARGET_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
