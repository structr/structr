/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.flow.impl.FlowDecision;
import org.structr.flow.impl.FlowNode;

public class DecisionHandler implements FlowHandler {

	@Override
	public FlowNode handle(final Context context, final FlowNode flowNode) throws FlowException {

		final FlowDecision flowElement = flowNode.as(FlowDecision.class);
		final FlowDataSource condition = flowElement.getCondition();

		if (condition != null) {

			final Object value = condition.get(context);

			if (isTrue(value)) {

				return flowElement.getTrueElement();

			} else {

				return flowElement.getFalseElement();
			}
		} else {

			return flowElement.getFalseElement();
		}
	}

	// ----- private methods -----
	private boolean isTrue(final Object value) {

		if (value != null) {

			if (value instanceof Boolean) {

				return (Boolean)value;
			}

			if (value instanceof String) {

				return Boolean.valueOf((String)value);
			}

			// not sure if good..
			if (value instanceof Number) {

				return ((Number)value).longValue() > 0;
			}
		}

		return false;
	}
}
