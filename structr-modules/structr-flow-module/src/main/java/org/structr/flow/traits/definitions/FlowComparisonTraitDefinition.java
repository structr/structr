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
import org.structr.flow.impl.FlowComparison;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.traits.operations.DataSourceOperations;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.structr.flow.traits.definitions.FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY;

public class FlowComparisonTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String OPERATION_PROPERTY    = "operation";
	public static final String VALUE_SOURCE_PROPERTY = "valueSource";
	public static final String DECISIONS_PROPERTY     = "decisions";

	public FlowComparisonTraitDefinition() {
		super(StructrTraits.FLOW_COMPARISON);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource node) throws FlowException {

					final FlowComparison comparison         = node.as(FlowComparison.class);
					final String op                         = comparison.getOperation();
					final FlowDataSource dataSource         = comparison.getDataSource();
					final FlowDataSource valueSource        = comparison.getValueSource();

					if (op == null || dataSource == null) {
						return false;
					}

					Boolean result = true;

					Object data = dataSource.get(context);
					Object value = valueSource == null ? null : valueSource.get(context);

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
								result = result && ((c == null && value != null) || (c != null && value != null && c.compareTo(value) != 0));
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
								result = result && ((c == null && value != null) || (c != null && value != null && c.compareTo(value) <= 0));
								break;
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

		final Property<String> operation                    = new EnumProperty(OPERATION_PROPERTY, FlowComparison.Operation.class);
		final Property<NodeInterface> valueSource           = new StartNode(VALUE_SOURCE_PROPERTY, StructrTraits.FLOW_VALUE_INPUT);
		final Property<Iterable<NodeInterface>> decisions   = new EndNodes(DECISIONS_PROPERTY, StructrTraits.FLOW_DECISION_CONDITION);

		return newSet(
			operation,
			valueSource,
			decisions
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				DATA_SOURCE_PROPERTY, VALUE_SOURCE_PROPERTY, DECISIONS_PROPERTY, OPERATION_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				DATA_SOURCE_PROPERTY, VALUE_SOURCE_PROPERTY, DECISIONS_PROPERTY, OPERATION_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
