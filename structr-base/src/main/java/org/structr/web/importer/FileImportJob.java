/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.ContextStore;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.scheduler.ScheduledJob;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.File;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

abstract class FileImportJob extends ScheduledJob {

	private static final Logger logger = LoggerFactory.getLogger(FileImportJob.class);

	protected String fileUuid;
	protected String filePath;
	protected String fileName;
	protected Long fileSize;
	protected Integer processedChunks  = 0;
	protected Integer processedObjects = 0;

	public FileImportJob (final File file, final Principal user, final Map<String, Object> configuration, final ContextStore ctxStore) {

		super(file.getUuid(), user, configuration, ctxStore);

		this.fileUuid = file.getUuid();
		this.filePath = file.getPath();
		this.fileName = file.getName();
		this.fileSize = StorageProviderFactory.getStorageProvider(file).size();
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

	public Long getFileSize () {
		return fileSize;
	}

	public Integer getProcessedChunks () {
		return processedChunks;
	}

	public Integer getProcessedObjects () {
		return processedObjects;
	}

	@Override
	public Map<String, Object> getStatusData (final JobStatusMessageSubtype subtype) {

		final Map<String, Object> data = new LinkedHashMap();

		data.put("jobId",      jobId());
		data.put("type",       getJobStatusType());
		data.put("jobtype",    getJobType());
		data.put("subtype",    subtype);
		data.put("username",   username);

		data.put("filename",   fileName);
		data.put("filepath",   filePath);

		return data;
	}

	@Override
	public Map<String, Object> getJobInfo () {

		final LinkedHashMap<String, Object> jobInfo = new LinkedHashMap<>();

		jobInfo.put("jobId",            jobId());
		jobInfo.put("jobtype",          getJobType());
		jobInfo.put("username",         getUsername());
		jobInfo.put("status",           getCurrentStatus());

		jobInfo.put("fileUuid",         getFileUuid());
		jobInfo.put("filepath",         getFilePath());
		jobInfo.put("filesize",         getFileSize());
		jobInfo.put("processedChunks",  getProcessedChunks());
		jobInfo.put("processedObjects", getProcessedObjects());

		if (getEncounteredException() != null) {

			final HashMap exceptionMap = new HashMap();
			exceptionMap.put("message", getEncounteredException().getMessage());
			exceptionMap.put("cause", getEncounteredException().getCause());
			exceptionMap.put("stacktrace", ExceptionUtils.getStackTrace(getEncounteredException()));
			jobInfo.put("exception", exceptionMap);
		}

		return jobInfo;
	}

	protected void chunkFinished(final long chunkStartTime, final int currentChunkNo, final int chunkSize, final int overallCount, final int ignoreCount) {

		processedChunks                   = currentChunkNo;
		processedObjects                  = overallCount;

		final long duration               = System.currentTimeMillis() - chunkStartTime;
		final DecimalFormat decimalFormat = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		final String formattedDuration    = decimalFormat.format((duration / 1000.0)) + "s";
		final String objectsPerSecond     = decimalFormat.format(chunkSize / (duration / 1000.0));

		logger.info("{}: Committing chunk {}. (Objects: {} - Time: {} - Objects/s: {} - Objects overall: {})", getJobType(), currentChunkNo, chunkSize, formattedDuration, objectsPerSecond, overallCount);

		final Map<String, Object> data = getStatusData(JobStatusMessageSubtype.CHUNK);
		data.put("currentChunkNo",   currentChunkNo);
		data.put("objectsCreated",   chunkSize);
		data.put("objectsIgnored",   ignoreCount);
		data.put("duration",         formattedDuration);
		data.put("objectsPerSecond", objectsPerSecond);
		TransactionCommand.simpleBroadcastGenericMessage(data);

	}

	protected void importFinished(final long startTime, final int objectCount, final int ignoreCount) {

		processedObjects                  = objectCount;

		final long duration               = System.currentTimeMillis() - startTime;
		final DecimalFormat decimalFormat = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		final String formattedDuration    = decimalFormat.format((duration / 1000.0)) + "s";
		final String objectsPerSecond     = decimalFormat.format(objectCount / (duration / 1000.0));

		logger.info("{}: Finished importing file '{}' (Objects overall: {} - Time: {} - Objects/s: {})", getJobType(), filePath, objectCount, formattedDuration, objectsPerSecond);

		final Map<String, Object> data = getStatusData(JobStatusMessageSubtype.END);
		data.put("duration",         formattedDuration);
		data.put("objectsCreated",   objectCount);
		data.put("objectsPerSecond", objectsPerSecond);
		data.put("objectsIgnored",   ignoreCount);
		TransactionCommand.simpleBroadcastGenericMessage(data);

	}

	@Override
	public void reportException(Exception ex) {

		setEncounteredException(ex);

		final Map<String, Object> data = new LinkedHashMap();
		data.put("type",       getJobExceptionMessageType());
		data.put("importtype", getJobType());
		data.put("filename",   fileName);
		data.put("filepath",   filePath);
		data.put("username",   username);
		TransactionCommand.simpleBroadcastException(ex, data, true);
	}

	protected InputStream getFileInputStream(final SecurityContext ctx) {

		final App app = StructrApp.getInstance(ctx);

		InputStream is = null;

		try (final Tx tx = app.tx()) {

			final File file = app.get(File.class, fileUuid);
			is              = file.getInputStream();

			tx.success();

		} catch (FrameworkException fex) {

			final Map<String, Object> data = new LinkedHashMap();
			data.put("type",     getJobExceptionMessageType());
			data.put("filename", fileName);
			data.put("filepath", filePath);
			data.put("username", ctx.getUser(false).getName());
			TransactionCommand.simpleBroadcastException(fex, data, true);

		}

		return is;
	}
}
