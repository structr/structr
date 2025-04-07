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
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.FlowType;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowAction;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowNode;
import org.structr.flow.traits.operations.ActionOperations;
import org.structr.flow.traits.operations.DataSourceOperations;
import org.structr.flow.traits.operations.GetFlowType;

import java.util.Map;
import java.util.Set;

public class FlowActionTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DATA_TARGET_PROPERTY       = "dataTarget";
	public static final String EXCEPTION_HANDLER_PROPERTY = "exceptionHandler";
	public static final String SCRIPT_PROPERTY            = "script";

	public FlowActionTraitDefinition() {
		super(StructrTraits.FLOW_ACTION);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			GetFlowType.class,
			new GetFlowType() {

				@Override
				public FlowType getFlowType(final FlowNode flowNode) {
					return FlowType.Action;
				}
			},

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource dataSource) throws FlowException {

					final FlowAction action = dataSource.as(FlowAction.class);
					final String uuid = dataSource.getUuid();

					if (!context.hasData(uuid)) {
						action.execute(context);
					}

					return context.getData(uuid);
				}
			},

			ActionOperations.class,
			new ActionOperations() {

				@Override
				public void execute(final Context context, final FlowAction action) throws FlowException {

					final String _script = action.getScript();
					if (_script != null) {

						final String uuid = action.getUuid();

						try {

							final FlowDataSource _dataSource = action.getDataSource();

							// make data available to action if present
							if (_dataSource != null) {
								context.setData(uuid, _dataSource.get(context));
							}

							// Evaluate script and write result to context
							Object result = Scripting.evaluate(context.getActionContext(action.getSecurityContext(), action), action, "${" + _script.trim() + "}", "FlowAction(" + uuid + ")");
							context.setData(uuid, result);

						} catch (FrameworkException fex) {

							throw new FlowException(fex, action);
						}
					}
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowAction.class, (traits, node) -> new FlowAction(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> dataTarget = new EndNodes(DATA_TARGET_PROPERTY, StructrTraits.FLOW_DATA_INPUT);
		final Property<NodeInterface> exceptionHandler     = new EndNode(EXCEPTION_HANDLER_PROPERTY, StructrTraits.FLOW_EXCEPTION_HANDLER_NODES);
		final Property<String> script                      = new StringProperty(SCRIPT_PROPERTY);

		return newSet(
			dataTarget,
			exceptionHandler,
			script
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				SCRIPT_PROPERTY, DATA_TARGET_PROPERTY, EXCEPTION_HANDLER_PROPERTY, FlowNodeTraitDefinition.IS_START_NODE_OF_CONTAINER_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
