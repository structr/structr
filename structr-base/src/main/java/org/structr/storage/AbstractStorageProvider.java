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
package org.structr.storage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;
import org.structr.storage.config.StorageProviderConfig;
import org.structr.web.entity.AbstractFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class AbstractStorageProvider implements StorageProvider {
	private final AbstractFile file;
	private final StorageProviderConfig config;

	public AbstractStorageProvider(final AbstractFile file, final StorageProviderConfig config) {
		this.file = file;
		this.config = config;
	}

	@Override
	public AbstractFile getAbstractFile() {
		return file;
	}

	@Override
	public StorageProviderConfig getConfig() {
		return this.config;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof StorageProvider) {
			StorageProvider otherProvider = (StorageProvider)obj;
			return otherProvider.getClass().equals(this.getClass());
		}
		return false;
	}

	@Override
	public void moveTo(final StorageProvider newFileStorageProvider) {
		final StorageProvider destinationStorageProvider = newFileStorageProvider != null ? newFileStorageProvider :  StorageProviderFactory.getSpecificStorageProvider(getAbstractFile(), null);

		if (destinationStorageProvider != null && !this.config.Name().equals(destinationStorageProvider.getConfig().Name())) {
			try {

				// Move binary content from old sp to new sp
				try (final InputStream is = this.getInputStream(); final OutputStream os = destinationStorageProvider.getOutputStream()) {
					IOUtils.copy(is, os);
				}
				// Clean up old binary data on previous sp
				//previousSP.delete();
			} catch (IOException ex) {

				LoggerFactory.getLogger(AbstractStorageProvider.class).error(ExceptionUtils.getStackTrace(ex));
			}
		}
	}
}
