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
import org.structr.flow.api.FlowHandler;
import org.structr.flow.api.DataHandler;
import org.structr.flow.api.DataSource;
import org.structr.flow.api.ForEach;
import org.structr.flow.api.FlowElement;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class ForEachHandler<T> implements FlowHandler<ForEach<T>> {

	@Override
	public FlowElement handle(final Context context, final ForEach<T> flowElement) {

		final FlowEngine engine       = new FlowEngine(context);
		final DataHandler dataHandler = flowElement.getDataHandler();
		final DataSource dataSource   = flowElement.getDataSource();
		final FlowElement loopBody    = flowElement.getLoopBody();
		final Object data             = dataSource.get(context);

		// Register current data in context
		if (data != null) {
			context.setData(data);
		}

		Context loopContext = new Context(context.getThisObject());

		if (data instanceof Collection) {

			for (final Object o : ((Collection)data)) {

				dataHandler.data(loopContext, o);

				// ignore sub result for now..
				engine.execute(loopContext, loopBody);
			}

		} else {

			dataHandler.data(loopContext, data);

			// ignore sub result for now..
			engine.execute(loopContext, loopBody);
		}

		return flowElement.next();
	}
}
