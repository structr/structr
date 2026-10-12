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
package org.structr.web.importer;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.graalvm.polyglot.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.structr.common.AccessMode;
import org.structr.common.ContextStore;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.scheduler.ScheduledJob;
import org.structr.core.script.Scripting;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.core.script.polyglot.config.ScriptConfig;
import org.structr.core.script.polyglot.context.ContextFactory;
import org.structr.schema.action.ActionContext;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 */
public class ScriptJob extends ScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(ScriptJob.class);
	private Object script              = null;

	public ScriptJob(final Principal user, final Map<String, Object> configuration, final Object script, final ContextStore ctxStore, final String jobTitle, final String queueName) {

		super(jobTitle, user, configuration, ctxStore, queueName);

		this.script  = script;
	}

	@Override
	public boolean runInitialChecks() throws FrameworkException {

		return true;
	}

	/**
	 * The security context the job runs in.
	 *
	 * A job reuses the scheduling user's own context so that its script still sees the request that
	 * scheduled it. Two principals have none to hand over: an anonymous context carries no user at all,
	 * and a SuperUser answers null. Both then get a context of their own carrying the job's store, which
	 * has no request behind it -- request data is only available to a job whose user carried the context
	 * the request was served with.
	 */
	private SecurityContext getJobSecurityContext() {

		final SecurityContext userContext = (user != null) ? user.getSecurityContext() : null;

		if (userContext != null) {

			return userContext;
		}

		final SecurityContext jobContext = (user instanceof SuperUser)
			? SecurityContext.getSuperUserInstance()
			: SecurityContext.getInstance(user, AccessMode.Backend);

		jobContext.setContextStore(ctxStore);

		return jobContext;
	}

	@Override
	public Runnable getRunnable() {

		final Map<String, String> contextMap = MDC.getCopyOfContextMap();

		return () -> {

			MDC.setContextMap(contextMap);

			final SecurityContext securityContext = getJobSecurityContext();

			try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

				final ActionContext actionContext = new ActionContext(securityContext);

				reportBegin();

				// If a polyglot function was supplied, execute it directly
				if (script instanceof PolyglotWrapper.FunctionWrapper functionWrapper) {

					functionWrapper.execute();

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
		jobInfo.put("queue",           getQueueName());

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