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

import org.structr.core.entity.Relation;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowFirst;
import org.structr.flow.traits.operations.DataSourceOperations;

import java.util.Iterator;
import java.util.Map;

public class FlowFirstTraitDefinition extends AbstractNodeTraitDefinition {

	public FlowFirstTraitDefinition() {
		super("FlowFirst");
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			DataSourceOperations.class,
			new DataSourceOperations() {

				@Override
				public Object get(final Context context, final FlowDataSource node) throws FlowException {

					final FlowDataSource _dataSource = node.getDataSource();
					if (_dataSource != null) {

						final String uuid = node.getUuid();

						Object currentData = context.getData(uuid);

						if (currentData != null) {
							return currentData;
						}

						Object dsData = _dataSource.get(context);

						if (dsData instanceof Iterable) {
							Iterable c = (Iterable)dsData;
							Iterator it = c.iterator();

							if (it.hasNext()) {

								Object data = it.next();
								context.setData(uuid, data);
								return data;
							}
						}
					}

					return null;
				}
			}
		);
	}
					@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowFirst.class, (traits, node) -> new FlowFirst(traits, node)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
