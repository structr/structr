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
import org.structr.schema.action.ActionContext;

import java.util.Map;

public class JobInfoFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_JOB_INFO    = "Usage: ${job_info(jobId)}. Example: ${job_info(1)}";
	public static final String ERROR_MESSAGE_JOB_INFO_JS = "Usage: ${{Structr.job_info(jobId)}}. Example: ${{Structr.job_info(1}}";

	@Override
	public String getName() {
		return "job_info";
	}

	@Override
	public String getSignature() {
		return "jobId";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		try {
			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (!(sources[0] instanceof Number)) {
				throw new IllegalArgumentException("job_info(): JobId must be numeric");
			}

			final Long jobId = ((Number)sources[0]).longValue();

			final Map<String, Object> jobInfo = JobQueueManager.getInstance().jobInfo(jobId);

			if (jobInfo == null) {
				logger.warn("job_info(): Job with ID {} not found", jobId);
				return false;
			}

			return jobInfo;

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return null;
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_JOB_INFO : ERROR_MESSAGE_JOB_INFO_JS);
	}

	@Override
	public String shortDescription() {
		return "Returns job information for the given job id - if the job does not exist, false is returned";
	}
}
