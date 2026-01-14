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
package org.structr.flow.impl;

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.flow.traits.definitions.FlowComparisonTraitDefinition;
import org.structr.module.api.DeployableEntity;

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

	public final Iterable<FlowDataSource> getDataSources() {

		final Iterable<NodeInterface> dataSources = wrappedObject.getProperty(traits.key(FlowComparisonTraitDefinition.DATA_SOURCES_PROPERTY));
		return Iterables.map(n -> n.as(FlowDataSource.class), dataSources);
	}

	public final void setDataSources(final Iterable<FlowDataSource> dataSources) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FlowComparisonTraitDefinition.DATA_SOURCES_PROPERTY), dataSources);
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

	public enum Operation {
		equal,
		notEqual,
		greater,
		greaterOrEqual,
		less,
		lessOrEqual
	}
}
