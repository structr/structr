/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.test;

import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.User;

/**
 *
 */
public class UserTest extends StructrUiTest {

	public void test001EMailAddressConstraint() {

		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute(User.name, "TestUser1"),
				new NodeAttribute(User.eMail, "user@structr.test")
			);

			app.create(User.class,
				new NodeAttribute(User.name, "TestUser2"),
				new NodeAttribute(User.eMail, "user@structr.test")
			);

			tx.success();

			fail("Expected exception to be thrown.");

		} catch (FrameworkException fex) {
			assertEquals("Invalid error code", 422, fex.getStatus());
		}

		check();

		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute(User.name, "TestUser1"),
				new NodeAttribute(User.eMail, "user@structr.test")
			);

			app.create(User.class,
				new NodeAttribute(User.name, "TestUser2"),
				new NodeAttribute(User.eMail, "User@Structr.test")
			);

			tx.success();

			fail("Expected exception to be thrown.");

		} catch (FrameworkException fex) {
			assertEquals("Invalid error code", 422, fex.getStatus());
		}

		check();
	}

	private void check() {

		try (final Tx tx = app.tx()) {

			final List<User> users = app.nodeQuery(User.class).getAsList();

			assertEquals("Expected no users to be created because of constraints", 0, users.size());

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
			fex.printStackTrace();
		}

	}
}
