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
package org.structr.console.shell;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.PrincipalInterface;
import org.structr.util.Writable;
import org.structr.web.maintenance.DeployDataCommand;

import java.io.IOException;
import java.util.List;

/**
 * A console wrapper for DeployDataCommand, export mode.
 */
public class ExportDataConsoleCommand extends AdminConsoleCommand {

	static {
		AdminConsoleCommand.registerCommand("export-data", ExportDataConsoleCommand.class);
	}

	@Override
	public void run(final SecurityContext securityContext, final List<String> parameters, final Writable writable) throws FrameworkException, IOException {

		final PrincipalInterface user = securityContext.getUser(false);
		if (user != null && user.isAdmin()) {

			final DeployDataCommand cmd = StructrApp.getInstance(securityContext).command(DeployDataCommand.class);

			cmd.setLogBuffer(writable);
			cmd.execute(toMap(
					"mode", "export",
					"target", getParameter(parameters, 1),
					"types", getParameter(parameters, 2)
			));

		} else {

			writable.println("You must be admin user to use this command.");
		}
	}

	@Override
	public void commandHelp(final Writable writable) throws IOException {
		writable.println("Exports data from the Structr application to a directory.");
	}

	@Override
	public void detailHelp(final Writable writable) throws IOException {
		writable.println("export-data <target> <types>  -  Exports data from this application to a path in the file system.");
		writable.println("");
		writable.println("  <target> - absolute path to the target directory");
		writable.println("  <types>  - comma-separated list of types to export");
	}
}
