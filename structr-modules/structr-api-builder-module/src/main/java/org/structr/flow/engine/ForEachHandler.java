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
package org.structr.flow.engine;

import java.util.Collection;

import org.structr.flow.api.*;
import org.structr.flow.impl.FlowForEach;

/**
 *
 */
public class ForEachHandler implements FlowHandler<FlowForEach> {

	@Override
	public FlowElement handle(final Context context, final FlowForEach flowElement) {

		final FlowEngine engine       = new FlowEngine(context);
		final DataSource dataSource   = flowElement.getDataSource();
		final FlowElement loopBody    = flowElement.getLoopBody();
		final Object data             = dataSource.get(context);

		Context loopContext = new Context(context.getThisObject());

		if (data instanceof Collection) {

			for (final Object o : ((Collection)data)) {

				loopContext.setData(flowElement.getUuid(), o);
			}

		} else {

			loopContext.setData(flowElement.getUuid(), data);

		}

		// ignore sub result for now..
		engine.execute(loopContext, loopBody);

		return flowElement.next();
	}
}
