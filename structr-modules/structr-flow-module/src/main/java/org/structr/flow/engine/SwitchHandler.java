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
package org.structr.flow.engine;

import org.structr.flow.api.DataSource;
import org.structr.flow.api.FlowElement;
import org.structr.flow.api.FlowHandler;
import org.structr.flow.api.Switch;
import org.structr.flow.impl.FlowSwitch;
import org.structr.flow.impl.FlowSwitchCase;

public class SwitchHandler implements FlowHandler<Switch> {

	@Override
	public FlowElement handle(Context context, Switch flowElement) throws FlowException {

		if (flowElement instanceof FlowSwitch) {

			FlowSwitch switchElement = (FlowSwitch)flowElement;

			DataSource _ds = switchElement.getProperty(FlowSwitch.dataSource);
			if (_ds != null) {

				Object data = _ds.get(context);

				Iterable<FlowSwitchCase> cases = switchElement.getProperty(FlowSwitch.cases);

				if (cases != null) {

					for (FlowSwitchCase switchCase : cases) {

						final Object caseValue = switchCase.getProperty(FlowSwitchCase.switchCase);
						if (caseValue != null && data != null && caseValue.equals(data.toString())) {

							return switchCase.getProperty(FlowSwitchCase.next);
						}
					}
				}
			}

		}

		return flowElement.next();
	}
}
