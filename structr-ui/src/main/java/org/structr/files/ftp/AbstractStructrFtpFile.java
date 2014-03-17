/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
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
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractUser;
import org.structr.core.entity.Principal;
import org.structr.core.graph.DeleteNodeCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

/**
 *
 * @author Axel Morgner
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
		
		return FileHelper.getFolderPath(structrFile);
	}

	@Override
	public String getName() {
		String name = structrFile != null ? structrFile.getProperty(File.name) : 
			(newPath.contains("/") ? StringUtils.substringAfterLast(newPath, "/") : newPath);
		
		return name == null ? structrFile.getUuid() : name;
	}

	@Override
	public boolean isHidden() {
		return structrFile.getProperty(File.hidden);
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
		Principal owner = getOwner();
		return owner != null ? owner.getProperty(AbstractUser.name) : "";
	}

	@Override
	public String getGroupName() {
		
		Principal owner = getOwner();
		
		if (owner != null) {
			List<Principal> parents = owner.getParents();
			if (!parents.isEmpty()) {
				
				return parents.get(0).getProperty(AbstractNode.name);
				
			}
		}
		
		return "";
	}

	@Override
	public int getLinkCount() {
		return 1;
	}

	@Override
	public long getLastModified() {
		return structrFile.getProperty(AbstractFile.lastModifiedDate).getTime();
	}

	@Override
	public boolean setLastModified(final long l) {

		final App app = StructrApp.getInstance();

		try {
			app.beginTx();
			structrFile.setProperty(AbstractFile.lastModifiedDate, new Date(l));
			app.commitTx();

		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);

		} finally {
			app.finishTx();
		}

		return true;
	}

	@Override
	public boolean delete() {

		final App app = StructrApp.getInstance();

		try {
			app.beginTx();
			app.delete(structrFile);
			app.commitTx();

		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);

		} finally {
			app.finishTx();
		}

		return true;
		
	}
	

	@Override
	public boolean move(final FtpFile target) {
	
		logger.log(Level.INFO, "move()");
		
		final AbstractStructrFtpFile targetFile = (AbstractStructrFtpFile) target;
		final String path                       = targetFile instanceof StructrFtpFile ? "/" : targetFile.getAbsolutePath();
		final App app                           = StructrApp.getInstance();

		try {
			app.beginTx();

			if (path.contains("/")) {

				String newParentPath = StringUtils.substringBeforeLast(path, "/");
				AbstractFile newParent = FileHelper.getFileByAbsolutePath(newParentPath);

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
			
			app.commitTx();

		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, "Could not move ftp file", ex);
			return false;

		} finally {
			app.finishTx();
		}

		return true;
	}
	
	private Principal getOwner() {
		return structrFile.getProperty(File.owner);
	}

	protected AbstractFile getStructrFile() {
		return structrFile;
	}

}