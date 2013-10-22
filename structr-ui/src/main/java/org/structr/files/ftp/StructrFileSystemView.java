/*
 *  Copyright (C) 2013 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.files.ftp;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.auth.AuthHelper;
import org.structr.core.entity.AbstractUser;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;

/**
 *
 * @author Axel Morgner
 */
public class StructrFileSystemView implements FileSystemView {

	private static final Logger logger = Logger.getLogger(StructrFileSystemView.class.getName());
	private final StructrFtpUser user;

	public StructrFileSystemView(final User user) {
		org.structr.web.entity.User structrUser = (org.structr.web.entity.User) AuthHelper.getPrincipalForCredential(AbstractUser.name, user.getName());
		this.user = new StructrFtpUser(structrUser);
	}

	@Override
	public FtpFile getHomeDirectory() throws FtpException {
		org.structr.web.entity.User structrUser = (org.structr.web.entity.User) AuthHelper.getPrincipalForCredential(AbstractUser.name, user.getName());
		return new StructrFtpFolder(structrUser.getProperty(org.structr.web.entity.User.homeDirectory));
	}

	@Override
	public FtpFile getWorkingDirectory() throws FtpException {
		org.structr.web.entity.User structrUser = (org.structr.web.entity.User) AuthHelper.getPrincipalForCredential(AbstractUser.name, user.getName());
		
		Folder workingDir = structrUser.getProperty(org.structr.web.entity.User.workingDirectory);
		if (workingDir == null) {
			workingDir = structrUser.getProperty(org.structr.web.entity.User.homeDirectory);
		}
		
		return new StructrFtpFolder(workingDir);
	}

	@Override
	public boolean changeWorkingDirectory(String requestedPath) throws FtpException {
		
		final org.structr.web.entity.User structrUser = (org.structr.web.entity.User) AuthHelper.getPrincipalForCredential(AbstractUser.name, user.getName());
		final StructrFtpFolder newWorkingDirectory = (StructrFtpFolder) getFile(requestedPath);
		
		try {
			Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					structrUser.setProperty(org.structr.web.entity.User.workingDirectory, (Folder) newWorkingDirectory.getStructrFile());
					return null;
				}

			});
		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);
			return false;
		}

		return true;
		
	}

	@Override
	public FtpFile getFile(String requestedPath) throws FtpException {

		logger.log(Level.INFO, "Requested path: {0}", requestedPath);

		if (requestedPath.equals("/")) {
			return getHomeDirectory();
		}
		
		StructrFtpFolder cur = (StructrFtpFolder) getWorkingDirectory();
		if (requestedPath.equals("./")) {
			return cur;
		}

		if (requestedPath.equals("..")) {
			return new StructrFtpFolder(cur.getStructrFile().getProperty(AbstractFile.parent));
		}
		
		String curPath = cur.getAbsolutePath();
		
		List<FtpFile> files = cur.listFiles();
		for (FtpFile file : files) {

			String path = file.getAbsolutePath();
			
			if ("/".equals(curPath)) {
				
				// We're on root level
				if (file.getName().equals(requestedPath)) {
					return file;
				}
			}
			
			if (path.equals(curPath.concat(requestedPath))) {
				
				String type = file.isDirectory() ? "Directory" : (file.isFile() ? "File" : "unknown");
				logger.log(Level.INFO, "{0} found: {1}", new Object[] {type, file.getAbsolutePath()});
				
				return file;
			}

		}
		
		//return new StructrFtpFolder(curPath.concat("/").concat(requestedPath), user);
		
		logger.log(Level.WARNING, "File not found: {0}", requestedPath);
		
		throw new FtpException("File not found: " + requestedPath);
		
	}

	@Override
	public boolean isRandomAccessible() throws FtpException {
		logger.log(Level.INFO, "isRandomAccessible(), returning true");
		return true;
	}

	@Override
	public void dispose() {
		logger.log(Level.INFO, "dispose() does nothing");
	}



}
	