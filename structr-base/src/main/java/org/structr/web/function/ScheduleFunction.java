/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.web.function;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.scheduler.JobQueueManager;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;
import org.structr.web.importer.ScriptJob;

import java.util.Collections;
import java.util.List;

public class ScheduleFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "schedule";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("script [, title ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 3);

			final String jobName                  = (sources.length >= 2) ? sources[1].toString() : "Untitled script job";
			final Object jobFinishedScript        = (sources.length == 3) ? sources[2] : null;
			final SecurityContext securityContext = ctx.getSecurityContext();
			final ScriptJob job                   = new ScriptJob(securityContext.getCachedUser(), Collections.EMPTY_MAP, sources[0], securityContext.getContextStore(), jobName);

			job.setOnFinishScript(jobFinishedScript);

			TransactionCommand.queuePostProcessProcedure(() -> {

				try {

					JobQueueManager.getInstance().addJob(job);

				} catch (FrameworkException ex) {

					logException(ex, ex.getMessage(), null);
				}
			});

			return job.jobId();

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return null;
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${schedule(expression[, title])}."),
			Usage.javaScript("Usage: ${{$.schedule(expression[, title])}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Schedules a script or a function to be executed in a separate thread.";
	}

	@Override
	public String getLongDescription() {
		return """
		Allows the user to insert a script snippet into the import queue for later execution. 
		Useful in situations where a script should run after a long-running import job, or if the script should run in 
		a separate transaction that is independent of the calling transaction.
		The `title` parameter is optional and is displayed in the Structr admin UI in the Importer section and in the 
		notification messages when a script is started or finished.
		The `onFinish` parameter is a script snippet which will be called when the process finishes (successfully or with an exception).
		A parameter `jobInfo` is injected in the context of the `onFinish` function (see `job_info()` for more information on this object).
		The schedule function returns the job id under which it is registered.
		""";
	}



	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${schedule('call(\"myCleanupScript\")', 'Cleans unconnected nodes from the graph')}"),
				Example.javaScript("""
						${{
						    $.schedule(function() {
						        // execute global method
						        $.call('myCleanupScript');
						    }, 'Cleans unconnected nodes from the graph');
						}}
						"""),
				Example.javaScript("""
						${{
						    $.schedule(function() {
						        // execute global method
						        Structr.call('myCleanupScript');
						    }, 'Cleans unconnected nodes from the graph', function() {
						        $.log('scheduled function finished!');
						        $.log('Job Info: ', $.get('jobInfo'));
						    });
						}}
						""")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("expression", "function to run later"),
				Parameter.optional("title", "title of schedule"),
				Parameter.optional("onFinish", "function to be called when main expression finished")
				);
	}
}
