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
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.FlowType;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowAggregate;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowNode;
import org.structr.flow.traits.operations.DataSourceOperations;
import org.structr.flow.traits.operations.GetFlowType;

import java.util.Map;
import java.util.Set;

public class FlowAggregateTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DATA_TARGET_PROPERTY       = "dataTarget";
	public static final String START_VALUE_PROPERTY       = "startValue";
	public static final String EXCEPTION_HANDLER_PROPERTY = "exceptionHandler";
	public static final String SCRIPT_PROPERTY            = "script";

	public FlowAggregateTraitDefinition() {
		super(StructrTraits.FLOW_AGGREGATE);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			GetFlowType.class,
			new GetFlowType() {

				@Override
				public FlowType getFlowType(FlowNode flowNode) {
					return FlowType.Aggregation;
				}
			},

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource dataSource) throws FlowException {

					final FlowAggregate aggregate = dataSource.as(FlowAggregate.class);
					final String uuid             = dataSource.getUuid();

					if (context.getData(uuid) == null) {

						FlowDataSource startValue = aggregate.getStartValueSource();
						if (startValue != null) {

							context.setData(uuid, startValue.get(context));
						}
					}

					return context.getData(uuid);
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowAggregate.class, (traits, node) -> new FlowAggregate(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> dataTarget = new EndNodes(traitsInstance, DATA_TARGET_PROPERTY, StructrTraits.FLOW_DATA_INPUT);
		final Property<NodeInterface> startValue           = new StartNode(traitsInstance, START_VALUE_PROPERTY, StructrTraits.FLOW_AGGREGATE_START_VALUE);
		final Property<NodeInterface> exceptionHandler     = new EndNode(traitsInstance, EXCEPTION_HANDLER_PROPERTY, StructrTraits.FLOW_EXCEPTION_HANDLER_NODES);
		final Property<String> script                      = new StringProperty(SCRIPT_PROPERTY);

		return newSet(
			dataTarget,
			startValue,
			exceptionHandler,
			script
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				SCRIPT_PROPERTY, START_VALUE_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY, DATA_TARGET_PROPERTY, EXCEPTION_HANDLER_PROPERTY, FlowNodeTraitDefinition.IS_START_NODE_OF_CONTAINER_PROPERTY
			),
			PropertyView.Ui,
			newSet(
				SCRIPT_PROPERTY, START_VALUE_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY, DATA_TARGET_PROPERTY, EXCEPTION_HANDLER_PROPERTY, FlowNodeTraitDefinition.IS_START_NODE_OF_CONTAINER_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
