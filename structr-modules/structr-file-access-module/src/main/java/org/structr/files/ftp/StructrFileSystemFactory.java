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

import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;

/**
 *
 *
 */
public class StructrFileSystemFactory implements FileSystemFactory {

	private static final Logger logger = LoggerFactory.getLogger(StructrFileSystemFactory.class.getName());

	@Override
	public FileSystemView createFileSystemView(final User user) throws FtpException {
		
		try (Tx tx = StructrApp.getInstance().tx()) {
		
			FileSystemView fileSystemView = new StructrFileSystemView(user);
			
			logger.debug("Created Structr File System View [user, homeDir, workingDir]: {}, {}, {}", user.getName(), fileSystemView.getHomeDirectory().getAbsolutePath(), fileSystemView.getWorkingDirectory().getAbsolutePath());
			
			tx.success();
			
			return fileSystemView;

		} catch (FrameworkException fex) {
			logger.error("Could not create file system view for user {}", user);

		}
		
		return null;
	}
}
