/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.storage.providers.local;

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
import java.nio.file.StandardOpenOption;
import java.util.Set;

import static java.nio.file.StandardOpenOption.*;
import org.structr.web.entity.StorageConfiguration;

public class LocalFSStorageProvider extends AbstractStorageProvider {

	private static final Logger logger = LoggerFactory.getLogger(LocalFSStorageProvider.class);
	private final LocalFSHelper fsHelper;

	public LocalFSStorageProvider(final AbstractFile file) {
		this(file, null);
	}

	public LocalFSStorageProvider(final AbstractFile file, final StorageConfiguration config) {

		super(file, config);

		fsHelper = new LocalFSHelper(config);
	}

	@Override
	public InputStream getInputStream() {
		try {

			ensureFileExists();
			return new FileInputStream(fsHelper.getFileOnDisk(getAbstractFile()));
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
			return new FileOutputStream(fsHelper.getFileOnDisk(getAbstractFile()), append);
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
	public SeekableByteChannel getSeekableByteChannel(final Set<? extends OpenOption> options) {
		try {

			if (options.isEmpty()) {
				ensureFileExists();
				return FileChannel.open(fsHelper.getFileOnDisk(getAbstractFile()).toPath(), new java.util.HashSet<OpenOption>(Set.of(CREATE, READ, WRITE, SYNC)));
			}

			return FileChannel.open(fsHelper.getFileOnDisk(getAbstractFile()).toPath(), options);
		} catch (IOException ex) {

			logger.error("Could not open file", ex);
		}

		return null;
	}

	@Override
	public void moveTo(final StorageProvider newFileStorageProvider) {

		// Check if new provider is different from current one, if so use default implementation
		if (newFileStorageProvider != null && !this.equals(newFileStorageProvider)) {

			super.moveTo(newFileStorageProvider);
		}

		// If provider class is local as well, check if file needs to be moved
		// Ensure files exist and abstract files are actual files
		if (getAbstractFile() != null && getAbstractFile() instanceof org.structr.web.entity.File && newFileStorageProvider.getAbstractFile() != null && newFileStorageProvider.getAbstractFile() instanceof org.structr.web.entity.File) {
			// Check file paths and only move if they differ
			if (!getAbstractFile().getPath().equals(newFileStorageProvider.getAbstractFile().getPath())) {

				fsHelper.getFileOnDisk(getAbstractFile()).renameTo(fsHelper.getFileOnDisk(newFileStorageProvider.getAbstractFile()));
			}
		}
	}

	@Override
	public void delete() {

		java.io.File fileOnDisk = fsHelper.getFileOnDisk(getAbstractFile());
		if (fileOnDisk.exists() && fileOnDisk.isFile()) {

			fileOnDisk.delete();
		}
	}

	@Override
	public long size() {
		try {

			ensureFileExists();
			java.io.File fileOnDisk = fsHelper.getFileOnDisk(getAbstractFile());

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
			java.io.File fileOnDisk = fsHelper.getFileOnDisk(getAbstractFile());

			if (!fileOnDisk.exists()) {

				fileOnDisk.createNewFile();
			}
		} catch (IOException ex) {

			logger.error("Could not create physical file for file {}. {}", getAbstractFile(), ex);
		}
	}

}
