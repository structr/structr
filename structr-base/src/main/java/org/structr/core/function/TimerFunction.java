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
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

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

			final String name   = sources[0].toString();
			final String action = sources[1].toString();

			switch (action) {
				case "start":
					ctx.startTimer(name);
					return null;

				case "pause":
					return ctx.pauseTimer(name);

				case "clear":
					return ctx.clearTimer(name);

				case "get":
					return ctx.getTimerElapsedMs(name);

				default:
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
		return "Controls a named timer by starting, pausing, clearing, or returning its current elapsed time in milliseconds.";
	}

	@Override
	public String getLongDescription() {
		return """
			This function measures the execution time of sections of code.

			A timer is identified by its name. Depending on the specified `action`, the timer can be started, paused, cleared, or queried. All time values are returned in **milliseconds**.

			### Supported Actions

			| Action  | Description                                                                                                                 | Return Value                                     |
			| ------- | --------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------ |
			| `start` | Starts a new timer or resumes an existing paused timer. If the timer is already running, the call has no effect.            | No return value.                                 |
			| `pause` | Stops a running timer and accumulates the elapsed time into the total. If the timer is not running, no state changes occur. | Time elapsed since the last `start` (interval).  |
			| `get`   | Retrieves the current total elapsed time. If the timer is running, the current interval is included in the result.          | Current total elapsed time.                      |
			| `clear` | Stops the timer (if running), resets its elapsed time to zero, and removes any running state.                               | Total elapsed time before clearing.              |

			If the specified timer does not yet exist, it is treated as having an elapsed time of `0`.
			
			No manual timer cleanup is required. Timers are scoped to the current request. This means timers started in one method can be accessed and continued in another method executed within the same request.
			""";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${timer('benchmark1', 'start')}"),
			Example.javaScript("""
				${{
					$.timer('whole_function', 'start');
				
					const estimateDurations = [];
					const budgetDurations   = [];
				
					const projects = $.find('Project');
				
					if (projects.length > 0) {
				
						for (const project of projects) {
				
							$.timer('estimates', 'start');
							project.calculateTotalEstimateByTaskEstimates();
							estimateDurations.push($.timer('estimates', 'pause'));
				
							$.timer('budgets', 'start');
							project.calculateRequiredBudget();
							budgetDurations.push($.timer('budgets', 'pause'));
						}
				
						const estimatesTotalTime = $.timer('estimates', 'get');
						const estimatesMeanTime  = estimatesTotalTime / estimateDurations.length;
						const estimatesMin       = Math.min(...estimateDurations);
						const estimatesMax       = Math.max(...estimateDurations);
				
						const budgetsTotalTime = $.timer('budgets', 'get');
						const budgetsMeanTime  = budgetsTotalTime / budgetDurations.length;
						const budgetsMin       = Math.min(...budgetDurations);
						const budgetsMax       = Math.max(...budgetDurations);
				
						$.log('Estimates took: ', estimatesTotalTime, ' ms. Mean: ', estimatesMeanTime, ' ms, Min: ', estimatesMin, ' ms, Max: ', estimatesMax, ' ms');
						$.log('Budgets took: ', budgetsTotalTime, ' ms. Mean: ', budgetsMeanTime, ' ms, Min: ', budgetsMin, ' ms, Max: ', budgetsMax, ' ms');
					}
				
					const totalTime = $.timer('whole_function', 'get');
				
					$.log(projects.length, ' projects analyzed in ', totalTime, ' ms');
				}}
				""")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("name", "name of timer"),
			Parameter.mandatory("action", "action (`start`, `pause`, `get` or `clear`)")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
				"Calling `start` on a running timer has no effect.",
				"Before the first `start` of a timer, using `get`, `pause` or `clear` will return 0."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.System;
	}
}
