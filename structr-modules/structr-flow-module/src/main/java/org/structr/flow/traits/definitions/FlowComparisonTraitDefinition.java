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
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowComparison;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.traits.operations.DataSourceOperations;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class FlowComparisonTraitDefinition extends AbstractNodeTraitDefinition {

	public FlowComparisonTraitDefinition() {
		super("FlowComparison");
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource node) throws FlowException {

					final FlowComparison comparison         = node.as(FlowComparison.class);
					final List<FlowDataSource> _dataSources = Iterables.toList(comparison.getDataSources());
					if (_dataSources.isEmpty()) {

						return false;
					}

					final FlowDataSource _dataSource = comparison.getDataSource();
					final String op                  = comparison.getOperation();

					if (_dataSource == null || op == null) {
						return false;
					}

					Object value = _dataSource.get(context);

					Boolean result = true;

					for (final FlowDataSource _ds : _dataSources) {

						Object data = _ds.get(context);

						if (data == null || data instanceof Comparable) {

							if (data != null && data.getClass().isEnum()) {

								data = ((Enum)data).name();
							} else if (data instanceof Number && value instanceof Number) {

								data = ((Number)data).doubleValue();
								value = ((Number)value).doubleValue();
							}

							Comparable c = (Comparable) data;

							switch (op) {
								case "equal":
									result = result && ((c == null && value == null) || (c != null && value != null && c.compareTo(value) == 0));
									break;
								case "notEqual":
									result = result && ((c == null && value != null) || (c != null && value == null) || (c != null && value != null && c.compareTo(value) != 0));
									break;
								case "greater":
									result = result && ((c != null && value == null) || (c != null && value != null && c.compareTo(value) > 0));
									break;
								case "greaterOrEqual":
									result = result && ((c == null && value == null) || (c != null && value != null && c.compareTo(value) >= 0));
									break;
								case "less":
									result = result && ((c == null && value != null) || (c != null && value != null && c.compareTo(value) < 0));
									break;
								case "lessOrEqual":
									result = result && ((c == null && value != null) || (c == null && value == null) || (c != null && value != null && c.compareTo(value) <= 0));
									break;
							}

						}

					}

					return result;
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowComparison.class, (traits, node) -> new FlowComparison(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String> operation                    = new EnumProperty("operation", FlowComparison.Operation.class);
		final Property<Iterable<NodeInterface>> dataSources = new StartNodes("dataSources", "FlowDataInputs");
		final Property<NodeInterface> condition             = new EndNode("condition", "FlowConditionCondition");
		final Property<Iterable<NodeInterface>> decision    = new EndNodes("decision", "FlowDecisionCondition");

		return newSet(
			operation,
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
				"dataSources", "dataSource", "condition", "decision", "operation"
			),
			PropertyView.Ui,
			newSet(
				"dataSources", "dataSource", "condition", "decision", "operation"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
