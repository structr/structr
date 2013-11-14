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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.apache.ftpserver.ftplet.User;
import org.structr.core.auth.AuthHelper;
import org.structr.core.entity.AbstractUser;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

/**
 *
 * @author Axel Morgner
 */
public class StructrFileSystemView implements FileSystemView {

	private static final Logger logger = Logger.getLogger(StructrFileSystemView.class.getName());
	private final StructrFtpUser user;
	
	private String workingDir = "/";

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
//		org.structr.web.entity.User structrUser = (org.structr.web.entity.User) AuthHelper.getPrincipalForCredential(AbstractUser.name, user.getName());
//		
//		Folder workingDir = structrUser.getProperty(org.structr.web.entity.User.workingDirectory);
//		if (workingDir == null) {
//			workingDir = structrUser.getProperty(org.structr.web.entity.User.homeDirectory);
//		}
		AbstractFile structrWorkingDir = FileHelper.getFileByAbsolutePath(workingDir);
		if (structrWorkingDir == null || structrWorkingDir instanceof File) {
			return new StructrFtpFolder(null);
		}
		
		return new StructrFtpFolder((Folder) structrWorkingDir);
	}

	@Override
	public boolean changeWorkingDirectory(String requestedPath) throws FtpException {
		
		//final org.structr.web.entity.User structrUser = (org.structr.web.entity.User) AuthHelper.getPrincipalForCredential(AbstractUser.name, user.getName());
		final StructrFtpFolder newWorkingDirectory = (StructrFtpFolder) getFile(requestedPath);
		
		workingDir = newWorkingDirectory.getAbsolutePath();
		
//		try {
//			Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {
//
//				@Override
//				public Object execute() throws FrameworkException {
//
//					structrUser.setProperty(org.structr.web.entity.User.workingDirectory, (Folder) newWorkingDirectory.getStructrFile());
//					return null;
//				}
//
//			});
//		} catch (FrameworkException ex) {
//			logger.log(Level.SEVERE, null, ex);
//			return false;
//		}

		return true;
		
	}

	@Override
	public FtpFile getFile(String requestedPath) throws FtpException {

		logger.log(Level.INFO, "Requested path: {0}", requestedPath);

		
		if (StringUtils.isBlank(requestedPath) || "/".equals(requestedPath)) {
			return getHomeDirectory();
		}
		
		StructrFtpFolder cur = (StructrFtpFolder) getWorkingDirectory();
		
		if (".".equals(requestedPath) || "./".equals(requestedPath)) {
			return cur;
		}

		if ("..".equals(requestedPath) || "../".equals(requestedPath)) {
			return new StructrFtpFolder(cur.getStructrFile().getProperty(AbstractFile.parent));
		}

		// If relative path requested, prepend base path
		if (!requestedPath.startsWith("/")) {

			String basePath = cur.getAbsolutePath();
			
			logger.log(Level.INFO, "Base path: {0}", basePath);

			while (requestedPath.startsWith("..")) {
				requestedPath = StringUtils.stripStart(StringUtils.stripStart(requestedPath, ".."), "/");
				basePath = StringUtils.substringBeforeLast(basePath, "/");
			}
		
			requestedPath = StringUtils.stripEnd(basePath.equals("/") ? "/".concat(requestedPath) : basePath.concat("/").concat(requestedPath), "/");

			logger.log(Level.INFO, "Base path: {0}, requestedPath: {1}", new Object[] {basePath, requestedPath});
		
		}

		AbstractFile file = FileHelper.getFileByAbsolutePath(requestedPath);
		
		if (file != null) {
			
			if (file instanceof Folder) {
				return new StructrFtpFolder((Folder) file);
			} else {
				return new StructrFtpFile((File) file);
			}
		}
		
//		List<FtpFile> files = cur.listFiles();
//		for (FtpFile file : files) {
//
//			String path = file.getAbsolutePath();
//
//			if (path.equals(requestedPath)) {
//
//				String type = file.isDirectory() ? "Directory" : (file.isFile() ? "File" : "unknown");
//				logger.log(Level.INFO, "{0} found: {1}", new Object[] {type, file.getAbsolutePath()});
//
//				return file;
//			}
//
//		}
		
		logger.log(Level.WARNING, "No existing file found: {0}", requestedPath);
		
		return new FileOrFolder(requestedPath, user);
		
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
	