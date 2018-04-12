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
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.impl.rels.FlowConditionDataInput;

/**
 *
 */
public class FlowNotNull extends FlowCondition implements DataSource {

	public static final Property<List<FlowDataSource>> dataSources = new StartNodes<>("dataSources", FlowConditionDataInput.class);

	public static final View defaultView = new View(FlowNotNull.class, PropertyView.Public, dataSources);
	public static final View uiView      = new View(FlowNotNull.class, PropertyView.Ui,     dataSources);

	@Override
	public Object get(final Context context) {

		final List<FlowDataSource> _dataSources = getProperty(dataSources);
		if (_dataSources.isEmpty()) {

			return false;
		}

		for (final FlowDataSource _dataSource : getProperty(dataSources)) {

			if (_dataSource.get(context) == null) {
				return false;
			}
		}

		return true;
	}
}
