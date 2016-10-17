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
package org.structr.console;

import java.util.Collections;
import static junit.framework.TestCase.assertEquals;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;

public class ConsoleTest extends StructrUiTest {

	public void testUserCommand() {

		final Console console = new Console(securityContext, Collections.emptyMap());
		Principal admin       = null;

		try {

			assertEquals("Invalid console execution result", "\r\nMode set to 'AdminShell'. Type 'help' to get a list of commands.\r\n", console.run("Console.setMode('shell')"));
			assertEquals("Invalid console execution result", "\r\n\r\n", console.run("user list"));

			// create a user
			assertEquals("Invalid console execution result", "\r\nUser created.\r\n", console.run("user add tester tester@test.de"));
			assertEquals("Invalid console execution result", "\r\nUser created.\r\n", console.run("user add admin admin@localhost isAdmin"));
			assertEquals("Invalid console execution result", "\r\nUser created.\r\n", console.run("user add root isAdmin"));

			// check success
			try (final Tx tx = app.tx()) {

				final User user = app.nodeQuery(User.class).andName("tester").getFirst();

				assertNotNull("Invalid console execution result", user);
				assertEquals("Invalid console execution result", "tester",         user.getProperty(User.name));
				assertEquals("Invalid console execution result", "tester@test.de", user.getProperty(User.eMail));
				assertEquals("Invalid console execution result", Boolean.FALSE,    user.getProperty(User.isAdmin));

				tx.success();
			}

			// check list
			assertEquals("Invalid console execution result", "\r\nadmin, root, tester\r\n", console.run("user list"));

			// delete user
			assertEquals("Invalid console execution result", "\r\nUser deleted.\r\n", console.run("user delete tester"));

			// check list
			assertEquals("Invalid console execution result", "\r\nadmin, root\r\n", console.run("user list"));

			// check "root" user
			try (final Tx tx = app.tx()) {

				final User root = app.nodeQuery(User.class).andName("root").getFirst();

				assertNotNull("Invalid console execution result", root);
				assertEquals("Invalid console execution result", "root",           root.getProperty(User.name));
				assertEquals("Invalid console execution result", Boolean.TRUE,      root.getProperty(User.isAdmin));

				tx.success();
			}

			// make check "admin" user
			try (final Tx tx = app.tx()) {

				admin = app.nodeQuery(User.class).andName("admin").getFirst();

				assertNotNull("Invalid console execution result", admin);
				assertEquals("Invalid console execution result", "admin",           admin.getProperty(User.name));
				assertEquals("Invalid console execution result", "admin@localhost", admin.getProperty(User.eMail));
				assertEquals("Invalid console execution result", Boolean.TRUE,      admin.getProperty(User.isAdmin));

				final Folder folder = app.create(Folder.class, "folder");
				folder.setProperty(Folder.owner, admin);

				tx.success();
			}

			final String idHash = admin.getUuid().substring(7, 11);

			// delete user without confirmation
			assertEquals("Invalid console execution result", "\r\nUser 'admin' has owned nodes, please confirm deletion with 'user delete admin " + idHash + "'.\r\n", console.run("user delete admin"));

			// delete user with confirmation
			assertEquals("Invalid console execution result", "\r\nUser deleted.\r\n", console.run("user delete admin " + idHash));

			// check list
			assertEquals("Invalid console execution result", "\r\nroot\r\n", console.run("user list"));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

}
