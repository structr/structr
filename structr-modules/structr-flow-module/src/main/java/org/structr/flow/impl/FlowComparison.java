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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class FlowComparison extends FlowCondition implements DataSource, DeployableEntity {

	public FlowComparison(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public Iterable<DataSource> getDataSources() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key("dataSources"));

		return Iterables.map(n -> n.as(DataSource.class), nodes);
	}

	public String getOperation() {
		return wrappedObject.getProperty(traits.key("operation"));
	}

	@Override
	public Object get(final Context context) throws FlowException {

		final List<DataSource> _dataSources = Iterables.toList(getDataSources());
		if (_dataSources.isEmpty()) {

			return false;
		}

		final DataSource _dataSource = getDataSource();
		final String op = getOperation();

		if (_dataSource == null || op == null) {
			return false;
		}

		Object value = _dataSource.get(context);

		Boolean result = true;

		for (final DataSource _ds : _dataSources) {

			Object data = _ds.get(context);

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
						result = result && ((c == null && value != null) || (c != null && value == null) || (c != null && value != null && c.compareTo(value) != 0));
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
						result = result && ((c == null && value != null) || (c == null && value == null) || (c != null && value != null && c.compareTo(value) <= 0));
						break;
				}

			}

		}

		return result;
	}

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id",        getUuid());
		result.put("type",      getType());
		result.put("operation", getOperation());

		return result;
	}

	public enum Operation {
		equal,
		notEqual,
		greater,
		greaterOrEqual,
		less,
		lessOrEqual
	}
}
