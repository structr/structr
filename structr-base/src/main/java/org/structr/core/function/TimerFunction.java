/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.Date;
import java.util.List;

public class TimerFunction extends CoreFunction {

	@Override
	public String getName() {
		return "timer";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("name, action");
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
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${timer(name, action)}."),
			Usage.javaScript("Usage: ${{$.timer(name, action)}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Starts/Stops/Pings a timer.";
	}

	@Override
	public String getLongDescription() {
		return "This function can be used to measure the performance of sections of code. The `action` parameter can be `start` to create a new timer or `get` to retrieve the elapsed time (in milliseconds) since the start of the timer.";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${timer('benchmark1', 'start')}"),
			Example.javaScript("${{ $.timer('benchmark1', 'start') }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("name", "name of timer"),
			Parameter.mandatory("action", "action (`start` or `get`)")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Using the `get` action before the `start` action returns 0 and starts the timer.",
			"Using the `start` action on an already existing timer overwrites the timer."
		);
	}

}
