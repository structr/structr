/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.api.util.Iterables;
import org.structr.common.AccessControllable;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;

import java.util.Date;
import java.util.List;

/**
 *
 *
 */
public abstract class AbstractStructrFtpFile implements FtpFile {

	private static final Logger logger = LoggerFactory.getLogger(AbstractStructrFtpFile.class.getName());

	protected AbstractFile structrFile;

	protected SecurityContext securityContext = null;

	protected StructrFtpUser owner;
	protected String newPath = "/";


	public AbstractStructrFtpFile(final SecurityContext securityContext, final AbstractFile file) {
		this.structrFile     = file;
		this.securityContext = securityContext;
	}

	public AbstractStructrFtpFile(final String path, final StructrFtpUser user) {
		this.newPath = path;
		this.owner   = user;
		this.securityContext = user.getStructrUser().getSecurityContext();

	}

	@Override
	public String getAbsolutePath() {

		if (structrFile == null) {
			return newPath;
		}

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			String path = structrFile.getPath();

			tx.success();

			return path;

		} catch (FrameworkException fex) {
			logger.error("Error in getName() of abstract ftp file", fex);
		}

		return null;
	}

	@Override
	public String getName() {

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			String name = null;

			if (!("/").equals(newPath)) {

				name = newPath.contains("/") ? StringUtils.substringAfterLast(newPath, "/") : newPath;

			} else {

				if (structrFile != null) {
					name = structrFile.getName();
				}
			}

			tx.success();

			if (name != null) {
				return name;
			}

			if (structrFile != null) {
				return structrFile.getUuid();
			}

		} catch (FrameworkException fex) {
			logger.error("Error in getName() of abstract ftp file", fex);
		}

		return null;
	}

	@Override
	public boolean isHidden() {

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final boolean hidden = structrFile.isHidden();

			tx.success();

			return hidden;

		} catch (FrameworkException fex) {
			logger.error("Error in isHidden() of abstract ftp file", fex);
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

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final Principal owner = getOwner();

			String name = "";

			if (owner != null) {
				name = owner.getName();
			}

			tx.success();

			return name;

		} catch (FrameworkException fex) {
			logger.error("Error while getting owner name of " + this, fex);
		}

		return null;
	}

	@Override
	public String getGroupName() {

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final Principal owner = getOwner();
			String name = "";

			if (owner != null) {

				final List<Group> parents = Iterables.toList(owner.getParents());
				if (!parents.isEmpty()) {

					name = parents.get(0).getName();
				}
			}

			tx.success();
			return name;

		} catch (FrameworkException fex) {
			logger.error("Error while getting group name of " + this, fex);
		}

		return "";
	}

	@Override
	public int getLinkCount() {
		return 1;
	}

	@Override
	public long getLastModified() {

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final Date date = structrFile.getLastModifiedDate();

			tx.success();

			return date.getTime();

		} catch (FrameworkException fex) {
			logger.error("Error while last modified date of " + this, fex);
		}

		return 0L;
	}

	@Override
	public boolean setLastModified(final long l) {

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			structrFile.setLastModifiedDate(new Date(l));

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		return true;
	}

	@Override
	public boolean delete() {

		final App app = StructrApp.getInstance(securityContext);

		try (Tx tx = StructrApp.getInstance().tx()) {

			app.delete(structrFile);

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		return true;

	}

	@Override
	public boolean move(final FtpFile target) {

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			logger.info("move()");

			final AbstractStructrFtpFile targetFile = (AbstractStructrFtpFile) target;
			final String path = targetFile instanceof StructrFtpFile ? "/" : targetFile.getAbsolutePath();

			try {
				if (path.contains("/")) {

					String newParentPath    = StringUtils.substringBeforeLast(path, "/");
					NodeInterface newParent = FileHelper.getFileByAbsolutePath(securityContext, newParentPath);

					if (newParent != null && newParent.is("Folder")) {

						structrFile.setParent(newParent.as(Folder.class));

					} else {

						// Move to /
						structrFile.setParent(null);
					}

				}

				if (!("/".equals(path))) {
					final String newName = path.contains("/") ? StringUtils.substringAfterLast(path, "/") : path;
					structrFile.setName(newName);
				}

			} catch (FrameworkException ex) {
				logger.error("Could not move ftp file", ex);
				return false;
			}

			tx.success();

			return true;

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		return false;
	}

	private Principal getOwner() {

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			Principal owner = structrFile.as(AccessControllable.class).getOwnerNode();

			tx.success();

			return owner;

		} catch (FrameworkException fex) {
			logger.error("Error while getting owner of " + this, fex);
		}

		return null;
	}

	protected AbstractFile getStructrFile() {
		return structrFile;
	}

}
