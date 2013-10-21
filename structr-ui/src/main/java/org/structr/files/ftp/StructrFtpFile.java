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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.ftpserver.ftplet.FtpFile;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;
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
public class StructrFtpFile extends AbstractStructrFtpFile {

	private static final Logger logger = Logger.getLogger(StructrFtpFile.class.getName());

	public StructrFtpFile(final File file) {
		super(file);
	}

	public StructrFtpFile(final String newPath, final StructrFtpUser user) {
		super(newPath, user);
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public boolean isFile() {
		return true;
	}

	@Override
	public long getSize() {
		return ((File) structrFile).getSize();
	}

	@Override
	public boolean mkdir() {
		
		logger.log(Level.INFO, "mkdir() File");
		
		AbstractFile existing = FileHelper.getFileByPath(newPath);
		if (existing != null) {
			logger.log(Level.WARNING, "File {0} already exists.", newPath);
			return false;
		}
		
		final Folder parentFolder = (Folder) FileHelper.getFileByPath(StringUtils.substringBeforeLast(newPath, "/"));
		
		if (parentFolder != null) {
		
			try {
				Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						File newFile = (File) Services.command(SecurityContext.getSuperUserInstance(), CreateNodeCommand.class).execute(
							new NodeAttribute(AbstractNode.type, File.class.getSimpleName()),
							new NodeAttribute(AbstractNode.name, getName())
						);

						newFile.setProperty(AbstractFile.parent, parentFolder);

						return null;
					}

				});

			} catch (FrameworkException ex) {
				logger.log(Level.SEVERE, null, ex);
				return false;
			}

		}

		return true;
		
		
	}

	@Override
	public OutputStream createOutputStream(final long l) throws IOException {
		return ((File) structrFile).getOutputStream();
	}

	@Override
	public InputStream createInputStream(final long l) throws IOException {
		return ((File) structrFile).getInputStream();
	}		

	@Override
	public List<FtpFile> listFiles() {
		logger.log(Level.INFO, "listFiles()");
		return null;
	}
	
}

