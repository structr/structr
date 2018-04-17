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

import org.structr.flow.api.FlowHandler;
import org.structr.flow.api.FlowElement;
import org.structr.flow.api.FlowType;
import java.util.EnumMap;
import java.util.Map;
import org.structr.core.GraphObject;
import org.structr.flow.api.FlowResult;

/**
 *
 */
public class FlowEngine {

	private final Map<FlowType, FlowHandler> handlers = new EnumMap<>(FlowType.class);
	private Context context                           = null;

	public FlowEngine() {
		this((GraphObject)null);
	}

	public FlowEngine(final GraphObject thisObject) {
		this(new Context(thisObject));
	}

	public FlowEngine(final Context context) {

		init();

		this.context = context;
	}

	public FlowResult execute(final FlowElement step) {
		return this.execute(this.context,step);
	}

	public FlowResult execute(final Context context, final FlowElement step) {

		FlowElement current = step;

		while (current != null) {

			final FlowHandler handler = handlers.get(current.getFlowType());
			if (handler != null) {

				current = handler.handle(context, current);

			} else {

				System.out.println("No handler registered for type " + current.getFlowType() + ", aborting.");
			}

			// check for error or return values and break early
			if (context.hasResult() || context.hasError()) {
				return new FlowResult(context);
			}
		}

		return new FlowResult(context);
	}

	// ----- private methods -----
	private void init() {

		handlers.put(FlowType.Action,   new ActionHandler());
		handlers.put(FlowType.Decision, new DecisionHandler());
		handlers.put(FlowType.Return,   new ReturnHandler());
		handlers.put(FlowType.ForEach,  new ForEachHandler());
		handlers.put(FlowType.Store, new StoreHandler());
	}
}
