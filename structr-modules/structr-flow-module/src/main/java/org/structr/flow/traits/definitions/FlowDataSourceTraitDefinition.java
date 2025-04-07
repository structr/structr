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
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.traits.operations.DataSourceOperations;

import java.util.Map;
import java.util.Set;

public class FlowDataSourceTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DATA_TARGET_PROPERTY       = "dataTarget";
	public static final String EXCEPTION_HANDLER_PROPERTY = "exceptionHandler";
	public static final String QUERY_PROPERTY             = "query";


	public FlowDataSourceTraitDefinition() {
		super(StructrTraits.FLOW_DATA_SOURCE);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource dataSource) throws FlowException {

					final String uuid = dataSource.getUuid();

					if (!context.hasData(uuid)) {

						final FlowDataSource _ds = dataSource.getDataSource();
						if (_ds != null) {

							Object data = _ds.get(context);
							context.setData(uuid, data);
						}

						final String _script = dataSource.getQuery();
						if (_script != null) {

							try {

								Object result = Scripting.evaluate(context.getActionContext(dataSource.getSecurityContext(), dataSource), context.getThisObject(), "${" + _script.trim() + "}", "FlowDataSource(" + uuid + ")");

								context.setData(dataSource.getUuid(), result);

								return result;

							} catch (FrameworkException fex) {

								throw new FlowException(fex, dataSource);
							}
						}

					} else {

						return context.getData(uuid);
					}

					return null;
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowDataSource.class, (traits, node) -> new FlowDataSource(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> dataTarget = new EndNodes(DATA_TARGET_PROPERTY, StructrTraits.FLOW_DATA_INPUTS);
		final Property<NodeInterface> exceptionHandler     = new EndNode(EXCEPTION_HANDLER_PROPERTY, StructrTraits.FLOW_EXCEPTION_HANDLER_NODES);
		final Property<String> query                       = new StringProperty(QUERY_PROPERTY);

		return newSet(
			dataTarget,
			exceptionHandler,
			query
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				QUERY_PROPERTY, DATA_TARGET_PROPERTY, EXCEPTION_HANDLER_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY
			),
			PropertyView.Ui,
			newSet(
				QUERY_PROPERTY, DATA_TARGET_PROPERTY, EXCEPTION_HANDLER_PROPERTY, FlowBaseNodeTraitDefinition.DATA_SOURCE_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
