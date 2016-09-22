/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.maintenance.deploy;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import static org.structr.core.graph.NodeInterface.name;
import org.structr.core.graph.Tx;
import org.structr.dynamic.File;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;

/**
 *
 */
public class FileImportVisitor implements FileVisitor<Path> {

	private static final Logger logger      = Logger.getLogger(FileImportVisitor.class.getName());
	private SecurityContext securityContext = null;
	private Path basePath                   = null;
	private App app                         = null;

	public FileImportVisitor(final Path basePath) {

		this.securityContext    = SecurityContext.getSuperUserInstance();
		this.basePath           = basePath;
		this.app                = StructrApp.getInstance();
	}

	@Override
	public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {

		if (attrs.isDirectory()) {

			createFolder(file);

		} else if (attrs.isRegularFile()) {

			final String fileName = file.getFileName().toString();

			createFile(file, fileName);
		}

		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {

		logger.log(Level.WARNING, "Exception while importing file {0}: {1}", new Object[] { file.toString(), exc.getMessage() });
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	// ----- private methods -----
	private void createFolder(final Path file) {

		try (final Tx tx = app.tx()) {

			// create folder
			FileHelper.createFolderPath(securityContext, basePath.relativize(file).toString());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	private void createFile(final Path file, final String fileName) throws IOException {

		try (final Tx tx = app.tx()) {

			final Path parentPath   = basePath.relativize(file).getParent();
			final Folder parent     = parentPath != null ? FileHelper.createFolderPath(securityContext, parentPath.toString()) : null;
			final FileBase existing = app.nodeQuery(FileBase.class).and(FileBase.parent, parent).and(FileBase.name, fileName).getFirst();
			final String fullPath   = (parentPath != null ? "/" + parentPath.toString() : "") + "/" + fileName;

			if (existing != null) {

				final Long checksumOfExistingFile = existing.getChecksum();
				final Long checksumOfNewFile      = FileHelper.getChecksum(file.toFile());

				if (checksumOfExistingFile != null && checksumOfNewFile != null && checksumOfExistingFile.equals(checksumOfNewFile)) {

					logger.log(Level.INFO, "{0} is unmodified, skipping import.", fullPath);
					return;
				}

				// remove existing file first!
				app.delete(existing);
			}

			logger.log(Level.INFO, "Importing {0}..", fullPath);

			// close input stream
			try (final FileInputStream fis = new FileInputStream(file.toFile())) {

				// create file in folder structure
				final FileBase newFile   = FileHelper.createFile(securityContext, fis, null, File.class, fileName);
				final String contentType = newFile.getContentType();

				// modify file type according to content
				if (StringUtils.startsWith(contentType, "image") || ImageHelper.isImageType(newFile.getProperty(name))) {

					newFile.unlockSystemPropertiesOnce();
					newFile.setProperty(NodeInterface.type, Image.class.getSimpleName());
				}

				// move file to folder
				newFile.setProperty(FileBase.parent, parent);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}
}
