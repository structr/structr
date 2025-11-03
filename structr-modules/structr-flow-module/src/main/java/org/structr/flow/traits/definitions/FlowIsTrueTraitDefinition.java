/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowBaseNode;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowIsTrue;
import org.structr.flow.impl.FlowLogicCondition;
import org.structr.flow.traits.operations.DataSourceOperations;
import org.structr.flow.traits.operations.GetExportData;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FlowIsTrueTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DATA_SOURCES_PROPERTY = "dataSources";
	public static final String CONDITION_PROPERTY    = "condition";
	public static final String DECISION_PROPERTY     = "decision";

	public FlowIsTrueTraitDefinition() {
		super(StructrTraits.FLOW_IS_TRUE);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

				DataSourceOperations.class,
				new DataSourceOperations() {

					@Override
					public Object get(final Context context, final FlowDataSource node) throws FlowException {

						final FlowIsTrue isTrue                 = node.as(FlowIsTrue.class);
						final List<FlowDataSource> _dataSources = Iterables.toList(isTrue.getDataSources());
						if (_dataSources.isEmpty()) {

							return false;
						}

						Boolean result = null;

						for (final FlowDataSource _dataSource : _dataSources) {

							result = isTrue.combine(result, FlowLogicCondition.getBoolean(context, _dataSource));
						}

						return result;
					}
				},

				GetExportData.class,
				new GetExportData() {

					@Override
					public Map<String, Object> getExportData(final FlowBaseNode flowBaseNode) {

						final Map<String, Object> result = new TreeMap<>();

						result.put(GraphObjectTraitDefinition.ID_PROPERTY,   flowBaseNode.getUuid());
						result.put(GraphObjectTraitDefinition.TYPE_PROPERTY, flowBaseNode.getType());

						return result;
					}
				}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowIsTrue.class, (traits, node) -> new FlowIsTrue(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> dataSources = new StartNodes(traitsInstance, DATA_SOURCES_PROPERTY, StructrTraits.FLOW_DATA_INPUTS);
		final Property<NodeInterface> condition             = new EndNode(traitsInstance, CONDITION_PROPERTY, StructrTraits.FLOW_CONDITION_CONDITION);
		final Property<Iterable<NodeInterface>> decision    = new EndNodes(traitsInstance, DECISION_PROPERTY, StructrTraits.FLOW_DECISION_CONDITION);

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
