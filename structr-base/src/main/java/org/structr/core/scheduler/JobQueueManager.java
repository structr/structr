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
package org.structr.core.scheduler;

import org.structr.common.error.FrameworkException;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public class JobQueueManager {

	private static JobQueueManager singletonInstance = null;

	private final Map<Long, ScheduledJob> queuedJobs  = new ConcurrentHashMap<>();
	private final Map<Long, ScheduledJob> activeJobs  = new ConcurrentHashMap<>();
	private final Queue<Long> jobIdQueue              = new ConcurrentLinkedDeque<>();
	private final AtomicLong importJobIdCount         = new AtomicLong(0);

	private JobQueueManager() { }

	/*
	 * Public API
	 */
	public static JobQueueManager getInstance() {

		if (singletonInstance == null) {
			singletonInstance = new JobQueueManager();
		}

		return singletonInstance;
	}

	public void addJob(final ScheduledJob job) throws FrameworkException {

		if (job.runInitialChecks()) {

			final Long jobId = importJobIdCount.incrementAndGet();
			job.setJobId(jobId);

			appendToQueueInternal(job);

			if (canRunMoreJobs()) {

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

		final ScheduledJob job = removeFromQueueInternal(jobId);

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

	public void cancelAllQueuedJobsAfter(final Long jobId) {

		queuedJobs.keySet().forEach((queuedJobId) -> {
			if (queuedJobId > jobId) {
				removeFromQueueInternal(queuedJobId);
			}
		});
	}

	public List<Map<String, Object>> listJobs () {

		final List<Map<String, Object>> jobInfoList = new LinkedList<>();

		activeJobs.values().forEach((ScheduledJob job) -> {
			addJobToList(jobInfoList, job);
		});

		jobIdQueue.forEach((Long jobId) -> {
			addJobToList(jobInfoList, queuedJobs.get(jobId));
		});

		return jobInfoList;
	}

	public Map<String, Object> jobInfo (final Long jobId) {

		if (activeJobs.containsKey(jobId)) {

			return activeJobs.get(jobId).getJobInfo();

		} else if (queuedJobs.containsKey(jobId)) {

			return queuedJobs.get(jobId).getJobInfo();
		}

		return null;
	}

	private void addJobToList (final List<Map<String, Object>> list, final ScheduledJob job) {
		list.add(job.getJobInfo());
	}

	protected void jobFinished (final ScheduledJob job) {

		activeJobs.remove(job.jobId());

		if (canRunMoreJobs()) {
			startNextJobInQueue();
		}

	}

	protected void jobAborted (final ScheduledJob job) {

		activeJobs.remove(job.jobId());

		if (canRunMoreJobs()) {
			startNextJobInQueue();
		}
	}

	private void appendToQueueInternal (final ScheduledJob job) {
		jobIdQueue.add(job.jobId());
		queuedJobs.put(job.jobId(), job);
	}

	private ScheduledJob removeFromQueueInternal (final Long jobId) {
		jobIdQueue.remove(jobId);
		return queuedJobs.remove(jobId);
	}

	private boolean canRunMoreJobs() {

		int multiThreadedJobCount = 0;

		// either one non-threaded job can run, or N threaded jobs
		for (final ScheduledJob job : activeJobs.values()) {

			if (!job.canRunMultiThreaded()) {

				return false;

			} else {

				multiThreadedJobCount++;
			}
		}

		return multiThreadedJobCount < 8;
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

