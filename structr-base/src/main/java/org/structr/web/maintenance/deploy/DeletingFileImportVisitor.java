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
package org.structr.web.maintenance.deploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 *
 */
public abstract class DeletingFileImportVisitor extends FileImportVisitor {

	private static final Logger logger = LoggerFactory.getLogger(DeletingFileImportVisitor.class.getName());

	private int count      = 0;
	private long totalTime = 0;
	private int batchCount = 0;

	private int batchSize;
	private long startTime;

	private List<String> parents;

	public DeletingFileImportVisitor(final SecurityContext securityContext, final Path basePath, final Map<String, Object> metadata, final int batchSize, final List<String> requiredParentsPaths) {

		super(securityContext, basePath, metadata);

		this.batchSize = batchSize;
		this.startTime = System.currentTimeMillis();

		this.parents = requiredParentsPaths;
	}

	protected abstract void sendProgressUpdateNotification(final int count, final long duration, final long meanDuration);

	// ----- private methods -----
	@Override
	protected void createFolder(final Path folderObj) {

		final String folderPath = harmonizeFileSeparators("/", basePath.relativize(folderObj).toString());

		// only delete folder if it was in the export set and NOT as a required parent (meaning that it was deliberately exported and not just because it was required because a child was exported)
		final boolean folderWasOnlyExportedAsParent = parents.contains(folderPath);

		if (!folderWasOnlyExportedAsParent) {

			try (final Tx tx = app.tx()) {

				tx.disableChangelog();

				final NodeInterface folder = getExistingFolder(folderPath);
				if (folder != null) {

					this.folderCache.remove(folderPath);

					app.delete(folder);
				}

			} catch (Exception ex) {

				logger.error("Error occurred while deleting folder before import: " + folderObj, ex);
			}
		}

		super.createFolder(folderObj);

		objectProcessed();
	}

	@Override
	protected void createFile(final Path path, final String fileName) throws IOException {

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			final String fullPath                   = harmonizeFileSeparators("/", basePath.relativize(path).toString());
			final Map<String, Object> rawProperties = getRawPropertiesForFileOrFolder(fullPath);

			if (rawProperties != null) {

				final String id = (String)rawProperties.get("id");

				if (id != null) {

					final NodeInterface existingFile = app.getNodeById(id);

					if (existingFile != null) {

						app.delete(existingFile);
					}
				}
			}

			tx.success();

		} catch (FrameworkException ex) {

			logger.error("Error occurred while deleting file before import: " + fileName, ex);
		}

		super.createFile(path, fileName);

		objectProcessed();
	}

	private void objectProcessed() {

		count++;

		if (count % batchSize == 0) {

			batchCount++;

			final long endTime = System.currentTimeMillis();

			final long duration = endTime - startTime;
			totalTime += duration;

			startTime = endTime;

			sendProgressUpdateNotification(count, duration, totalTime / batchCount);
		}
	}

	public void finished() {

		if (count % batchSize != 0) {

			batchCount++;

			final long endTime = System.currentTimeMillis();

			final long duration = endTime - startTime;
			totalTime += duration;

			sendProgressUpdateNotification(count, duration, totalTime / batchCount);
		}
	}
}
