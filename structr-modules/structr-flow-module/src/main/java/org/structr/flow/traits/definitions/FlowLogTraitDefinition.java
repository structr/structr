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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowAction;
import org.structr.flow.impl.FlowContainer;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowKeyValue;
import org.structr.flow.traits.operations.ActionOperations;

import java.util.Map;
import java.util.Set;

public class FlowLogTraitDefinition extends AbstractNodeTraitDefinition {

	private static final Logger logger = LoggerFactory.getLogger(FlowLogTraitDefinition.class);

	public FlowLogTraitDefinition() {
		super("FlowLog");
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

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
						final Object result = Scripting.evaluate(context.getActionContext(action.getSecurityContext(), action), action, "${" + _script.trim() + "}", "FlowAction(" + uuid + ")");

						final FlowContainer container = action.getFlowContainer();

						logger.info( (container.getName() != null ? ("[" + container.getEffectiveName() + "]") : "") + ("([" + action.getType() + "]" + uuid + "): ") + result	);

					} catch (FrameworkException fex) {

						throw new FlowException(fex, action);
					}
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowKeyValue.class, (traits, node) -> new FlowKeyValue(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> dataSource       = new StartNode("dataSource", "FlowDataInput");
		final Property<NodeInterface> exceptionHandler = new EndNode("exceptionHandler", "FlowExceptionHandlerNodes");
		final Property<String> script                  = new StringProperty("script");

		return newSet(
			dataSource,
			exceptionHandler,
			script
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"script", "dataSource", "exceptionHandler", "isStartNodeOfContainer"
			),
			PropertyView.Ui,
			newSet(
				"script", "dataSource", "exceptionHandler", "isStartNodeOfContainer"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
