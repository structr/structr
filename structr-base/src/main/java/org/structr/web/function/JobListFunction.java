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

public class JobListFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_JOB_LIST    = "Usage: ${job_list()}. Example: ${job_info(1)}";
	public static final String ERROR_MESSAGE_JOB_LIST_JS = "Usage: ${{Structr.job_list()}}. Example: ${{Structr.job_list(}}";

	@Override
	public String getName() {
		return "job_list";
	}

	@Override
	public String getSignature() {
		return null;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		try {

			return JobQueueManager.getInstance().listJobs();
		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return null;
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_JOB_LIST : ERROR_MESSAGE_JOB_LIST_JS);
	}

	@Override
	public String shortDescription() {
		return "Returns a list of running jobs";
	}
}
