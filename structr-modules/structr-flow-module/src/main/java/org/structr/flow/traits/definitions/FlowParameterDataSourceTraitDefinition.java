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

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowParameterDataSource;
import org.structr.flow.traits.operations.DataSourceOperations;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FlowParameterDataSourceTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DATA_TARGET_PROPERTY = "dataTarget";
	public static final String KEY_PROPERTY         = "key";

	public FlowParameterDataSourceTraitDefinition() {
		super(StructrTraits.FLOW_PARAMETER_DATA_SOURCE);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource node) throws FlowException {

					final FlowParameterDataSource dataSource = node.as(FlowParameterDataSource.class);
					final String _key                        = dataSource.getKey();

					if (_key != null) {

						if (_key.contains(".")) {

							final List<String> parts = Arrays.stream(_key.split("\\.")).collect(Collectors.toList());
							if (!parts.isEmpty()) {

								Object entity = context.getParameter(parts.get(0));
								parts.remove(0);

								return dataSource.resolveParts(entity, parts);

							} else {

								return null;
							}
						}
						return context.getParameter(_key);
					}

					return null;
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowParameterDataSource.class, (traits, node) -> new FlowParameterDataSource(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> dataTarget = new EndNodes(DATA_TARGET_PROPERTY, StructrTraits.FLOW_DATA_INPUT);
		final Property<String> key                         = new StringProperty(KEY_PROPERTY);

		return newSet(
			dataTarget,
			key
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				KEY_PROPERTY, DATA_TARGET_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				KEY_PROPERTY, DATA_TARGET_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
