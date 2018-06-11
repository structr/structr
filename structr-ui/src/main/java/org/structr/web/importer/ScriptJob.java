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
package org.structr.web.importer;

import java.util.LinkedHashMap;
import java.util.Map;
import org.mozilla.javascript.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.ContextStore;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.scheduler.ScheduledJob;
import org.structr.core.script.Scripting;
import org.structr.core.script.Snippet;
import org.structr.schema.action.ActionContext;

/**
 */
public class ScriptJob extends ScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(ScriptJob.class);
	private Object script              = null;

	public ScriptJob(final Principal user, final Map<String, Object> configuration, final Object script, final ContextStore ctxStore) {

		super(null, user, configuration, ctxStore);

		this.script  = script;
	}

	@Override
	public boolean runInitialChecks() throws FrameworkException {
		return true;
	}

	@Override
	public Runnable getRunnable() {

		return () -> {

			try {

				final SecurityContext securityContext = SecurityContext.getInstance(user, AccessMode.Backend);
				securityContext.setContextStore(ctxStore);
				final ActionContext actionContext     = new ActionContext(securityContext);
				final long startTime                  = System.currentTimeMillis();

				reportBegin();

				// called from JavaScript?
				if (script instanceof Script) {

					Scripting.evaluateJavascript(actionContext, null, new Snippet((Script)script));

				} else if (script instanceof String) {

					Scripting.evaluate(actionContext, null, (String)script, jobName);

				} else if (script != null) {

					logger.warn("Unable to schedule script of type {}, ignoring", script.getClass().getName());
				}

				reportFinished();

			} catch (Exception e) {

				reportException(e);

			} finally {

				jobFinished();
			}
		};
	}

	@Override
	public String getJobType() {
		return "SCRIPT";
	}

	@Override
	public String getJobStatusType() {
		return "SCRIPT_JOB_STATUS";
	}

	@Override
	public String getJobExceptionMessageType() {
		return "SCRIPT_JOB_EXCEPTION";
	}

	@Override
	public Map<String, Object> getStatusData (final JobStatusMessageSubtype subtype) {

		final Map<String, Object> data = new LinkedHashMap();

		data.put("jobId",      jobId());
		data.put("type",       getJobStatusType());
		data.put("jobtype",    getJobType());
		data.put("subtype",    subtype);
		data.put("username",   getUsername());

		return data;
	}

	@Override
	public Map<String, Object> getJobInfo () {

		final LinkedHashMap<String, Object> jobInfo = new LinkedHashMap<>();

		jobInfo.put("jobId",           jobId());
		jobInfo.put("jobtype",         getJobType());
		jobInfo.put("username",        getUsername());
		jobInfo.put("status",          getCurrentStatus());

		return jobInfo;
	}

	// ----- private methods -----
	private void reportException(Exception ex) {

		final Map<String, Object> data = new LinkedHashMap<>();

		data.put("type",       getJobExceptionMessageType());
		data.put("jobtype",    getJobType());
		data.put("username",   getUsername());

		TransactionCommand.simpleBroadcastException(ex, data, true);
	}
}
