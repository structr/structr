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

import org.apache.ftpserver.ftplet.FtpFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.storage.StorageProviderFactory;
import org.structr.web.entity.File;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 *
 *
 */
public class StructrFtpFile extends AbstractStructrFtpFile {

	private static final Logger logger = LoggerFactory.getLogger(StructrFtpFile.class.getName());

	public StructrFtpFile(final SecurityContext securityContext, final File file) {
		super(securityContext, file);
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

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final Long size = StorageProviderFactory.getStorageProvider(structrFile).size();

			tx.success();

			return size == null ? 0L : size;

		} catch (FrameworkException fex) {}

		return 0L;
	}

	@Override
	public boolean mkdir() {
		logger.info("mkdir()");
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public OutputStream createOutputStream(final long l) throws IOException {

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final OutputStream outputStream = ((File) structrFile).getOutputStream();

			tx.success();

			return outputStream;

		} catch (FrameworkException fex) {
			logger.error(null, fex);
		}

		return null;
	}

	@Override
	public InputStream createInputStream(final long l) throws IOException {

		try (Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final InputStream inputStream = ((File) structrFile).getInputStream();

			tx.success();

			return inputStream;

		} catch (FrameworkException fex) {
			logger.error(null, fex);
		}

		return null;
	}

	@Override
	public List<FtpFile> listFiles() {
		logger.info("listFiles()");
		return null;
	}

	@Override
	public Object getPhysicalFile() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
