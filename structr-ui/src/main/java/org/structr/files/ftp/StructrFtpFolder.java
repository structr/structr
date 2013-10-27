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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.ftpserver.ftplet.FtpFile;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

/**
 *
 * @author Axel Morgner
 */
public class StructrFtpFolder extends AbstractStructrFtpFile implements FtpFile {

	private static final Logger logger = Logger.getLogger(StructrFtpFolder.class.getName());

	public StructrFtpFolder(final Folder folder) {
		super(folder);
	}

//	public StructrFtpFolder(final String newPath, final StructrFtpUser user) {
//		super(newPath, user);
//	}

	@Override
	public boolean doesExist() {
		boolean exists = "/".equals(newPath) || super.doesExist();
		return exists;
	}

	@Override
	public boolean isDirectory() {
		return true;
	}

	@Override
	public boolean isFile() {
		return false;
	}

	@Override
	public int getLinkCount() {
		return 1;
	}

	@Override
	public long getLastModified() {
		return structrFile.getProperty(Folder.lastModifiedDate).getTime();
	}

	@Override
	public long getSize() {
		return listFiles().size();
	}

	@Override
	public List<FtpFile> listFiles() {

		List<FtpFile> ftpFiles = new ArrayList();
		
		String requestedPath = getAbsolutePath();
		logger.log(Level.INFO, "Children of {0} requested", requestedPath);
		
		if ("/".equals(requestedPath)) {
			
			// Find all folders and files which have no parent
			List<SearchAttribute> searchAttrs = new LinkedList<>();
			searchAttrs.add(Search.orExactTypeAndSubtypes(org.structr.web.entity.Folder.class));

			try {
				Result<Folder> results = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(false, false, searchAttrs);
				logger.log(Level.INFO, "{0} folders found", results.size());

				for (Folder f : results.getResults()) {
					
					if (f.getProperty(AbstractFile.parent) != null) {
						continue;
					}
					
					FtpFile ftpFile = new StructrFtpFolder(f);
					logger.log(Level.INFO, "Folder found: {0}", ftpFile.getAbsolutePath());

					ftpFiles.add(ftpFile);
					
				}

			} catch (FrameworkException ex) {
				logger.log(Level.SEVERE, null, ex);
			}
			
			searchAttrs = new LinkedList<>();
			searchAttrs.add(Search.orExactTypeAndSubtypes(org.structr.web.entity.File.class));

			try {
				Result<File> results = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(false, false, searchAttrs);
				logger.log(Level.INFO, "{0} files found", results.size());

				for (File f : results.getResults()) {
					
					if (f.getProperty(AbstractFile.parent) != null) {
						continue;
					}
					
					FtpFile ftpFile = new StructrFtpFile(f);
					logger.log(Level.INFO, "File found: {0}", ftpFile.getAbsolutePath());

					ftpFiles.add(ftpFile);
					
				}

			} catch (FrameworkException ex) {
				logger.log(Level.SEVERE, null, ex);
			}

			return ftpFiles;
			
		}
		
		List<Folder> folders = ((Folder) structrFile).getProperty(Folder.folders);
		
		for (Folder f : folders) {
			
			FtpFile ftpFile = new StructrFtpFolder(f);
			logger.log(Level.INFO, "Subfolder found: {0}", ftpFile.getAbsolutePath());
			
			ftpFiles.add(ftpFile);
		}
		
		List<File> files = ((Folder) structrFile).getProperty(Folder.files);
		
		for (File f : files) {
			
			FtpFile ftpFile = new StructrFtpFile(f);
			logger.log(Level.INFO, "File found: {0}", ftpFile.getAbsolutePath());
			
			ftpFiles.add(ftpFile);
		}
		return ftpFiles;
	}

	@Override
	public boolean mkdir() {
		logger.log(Level.SEVERE, "Use FileOrFolder#createOutputStream() instead!");
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	@Override
	public OutputStream createOutputStream(final long l) throws IOException {
		logger.log(Level.INFO, "createOutputStream()");
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public InputStream createInputStream(final long l) throws IOException {
		logger.log(Level.INFO, "createInputStream()");
		throw new UnsupportedOperationException("Not supported.");
	}

}