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
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.Property;
import org.structr.core.property.StartNodes;
import org.structr.core.traits.Traits;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.rels.FlowConditionCondition;
import org.structr.module.api.DeployableEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.structr.flow.impl.FlowAction.script;

/**
 *
 */
public abstract class FlowLogicCondition extends FlowCondition implements DeployableEntity {

	public static final Property<Iterable<FlowCondition>> dataSources = new StartNodes<>("conditions", FlowConditionCondition.class);

	public static final View defaultView = new View(FlowAnd.class, PropertyView.Public, script, dataSources);
	public static final View uiView      = new View(FlowAnd.class, PropertyView.Ui,     script, dataSources);

	public FlowLogicCondition(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	protected abstract Boolean combine(final Boolean result, final Boolean value);

	@Override
	public Object get(final Context context) throws FlowException {

		final List<FlowCondition> _dataSources = Iterables.toList(getProperty(dataSources));
		if (_dataSources.isEmpty()) {

			return false;
		}


		if (StreamSupport.stream(getProperty(dataSources).spliterator(), false).count() == 1) {

			return combine(null, getBoolean(context, getProperty(dataSources).iterator().next()));
		}

		Boolean result = null;

		for (final FlowCondition _dataSource : getProperty(dataSources)) {

			result = combine(result, getBoolean(context, _dataSource));
		}

		return result;
	}

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());
		result.put("visibleToPublicUsers", this.getProperty(visibleToPublicUsers));
		result.put("visibleToAuthenticatedUsers", this.getProperty(visibleToAuthenticatedUsers));

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
