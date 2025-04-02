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
package org.structr.flow.impl;

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.traits.definitions.FlowComparisonTraitDefinition;
import org.structr.flow.traits.definitions.FlowIsTrueTraitDefinition;
import org.structr.flow.traits.operations.LogicConditionOperations;
import org.structr.module.api.DeployableEntity;

import java.util.HashMap;
import java.util.Map;

public class FlowComparison extends FlowCondition implements DeployableEntity {

	public FlowComparison(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public final FlowDataSource getValueSource() {
		final NodeInterface node = wrappedObject.getProperty(traits.key(FlowComparisonTraitDefinition.VALUE_SOURCE_PROPERTY));

		if (node != null) {
			return node.as(FlowDataSource.class);
		}

		return null;
	}

	public final void setValueSource(final FlowDataSource valueSource) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowComparisonTraitDefinition.VALUE_SOURCE_PROPERTY), valueSource);
	}

	public final String getOperation() {
		return wrappedObject.getProperty(traits.key(FlowComparisonTraitDefinition.OPERATION_PROPERTY));
	}

	public final void setOperation(final Operation operation) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowComparisonTraitDefinition.OPERATION_PROPERTY), operation.toString());
	}

	public final Iterable<FlowDecision> getDecisions() {
		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key(FlowComparisonTraitDefinition.DECISIONS_PROPERTY));
		return Iterables.map(n -> n.as(FlowDecision.class), nodes);
	}

	public final void setDecisions(final Iterable<FlowDecision> decisions) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowComparisonTraitDefinition.DECISIONS_PROPERTY), decisions);
	}

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put(GraphObjectTraitDefinition.ID_PROPERTY,           getUuid());
		result.put(GraphObjectTraitDefinition.TYPE_PROPERTY,         getType());
		result.put(FlowComparisonTraitDefinition.OPERATION_PROPERTY, getOperation());

		return result;
	}

	public enum Operation {
		equal,
		notEqual,
		greater,
		greaterOrEqual,
		less,
		lessOrEqual
	}
}
