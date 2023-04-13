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
package org.structr.core.storage;

import org.structr.core.storage.local.LocalFSStorageProvider;
import org.structr.core.storage.memory.InMemoryStorageProvider;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Folder;

public abstract class StorageProviderFactory {
	public static StorageProvider getStorageProvider(final AbstractFile file) {
		final String provider = getProviderType(file);

		if (provider == null) {
			return new LocalFSStorageProvider(file);
		}

		switch (provider) {
			case "memory":
				return new InMemoryStorageProvider(file);
			default:
				return new LocalFSStorageProvider(file);
		}
	}

	public static StorageProvider getSpecificStorageProvider(final AbstractFile file, final String forcedProvider) {

		if (forcedProvider == null) {
			return new LocalFSStorageProvider(file);
		}

		switch (forcedProvider) {
			case "memory":
				return new InMemoryStorageProvider(file);
			default:
				return new LocalFSStorageProvider(file);
		}
	}

	private static String getProviderType(final AbstractFile abstractFile) {

		if (abstractFile.getStorageProvider() != null) {

			return abstractFile.getStorageProvider();
		}

		final Folder parentFolder = abstractFile.getParent();

		if (parentFolder != null) {

			if (parentFolder.getStorageProvider() != null) {

				return parentFolder.getStorageProvider();
			} else {

				return getProviderType(parentFolder);
			}
		}

		return null;
	}
}
