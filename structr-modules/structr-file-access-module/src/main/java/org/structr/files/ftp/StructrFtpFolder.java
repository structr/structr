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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.ftpserver.ftplet.FtpFile;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.FileBase;
import org.structr.web.entity.Folder;
import org.structr.web.entity.dom.Page;

/**
 *
 *
 */
public class StructrFtpFolder extends AbstractStructrFtpFile implements FtpFile {

	private static final Logger logger = Logger.getLogger(StructrFtpFolder.class.getName());

	public StructrFtpFolder(final SecurityContext securityContext, final Folder folder) {
		super(securityContext, folder);		
	}

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
		
		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {
		
			final Date date = structrFile.getProperty(Folder.lastModifiedDate);
			
			tx.success();
			
			return date.getTime();
		
		} catch (Exception ex) {
		}
		
		return 0L;
	}

	@Override
	public long getSize() {
		return listFiles().size();
	}

	@Override
	public List<FtpFile> listFiles() {

		final List<FtpFile> ftpFiles = new ArrayList();

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			String requestedPath = getAbsolutePath();
			logger.log(Level.FINE, "Children of {0} requested", requestedPath);

			if ("/".equals(requestedPath)) {
				try {
					Result<Folder> folders = app.nodeQuery(Folder.class).getResult();
					logger.log(Level.FINE, "{0} folders found", folders.size());

					for (Folder f : folders.getResults()) {

						if (f.getProperty(AbstractFile.hasParent)) {
							continue;
						}

						FtpFile ftpFile = new StructrFtpFolder(securityContext, f);
						logger.log(Level.FINE, "Folder found: {0}", ftpFile.getAbsolutePath());

						ftpFiles.add(ftpFile);

					}

					Result<FileBase> files = app.nodeQuery(FileBase.class).getResult();
					logger.log(Level.FINE, "{0} files found", files.size());

					for (FileBase f : files.getResults()) {

						if (f.getProperty(AbstractFile.hasParent)) {
							continue;
						}

						logger.log(Level.FINEST, "Structr file found: {0}", f);

						FtpFile ftpFile = new StructrFtpFile(securityContext, f);
						logger.log(Level.FINE, "File found: {0}", ftpFile.getAbsolutePath());

						ftpFiles.add(ftpFile);

					}

					Result<Page> pages = app.nodeQuery(Page.class).getResult();
					logger.log(Level.FINE, "{0} pages found", pages.size());

					for (Page p : pages.getResults()) {

						logger.log(Level.FINE, "Structr page found: {0}", p);

						ftpFiles.add(new FtpFilePageWrapper(p));

					}

					return ftpFiles;

				} catch (FrameworkException ex) {
					logger.log(Level.SEVERE, null, ex);
				}

			}

			List<Folder> folders = ((Folder) structrFile).getProperty(Folder.folders);

			for (Folder f : folders) {

				FtpFile ftpFile = new StructrFtpFolder(securityContext, f);
				logger.log(Level.FINE, "Subfolder found: {0}", ftpFile.getAbsolutePath());

				ftpFiles.add(ftpFile);
			}

			List<FileBase> files = ((Folder) structrFile).getProperty(Folder.files);

			for (FileBase f : files) {

				FtpFile ftpFile = new StructrFtpFile(securityContext, f);
				logger.log(Level.FINE, "File found: {0}", ftpFile.getAbsolutePath());

				ftpFiles.add(ftpFile);
			}

			tx.success();

			return ftpFiles;

		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error in listFiles()", fex);
		}

		return null;

	}

	@Override
	public boolean mkdir() {
		logger.log(Level.SEVERE, "Use FileOrFolder#createOutputStream() instead!");
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public OutputStream createOutputStream(final long l) throws IOException {
		logger.log(Level.FINE, "createOutputStream()");
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public InputStream createInputStream(final long l) throws IOException {
		logger.log(Level.FINE, "createInputStream()");
		throw new UnsupportedOperationException("Not supported.");
	}

}
