/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.files.ftp;

import org.apache.commons.lang3.StringUtils;
import org.apache.ftpserver.ftplet.FtpFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessControllable;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * This class is an equivalent to Java's native File class for file and folder
 * creation.
 *
 * It's a thin wrapper, implementing mkdir() and createOutputStream() only.
 *
 *
 */
public class FileOrFolder extends AbstractStructrFtpFile {

	private static final Logger logger = LoggerFactory.getLogger(FileOrFolder.class.getName());

	public FileOrFolder(final String newPath, final StructrFtpUser user) {
		super(newPath, user);
	}

	@Override
	public boolean isDirectory() {
		logger.error("isDirectory()");
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isFile() {
		logger.error("isFile()");
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public long getSize() {
		logger.error("getSize()");
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public InputStream createInputStream(long l) throws IOException {
		logger.error("createInputStream()");
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Object getPhysicalFile() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean mkdir() {

		final App app = StructrApp.getInstance(securityContext);
		try (final Tx tx = app.tx()) {

			logger.info("mkdir() Folder");

			NodeInterface existing = FileHelper.getFileByAbsolutePath(SecurityContext.getSuperUserInstance(), newPath);
			if (existing != null) {
				logger.warn("File {} already exists.", newPath);
				return false;
			}

			final NodeInterface parentFolder = FileHelper.getFileByAbsolutePath(securityContext, StringUtils.substringBeforeLast(newPath, "/"));
			final Traits traits              = Traits.of(StructrTraits.FOLDER);

			try {

				NodeInterface newFolder = app.command(CreateNodeCommand.class).execute(
					new NodeAttribute(traits.key(GraphObjectTraitDefinition.TYPE_PROPERTY),   StructrTraits.FOLDER),
					new NodeAttribute(traits.key(NodeInterfaceTraitDefinition.OWNER_PROPERTY),  owner.getStructrUser()),
					new NodeAttribute(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),   getName()),
					new NodeAttribute(traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY), parentFolder)
				);

			} catch (FrameworkException ex) {
				logger.error("", ex);
				return false;
			}

			tx.success();

			return true;

		} catch (FrameworkException ex) {
			logger.error("", ex);
			return false;
		}
	}

	@Override
	public List<FtpFile> listFiles() {
		logger.error("listFiles()");
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public OutputStream createOutputStream(final long l) throws IOException {

		try (Tx tx = StructrApp.getInstance().tx()) {

			if (structrFile == null) {

				final NodeInterface parentFolder = FileHelper.getFileByAbsolutePath(securityContext, StringUtils.substringBeforeLast(newPath, "/"));

				try {
					structrFile = FileHelper.createFile(securityContext, new byte[0], null, StructrTraits.FILE, getName(), false).as(AbstractFile.class);

					structrFile.as(AccessControllable.class).setOwner(owner.getStructrUser());

					if (parentFolder != null) {
						structrFile.setParent(parentFolder.as(Folder.class));
					}

				} catch (FrameworkException ex) {
					logger.error("", ex);
					return null;
				}
			}

			tx.success();

			return structrFile.as(File.class).getOutputStream();

		} catch (FrameworkException fex) {
			logger.error(null, fex);
		}

		return null;
	}

}
