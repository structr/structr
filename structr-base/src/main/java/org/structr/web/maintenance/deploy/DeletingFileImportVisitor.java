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
package org.structr.web.maintenance.deploy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.web.entity.Folder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 *
 */
public class DeletingFileImportVisitor extends FileImportVisitor {

	private static final Logger logger = LoggerFactory.getLogger(DeletingFileImportVisitor.class.getName());

	public DeletingFileImportVisitor(final SecurityContext securityContext, final Path basePath, final Map<String, Object> metadata) {

		super(securityContext, basePath, metadata);
	}

	// ----- private methods -----
	@Override
	protected void createFolder(final Path folderObj) {

		final String folderPath = harmonizeFileSeparators("/", basePath.relativize(folderObj).toString());

		try (final Tx tx = app.tx()) {

			tx.disableChangelog();

			final Folder folder = getExistingFolder(folderPath);
			if (folder != null) {

				this.folderCache.remove(folderPath);

				app.delete(folder);
			}

		} catch (Exception ex) {

			logger.error("Error occurred while deleting folder before import: " + folderObj, ex);
		}

		super.createFolder(folderObj);
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
	}
}
