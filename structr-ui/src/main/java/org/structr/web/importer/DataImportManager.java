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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import org.structr.common.error.FrameworkException;

public class DataImportManager {

	private static DataImportManager singletonInstance = null;

	private final AtomicLong importJobIdCount      = new AtomicLong(0);
	private final Map<Long, ImportJob> queuedJobs  = new ConcurrentHashMap<>();
	private final Map<Long, ImportJob> activeJobs  = new ConcurrentHashMap<>();
	private final Queue<Long> jobIdQueue           = new ConcurrentLinkedDeque<>();

	private DataImportManager() { }

	/*
	 * Public API
	 */
	public static DataImportManager getInstance() {

		if (singletonInstance == null) {
			singletonInstance = new DataImportManager();
		}

		return singletonInstance;
	}

	public void addJob(final ImportJob job) throws FrameworkException {

		if (job.runInitialChecks()) {

			final Long jobId = importJobIdCount.incrementAndGet();
			job.setJobId(jobId);

			appendToQueueInternal(job);

			if (!hasRunningJobs()) {

				startNextJobInQueue();

			} else {

				job.reportQueued();

			}
		}
	}

	/**
	 * Starts an import job if it exists. Returns true if it is started.
	 *
	 * @param jobId Job to start
	 * @return boolean "job started"
	 */
	public boolean startJob(final Long jobId) {

		final ImportJob job = removeFromQueueInternal(jobId);

		if (job != null) {
			activeJobs.put(jobId, job);
			job.startJob();
			return true;
		} else {
			return false;
		}
	}

	public void pauseRunningJob(final Long jobId) {
		activeJobs.get(jobId).pauseJob();
	}

	public void resumePausedJob(final Long jobId) {
		activeJobs.get(jobId).resumeJob();
	}

	public void abortActiveJob(final Long jobId) {
		activeJobs.get(jobId).abortJob();
	}

	public void cancelQueuedJob(final Long jobId) {
		removeFromQueueInternal(jobId);
	}

	public List listJobs () {

		final List jobInfoList = new LinkedList();

		activeJobs.values().forEach((ImportJob job) -> {
			addJobToList(jobInfoList, job);
		});

		jobIdQueue.forEach((Long jobId) -> {
			addJobToList(jobInfoList, queuedJobs.get(jobId));
		});

		return jobInfoList;
	}

	private void addJobToList (List list, ImportJob job) {

		final LinkedHashMap jobInfo = new LinkedHashMap();
		jobInfo.put("jobId", job.jobId());
		jobInfo.put("fileUuid", job.getFileUuid());
		jobInfo.put("filepath", job.getFilePath());
		jobInfo.put("filesize", job.getFileSize());
		jobInfo.put("username", job.getUsername());
		jobInfo.put("status", job.getCurrentStatus());
		jobInfo.put("processedChunks", job.getProcessedChunks());

		list.add(jobInfo);
	}

	protected void jobFinished (final ImportJob job) {

		activeJobs.remove(job.jobId());

		if (!hasRunningJobs()) {
			startNextJobInQueue();
		}

	}

	protected void jobAborted (final ImportJob job) {

		activeJobs.remove(job.jobId());

		if (!hasRunningJobs()) {
			startNextJobInQueue();
		}

	}


	//~--- private methods ----------------------------------------------------

	private void appendToQueueInternal (final ImportJob job) {
		jobIdQueue.add(job.jobId());
		queuedJobs.put(job.jobId(), job);
	}

	private ImportJob removeFromQueueInternal (final Long jobId) {
		jobIdQueue.remove(jobId);
		return queuedJobs.remove(jobId);
	}

	private boolean hasRunningJobs() {

		// if any job is RUNNING or PAUSED, dont auto-start new tasks
		return (!activeJobs.isEmpty());

//		// if any job is RUNNING, dont auto-start new tasks
//		// this effectively means that if a user PAUSES a job, the queue is stopped BUT
//		// if another job is queued or the last running job finishes, then the next queued job is started
//		boolean hasRunningJob = false;
//
//		for (ImportJob job : activeJobs.values()) {
//			if (job.getCurrentStatus().equals(ImportJob.JobStatus.RUNNING)) {
//				hasRunningJob = true;
//			}
//		}
//
//		return hasRunningJob;

	}

	private void startNextJobInQueue() {
		final Long jobId = jobIdQueue.peek();

		if (jobId != null) {
			boolean didStart = startJob(jobId);
			if (!didStart) {
				startNextJobInQueue();
			}
		}
	}
}

