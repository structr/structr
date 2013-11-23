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
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

/**
 * This class is an equivalent to Java's native File class for file and folder creation.
 * 
 * It's a thin wrapper, implementing mkdir() and createOutputStream() only.
 * 
 * @author Axel Morgner
 */
public class FileOrFolder extends AbstractStructrFtpFile {

	private static final Logger logger = Logger.getLogger(FileOrFolder.class.getName());
	
	public FileOrFolder(final String newPath, final StructrFtpUser user) {
		super(newPath, user);
	}
	
	@Override
	public boolean isDirectory() {
		logger.log(Level.SEVERE, "isDirectory()");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isFile() {
		logger.log(Level.SEVERE, "isFile()");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public long getSize() {
		logger.log(Level.SEVERE, "getSize()");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean mkdir() {
		
		logger.log(Level.INFO, "mkdir() Folder");
		
		AbstractFile existing = FileHelper.getFileByAbsolutePath(newPath);
		if (existing != null) {
			logger.log(Level.WARNING, "File {0} already exists.", newPath);
			return false;
		}
		
		final Folder parentFolder = (Folder) FileHelper.getFileByAbsolutePath(StringUtils.substringBeforeLast(newPath, "/"));
		final App app             = StructrApp.getInstance();

		try {
			app.beginTx();
			
			Folder newFolder = (Folder) StructrApp.getInstance().command(CreateNodeCommand.class).execute(
				new NodeAttribute(AbstractNode.type, Folder.class.getSimpleName()),
				new NodeAttribute(AbstractNode.owner, owner.getStructrUser()),
				new NodeAttribute(AbstractNode.name, getName())
			);

			if (parentFolder != null) {
				newFolder.setProperty(AbstractFile.parent, parentFolder);
			}
			
			app.commitTx();
			
		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);
			return false;

			} finally {

				app.finishTx();
			}
		
		return true;
	}

	@Override
	public List<FtpFile> listFiles() {
		logger.log(Level.SEVERE, "listFiles()");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public OutputStream createOutputStream(final long l) throws IOException {
		
		if (structrFile == null) {

			final Folder parentFolder = (Folder) FileHelper.getFileByAbsolutePath(StringUtils.substringBeforeLast(newPath, "/"));
			final App app             = StructrApp.getInstance();

			try {
				app.beginTx();
				
				structrFile = FileHelper.createFile(SecurityContext.getSuperUserInstance(), new byte[0], null, File.class);
				structrFile.setProperty(AbstractNode.type, File.class.getSimpleName());
				structrFile.setProperty(AbstractNode.owner, owner.getStructrUser());
				structrFile.setProperty(AbstractNode.name, getName());

				if (parentFolder != null) {
					structrFile.setProperty(AbstractFile.parent, parentFolder);
				}
				
				app.commitTx();

			} catch (FrameworkException ex) {
				logger.log(Level.SEVERE, null, ex);
				return null;

			} finally {

				app.finishTx();
			}
		}
		
		return ((File) structrFile).getOutputStream();
	}


	@Override
	public InputStream createInputStream(long l) throws IOException {
		logger.log(Level.SEVERE, "createInputStream()");
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

}
