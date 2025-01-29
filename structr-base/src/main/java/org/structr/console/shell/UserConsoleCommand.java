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
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.util.Writable;
import org.structr.web.entity.User;

import java.io.IOException;
import java.util.List;

/**
 * A console command for user management.
 */
public class UserConsoleCommand extends AdminConsoleCommand {

	static {
		AdminConsoleCommand.registerCommand("user", UserConsoleCommand.class);
	}

	@Override
	public void run(final SecurityContext securityContext, final List<String> parameters, final Writable writable) throws FrameworkException, IOException {

		final String command    = getParameter(parameters, 1);
		if (command != null) {

			final Principal user = securityContext.getUser(false);
			if (user != null && user.isAdmin()) {

				switch (command) {

					case "list":
						handleList(securityContext, writable);
						break;

					case "add":
						handleAdd(securityContext, writable, getParameter(parameters, 2), getParameter(parameters, 3), getParameter(parameters, 4));
						break;

					case "delete":
						handleDelete(securityContext, writable, getParameter(parameters, 2), getParameter(parameters, 3));
						break;

					case "password":
						handlePwd(securityContext, writable, getParameter(parameters, 2), getParameter(parameters, 3));
						break;
				}

			} else {

				writable.println("You must be admin user to use this command.");
			}

		} else {

			writable.println("Missing command, must be one of 'list', 'add', 'delete' or 'password'.");
		}
	}

	@Override
	public void commandHelp(final Writable writable) throws IOException {
		writable.println("Creates and deletes users, sets passwords.");
	}

	@Override
	public void detailHelp(final Writable writable) throws IOException {

		writable.println("user list                          - lists all user in the database");
		writable.println("user add <name> [<e-mail>|isAdmin] - adds a new user with the given name and optional e-mail address");
		writable.println("user delete <name>                 - deletes the user with the given name");
		writable.println("user password <name> <password>    - sets the password for the given user");
	}

	// ----- private methods -----
	private void handleList(final SecurityContext securityContext, final Writable writable) throws FrameworkException, IOException {

		final App app = StructrApp.getInstance(securityContext);
		boolean first = true;

		try (final Tx tx = app.tx()) {

			for (final NodeInterface user : app.nodeQuery(StructrTraits.USER).sort(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getResultStream()) {

				final String name = user.getName();

				if (!first) {
					writable.print(", ");
				}

				writable.print(name);

				first = false;
			}

			writable.println();

			tx.success();
		}
	}

	private void handleAdd(final SecurityContext securityContext, final Writable writable, final String name, final String eMail, final String isAdmin) throws FrameworkException, IOException {

		if (StringUtils.isEmpty(name)) {
			throw new FrameworkException(422, "Missing user name for add command.");
		}

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name));
			final User user          = node.as(User.class);

			// set e-mail address
			if (eMail != null && !"isAdmin".equals(eMail)) {
				user.setEMail(eMail);
			}

			// set isAdmin flag
			if ("isAdmin".equals(eMail) || "isAdmin".equals(isAdmin)) {
				user.setIsAdmin(true);
			}

			writable.println("User created.");

			tx.success();
		}
	}

	private void handleDelete(final SecurityContext securityContext, final Writable writable, final String name, final String confirm) throws FrameworkException, IOException {

		if (StringUtils.isEmpty(name)) {
			throw new FrameworkException(422, "Missing user name for delete command.");
		}

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			NodeInterface user = app.nodeQuery(StructrTraits.USER).andName(name).getFirst();
			if (user == null) {

				user = app.getNodeById(StructrTraits.USER, name);
			}

			if (user != null) {

				final List<NodeInterface> ownedNodes = Iterables.toList(user.as(Principal.class).getOwnedNodes());
				if (ownedNodes.isEmpty()) {

					app.delete(user);

					writable.println("User deleted.");

				} else {

					final String hash = user.getUuid().substring(7, 11);

					if (confirm == null || !confirm.equals(hash)) {

						writable.print("User '");
						writable.print(name);
						writable.print("' has owned nodes, please confirm deletion with 'user delete ");
						writable.print(name);
						writable.print(" ");
						writable.print(hash);
						writable.println("'.");

					} else {

						app.delete(user);

						writable.println("User deleted.");
					}
				}

			} else {

				throw new FrameworkException(422, "User " + name + " not found.");
			}

			tx.success();
		}
	}

	private void handlePwd(final SecurityContext securityContext, final Writable writable, final String name, final String password) throws FrameworkException, IOException {

		if (StringUtils.isEmpty(name)) {
			throw new FrameworkException(422, "Missing user name for password command.");
		}

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.nodeQuery(StructrTraits.USER).andName(name).getFirst();
			if (node != null) {

				final Principal user = node.as(Principal.class);

				if (StringUtils.isNotBlank(password)) {

					user.setPassword(password);

					writable.println("Password changed.");

				} else {

					throw new FrameworkException(422, "Will not set empty password");
				}

			} else {

				throw new FrameworkException(422, "User " + name + " not found.");
			}

			tx.success();
		}
	}
}
