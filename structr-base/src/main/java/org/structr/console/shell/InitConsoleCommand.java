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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.graph.*;
import org.structr.util.Writable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A console wrapper for BulkRebuildIndexCommand.
 */
public class InitConsoleCommand extends AdminConsoleCommand {

	static {
		AdminConsoleCommand.registerCommand("init", InitConsoleCommand.class);
	}

	@Override
	public void run(final SecurityContext securityContext, final List<String> parameters, final Writable writable) throws FrameworkException, IOException {

		final PrincipalInterface user    = securityContext.getUser(false);
		final int paramCount    = parameters.size();

		if (user != null && user.isAdmin()) {

			NodeServiceCommand command = null;
			boolean allNodes           = true;
			boolean allRels            = true;
			boolean isCreateLabels     = false;
			boolean isIndex            = false;
			boolean isIds              = false;
			boolean hasCommand         = false;
			boolean hasFor             = false;
			boolean error              = false;
			String type                = null;
			String typeKey             = null;
			String mode                = null;

			// parse parameters
			for (int i=1; i < paramCount && !error; i++) {

				final String param = getParameter(parameters, i);

				switch (param) {

					case "index":

						if (hasCommand) {

							writable.println("Syntax error, too many parameters.");
							error = true;

						} else {

							command = StructrApp.getInstance(securityContext).command(BulkRebuildIndexCommand.class);
							command.setLogBuffer(writable);
							isIndex    = true;
							hasCommand = true;
						}
						break;

					case "ids":

						if (hasCommand) {

							writable.println("Syntax error, too many parameters.");
							error = true;

						} else {

							command = StructrApp.getInstance(securityContext).command(BulkSetUuidCommand.class);
							command.setLogBuffer(writable);
							isIds      = true;
							hasCommand = true;
						}
						break;

					case "labels":

						if (hasCommand) {

							writable.println("Syntax error, too many parameters.");
							error = true;

						} else {

							if ("relsOnly".equals(mode)) {

								writable.println("Cannot set labels on relationships.");
								error = true;

							} else {

								command = StructrApp.getInstance(securityContext).command(BulkCreateLabelsCommand.class);
								command.setLogBuffer(writable);
								isCreateLabels = true;
								hasCommand     = true;
							}
						}
						break;

					case "node":

						if (hasCommand) {

							if (isIndex) {

								writable.println("Index type must be specified before the 'index' keyword.");
								error = true;

							} else if (isIds) {

								writable.println("Entity type must be specified before the 'ids' keyword.");
								error = true;
							}

						} else {

							mode    = "nodesOnly";
							typeKey = "type";
							allRels = false;
						}
						break;

					case "rel":
					case "relationship":

						if (hasCommand) {

							if (isIndex) {

								writable.println("Index type must be specified before the 'index' keyword.");
								error = true;

							} else if (isIds) {

								writable.println("Entity type must be specified before the 'ids' keyword.");
								error = true;
							}

						} else {

							if (isCreateLabels) {

								writable.println("Cannot set labels on relationships.");
								error = true;
							}

							mode     = "relsOnly";
							typeKey  = "relType";
							allNodes = false;
						}
						break;

					case "for":

						if (!hasCommand) {

							writable.println("Unknown init mode 'for'.");
							error = true;
						}
						hasFor = true;
						break;

					default:
						// specify node or rel type
						if (hasCommand && hasFor) {

							// prevent too many parameters from being accepted
							if (StringUtils.isNotBlank(type)) {

								writable.println("Syntax error, too many parameters.");
								error = true;
								break;
							}

							type = param;

							// only set type key if not already set, default is "type" not "relType"
							if (typeKey == null) {
								typeKey = "type";
							}

						} else {

							if (!hasCommand) {

								writable.println("Unknown init mode '" + param + "'.");
								error = true;

							} else {

								writable.println("Syntax error, please specify something like 'init node index for User'.");
								error = true;
							}
						}
						break;
				}

				// break early on errors
				if (error) {
					break;
				}
			}

			if (!error && !hasCommand) {

				writable.println("Please specify what to initialize.");
				error = true;
			}

			if (!error && hasCommand && hasFor && StringUtils.isEmpty(type)) {

				writable.println("Missing type specification, please specify something like 'init node index for User'.");
				error = true;
			}

			if (!error) {

				if (command instanceof MaintenanceCommand) {

					final Map<String, Object> data = toMap("mode", mode, typeKey, type);

					if (type == null) {

						data.put("allNodes", allNodes);
						data.put("allRels", allRels);
					}

					((MaintenanceCommand)command).execute(data);

				} else if (command != null) {

					writable.println("Cannot execute command '" + command.getClass().getSimpleName() + "', wrong type.");

				} else {

					writable.println("Cannot execute null command.");
				}
			}

		} else {

			writable.println("You must be admin user to use this command.");
		}
	}

	@Override
	public void commandHelp(final Writable buf) throws IOException {
		buf.println("Initializes UUIDs, labels and indexes on nodes and relationships.");
	}

	@Override
	public void detailHelp(final Writable buf) throws IOException {
		buf.println("init [node|rel] index [for <type>] - Rebuilds the node/relationship index.");
		buf.println("init [node|rel] ids [for <type>]   - Sets UUIDs on nodes and/or relationship.");
		buf.println("init [node] labels [for <type>]    - Sets labels on nodes and/or relationship.");
	}
}
