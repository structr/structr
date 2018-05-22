/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import static org.structr.flow.impl.FlowAction.script;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.impl.rels.FlowConditionCondition;

/**
 *
 */
public abstract class FlowLogicCondition extends FlowCondition implements DataSource {

	public static final Property<List<FlowCondition>> dataSources = new StartNodes<>("conditions", FlowConditionCondition.class);

	public static final View defaultView = new View(FlowAnd.class, PropertyView.Public, script, dataSources);
	public static final View uiView      = new View(FlowAnd.class, PropertyView.Ui,     script, dataSources);

	protected abstract boolean combine(final boolean result, final boolean value);

	@Override
	public Object get(final Context context) {

		final List<FlowCondition> _dataSources = getProperty(dataSources);
		if (_dataSources.isEmpty()) {

			return false;
		}

		boolean result = true;

		for (final FlowCondition _dataSource : getProperty(dataSources)) {

			result = combine(result, getBoolean(context, _dataSource));
		}

		return result;
	}

	// ----- protected methods -----
	protected boolean getBoolean(final Context context, final DataSource source) {

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
