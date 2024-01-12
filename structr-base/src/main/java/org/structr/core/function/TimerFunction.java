/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import java.util.Date;

public class TimerFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_TIMER = "Usage: ${timer(name, action)}. Example: ${timer('benchmark1', 'start')}";
	public static final String ERROR_MESSAGE_TIMER_JS = "Usage: ${{Structr.timer(name, action)}}. Example: ${{Structr.timer('benchmark1', 'start')}}";

	@Override
	public String getName() {
		return "timer";
	}

	@Override
	public String getSignature() {
		return "name, action";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			final String name = sources[0].toString();
			final String action = sources[1].toString();

			if (action.equals("start")) {

				ctx.addTimer(name);

				return null;

			} else if (action.equals("get")) {

				final Date begin = ctx.getTimer(name);

				if (begin == null) {

					logger.warn("Timer {} has not been started yet. Starting it.", name);

					ctx.addTimer(name);

					return 0;

				} else {

					return (new Date()).getTime() -  begin.getTime();

				}

			} else {

				logger.warn("Unknown action for timer function: {}", action);

			}

		} catch (ArgumentCountException | ArgumentNullException ace) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

		}

		return usage(ctx.isJavaScriptContext());

	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_TIMER_JS : ERROR_MESSAGE_TIMER);
	}

	@Override
	public String shortDescription() {
		return "Starts/Stops/Pings a timer";
	}

}
