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
package org.structr.files.ftp;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.ftpserver.ftplet.FtpFile;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractUser;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.dynamic.File;
import org.structr.web.entity.Folder;

/**
 *
 *
 */
public abstract class AbstractStructrFtpFile implements FtpFile {

	private static final Logger logger = Logger.getLogger(AbstractStructrFtpFile.class.getName());

	protected AbstractFile structrFile;

	protected StructrFtpUser owner;
	protected String newPath = "/";

	public AbstractStructrFtpFile(final AbstractFile file) {
		structrFile = file;
	}

	public AbstractStructrFtpFile(final String path, final StructrFtpUser user) {
		newPath = path;
		owner = user;
	}

	@Override
	public String getAbsolutePath() {

		if (structrFile == null) {
			return newPath;
		}
		try (Tx tx = StructrApp.getInstance().tx()) {
			String path = FileHelper.getFolderPath(structrFile);
			tx.success();
			return path;
		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error in getName() of abstract ftp file", fex);
		}
		return null;
	}

	@Override
	public String getName() {
		try (Tx tx = StructrApp.getInstance().tx()) {

			String name = null;
			if (!("/").equals(newPath)) {
				name = newPath.contains("/") ? StringUtils.substringAfterLast(newPath, "/") : newPath;
			} else {
				if (structrFile != null) {
					name = structrFile.getProperty(File.name);
				}
			}

			tx.success();
			return name == null ? structrFile.getUuid() : name;

		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error in getName() of abstract ftp file", fex);
		}
		return null;
	}

	@Override
	public boolean isHidden() {
		try (Tx tx = StructrApp.getInstance().tx()) {
			final boolean hidden = structrFile.getProperty(File.hidden);
			tx.success();
			return hidden;

		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error in isHidden() of abstract ftp file", fex);
		}
		return true;
	}

	@Override
	public boolean doesExist() {
		return structrFile != null;
	}

	@Override
	public boolean isReadable() {
		return true;
	}

	@Override
	public boolean isWritable() {
		return true;
	}

	@Override
	public boolean isRemovable() {
		return true;
	}

	@Override
	public String getOwnerName() {
		try (Tx tx = StructrApp.getInstance().tx()) {
			final Principal owner = getOwner();
			String name = "";
			if (owner != null) {
				name = owner.getProperty(AbstractUser.name);
			}
			tx.success();
			return name;
		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error while getting owner name of " + this, fex);
		}
		return null;
	}

	@Override
	public String getGroupName() {

		try (Tx tx = StructrApp.getInstance().tx()) {

			final Principal owner = getOwner();
			String name = "";

			if (owner != null) {
				List<Principal> parents = owner.getParents();
				if (!parents.isEmpty()) {

					name = parents.get(0).getProperty(AbstractNode.name);

				}
			}

			tx.success();
			return name;

		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error while getting group name of " + this, fex);
		}

		return "";
	}

	@Override
	public int getLinkCount() {
		return 1;
	}

	@Override
	public long getLastModified() {
		try (Tx tx = StructrApp.getInstance().tx()) {
			final Date date = structrFile.getProperty(AbstractFile.lastModifiedDate);
			tx.success();
			return date.getTime();
		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error while last modified date of " + this, fex);
		}
		return 0L;
	}

	@Override
	public boolean setLastModified(final long l) {
		try (Tx tx = StructrApp.getInstance().tx()) {
			structrFile.setProperty(AbstractFile.lastModifiedDate, new Date(l));
			tx.success();
		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return true;
	}

	@Override
	public boolean delete() {

		final App app = StructrApp.getInstance();

		try (Tx tx = StructrApp.getInstance().tx()) {
			app.delete(structrFile);
			tx.success();
		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return true;

	}

	@Override
	public boolean move(final FtpFile target) {

		try (Tx tx = StructrApp.getInstance().tx()) {

			logger.log(Level.INFO, "move()");

			final AbstractStructrFtpFile targetFile = (AbstractStructrFtpFile) target;
			final String path = targetFile instanceof StructrFtpFile ? "/" : targetFile.getAbsolutePath();

			try {
				if (path.contains("/")) {

					String newParentPath = StringUtils.substringBeforeLast(path, "/");
					AbstractFile newParent = FileHelper.getFileByAbsolutePath(SecurityContext.getSuperUserInstance(), newParentPath);

					if (newParent != null && newParent instanceof Folder) {

						Folder newParentFolder = (Folder) newParent;
						structrFile.setProperty(AbstractFile.parent, newParentFolder);

					} else {

						// Move to /
						structrFile.setProperty(AbstractFile.parent, null);

					}

				}

				if (!("/".equals(path))) {
					final String newName = path.contains("/") ? StringUtils.substringAfterLast(path, "/") : path;
					structrFile.setProperty(AbstractNode.name, newName);
				}

			} catch (FrameworkException ex) {
				logger.log(Level.SEVERE, "Could not move ftp file", ex);
				return false;
			}

			tx.success();

			return true;
		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return false;
	}

	private Principal getOwner() {
		try (Tx tx = StructrApp.getInstance().tx()) {
			Principal owner = structrFile.getProperty(File.owner);
			tx.success();
			return owner;
		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error while getting owner of " + this, fex);
		}
		return null;
	}

	protected AbstractFile getStructrFile() {
		return structrFile;
	}

}
