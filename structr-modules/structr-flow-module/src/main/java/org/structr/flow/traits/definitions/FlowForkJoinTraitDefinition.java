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

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.FlowType;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowAction;
import org.structr.flow.impl.FlowForkJoin;
import org.structr.flow.impl.FlowNode;
import org.structr.flow.traits.operations.ActionOperations;
import org.structr.flow.traits.operations.GetFlowType;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FlowForkJoinTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String EXCEPTION_HANDLER_PROPERTY = "exceptionHandler";


	public FlowForkJoinTraitDefinition() {
		super(StructrTraits.FLOW_FORK_JOIN);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

				GetFlowType.class,
				new GetFlowType() {

					@Override
					public FlowType getFlowType(FlowNode flowNode) {
						return FlowType.Action;
					}
				},

				ActionOperations.class,
				new ActionOperations() {

					@Override
					public void execute(final Context context, final FlowAction action) throws FlowException {

						try {

							final Queue<Future> futures = context.getForkFutures();
							while(!futures.isEmpty()) {

								//Poll head and invoke get to force the promise to resolve and thus waiting for thread termination
								Future f = futures.poll();
								if (f != null) {

									f.get();
								}
							}

						} catch (ExecutionException | InterruptedException ex) {

							throw new FlowException(ex, action);
						}
					}
				}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowForkJoin.class, (traits, node) -> new FlowForkJoin(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> exceptionHandler = new EndNode(traitsInstance, EXCEPTION_HANDLER_PROPERTY, StructrTraits.FLOW_EXCEPTION_HANDLER_NODES);

		return newSet(
			exceptionHandler
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				EXCEPTION_HANDLER_PROPERTY, FlowNodeTraitDefinition.IS_START_NODE_OF_CONTAINER_PROPERTY
			),
			PropertyView.Ui,
			newSet(
				EXCEPTION_HANDLER_PROPERTY, FlowNodeTraitDefinition.IS_START_NODE_OF_CONTAINER_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
