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
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import static org.structr.core.graph.NodeInterface.name;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.dynamic.File;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;

/**
 *
 */
public class FileImportVisitor implements FileVisitor<Path> {

	private static final Logger logger      = Logger.getLogger(FileImportVisitor.class.getName());
	private Map<String, Object> config      = null;
	private SecurityContext securityContext = null;
	private Path basePath                   = null;
	private App app                         = null;

	public FileImportVisitor(final Path basePath, final Map<String, Object> config) {

		this.securityContext = SecurityContext.getSuperUserInstance();
		this.basePath        = basePath;
		this.config          = config;
		this.app             = StructrApp.getInstance();
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

		try (final Tx tx = app.tx(false, false, false)) {

			// create folder
			final Folder folder = createFolders(basePath.relativize(file));
			if (folder != null) {

				// set properties from files.json
				final PropertyMap properties = getPropertiesForFileOrFolder(folder.getPath());
				if (properties != null) {

					folder.setProperties(securityContext, properties);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	private void createFile(final Path file, final String fileName) throws IOException {

		try (final Tx tx = app.tx(false, false, false)) {

			final Path parentPath   = basePath.relativize(file).getParent();
			final Folder parent     = createFolders(parentPath);
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

				// set properties from files.json
				final PropertyMap properties = getPropertiesForFileOrFolder(newFile.getPath());
				if (properties != null) {

					newFile.setProperties(securityContext, properties);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	private PropertyMap getPropertiesForFileOrFolder(final String path) throws FrameworkException {

		final Object data = config.get(path);
		if (data != null && data instanceof Map) {

			return PropertyMap.inputTypeToJavaType(SecurityContext.getSuperUserInstance(), AbstractFile.class, (Map<String, Object>)data);
		}

		return null;
	}

	private Folder createFolders(final Path folder) throws FrameworkException {

		if (folder != null) {

			final App app  = StructrApp.getInstance();
			Folder current = null;
			Folder parent  = null;

			for (final Iterator<Path> it = folder.iterator(); it.hasNext(); ) {

				final Path part   = it.next();
				final String name = part.toString();

				current = app.nodeQuery(Folder.class).andName(name).and(FileBase.parent, parent).getFirst();
				if (current == null) {

					current = app.create(Folder.class,
						new NodeAttribute(AbstractNode.name, name),
						new NodeAttribute(Folder.parent, parent)
					);
				}

				if (current != null) {

					// set properties from files.json
					final PropertyMap properties = getPropertiesForFileOrFolder(current.getPath());
					if (properties != null) {

						current.setProperties(securityContext, properties);
					}
				}

				// make next folder child of new one
				parent = current;
			}

			return current;
		}

		return null;
	}
}
