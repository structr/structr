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
import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowNotEmpty;
import org.structr.flow.traits.operations.DataSourceOperations;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlowNotEmptyTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DATA_SOURCES_PROPERTY = "dataSources";
	public static final String CONDITION_PROPERTY    = "condition";
	public static final String DECISION_PROPERTY     = "decision";

	public FlowNotEmptyTraitDefinition() {
		super(StructrTraits.FLOW_NOT_EMPTY);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource node) throws FlowException {

					final FlowNotEmpty notEmpty             = node.as(FlowNotEmpty.class);
					final List<FlowDataSource> _dataSources = Iterables.toList(notEmpty.getDataSources());

					if (_dataSources.isEmpty()) {

						return false;
					}

					for (final FlowDataSource _dataSource : _dataSources) {

						Object currentData = _dataSource.get(context);
						if (currentData == null) {
							return false;
						} else if (currentData instanceof String && ((String) currentData).length() == 0) {
							return false;
						} else if (currentData instanceof Iterable && Iterables.toList((Iterable) currentData).size() == 0) {
							return false;
						}
					}

					return true;
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowNotEmpty.class, (traits, node) -> new FlowNotEmpty(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> dataSources = new StartNodes(DATA_SOURCES_PROPERTY, StructrTraits.FLOW_DATA_INPUTS);
		final Property<NodeInterface> condition             = new EndNode(CONDITION_PROPERTY, StructrTraits.FLOW_CONDITION_CONDITION);
		final Property<Iterable<NodeInterface>> decision    = new EndNodes(DECISION_PROPERTY, StructrTraits.FLOW_DECISION_CONDITION);

		return newSet(
			dataSources,
			condition,
			decision
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				DATA_SOURCES_PROPERTY, CONDITION_PROPERTY, DECISION_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				DATA_SOURCES_PROPERTY, CONDITION_PROPERTY, DECISION_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
