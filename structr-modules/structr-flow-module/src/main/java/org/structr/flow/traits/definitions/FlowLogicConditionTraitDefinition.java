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

import org.structr.api.util.Iterables;
import org.structr.core.entity.Relation;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.FlowType;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowCondition;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowLogicCondition;
import org.structr.flow.impl.FlowNode;
import org.structr.flow.traits.operations.DataSourceOperations;
import org.structr.flow.traits.operations.GetFlowType;

import java.util.List;
import java.util.Map;

/**
 *
 */
public class FlowLogicConditionTraitDefinition extends AbstractNodeTraitDefinition {

	public FlowLogicConditionTraitDefinition() {
		super("FlowLogicCondition");
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowLogicCondition.class, (traits, node) -> new FlowLogicCondition(traits, node)
		);
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

					final FlowLogicCondition flowNode      = node.as(FlowLogicCondition.class);
					final List<FlowCondition> _dataSources = Iterables.toList(flowNode.getConditions());
					if (_dataSources.isEmpty()) {

						return flowNode.combine(null, false);
					}

					if (_dataSources.size() == 1) {

						return flowNode.combine(null, FlowLogicCondition.getBoolean(context, _dataSources.get(0)));
					}

					Boolean result = null;

					for (final FlowCondition _dataSource : _dataSources) {

						result = flowNode.combine(result, FlowLogicCondition.getBoolean(context, _dataSource));
					}

					return result;
				}
			}
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
