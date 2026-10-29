/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.extraction.processing;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.structr.common.ContextStore;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.scheduler.ScheduledJob;
import org.structr.core.traits.StructrTraits;
import org.structr.extraction.api.processing.ExtractionStatus;
import org.structr.extraction.api.processing.ProcessingResult;
import org.structr.extraction.filesystem.FolderTreeWalker;
import org.structr.extraction.options.ContentOptionsResolver;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts a list of files, one after the other, in a named job queue of its own, so that long
 * extractions never hold up the default queue. The job can be paused and aborted between files, and
 * reports its progress to the job queue panel.
 */
public class ContentExtractionJob extends ScheduledJob {

	private final List<String> uuids;
	private final Map<String, Object> overrides;
	private final boolean force;
	private final boolean automatic;

	private List<String> fileUuids = List.of();

	private int processed = 0;
	private int done      = 0;
	private int unchanged = 0;
	private int skipped   = 0;
	private int failed    = 0;

	/**
	 * @param user      the user who started the extraction, or null for automatic extraction
	 * @param fileUuids the files to extract
	 * @param overrides per-call options (see {@link ContentOptionsResolver#OPTIONS})
	 * @param force     whether to extract files even if content and options are unchanged
	 * @param queueName the job queue
	 */
	public ContentExtractionJob(final Principal user, final List<String> fileUuids, final Map<String, Object> overrides, final boolean force, final String queueName) {

		this("Content extraction of " + fileUuids.size() + (fileUuids.size() == 1 ? " file" : " files"), user, fileUuids, overrides, force, false, queueName);
	}

	private ContentExtractionJob(final String name, final Principal user, final List<String> uuids, final Map<String, Object> overrides, final boolean force, final boolean automatic, final String queueName) {

		super(name, user, Map.of(), new ContextStore(), queueName);

		this.uuids     = List.copyOf(uuids);
		this.overrides = Map.copyOf(overrides);
		this.force     = force;
		this.automatic = automatic;
	}

	/**
	 * Automatic extraction after content changes and moves: the given files, and the files below the
	 * given folders, as far as they are marked for automatic extraction when the job runs.
	 */
	public static ContentExtractionJob automatic(final List<String> fileAndFolderUuids, final String queueName) {

		return new ContentExtractionJob("Automatic content extraction", null, fileAndFolderUuids, Map.of(), false, true, queueName);
	}

	@Override
	public boolean runInitialChecks() {

		return true;
	}

	@Override
	public Runnable getRunnable() {

		return () -> {

			try {

				// before BEGIN, so that the first status message already carries the number of files
				fileUuids = automatic ? markedFiles(uuids) : uuids;

				// an automatic job runs after every upload and move into any folder; most find nothing to do
				// and should not show up as a job at all
				if (automatic && fileUuids.isEmpty()) {

					return;
				}

				reportBegin();

				for (final String fileUuid : fileUuids) {

					shouldPause();

					if (shouldAbort()) {

						return;
					}

					final ProcessingResult result = ContentExtractionProcessor.process(fileUuid, overrides, force);

					count(result);
					ContentExtractionProcessor.notifyExtracted(result);

					TransactionCommand.simpleBroadcastGenericMessage(getStatusData(JobStatusMessageSubtype.CHUNK));
				}

				reportFinished();

			} catch (final FrameworkException | RuntimeException ex) {

				reportException(ex);

			} finally {

				jobFinished();
			}
		};
	}

	@Override
	public String getJobType() {

		return "CONTENT_EXTRACTION";
	}

	@Override
	public String getJobStatusType() {

		return "CONTENT_EXTRACTION_STATUS";
	}

	@Override
	public String getJobExceptionMessageType() {

		return "CONTENT_EXTRACTION_EXCEPTION";
	}

	@Override
	public void reportException(final Exception ex) {

		setEncounteredException(ex);

		final Map<String, Object> data = new LinkedHashMap<>();

		data.put("type",     getJobExceptionMessageType());
		data.put("jobtype",  getJobType());
		data.put("username", getUsername());

		TransactionCommand.simpleBroadcastException(ex, data, true);
	}

	@Override
	public Map<String, Object> getStatusData(final JobStatusMessageSubtype subtype) {

		final Map<String, Object> data = new LinkedHashMap<>();

		data.put("jobId",    jobId());
		data.put("type",     getJobStatusType());
		data.put("jobtype",  getJobType());
		data.put("subtype",  subtype);
		data.put("username", getUsername());
		data.put("jobName",  jobName);
		data.put("queue",    getQueueName());

		addProgress(data);

		return data;
	}

	@Override
	public Map<String, Object> getJobInfo() {

		final Map<String, Object> info = new LinkedHashMap<>();

		info.put("jobId",    jobId());
		info.put("jobtype",  getJobType());
		info.put("username", getUsername());
		info.put("status",   getCurrentStatus());
		info.put("jobName",  jobName);
		info.put("queue",    getQueueName());

		addProgress(info);

		if (getEncounteredException() != null) {

			final Map<String, Object> exception = new HashMap<>();

			exception.put("message",    getEncounteredException().getMessage());
			exception.put("stacktrace", ExceptionUtils.getStackTrace(getEncounteredException()));

			info.put("exception", exception);
		}

		return info;
	}

	// ----- private methods -----
	/**
	 * The files among the given nodes, and the files below the given folders, that are marked for
	 * automatic extraction.
	 */
	private static List<String> markedFiles(final List<String> uuids) throws FrameworkException {

		final App app            = StructrApp.getInstance(SecurityContext.getSuperUserInstance());
		final List<String> files = new ArrayList<>();

		try (final Tx tx = app.tx()) {

			for (final String uuid : uuids) {

				final NodeInterface node = app.getNodeById(uuid);
				if (node == null) {

					continue;
				}

				if (node.is(StructrTraits.FILE)) {

					addIfMarked(node.as(File.class), files);

				} else if (node.is(StructrTraits.FOLDER)) {

					for (final File file : FolderTreeWalker.files(node.as(Folder.class), true)) {

						addIfMarked(file, files);
					}
				}
			}

			tx.success();
		}

		return files;
	}

	private static void addIfMarked(final File file, final List<String> files) {

		if (ContentOptionsResolver.shouldExtract(file).extract() && !files.contains(file.getUuid())) {

			files.add(file.getUuid());
		}
	}

	private void count(final ProcessingResult result) {

		processed++;

		if (result.status() == null) {

			unchanged++;

		} else if (result.status() == ExtractionStatus.DONE) {

			done++;

		} else if (result.status() == ExtractionStatus.SKIPPED) {

			skipped++;

		} else {

			failed++;
		}
	}

	private void addProgress(final Map<String, Object> data) {

		data.put("files",     fileUuids.size());
		data.put("processed", processed);
		data.put("done",      done);
		data.put("unchanged", unchanged);
		data.put("skipped",   skipped);
		data.put("failed",    failed);
	}
}
