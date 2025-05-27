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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.FlowResult;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowEngine;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.*;
import org.structr.flow.traits.operations.ActionOperations;
import org.structr.flow.traits.operations.DataSourceOperations;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlowCallTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DATA_TARGET_PROPERTY = "dataTarget";
	public static final String PARAMETERS_PROPERTY  = "parameters";
	public static final String FLOW_PROPERTY        = "flow";


	private static final Logger logger = LoggerFactory.getLogger(FlowCallTraitDefinition.class);

	public FlowCallTraitDefinition() {
		super(StructrTraits.FLOW_CALL);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource node) throws FlowException {

					final FlowCall call = node.as(FlowCall.class);
					final String uuid = node.getUuid();

					if (!context.hasData(uuid)) {
						call.execute(context);
					}

					return context.getData(uuid);
				}
			},

			ActionOperations.class,
			new ActionOperations() {

				@Override
				public void execute(final Context context, final FlowAction action) throws FlowException {

					final FlowCall call                   = action.as(FlowCall.class);
					final List<FlowParameterInput> params = Iterables.toList(call.getParameters());
					final FlowContainer flow              = call.getFlow();
					final String uuid                     = action.getUuid();

					if (flow != null) {

						final Context functionContext = new Context(context.getThisObject());
						final FlowEngine engine       = new FlowEngine(functionContext);
						final FlowNode startNode      = flow.getStartNode();

						if (startNode != null) {

							// Inject all parameters into context
							if (params != null) {

								for (FlowParameterInput p : params) {
									p.process(context, functionContext);
								}
							}

							try {
								final FlowResult result = engine.execute(functionContext, startNode);

								// Save result
								context.setData(uuid, result.getResult());

								if (result.getError() != null) {

									throw new FrameworkException(422, "FlowCall encountered an unexpected exception during execution." + result.getError().getMessage());
								}
							} catch (FrameworkException ex) {

								throw new FlowException(ex, action);
							}

						} else {

							logger.warn("Unable to evaluate FlowCall {}, flow container doesn't specify a start node.", uuid);
						}

						// TODO: handle error

					} else {

						logger.warn("Unable to evaluate FlowCall {}, missing flow container.", uuid);
					}

				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowCall.class, (traits, node) -> new FlowCall(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> dataTarget = new EndNodes(DATA_TARGET_PROPERTY, StructrTraits.FLOW_DATA_INPUT);
		final Property<Iterable<NodeInterface>> parameters = new StartNodes(PARAMETERS_PROPERTY, StructrTraits.FLOW_CALL_PARAMETER);
		final Property<NodeInterface> flow                 = new EndNode(FLOW_PROPERTY, StructrTraits.FLOW_CALL_CONTAINER);

		return newSet(
			dataTarget,
			parameters,
			flow
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				FLOW_PROPERTY, DATA_TARGET_PROPERTY, PARAMETERS_PROPERTY, FlowNodeTraitDefinition.IS_START_NODE_OF_CONTAINER_PROPERTY
			),
			PropertyView.Ui,
			newSet(
				FLOW_PROPERTY, DATA_TARGET_PROPERTY, PARAMETERS_PROPERTY, FlowNodeTraitDefinition.IS_START_NODE_OF_CONTAINER_PROPERTY, FlowBaseNodeTraitDefinition.FLOW_CONTAINER_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
