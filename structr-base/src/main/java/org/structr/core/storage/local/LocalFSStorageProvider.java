/*
 * Copyright (C) 2010-2023 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.storage.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.app.StructrApp;
import org.structr.core.storage.AbstractStorageProvider;
import org.structr.web.entity.AbstractFile;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.StandardOpenOption.*;

public class LocalFSStorageProvider extends AbstractStorageProvider {
	private static final Logger logger = LoggerFactory.getLogger(LocalFSStorageProvider.class);

	public LocalFSStorageProvider(final AbstractFile file) {
		super(file);
	}

	@Override
	public InputStream getInputStream() {
		try {

			ensureFileExists();
			return new FileInputStream(LocalFSHelper.getFileOnDisk(getFile()));
		} catch (FileNotFoundException ex) {

			logger.error("Could not find file", ex);
		}

		return null;
	}


	@Override
	public OutputStream getOutputStream() {

		return this.getOutputStream(false);
	}
	@Override
	public OutputStream getOutputStream(final boolean append) {
		try {

			ensureFileExists();
			return new FileOutputStream(LocalFSHelper.getFileOnDisk(getFile()), append);
		} catch (FileNotFoundException ex) {

			logger.error("Could not find file", ex);
		}

		return null;
	}

	@Override
	public String getContentType() {
		return getFile().getProperty(StructrApp.key(File.class, "contentType"));
	}

	@Override
	public String getName() {
		return getFile().getName();
	}

	@Override
	public SeekableByteChannel getSeekableByteChannel() {
		try {

			ensureFileExists();
			Set<OpenOption> options = Set.of(WRITE);

			return FileChannel.open(LocalFSHelper.getFileOnDisk(getFile()).toPath(), options);
		} catch (IOException ex) {

			logger.error("Could not open file", ex);
		}

		return null;
	}

	@Override
	public void delete() {

		java.io.File fileOnDisk = LocalFSHelper.getFileOnDisk(getFile());
		if (fileOnDisk.exists() && fileOnDisk.isFile()) {

			fileOnDisk.delete();
		}
	}

	@Override
	public long size() {
		try {

			ensureFileExists();
			java.io.File fileOnDisk = LocalFSHelper.getFileOnDisk(getFile());

			if (fileOnDisk.exists()) {

				return Files.size(fileOnDisk.toPath());
			}
		} catch (IOException ex) {

			logger.error("Could not read size of file.", ex);
		}

		return -1;
	}

	private void ensureFileExists() {
		try {
			java.io.File fileOnDisk = LocalFSHelper.getFileOnDisk(getFile());

			if (!fileOnDisk.exists()) {

				fileOnDisk.createNewFile();
			}
		} catch (IOException ex) {

			logger.error("Could not create physical file for file {}. {}", getFile(), ex);
		}
	}

}
