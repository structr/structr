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

import org.apache.commons.lang.StringUtils;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.util.Writable;

import java.io.IOException;
import java.util.Iterator;
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

			final PrincipalInterface user = securityContext.getUser(false);
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
	private void handleList(final SecurityContext securityContext, final Writable writable) throws FrameworkException, IOException{

		final Class<NodeInterface> type = StructrApp.getConfiguration().getNodeEntityClass("User");
		final App app                   = StructrApp.getInstance(securityContext);

		if (type != null) {

			try (final Tx tx = app.tx()) {

				final List<NodeInterface> users = app.nodeQuery(type).sort(AbstractNode.name).getAsList();
				for (final Iterator<NodeInterface> it = users.iterator(); it.hasNext();) {

					final NodeInterface user = it.next();
					final String name        = user.getProperty(AbstractNode.name);

					if (name != null) {

						writable.print(name);

					} else {

						writable.print(user.getUuid());
					}

					if (it.hasNext()) {

						writable.print(", ");
					}
				}

				writable.println();

				tx.success();
			}

		} else {

			throw new FrameworkException(422, "Cannot list users, no User class found.");
		}
	}

	private void handleAdd(final SecurityContext securityContext, final Writable writable, final String name, final String eMail, final String isAdmin) throws FrameworkException, IOException {

		if (StringUtils.isEmpty(name)) {
			throw new FrameworkException(422, "Missing user name for add command.");
		}

		final App app    = StructrApp.getInstance(securityContext);
		final Class type = StructrApp.getConfiguration().getNodeEntityClass("User");

		if (type != null) {

			try (final Tx tx = app.tx()) {

				final PrincipalInterface user = (PrincipalInterface)app.create(type, new NodeAttribute<>(AbstractNode.name, name));

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

		} else {

			throw new FrameworkException(422, "Cannot create user, no User class found.");
		}
	}

	private void handleDelete(final SecurityContext securityContext, final Writable writable, final String name, final String confirm) throws FrameworkException, IOException {

		if (StringUtils.isEmpty(name)) {
			throw new FrameworkException(422, "Missing user name for delete command.");
		}

		final Class<? extends NodeInterface> type = StructrApp.getConfiguration().getNodeEntityClass("User");
		final App app                             = StructrApp.getInstance(securityContext);

		if (type != null) {

			try (final Tx tx = app.tx()) {

				NodeInterface user = app.nodeQuery(type).andName(name).getFirst();
				if (user == null) {

					user = app.get(type, name);
				}

				if (user != null) {

					final List<NodeInterface> ownedNodes = Iterables.toList(user.getProperty(PrincipalInterface.ownedNodes));
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

		} else {

			throw new FrameworkException(422, "Cannot delete user, no User class found.");
		}
	}

	private void handlePwd(final SecurityContext securityContext, final Writable writable, final String name, final String password) throws FrameworkException, IOException {

		if (StringUtils.isEmpty(name)) {
			throw new FrameworkException(422, "Missing user name for password command.");
		}

		final Class<? extends PrincipalInterface> type = StructrApp.getConfiguration().getNodeEntityClass("User");
		final App app                          = StructrApp.getInstance(securityContext);

		if (type != null) {

			try (final Tx tx = app.tx()) {

				final PrincipalInterface user = app.nodeQuery(type).andName(name).getFirst();
				if (user != null) {

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

		} else {

			throw new FrameworkException(422, "Cannot change password, no User class found.");
		}
	}
}
