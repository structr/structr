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

import org.structr.core.scheduler.JobQueueManager;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;
import java.util.Map;

public class JobInfoFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "jobInfo";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("jobId");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		try {
			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (!(sources[0] instanceof Number)) {
				throw new IllegalArgumentException("jobInfo(): JobId must be numeric");
			}

			final Long jobId = ((Number)sources[0]).longValue();

			final Map<String, Object> jobInfo = JobQueueManager.getInstance().jobInfo(jobId);

			if (jobInfo == null) {
				logger.warn("jobInfo(): Job with ID {} not found", jobId);
				return false;
			}

			return jobInfo;

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return null;
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${jobInfo(jobId)}. Example: ${jobInfo(1)}"),
			Usage.javaScript("Usage: ${{Structr.jobInfo(jobId)}}. Example: ${{Structr.jobInfo(1)}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns information about the job with the given job ID.";
	}

	@Override
	public String getLongDescription() {
		return """
		If the job does not exist (anymore) the function returns `false`.

		For **script jobs** the returned information is:

		| Key | Value |
		| --- | --- |
		| `jobId` | The job ID |
		| `jobtype` | The job type |
		| `username` | The username of the user who started the job |
		| `status` | The current status of the job |
		| `jobName` | The name of the script job |
		| `exception` | <p>**If an exception was caught** during the execution, an exception object containing:</p><p></p><p>`message` : The message of the exception</p><p>`cause` : The cause of the exception</p><p>`stacktrace` : The stacktrace of the exception |

		For **file import** the returned information is:

		| Key | Value |
		| --- | --- |
		| `jobId` | The job ID |
		| `jobtype` | The job type |
		| `username` | The username of the user who started the job |
		| `status` | The current status of the job |
		| `fileUuid` | The UUID of the imported file |
		| `filepath` | The path of the imported file |
		| `filesize` | The size of the imported file |
		| `processedChunks` | The number of chunks already processed |
		| `processedObjects` | The number of objects already processed |
		| `exception` | <p>**If an exception was caught** during the execution, an exception object containing:</p><p></p><p>`message` : The message of the exception</p><p>`cause` : The cause of the exception</p><p>`stacktrace` : The stacktrace of the exception |
		""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("jobId", "ID of the job to query")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${jobInfo(1)}", "Return information about the job with ID 1")
		);
	}
}
