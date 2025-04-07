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
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNodes;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.KeyValue;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowKeyValue;
import org.structr.flow.impl.FlowObjectDataSource;
import org.structr.flow.traits.operations.DataSourceOperations;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class FlowObjectDataSourceTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String KEY_VALUE_SOURCES_PROPERTY = "keyValueSources";

	private static final Logger logger = LoggerFactory.getLogger(FlowObjectDataSourceTraitDefinition.class);

	public FlowObjectDataSourceTraitDefinition() {
		super(StructrTraits.FLOW_OBJECT_DATA_SOURCE);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource node) throws FlowException {

					final FlowObjectDataSource dataSource = node.as(FlowObjectDataSource.class);
					final Map<String, Object> result      = new LinkedHashMap<>();

					for (final FlowKeyValue _keySource : dataSource.getKeyValueSources()) {

						final Object item = _keySource.get(context);
						if (item != null && item instanceof KeyValue) {

							final KeyValue keyValue = (KeyValue)item;

							result.put(keyValue.getKey(), keyValue.getValue());

						} else {

							logger.warn("KeyValue source {} of {} returned invalid value {}", _keySource.getUuid(), node.getUuid(), item);
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
			FlowObjectDataSource.class, (traits, node) -> new FlowObjectDataSource(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> keyValueSources = new StartNodes(KEY_VALUE_SOURCES_PROPERTY, StructrTraits.FLOW_KEY_VALUE_OBJECT_INPUT);

		return newSet(
			keyValueSources
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				KEY_VALUE_SOURCES_PROPERTY
			),

			PropertyView.Ui,
			newSet(
				KEY_VALUE_SOURCES_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
