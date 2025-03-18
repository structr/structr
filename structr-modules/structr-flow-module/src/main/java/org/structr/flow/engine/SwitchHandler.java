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

import org.structr.flow.api.FlowHandler;
import org.structr.flow.impl.FlowDataSource;
import org.structr.flow.impl.FlowNode;
import org.structr.flow.impl.FlowSwitch;
import org.structr.flow.impl.FlowSwitchCase;

public class SwitchHandler implements FlowHandler {

	@Override
	public FlowNode handle(final Context context, final FlowNode switchElement) throws FlowException {

		final FlowDataSource _ds = switchElement.getDataSource();
		if (_ds != null) {

			final Iterable<FlowSwitchCase> cases = switchElement.as(FlowSwitch.class).getCases();
			final Object data                    = _ds.get(context);

			if (cases != null) {

				for (FlowSwitchCase switchCase : cases) {

					final String caseValue = switchCase.getSwitchCase();

					if (caseValue != null && data != null && caseValue.equals(data.toString())) {

						return switchCase.next();
					}
				}
			}
		}

		return switchElement.next();
	}
}
