/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.module;

import com.google.gson.Gson;
import org.structr.api.service.LicenseManager;
import org.structr.common.error.FrameworkException;

import java.nio.file.Path;
import java.util.Set;

/**
 */
public interface StructrModule {

	/**
	 * Called when the module is loaded.
	 */
	void onLoad();

	/**
	 * Registers the functions of the module
	 *
	 * @param licenseManager the license manager or null
	 */
	void registerModuleFunctions(final LicenseManager licenseManager);

	/**
	 * Returns the name of this module, with an optional version number.
	 *
	 * @return the name of this module
	 */
	String getName();

	/**
	 * Returns a set of module names (as returned by getName()) of modules
	 * this module depends on, or null if no dependencies exist.
	 *
	 * @return a set of module names or null
	 */
	Set<String> getDependencies();

	/**
	 * Returns the set of feature keys this module provides, or null if
	 * not applicable.
	 *
	 * @return a set of feature names or null
	 */
	Set<String> getFeatures();

	// ---- Deployment-specific methods
	default public boolean hasDeploymentData () {
		return false;
	}

	default public void exportDeploymentData (final Path target, final Gson gson) throws FrameworkException {};
	default public void importDeploymentData (final Path source, final Gson gson) throws FrameworkException {};
}
