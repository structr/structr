/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.flow.impl.FlowNode;
import org.structr.flow.impl.FlowReturn;

public class ReturnHandler implements FlowHandler {

	@Override
	public FlowNode handle(final Context context, final FlowNode flowElement) throws FlowException {

		context.setResult(flowElement.as(FlowReturn.class).getResult(context));

		return null;
	}
}
