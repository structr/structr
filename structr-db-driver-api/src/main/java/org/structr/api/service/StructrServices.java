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
package org.structr.api.service;

import org.structr.api.DatabaseService;

import java.util.Map;

/**
 *
 */
public interface StructrServices {

	void registerInitializationCallback(final InitializationCallback callback);
	<T extends Service> T getService(final Class<T> serviceClass, final String name);
	<T extends Service> Map<String, T> getServices(final Class<T> serviceClass);
	DatabaseService getDatabaseService();
	LicenseManager getLicenseManager();
	String getInstanceName();
	boolean hasExclusiveDatabaseAccess();
	String getVersion();
}
