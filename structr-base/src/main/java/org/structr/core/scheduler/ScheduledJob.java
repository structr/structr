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
package org.structr.core.scheduler;

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
import org.structr.core.script.Scripting;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(ScheduledJob.class.getName());

	protected enum JobStatus {
		QUEUED,		// the job is queued and will be started as soon as the queued ahead of it is empty
		RUNNING,	// the job is running currently
		PAUSED,		// the job was paused
		WAIT_PAUSE,	// the job is waiting to pause (pausing only happens after completion of a chunk)
		WAIT_ABORT	// the job is waiting to abort (aborting only happens after completion of a chunk)
	};

	protected enum JobStatusMessageSubtype { QUEUED, BEGIN, CHUNK, END, WAIT_PAUSE, PAUSED, RESUMED, WAIT_ABORT, ABORTED };

	final Lock lock          = new ReentrantLock();
	final Condition paused   = lock.newCondition();

	private Thread jobThread = null;
	private Long jobId       = null;

	protected Map<String, Object> configuration;
	protected PrincipalInterface user;
	protected String username;
	protected String jobName;
	protected JobStatus currentStatus;
	protected ContextStore ctxStore = null;

	private Object onFinishScript = null;

	public ScheduledJob (final String jobName, final PrincipalInterface user, final Map<String, Object> configuration, final ContextStore ctxStore) {

		this.user          = user;
		this.jobName       = jobName;
		this.username      = (user != null) ? user.getName() : "no user - anonymous context";
		this.configuration = configuration;
		this.ctxStore      = new ContextStore(ctxStore);

		this.currentStatus = JobStatus.QUEUED;
	}

	public abstract boolean runInitialChecks() throws FrameworkException;
	public abstract boolean canRunMultiThreaded();
	public abstract Runnable getRunnable();
	public abstract String getJobType();
	public abstract String getJobStatusType();
	public abstract String getJobExceptionMessageType();

	protected Exception encounteredException = null;
	public abstract void reportException(Exception ex);

	public abstract Map<String, Object> getStatusData (final JobStatusMessageSubtype subtype);
	public abstract Map<String, Object> getJobInfo ();

	public void startJob() {
		currentStatus = JobStatus.RUNNING;

		startNewThread(getRunnable(), false);
	}

	public void pauseJob() {
		// only allow if in RUNNING state
		currentStatus = JobStatus.WAIT_PAUSE;
		reportWaitingForPause();
	}

	public void resumeJob() {
		currentStatus = JobStatus.RUNNING;
		trySignal();
	}

	public void abortJob() {
		currentStatus = JobStatus.WAIT_ABORT;
		trySignal();
		reportWaitingForAbort();
	}

	/**
	 * all actions that can be called while a job is in PAUSED state must call trySignal
	 */
	public void trySignal() {
		lock.lock();
		try {
			paused.signal();
		} finally {
			lock.unlock();
		}
	}

	public void cancelQueuedJob() {
		if (currentStatus.equals(JobStatus.QUEUED)) {

		} else {
			// send warning?
		}
	}

	public Long jobId() {
		return jobId;
	}

	public void setJobId(final Long jobId) {
		this.jobId = jobId;
	}

	public String getUsername () {
		return username;
	}

	public JobStatus getCurrentStatus () {
		return currentStatus;
	}

	public Map<String, Object> getConfiguration () {
		return configuration;
	}

	public void waitForExit() throws InterruptedException {
		jobThread.join();
	}

	protected void jobFinished() {

		runOnFinishScript();

		JobQueueManager.getInstance().jobFinished(this);
	}

	protected void jobAborted() {
		JobQueueManager.getInstance().jobAborted(this);
	}

	protected void startNewThread(final Runnable runnable, final boolean wait) {

		jobThread = new Thread(runnable);

		jobThread.start();

		if (wait) {
			try { jobThread.join(); } catch (InterruptedException ex) {}
		}

	}

	protected void shouldPause() {

		if (currentStatus.equals(JobStatus.WAIT_PAUSE)) {
			logger.info("Pausing job {} ({})", jobId, jobName);

			currentStatus = JobStatus.PAUSED;
			reportPaused();

			lock.lock();
			try {
				while (currentStatus.equals(JobStatus.PAUSED)) {
					paused.await();
				}
			} catch (InterruptedException ex) {
				logger.error("", ex);
			} finally {
				lock.unlock();
				logger.info("Resuming job {} ({})", jobId, jobName);
				reportResumed();
			}
		}
	}

	protected boolean shouldAbort() {

		if (currentStatus.equals(JobStatus.WAIT_ABORT)) {

			logger.info("Aborting job {} ({})", jobId, jobName);
			reportAborted();
			jobAborted();

			return true;
		}

		return false;
	}

	protected void reportStatus(final JobStatusMessageSubtype subtype) {
		TransactionCommand.simpleBroadcastGenericMessage(getStatusData(subtype));
	}

	protected void reportQueued() {
		reportStatus(JobStatusMessageSubtype.QUEUED);
	}

	protected void reportBegin() {
		reportStatus(JobStatusMessageSubtype.BEGIN);
	}

	protected void reportWaitingForAbort() {
		reportStatus(JobStatusMessageSubtype.WAIT_ABORT);
	}

	protected void reportAborted() {
		reportStatus(JobStatusMessageSubtype.ABORTED);
	}

	protected void reportWaitingForPause() {
		reportStatus(JobStatusMessageSubtype.WAIT_PAUSE);
	}

	protected void reportPaused() {
		reportStatus(JobStatusMessageSubtype.PAUSED);
	}

	protected void reportResumed() {
		reportStatus(JobStatusMessageSubtype.RESUMED);
	}

	protected void reportFinished() {
		reportStatus(JobStatusMessageSubtype.END);
	}

	protected <T> T getOrDefault(final Object value, final T defaultValue) {

		if (value != null) {
			return (T)value;
		}

		return defaultValue;
	}

	protected int parseInt(final Object input, final int defaultValue) {

		if (input != null) {

			// do not parse if it's already a number
			if (input instanceof Number) {
				return ((Number)input).intValue();
			}

			// use string representation otherwise
			final String value = input.toString();

			// try to parse value but ignore any errors
			try { return Integer.parseInt(value); } catch (Throwable t) {}
		}

		return defaultValue;
	}

	protected Map<String, String> reverse(final Map<String, String> input) {

		final Map<String, String> output = new LinkedHashMap<>();

		// reverse map
		for (final Map.Entry<String, String> entry : input.entrySet()) {
			output.put(entry.getValue(), entry.getKey());
		}

		return output;
	}

	public Object getOnFinishScript() {
		return onFinishScript;
	}

	public void setOnFinishScript(final Object onFinishScript) {
		this.onFinishScript = onFinishScript;
	}

	public void runOnFinishScript() {

		if (onFinishScript != null) {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				final SecurityContext securityContext = SecurityContext.getInstance(user, AccessMode.Backend);
				securityContext.setContextStore(ctxStore);

				ctxStore.setConstant("jobInfo", getJobInfo());

				final ActionContext actionContext = new ActionContext(securityContext);

				// If a polyglot function was supplied, execute it directly
				if (onFinishScript instanceof PolyglotWrapper.FunctionWrapper) {

					((PolyglotWrapper.FunctionWrapper)onFinishScript).execute();
				} else if (onFinishScript instanceof String) {

					Scripting.evaluate(actionContext, null, (String)onFinishScript, jobName, null);

				} else if (onFinishScript != null) {

					logger.warn("Unable to run jobFinishedScript of type {}, ignoring", onFinishScript.getClass().getName());
				}

				tx.success();

			} catch (Exception e) {

				reportException(e);
			}
		}
	}

	public Exception getEncounteredException () {
		return encounteredException;
	}

	public void setEncounteredException (final Exception ex) {
		this.encounteredException = ex;
	}
}
