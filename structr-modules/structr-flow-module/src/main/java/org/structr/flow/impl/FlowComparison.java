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
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.flow.traits.definitions.FlowComparisonTraitDefinition;
import org.structr.flow.traits.definitions.FlowIsTrueTraitDefinition;
import org.structr.module.api.DeployableEntity;

import java.util.HashMap;
import java.util.Map;

public class FlowComparison extends FlowCondition implements DeployableEntity {

	public FlowComparison(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public final Iterable<FlowDataSource> getDataSources() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key(FlowComparisonTraitDefinition.DATA_SOURCES_PROPERTY));

		return Iterables.map(n -> n.as(FlowDataSource.class), nodes);
	}

	public final String getOperation() {
		return wrappedObject.getProperty(traits.key(FlowComparisonTraitDefinition.OPERATION_PROPERTY));
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
