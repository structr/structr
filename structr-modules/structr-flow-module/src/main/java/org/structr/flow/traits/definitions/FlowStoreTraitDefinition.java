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
import org.structr.flow.api.FlowType;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowNode;
import org.structr.flow.impl.FlowStore;
import org.structr.flow.traits.operations.DataSourceOperations;
import org.structr.flow.traits.operations.GetFlowType;

import java.util.Map;
import java.util.Set;

public class FlowStoreTraitDefinition extends AbstractNodeTraitDefinition {

	private static final Logger logger = LoggerFactory.getLogger(FlowStoreTraitDefinition.class);

	public FlowStoreTraitDefinition() {
		super("FlowStore");
	}

	public enum Operation {
		store,
		retrieve
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			GetFlowType.class,
			new GetFlowType() {

				@Override
				public FlowType getFlowType(FlowNode flowNode) {
					return FlowType.Store;
				}
			},

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource dataSource) throws FlowException {

					final FlowStore store = dataSource.as(FlowStore.class);
					final String op       = store.getOperation();

					try {

						if (op != null && op.equals("retrieve")) {

							store.handleStorage(context);
						}

					} catch (FlowException ex) {

						logger.error("Exception in FlowStore get: ", ex);
					}
					return context.getData(store.getUuid());
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowStore.class, (traits, node) -> new FlowStore(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> dataSource           = new StartNode("dataSource", "FlowDataInput");
		final Property<Iterable<NodeInterface>> dataTarget = new EndNodes("dataTarget", "FlowDataInput");
		final Property<String> operation                   = new EnumProperty("operation", FlowStore.Operation.class);
		final Property<String> key                         = new StringProperty("key");


		return newSet(
			dataSource,
			dataTarget,
			operation,
			key
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"key", "operation", "dataSource", "dataTarget", "isStartNodeOfContainer"
			),
			PropertyView.Ui,
			newSet(
				"key", "operation", "dataSource", "dataTarget", "isStartNodeOfContainer"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
