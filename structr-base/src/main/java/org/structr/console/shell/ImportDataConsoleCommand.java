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
package org.structr.console.shell;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.util.Writable;
import org.structr.web.maintenance.DeployDataCommand;

import java.io.IOException;
import java.util.List;

/**
 * A console wrapper for DeployDataCommand, import mode.
 */
public class ImportDataConsoleCommand extends AdminConsoleCommand {

	static {
		AdminConsoleCommand.registerCommand("import-data", ImportDataConsoleCommand.class);
	}

	@Override
	public void run(final SecurityContext securityContext, final List<String> parameters, final Writable writable) throws FrameworkException, IOException {

		final Principal user = securityContext.getUser(false);
		if (user != null && user.isAdmin()) {

			final DeployDataCommand cmd = StructrApp.getInstance(securityContext).command(DeployDataCommand.class);

			cmd.setLogBuffer(writable);
			cmd.execute(toMap(
					"mode",              "import",
					"source",            getParameter(parameters, 1),
					"doInnerCallbacks",  getParameter(parameters, 2),
					"doOuterCallbacks",  getParameter(parameters, 3),
					"doCascadingDelete", getParameter(parameters, 4)
			));

		} else {

			writable.println("You must be admin user to use this command.");
		}
	}

	@Override
	public void commandHelp(final Writable writable) throws IOException {
		writable.println("Imports data into a Structr application from a directory.");
	}

	@Override
	public void detailHelp(final Writable writable) throws IOException {
		writable.println("import-data <source> <doInnerCallbacks> <doOuterCallbacks> <doCascadingDelete>  -  Imports data for an application from a path in the file system.");
		writable.println("");
		writable.println("  <source>            - absolute path to the source directory");
		writable.println("  <doInnerCallbacks>  - (optional) decides if onCreate/onSave methods are run and function properties are evaluated during data deployment. Often this leads to errors because onSave contains validation code which will fail during data deployment. (default = false. Only set to true if you know what you're doing!)");
		writable.println("  <doOuterCallbacks>  - (optional) decides if afterCreate method is run during data deployment. Often this is undesirable during data deployment. (default = false. Only set to true if you know what you're doing!)");
		writable.println("  <doCascadingDelete> - (optional) decides if cascadingDelete is enabled during data deployment. This leads to errors because cascading delete triggers onSave methods on remote nodes which will fail during data deployment. (default = false. Only set to true if you know what you're doing!)");
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
