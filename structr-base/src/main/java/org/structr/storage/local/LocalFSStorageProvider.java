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
package org.structr.storage.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.app.StructrApp;
import org.structr.storage.AbstractStorageProvider;
import org.structr.storage.StorageProvider;
import org.structr.web.entity.AbstractFile;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
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
			return new FileInputStream(LocalFSHelper.getFileOnDisk(getAbstractFile()));
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
			return new FileOutputStream(LocalFSHelper.getFileOnDisk(getAbstractFile()), append);
		} catch (FileNotFoundException ex) {

			logger.error("Could not find file", ex);
		}

		return null;
	}

	@Override
	public String getContentType() {
		return getAbstractFile().getProperty(StructrApp.key(File.class, "contentType"));
	}

	@Override
	public String getName() {
		return getAbstractFile().getName();
	}

	@Override
	public SeekableByteChannel getSeekableByteChannel(boolean append, boolean truncate) {
		try {

			ensureFileExists();
			Set<OpenOption> options = new java.util.HashSet<>(Set.of(CREATE, READ, WRITE, SYNC));
			if (append) {
				options.add(APPEND);
			}
			if (truncate) {
				options.add(TRUNCATE_EXISTING);
			}

			return FileChannel.open(LocalFSHelper.getFileOnDisk(getAbstractFile()).toPath(), options);
		} catch (IOException ex) {

			logger.error("Could not open file", ex);
		}

		return null;
	}

	@Override
	public void moveTo(final StorageProvider newFileStorageProvider) {

		// Check if new provider is different from current one, if so use default implementation
		if (!this.equals(newFileStorageProvider)) {

			super.moveTo(newFileStorageProvider);
		}

		// If provider class is local as well, check if file needs to be moved
		// Ensure files exist and abstract files are actual files
		if(getAbstractFile() != null && getAbstractFile() instanceof org.structr.web.entity.File && newFileStorageProvider.getAbstractFile() != null && newFileStorageProvider.getAbstractFile() instanceof org.structr.web.entity.File) {
			// Check file paths and only move if they differ
			if (!getAbstractFile().getPath().equals(newFileStorageProvider.getAbstractFile().getPath())) {

				LocalFSHelper.getFileOnDisk(getAbstractFile()).renameTo(LocalFSHelper.getFileOnDisk(newFileStorageProvider.getAbstractFile()));
			}
		}
	}

	@Override
	public void delete() {

		java.io.File fileOnDisk = LocalFSHelper.getFileOnDisk(getAbstractFile());
		if (fileOnDisk.exists() && fileOnDisk.isFile()) {

			fileOnDisk.delete();
		}
	}

	@Override
	public long size() {
		try {

			ensureFileExists();
			java.io.File fileOnDisk = LocalFSHelper.getFileOnDisk(getAbstractFile());

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
			java.io.File fileOnDisk = LocalFSHelper.getFileOnDisk(getAbstractFile());

			if (!fileOnDisk.exists()) {

				fileOnDisk.createNewFile();
			}
		} catch (IOException ex) {

			logger.error("Could not create physical file for file {}. {}", getAbstractFile(), ex);
		}
	}

}
