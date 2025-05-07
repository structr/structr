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
package org.structr.storage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.StorageConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstract base class for all storage providers.
 */
public abstract class AbstractStorageProvider implements StorageProvider {

	private final StorageConfiguration config;
	private final AbstractFile file;
	private final String name;

	protected AbstractStorageProvider(final AbstractFile file, final StorageConfiguration config) {

		this.file   = file;
		this.config = config;
		this.name   = config != null ? config.getName() : "default";
	}

	@Override
	public String getProviderName() {
		return name;
	}

	@Override
	public AbstractFile getAbstractFile() {
		return file;
	}

	@Override
	public StorageConfiguration getConfig() {
		return this.config;
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == null) {

			return false;
		}

		if (obj instanceof StorageProvider otherProvider) {

			final boolean sameClass = otherProvider.getClass().equals(this.getClass());
			final boolean bothHaveNoOrEmptyConfigs = ((this.getConfig() == null || this.getConfig().getConfiguration().size() == 0) && (otherProvider.getConfig() == null || otherProvider.getConfig().getConfiguration().size() == 0));
			final boolean configsHaveMatchingUUIDs = !bothHaveNoOrEmptyConfigs && (this.getConfig() != null && otherProvider.getConfig() != null && this.getConfig().getUuid().equals(otherProvider.getConfig().getUuid()));
			final boolean sameConfigs = bothHaveNoOrEmptyConfigs || configsHaveMatchingUUIDs;

			return sameClass && sameConfigs;
		}

		return false;
	}

	@Override
	public void moveTo(final StorageProvider newFileStorageProvider) {

		// Either use provided destination provider or instantiate new one with a blank config
		final StorageProvider destinationStorageProvider = newFileStorageProvider != null ? newFileStorageProvider :  StorageProviderFactory.getDefaultStorageProvider(getAbstractFile());

		// Only try to move binary content, if destinationProvider exists and is different from this provider
		if (destinationStorageProvider != null && !this.equals(destinationStorageProvider)) {

			try {

				// Move binary content from old sp to new sp
				try (final InputStream is = this.getInputStream(); final OutputStream os = destinationStorageProvider.getOutputStream()) {

					IOUtils.copy(is, os);

					// Clean up old binary data on previous sp
					this.delete();

					os.flush();
				}
			} catch (IOException ex) {

				LoggerFactory.getLogger(AbstractStorageProvider.class).error(ExceptionUtils.getStackTrace(ex));
			}
		}
	}
}
