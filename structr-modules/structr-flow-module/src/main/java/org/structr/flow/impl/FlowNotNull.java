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
package org.structr.flow.impl;

import org.structr.api.util.Iterables;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.module.api.DeployableEntity;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public class FlowNotNull extends FlowCondition implements DataSource, DeployableEntity {

	public FlowNotNull(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public Iterable<FlowDataSource> getDataSources() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key("dataSources"));

		return Iterables.map(n -> n.as(FlowDataSource.class), nodes);
	}

	@Override
	public Object get(final Context context) throws FlowException {

		final List<FlowDataSource> _dataSources = Iterables.toList(getDataSources());
		if (_dataSources.isEmpty()) {

			return false;
		}

		for (final FlowDataSource _dataSource : _dataSources) {

			if (_dataSource.get(context) == null) {
				return false;
			}
		}

		return true;
	}

	@Override
	public Map<String, Object> exportData() {

		final Map<String, Object> result = new TreeMap<>();

		result.put("id",   getUuid());
		result.put("type", getType());

		return result;
	}
}
