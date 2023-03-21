/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.*;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.rels.FlowConditionCondition;
import org.structr.flow.impl.rels.FlowDataInput;
import org.structr.flow.impl.rels.FlowDataInputs;
import org.structr.flow.impl.rels.FlowDecisionCondition;
import org.structr.module.api.DeployableEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class FlowComparison extends FlowCondition implements DataSource, DeployableEntity {

	public static final Property<Iterable<DataSource>> dataSources 	= new StartNodes<>("dataSources", FlowDataInputs.class);
	public static final Property<DataSource> dataSource		= new StartNode<>("dataSource", FlowDataInput.class);
	public static final Property<FlowCondition> condition 		= new EndNode<>("condition", FlowConditionCondition.class);
	public static final Property<Iterable<FlowDecision>> decision 	= new EndNodes<>("decision", FlowDecisionCondition.class);

	public static final Property<Operation> operation 			= new EnumProperty<>("operation", Operation.class);

	public static final View defaultView 						= new View(FlowNotNull.class, PropertyView.Public, dataSources, dataSource, condition, decision, operation);
	public static final View uiView      						= new View(FlowNotNull.class, PropertyView.Ui,     dataSources, dataSource, condition, decision, operation);

	@Override
	public Object get(final Context context) throws FlowException {

		final List<DataSource> _dataSources = Iterables.toList(getProperty(dataSources));
		if (_dataSources.isEmpty()) {

			return false;
		}


		final DataSource _dataSource = getProperty(dataSource);
		final Operation op = getProperty(operation);

		if (_dataSource == null || op == null) {
			return false;
		}

		Object value = _dataSource.get(context);

		Boolean result = true;

		for (final DataSource _ds : getProperty(dataSources)) {

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
					case equal:
						result = result && ((c == null && value == null) || (c != null && value != null && c.compareTo(value) == 0));
						break;
					case notEqual:
						result = result && ((c == null && value != null) || (c != null && value == null) || (c != null && value != null && c.compareTo(value) != 0));
						break;
					case greater:
						result = result && ((c != null && value == null) || (c != null && value != null && c.compareTo(value) > 0));
						break;
					case greaterOrEqual:
						result = result && ((c == null && value == null) || (c != null && value != null && c.compareTo(value) >= 0));
						break;
					case less:
						result = result && ((c == null && value != null) || (c != null && value != null && c.compareTo(value) < 0));
						break;
					case lessOrEqual:
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

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("operation", this.getProperty(operation));

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
