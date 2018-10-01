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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.engine.FlowException;
import org.structr.flow.impl.rels.FlowCurrentDataInput;
import org.structr.flow.impl.rels.FlowNodeDataSource;
import org.structr.flow.impl.rels.FlowNameDataSource;

import java.util.Collection;

/**
 *
 */
public class FlowFirst extends FlowDataSource {

	private static final Logger logger = LoggerFactory.getLogger(FlowFirst.class);

	@Override
	public Object get(final Context context, FlowBaseNode requestingEntity) throws FlowException {

		final DataSource _dataSource = getProperty(dataSource);
		final DataSource _currentDataSource = getProperty(currentDataSource);

		if (_dataSource != null || _currentDataSource != null) {

			Object currentData = context.getData(getUuid());

			if (currentData != null) {
				return currentData;
			}

			Object dsData = _dataSource.get(context, this);

			if (dsData == null) {
				dsData = _currentDataSource.get(context, this);
			}

			if (dsData != null && dsData instanceof Collection) {
				Collection c = (Collection)dsData;

				if (!c.isEmpty()) {
					Object data = c.iterator().next();
					context.setData(getUuid(), data);
					return data;
				}

			}

		}

		return null;
	}
}
