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
public abstract class FlowLogicCondition extends FlowCondition implements DeployableEntity {

	public FlowLogicCondition(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	protected abstract Boolean combine(final Boolean result, final Boolean value);

	@Override
	public Object get(final Context context) throws FlowException {

		final List<FlowCondition> _dataSources = Iterables.toList(getConditions());
		if (_dataSources.isEmpty()) {

			return false;
		}

		if (_dataSources.size() == 1) {

			return combine(null, getBoolean(context, _dataSources.get(0)));
		}

		Boolean result = null;

		for (final FlowCondition _dataSource : _dataSources) {

			result = combine(result, getBoolean(context, _dataSource));
		}

		return result;
	}

	@Override
	public Map<String, Object> exportData() {

		final Map<String, Object> result = new TreeMap<>();

		result.put("id",                          getUuid());
		result.put("type",                        getType());
		result.put("visibleToPublicUsers",        isVisibleToPublicUsers());
		result.put("visibleToAuthenticatedUsers", isVisibleToAuthenticatedUsers());

		return result;
	}

	// ----- protected methods -----
	protected static boolean getBoolean(final Context context, final DataSource source) throws FlowException {

		if (source != null) {

			final Object value = source.get(context);
			if (value instanceof Boolean) {

				return (Boolean)value;
			}

			if (value instanceof String) {

				return Boolean.valueOf((String)value);
			}
		}

		return false;
	}
}
