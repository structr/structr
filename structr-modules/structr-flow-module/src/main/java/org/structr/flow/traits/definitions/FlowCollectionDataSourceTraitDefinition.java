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
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNodes;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowCollectionDataSource;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.traits.operations.DataSourceOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FlowCollectionDataSourceTraitDefinition extends AbstractNodeTraitDefinition {

	public FlowCollectionDataSourceTraitDefinition() {
		super("FlowCollectionDataSource");
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource node) throws FlowException {

					final FlowCollectionDataSource dataSource = node.as(FlowCollectionDataSource.class);
					final List<FlowDataSource> sources        = Iterables.toList(dataSource.getDataSources());
					final List<Object> result                 = new ArrayList<>();

					if (sources != null) {

						for (final FlowDataSource source : sources) {

							result.add(source.get(context));
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
			FlowCollectionDataSource.class, (traits, node) -> new FlowCollectionDataSource(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> dataSources = new StartNodes("dataSources", "FlowDataInputs");

		return newSet(
			dataSources
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet("dataSources"),
			PropertyView.Ui,
			newSet("dataSources")
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

}
