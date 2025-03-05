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

import org.structr.flow.api.DataSource;
import org.structr.module.api.DeployableEntity;

/**
 *
 */
public interface FlowNotNull extends FlowCondition, DataSource, DeployableEntity {

	/*

	public static final Property<Iterable<DataSource>> dataSources = new StartNodes<>("dataSources", FlowDataInputs.class);
	public static final Property<FlowCondition> condition          = new EndNode<>("condition", FlowConditionCondition.class);
	public static final Property<Iterable<FlowDecision>> decision  = new EndNodes<>("decision", FlowDecisionCondition.class);

	public static final View defaultView = new View(FlowNotNull.class, PropertyView.Public, dataSources, condition, decision);
	public static final View uiView      = new View(FlowNotNull.class, PropertyView.Ui,     dataSources, condition, decision);

	@Override
	public Object get(final Context context) throws FlowException {

		final List<DataSource> _dataSources = Iterables.toList(getProperty(dataSources));
		if (_dataSources.isEmpty()) {

			return false;
		}

		for (final DataSource _dataSource : getProperty(dataSources)) {

			if (_dataSource.get(context) == null) {
				return false;
			}
		}

		return true;
	}

	@Override
	public Map<String, Object> exportData() {
		Map<String, Object> result = new HashMap<>();

		result.put("id", this.getUuid());
		result.put("type", this.getClass().getSimpleName());

		return result;
	}
	*/
}
