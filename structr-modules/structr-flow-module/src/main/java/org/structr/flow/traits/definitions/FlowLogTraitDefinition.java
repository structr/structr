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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.FlowType;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.*;
import org.structr.flow.traits.operations.ActionOperations;
import org.structr.flow.traits.operations.GetExportData;
import org.structr.flow.traits.operations.GetFlowType;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FlowLogTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String EXCEPTION_HANDLER_PROPERTY = "exceptionHandler";
	public static final String SCRIPT_PROPERTY            = "script";


	private static final Logger logger = LoggerFactory.getLogger(FlowLogTraitDefinition.class);

	public FlowLogTraitDefinition() {
		super(StructrTraits.FLOW_LOG);
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

				ActionOperations.class,
				new ActionOperations() {

					@Override
					public void execute(final Context context, final FlowAction action) throws FlowException {

						final String uuid = action.getUuid();
						String _script    = action.getScript();

						if (_script == null) {

							_script = "data";
						}

						try {

							final FlowDataSource _dataSource = action.getDataSource();
							if (_dataSource != null) {

								// make data available to action if present
								context.setData(uuid, _dataSource.get(context));
							}

							// Evaluate script and write result to context
							final Object result = Scripting.evaluate(context.getActionContext(action.getSecurityContext(), action), action, "${" + _script.trim() + "}", "FlowLog(" + uuid + ")");

							final FlowContainer container = action.getFlowContainer();

							logger.info( (container.getName() != null ? ("[" + container.getEffectiveName() + "]") : "") + ("([" + action.getType() + "]" + uuid + "): ") + result	);

						} catch (FrameworkException fex) {

							throw new FlowException(fex, action);
						}
					}
				},

				GetExportData.class,
				new GetExportData() {

					@Override
					public Map<String, Object> getExportData(final FlowBaseNode flowBaseNode) {

						final Map<String, Object> result = new TreeMap<>();

						result.put(GraphObjectTraitDefinition.ID_PROPERTY,                             flowBaseNode.getUuid());
						result.put(GraphObjectTraitDefinition.TYPE_PROPERTY,                           flowBaseNode.getType());
						result.put(FlowLogTraitDefinition.SCRIPT_PROPERTY,                             flowBaseNode.as(FlowLog.class).getScript());
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
			FlowLog.class, (traits, node) -> new FlowLog(traits, node),
			FlowAction.class, (traits, node) -> new FlowLog(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> exceptionHandler = new EndNode(traitsInstance, EXCEPTION_HANDLER_PROPERTY, StructrTraits.FLOW_EXCEPTION_HANDLER_NODES);
		final Property<String> script                  = new StringProperty(SCRIPT_PROPERTY);

		return newSet(
			exceptionHandler,
			script
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				SCRIPT_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY, EXCEPTION_HANDLER_PROPERTY, FlowNodeTraitDefinition.IS_START_NODE_OF_CONTAINER_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				SCRIPT_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY, EXCEPTION_HANDLER_PROPERTY, FlowNodeTraitDefinition.IS_START_NODE_OF_CONTAINER_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
