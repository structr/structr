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
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.dynamic.File;
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

	public StructrFtpFolder(final Folder folder) {
		super(folder);
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
		try (Tx tx = StructrApp.getInstance().tx()) {
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

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			String requestedPath = getAbsolutePath();
			logger.log(Level.INFO, "Children of {0} requested", requestedPath);

			if ("/".equals(requestedPath)) {
				try {
					Result<Folder> folders = app.nodeQuery(Folder.class).getResult();
					logger.log(Level.INFO, "{0} folders found", folders.size());

					for (Folder f : folders.getResults()) {

						if (f.getProperty(AbstractFile.parent) != null) {
							continue;
						}

						FtpFile ftpFile = new StructrFtpFolder(f);
						logger.log(Level.INFO, "Folder found: {0}", ftpFile.getAbsolutePath());

						ftpFiles.add(ftpFile);

					}

					Result<File> files = app.nodeQuery(File.class).getResult();
					logger.log(Level.INFO, "{0} files found", files.size());

					for (File f : files.getResults()) {

						if (f.getProperty(AbstractFile.parent) != null) {
							continue;
						}

						logger.log(Level.FINEST, "Structr file found: {0}", f);

						FtpFile ftpFile = new StructrFtpFile(f);
						logger.log(Level.FINE, "File found: {0}", ftpFile.getAbsolutePath());

						ftpFiles.add(ftpFile);

					}

					Result<Page> pages = app.nodeQuery(Page.class).getResult();
					logger.log(Level.INFO, "{0} pages found", pages.size());

					for (Page p : pages.getResults()) {

						logger.log(Level.FINEST, "Structr page found: {0}", p);

						ftpFiles.add(p);

					}

					return ftpFiles;

				} catch (FrameworkException ex) {
					logger.log(Level.SEVERE, null, ex);
				}

			}

			List<Folder> folders = ((Folder) structrFile).getProperty(Folder.folders);

			for (Folder f : folders) {

				FtpFile ftpFile = new StructrFtpFolder(f);
				logger.log(Level.INFO, "Subfolder found: {0}", ftpFile.getAbsolutePath());

				ftpFiles.add(ftpFile);
			}

			List<FileBase> files = ((Folder) structrFile).getProperty(Folder.files);

			for (FileBase f : files) {

				FtpFile ftpFile = new StructrFtpFile(f);
				logger.log(Level.INFO, "File found: {0}", ftpFile.getAbsolutePath());

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
		logger.log(Level.INFO, "createOutputStream()");
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public InputStream createInputStream(final long l) throws IOException {
		logger.log(Level.INFO, "createInputStream()");
		throw new UnsupportedOperationException("Not supported.");
	}

}
