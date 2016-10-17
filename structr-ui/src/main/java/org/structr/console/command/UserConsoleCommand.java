/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.console.command;

import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Person;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.util.Writable;

/**
 *
 */
public class UserConsoleCommand extends ConsoleCommand {

	static {
		ConsoleCommand.registerCommand("user", UserConsoleCommand.class);
	}

	@Override
	public String run(final SecurityContext securityContext, final List<String> parameters, final Writable writable) throws FrameworkException {

		final StringBuilder buf = new StringBuilder("\r\n");
		final String command    = getParameter(parameters, 1);

		if (command != null) {

			final Principal user = securityContext.getUser(false);
			if (user != null && user.isAdmin()) {

				switch (command) {

					case "list":
						handleList(securityContext, buf);
						break;

					case "add":
						handleAdd(securityContext, buf, getParameter(parameters, 2), getParameter(parameters, 3), getParameter(parameters, 4));
						break;

					case "delete":
						handleDelete(securityContext, buf, getParameter(parameters, 2), getParameter(parameters, 3));
						break;

					case "password":
						handlePwd(securityContext, buf, getParameter(parameters, 2), getParameter(parameters, 3));
						break;
				}

			} else {

				buf.append("You must be admin user to use this command.");
			}

		} else {

			buf.append("Missing command, must be one of 'list', 'add', 'delete' or 'password'.");
		}

		return buf.toString();
	}

	@Override
	public String commandHelp() {
		return "Creates and deletes users, sets passwords.";
	}

	@Override
	public String detailHelp() {

		final StringBuilder buf = new StringBuilder();

		buf.append("user list                          - lists all user in the database\r\n");
		buf.append("user add <name> [<e-mail>|isAdmin] - adds a new user with the given name and optional e-mail address.\r\n");
		buf.append("user delete <name>                 - deletes the user with the given name\r\n");
		buf.append("user password <name> <password>    - sets the password for the given user\r\n");

		return buf.toString();
	}

	// ----- private methods -----
	private void handleList(final SecurityContext securityContext, final StringBuilder buf) throws FrameworkException {

		final Class<NodeInterface> type = StructrApp.getConfiguration().getNodeEntityClass("User");
		final App app                   = StructrApp.getInstance(securityContext);

		if (type != null) {

			try (final Tx tx = app.tx()) {

				final List<NodeInterface> users = app.nodeQuery(type).getAsList();
				for (final Iterator<NodeInterface> it = users.iterator(); it.hasNext();) {

					final NodeInterface user = it.next();
					final String name        = user.getProperty(AbstractNode.name);

					if (name != null) {

						buf.append(name);

					} else {

						buf.append(user.getUuid());
					}

					if (it.hasNext()) {

						buf.append(", ");
					}
				}

				buf.append("\r\n");

				tx.success();
			}

		} else {

			throw new FrameworkException(422, "Cannot list users, no User class found.");
		}
	}

	private void handleAdd(final SecurityContext securityContext, final StringBuilder buf, final String name, final String eMail, final String isAdmin) throws FrameworkException {

		if (StringUtils.isEmpty(name)) {
			throw new FrameworkException(422, "Missing user name for add command.");
		}

		final App app    = StructrApp.getInstance(securityContext);
		final Class type = StructrApp.getConfiguration().getNodeEntityClass("User");

		if (type != null) {

			try (final Tx tx = app.tx()) {

				final NodeInterface user = app.create(type, new NodeAttribute<>(AbstractNode.name, name));

				// set e-mail address
				if (eMail != null && !"isAdmin".equals(eMail)) {
					user.setProperty(Person.eMail, eMail);
				}

				// set isAdmin flag
				if ("isAdmin".equals(eMail) || "isAdmin".equals(isAdmin)) {
					user.setProperty(Principal.isAdmin, true);
				}

				buf.append("User created.\r\n");

				tx.success();
			}

		} else {

			throw new FrameworkException(422, "Cannot create user, no User class found.");
		}
	}

	private void handleDelete(final SecurityContext securityContext, final StringBuilder buf, final String name, final String confirm) throws FrameworkException {

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

					if (user.getProperty(Principal.ownedNodes).isEmpty()) {

						app.delete(user);

						buf.append("User deleted.\r\n");

					} else {

						final String hash = user.getUuid().substring(7, 11);

						if (confirm == null || !confirm.equals(hash)) {

							buf.append("User '");
							buf.append(name);
							buf.append("' has owned nodes, please confirm deletion with 'user delete ");
							buf.append(name);
							buf.append(" ");
							buf.append(hash);
							buf.append("'.\r\n");

						} else {

							app.delete(user);

							buf.append("User deleted.\r\n");
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

	private void handlePwd(final SecurityContext securityContext, final StringBuilder buf, final String name, final String password) throws FrameworkException {

		if (StringUtils.isEmpty(name)) {
			throw new FrameworkException(422, "Missing user name for password command.");
		}

		final Class<? extends NodeInterface> type = StructrApp.getConfiguration().getNodeEntityClass("User");
		final App app                             = StructrApp.getInstance(securityContext);

		if (type != null) {

			try (final Tx tx = app.tx()) {

				final NodeInterface user = app.nodeQuery(type).andName(name).getFirst();
				if (user != null) {

					if (StringUtils.isNotBlank(password)) {

						user.setProperty(Principal.password, password);

						buf.append("Password changed.\r\n");

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
