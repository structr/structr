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
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
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
import org.structr.flow.impl.FlowScriptCondition;
import org.structr.flow.traits.operations.DataSourceOperations;
import org.structr.flow.traits.operations.GetExportData;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FlowScriptConditionTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String SCRIPT_SOURCE_PROPERTY     = "scriptSource";
	public static final String DATA_TARGET_PROPERTY       = "dataTarget";
	public static final String EXCEPTION_HANDLER_PROPERTY = "exceptionHandler";
	public static final String SCRIPT_PROPERTY            = "script";

	public FlowScriptConditionTraitDefinition() {
		super(StructrTraits.FLOW_SCRIPT_CONDITION);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

				DataSourceOperations.class,
				new DataSourceOperations() {

					@Override
					public Object get(final Context context, final FlowDataSource node) throws FlowException {

						final FlowScriptCondition condition = node.as(FlowScriptCondition.class);

						try {

							final FlowDataSource _ds = condition.getDataSource();
							final FlowDataSource _sc = condition.getScriptSource();
							final String _script     = condition.getScript();
							final String uuid        = condition.getUuid();

							final String _dynamicScript = _sc != null ? (String)_sc.get(context) : null;

							if (_script != null || _dynamicScript != null) {

								if (_ds != null) {
									context.setData(uuid, _ds.get(context));
								}

								final String finalScript = _dynamicScript != null ? _dynamicScript : _script;

								Object result =  Scripting.evaluate(context.getActionContext(condition.getSecurityContext(), condition), context.getThisObject(), "${" + finalScript.trim() + "}", "FlowScriptCondition(" + uuid + ")");
								context.setData(condition.getUuid(), result);

								return result;
							}

						} catch (FrameworkException fex) {

							throw new FlowException(fex, condition);
						}

						return null;

					}
				},

				GetExportData.class,
				new GetExportData() {

					@Override
					public Map<String, Object> getExportData(final FlowBaseNode flowBaseNode) {

						final Map<String, Object> result = new TreeMap<>();

						result.put(GraphObjectTraitDefinition.ID_PROPERTY,                             flowBaseNode.getUuid());
						result.put(GraphObjectTraitDefinition.TYPE_PROPERTY,                           flowBaseNode.getType());
						result.put(FlowScriptConditionTraitDefinition.SCRIPT_PROPERTY,                 flowBaseNode.as(FlowScriptCondition.class).getScript());
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
			FlowScriptCondition.class, (traits, node) -> new FlowScriptCondition(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> scriptSource         = new StartNode(traitsInstance, SCRIPT_SOURCE_PROPERTY, StructrTraits.FLOW_SCRIPT_CONDITION_SOURCE);
		final Property<Iterable<NodeInterface>> dataTarget = new EndNodes(traitsInstance, DATA_TARGET_PROPERTY, StructrTraits.FLOW_DATA_INPUT);
		final Property<NodeInterface> exceptionHandler     = new EndNode(traitsInstance, EXCEPTION_HANDLER_PROPERTY, StructrTraits.FLOW_EXCEPTION_HANDLER_NODES);
		final Property<String> script                      = new StringProperty(SCRIPT_PROPERTY);

		return newSet(
			scriptSource,
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
				SCRIPT_PROPERTY, SCRIPT_SOURCE_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY, DATA_TARGET_PROPERTY, EXCEPTION_HANDLER_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				SCRIPT_PROPERTY, SCRIPT_SOURCE_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY, DATA_TARGET_PROPERTY, EXCEPTION_HANDLER_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
