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
package org.structr.web.importer;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.ContextStore;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.scheduler.ScheduledJob;
import org.structr.core.script.Scripting;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 */
public class ScriptJob extends ScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(ScriptJob.class);
	private Object script              = null;

	public ScriptJob(final PrincipalInterface user, final Map<String, Object> configuration, final Object script, final ContextStore ctxStore, final String jobTitle) {

		super(jobTitle, user, configuration, ctxStore);

		this.script  = script;
	}

	@Override
	public boolean runInitialChecks() throws FrameworkException {
		return true;
	}

	@Override
	public boolean canRunMultiThreaded() {
		// only those jobs that are actual string-based scripts can be run in parallel
		return script instanceof String;
	}

	@Override
	public Runnable getRunnable() {

		return () -> {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				final SecurityContext securityContext = SecurityContext.getInstance(user, AccessMode.Backend);

				securityContext.setContextStore(ctxStore);

				final ActionContext actionContext     = new ActionContext(securityContext);

				reportBegin();

				// If a polyglot function was supplied, execute it directly
				if (script instanceof PolyglotWrapper.FunctionWrapper) {

					((PolyglotWrapper.FunctionWrapper)script).execute();

				} else if (script instanceof String) {

					Scripting.evaluate(actionContext, null, (String)script, jobName, null);

				} else if (script != null) {

					logger.warn("Unable to schedule script of type {}, ignoring", script.getClass().getName());
				}

				reportFinished();

				tx.success();

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
		data.put("jobName",    jobName);

		return data;
	}

	@Override
	public Map<String, Object> getJobInfo () {

		final LinkedHashMap<String, Object> jobInfo = new LinkedHashMap<>();

		jobInfo.put("jobId",           jobId());
		jobInfo.put("jobtype",         getJobType());
		jobInfo.put("username",        getUsername());
		jobInfo.put("status",          getCurrentStatus());
		jobInfo.put("jobName",         jobName);

		if (getEncounteredException() != null) {

			final HashMap exceptionMap = new HashMap();
			exceptionMap.put("message", getEncounteredException().getMessage());
			exceptionMap.put("cause", getEncounteredException().getCause());
			exceptionMap.put("stacktrace", ExceptionUtils.getStackTrace(getEncounteredException()));
			jobInfo.put("exception", exceptionMap);
		}

		return jobInfo;
	}

	@Override
	public void reportException(Exception ex) {

		setEncounteredException(ex);

		final Map<String, Object> data = new LinkedHashMap<>();

		data.put("type",       getJobExceptionMessageType());
		data.put("jobtype",    getJobType());
		data.put("username",   getUsername());

		TransactionCommand.simpleBroadcastException(ex, data, true);
	}
}