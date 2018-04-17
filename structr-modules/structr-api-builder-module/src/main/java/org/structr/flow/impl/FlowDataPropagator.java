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

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;

public class FlowDataPropagator extends FlowActionNode {

	public static final View defaultView = new View(FlowAction.class, PropertyView.Public, dataSource);
	public static final View uiView      = new View(FlowAction.class, PropertyView.Ui,     dataSource);

	@Override
	public void execute(final Context context) {

		final DataSource _dataSource = getProperty(FlowAction.dataSource);

		if (dataSource != null) {

			// make data available to action if present
			if (_dataSource != null) {
				context.setData(_dataSource.get(context));
			}
		}
	}

}
