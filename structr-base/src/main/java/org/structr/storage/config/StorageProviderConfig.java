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
package org.structr.storage.config;

import org.structr.storage.StorageProvider;

import java.util.HashMap;
import java.util.Map;

public record StorageProviderConfig(String Name, Class<? extends StorageProvider> StorageProviderClass, Map<String, Object> SpecificConfigParameters) {

	public StorageProviderConfig(final String Name, final Class<? extends StorageProvider> StorageProviderClass, final Map<String, Object> SpecificConfigParameters) {

		this.Name = Name;
		this.StorageProviderClass = StorageProviderClass;
		this.SpecificConfigParameters = SpecificConfigParameters != null ? SpecificConfigParameters : new HashMap<>();
	}

	public StorageProviderConfig(final String name, final Class<? extends StorageProvider> storageProviderClass) {

		this(name, storageProviderClass, new HashMap<>());
	}

}
