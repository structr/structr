/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.web.entity.FileBase;

abstract class ImportJob {

	private static final Logger logger = LoggerFactory.getLogger(ImportJob.class.getName());

	private Thread importThread = null;
	final Lock lock             = new ReentrantLock();
	final Condition paused      = lock.newCondition();

	private Long jobId = null;

	protected enum JobStatus {
		QUEUED,		// the job is queued and will be started as soon as the queued ahead of it is empty
		RUNNING,	// the job is running currently
		PAUSED,		// the job was paused
		WAIT_PAUSE,	// the job is waiting to pause (pausing only happens after completion of a chunk)
		WAIT_ABORT	// the job is waiting to abort (aborting only happens after completion of a chunk)
	};
	protected enum JobStatusMessageSubtype { QUEUED, BEGIN, CHUNK, END, WAIT_PAUSE, PAUSED, RESUMED, WAIT_ABORT, ABORTED };

	protected Principal user;
	protected String username;
	protected Map<String, Object> configuration;
	protected String fileUuid;
	protected String filePath;
	protected String fileName;
	protected JobStatus currentStatus;
	protected Integer processedChunks = 0;

	public ImportJob (final FileBase file, final Principal user, final Map<String, Object> configuration) {

		this.fileUuid = file.getUuid();
		this.filePath = file.getPath();
		this.fileName = file.getName();
		this.user = user;
		this.username = user.getName();
		this.configuration = configuration;

		this.currentStatus = JobStatus.QUEUED;

	}

	abstract boolean runInitialChecks() throws FrameworkException;
	abstract Runnable getRunnable();
	abstract String getImportType();
	abstract String getImportStatusType();
	abstract String getImportExceptionMessageType();

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

	public String getFileUuid () {
		return fileUuid;
	}

	public String getFilePath () {
		return filePath;
	}

	public String getFileName () {
		return fileName;
	}

	public String getUsername () {
		return username;
	}

	public JobStatus getCurrentStatus () {
		return currentStatus;
	}

	public Integer getProcessedChunks () {
		return processedChunks;
	}

	public Map<String, Object> getConfiguration () {
		return configuration;
	}

	protected void jobFinished() {
		DataImportManager.getInstance().jobFinished(this);
	}

	protected void jobAborted() {
		DataImportManager.getInstance().jobAborted(this);
	}

	protected void startNewThread(final Runnable runnable, final boolean wait) {

		importThread = new Thread(runnable);

		importThread.start();

		if (wait) {
			try { importThread.join(); } catch (InterruptedException ex) {}
		}

	}

	protected void shouldPause() {

		if (currentStatus.equals(JobStatus.WAIT_PAUSE)) {
			logger.info("Pausing import job {} ({})", jobId, filePath);

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
				logger.info("Resuming import job {} ({})", jobId, filePath);
				reportResumed();
			}
		}
	}

	protected boolean shouldAbort() {

		if (currentStatus.equals(JobStatus.WAIT_ABORT)) {

			logger.info("Aborting import job {} ({})", jobId, filePath);
			reportAborted();
			jobAborted();

			return true;
		}

		return false;
	}

	protected Map<String, Object> getWebsocketStatusData (final JobStatusMessageSubtype subtype) {

		final Map<String, Object> data = new LinkedHashMap();
		data.put("type",       getImportStatusType());
		data.put("importtype", getImportType());
		data.put("subtype",    subtype);
		data.put("filename",   fileName);
		data.put("filepath",   filePath);
		data.put("username",   username);

		return data;
	}

	protected void reportStatus(final JobStatusMessageSubtype subtype) {
		TransactionCommand.simpleBroadcastGenericMessage(getWebsocketStatusData(subtype));
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

	protected void reportChunk(final int currentChunkNo) {
		processedChunks = currentChunkNo;

		final Map<String, Object> endMsgData = getWebsocketStatusData(JobStatusMessageSubtype.CHUNK);
		endMsgData.put("currentChunkNo", currentChunkNo);
		TransactionCommand.simpleBroadcastGenericMessage(endMsgData);
	}

	protected void reportEnd(final String duration) {
		final Map<String, Object> endMsgData = getWebsocketStatusData(JobStatusMessageSubtype.END);
		endMsgData.put("duration", duration);
		TransactionCommand.simpleBroadcastGenericMessage(endMsgData);
	}

	protected void reportException(Exception ex) {

		final Map<String, Object> data = new LinkedHashMap();
		data.put("type",       getImportExceptionMessageType());
		data.put("importtype", getImportType());
		data.put("filename",   fileName);
		data.put("filepath",   filePath);
		data.put("username",   username);
		TransactionCommand.simpleBroadcastException(ex, data, true);
	}

	protected InputStream getFileInputStream(final SecurityContext ctx) {

		final App app = StructrApp.getInstance(ctx);

		InputStream is = null;

		try (final Tx tx = app.tx()) {

			final FileBase file = (FileBase)app.get(fileUuid);
			is = file.getInputStream();

			tx.success();

		} catch (FrameworkException fex) {

			final Map<String, Object> data = new LinkedHashMap();
			data.put("type",     getImportExceptionMessageType());
			data.put("filename", fileName);
			data.put("filepath", filePath);
			data.put("username", ctx.getUser(false).getName());
			TransactionCommand.simpleBroadcastException(fex, data, true);

		}

		return is;
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

}
