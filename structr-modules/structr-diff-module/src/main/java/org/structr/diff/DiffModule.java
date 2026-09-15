/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.diff;

import org.structr.api.service.LicenseManager;
import org.structr.core.function.Functions;
import org.structr.module.StructrModule;
import org.structr.diff.function.CompareExportsFunction;
import org.structr.diff.websocket.CompareExportsCommand;

import java.util.Set;

/**
 * Registers what this module contributes to a running instance.
 *
 * The websocket command the admin UI uses and the compareExports() function every other caller uses,
 * both answered by the same code. The module deliberately contributes no schema and no traits: an
 * export is read as a file, and nothing about comparing two of them needs anything in the graph.
 */
public class DiffModule implements StructrModule {

	@Override
	public void onLoad() {

		CompareExportsCommand.register();
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		// the registry is the only seam a server-side caller needs: Functions.get("compareExports")
		// returns null on an instance without this module, which is the capability check
		Functions.put(licenseManager, new CompareExportsFunction());
	}

	@Override
	public String getName() {

		return "diff";
	}

	@Override
	public Set<String> getDependencies() {

		// ui, for the File type an export is stored as

		return Set.of("ui");
	}

	@Override
	public Set<String> getFeatures() {

		return null;
	}
}
