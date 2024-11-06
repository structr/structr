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
import org.structr.web.maintenance.DirectFileImportCommand;

import java.io.IOException;
import java.util.List;

/**
 * A console wrapper for DirectFileImportCommand
 */
public class DirectFileImportConsoleCommand extends AdminConsoleCommand {

	static {
		AdminConsoleCommand.registerCommand("file-import", DirectFileImportConsoleCommand.class);
	}

	@Override
	public void run(final SecurityContext securityContext, final List<String> parameters, final Writable writable) throws FrameworkException, IOException {

		final PrincipalInterface user = securityContext.getUser(false);
		if (user != null && user.isAdmin()) {

			final DirectFileImportCommand cmd = StructrApp.getInstance(securityContext).command(DirectFileImportCommand.class);

			cmd.setLogBuffer(writable);
			cmd.execute(toMap(
				"source",   getParameter(parameters, 1),
				"target",   getParameter(parameters, 2),
				"mode",     getParameter(parameters, 3),
				"existing", getParameter(parameters, 4),
				"index",    getParameter(parameters, 5)
			));

		} else {

			writable.println("You must be admin user to use this command.");
		}
	}

	@Override
	public void commandHelp(final Writable writable) throws IOException {
		writable.println("Imports files directly from a directory on the server.");
	}

	@Override
	public void detailHelp(final Writable writable) throws IOException {
		writable.println("file-import <source> <target> <mode> <existing> <index>  -  Import files directly from a server directoy.");
		writable.println("");
		writable.println("  <source>   - Path to a directory on the server.");
		writable.println("  <target>   - Target path in Structr's virtual file system.");
		writable.println("  <mode>     - Whether to copy or move the files into Structr's files directory. Possible values: copy (default), move");
		writable.println("  <existing> - How to handle files already existing with the same path in Structr. Possible values: skip (default), overwrite, rename");
		writable.println("  <index>    - Whether new files should be fulltext-indexed after import. Possible values: true (default), false");
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
