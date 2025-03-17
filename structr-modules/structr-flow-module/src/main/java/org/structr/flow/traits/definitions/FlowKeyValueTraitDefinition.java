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
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.api.KeyValue;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowKeyValue;
import org.structr.flow.traits.operations.DataSourceOperations;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class FlowKeyValueTraitDefinition extends AbstractNodeTraitDefinition {

	private static final Logger logger = LoggerFactory.getLogger(FlowKeyValueTraitDefinition.class);

	public FlowKeyValueTraitDefinition() {
		super("FlowKeyValue");
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource node) throws FlowException {

					final FlowKeyValue keyValue = node.as(FlowKeyValue.class);
					final String _key           = keyValue.getKey();
					final FlowDataSource _ds    = keyValue.getDataSource();
					final String uuid           = keyValue.getUuid();

					if (_key != null && _ds != null) {

						final Object data = _ds.get(context);
						if (_key.length() > 0) {

							return new KeyValue(_key, data);

						} else {

							logger.warn("Unable to evaluate FlowKeyValue {}, key was empty", uuid);
						}

					} else {

						logger.warn("Unable to evaluate FlowKeyValue {}, missing at least one source.", uuid);
					}

					return null;
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowKeyValue.class, (traits, node) -> new FlowKeyValue(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String> key                         = new StringProperty("key");
		final Property<NodeInterface> dataSource           = new StartNode("dataSource", "FlowDataInput");
		final Property<Iterable<NodeInterface>> dataTarget = new EndNodes("dataTarget", "FlowDataInput");

		return newSet(
			key,
			dataSource,
			dataTarget
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"key", "dataSource", "dataTarget"
			),
			PropertyView.Ui,
			newSet(
				"key", "dataSource", "dataTarget"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
