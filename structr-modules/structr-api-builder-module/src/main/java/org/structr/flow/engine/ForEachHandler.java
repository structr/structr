/**
 * Copyright (C) 2010-2019 Structr GmbH
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
import org.structr.flow.impl.FlowAggregate;
import org.structr.flow.impl.FlowForEach;
import org.structr.flow.impl.FlowNode;

/**
 *
 */
public class ForEachHandler implements FlowHandler<FlowForEach> {

	@Override
	public FlowElement handle(final Context context, final FlowForEach flowElement) throws FlowException {

		final DataSource dataSource   = flowElement.getDataSource();

		if (dataSource != null) {

			final FlowEngine engine = new FlowEngine(context);
			final FlowNode loopBody = flowElement.getLoopBody();

			if (loopBody != null) {

				final Object data = dataSource.get(context);
				Context loopContext = new Context(context);

				// Special handling for FlowAggregate to ensure it's properly reset for nested loops.
				FlowElement element = loopBody;
				do {
					if (element instanceof FlowAggregate) {
						loopContext.setData(((FlowAggregate) element).getUuid(), null);
					}
				} while ((element = element.next()) != null);


				if (data instanceof Iterable) {

					for (final Object o : ((Iterable) data)) {

						// Provide current element data for loop context and write evaluation result into main context data for this loop element
						loopContext.setData(flowElement.getUuid(), o);
						context.setData(flowElement.getUuid(), engine.execute(loopContext, loopBody));

						// Break when an intermediate result or error occurs
						if (context.hasResult() || context.hasError()) {
							break;
						}
					}

				} else {

					// Provide current element data for loop context and write evaluation result into main context data for this loop element
					loopContext.setData(flowElement.getUuid(), data);
					context.setData(flowElement.getUuid(), engine.execute(loopContext, loopBody));

				}

				context.deepCopy(loopContext);

				context.setData(flowElement.getUuid(), data);

			}

		}

		return flowElement.next();
	}

}
