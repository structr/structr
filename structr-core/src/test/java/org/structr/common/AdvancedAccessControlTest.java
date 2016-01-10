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
package org.structr.common;

import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestUser;
import org.structr.core.graph.Tx;

//~--- classes ----------------------------------------------------------------

/**
 * Test access control with different permission levels.
 *
 *
 */
public class AdvancedAccessControlTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(AdvancedAccessControlTest.class.getName());


	public void test01WriteAccess() {

		// remove auto-generated resource access objects
		clearResourceAccess();

		try {

			final TestUser owner = createTestNode(TestUser.class);
			final TestUser user  = createTestNode(TestUser.class);

			// create new node
			final TestOne t1     = createTestNode(TestOne.class, owner);

			final SecurityContext ownerContext = SecurityContext.getInstance(owner, AccessMode.Frontend);
			final SecurityContext userContext  = SecurityContext.getInstance(user, AccessMode.Frontend);
			final App ownerAppContext          = StructrApp.getInstance(ownerContext);
			final App userAppContext           = StructrApp.getInstance(userContext);

			// test with owner, expect success
			try (final Tx tx = ownerAppContext.tx()) {

				final TestOne t = StructrApp.getInstance(ownerContext).nodeQuery(TestOne.class).getFirst();

				assertNotNull(t);

				t.setProperty(TestOne.aString, "aString");
				assertEquals("aString", t.getProperty(TestOne.aString));

				tx.success();
			}

			// test with foreign user, expect failure, node should not be found
			try (final Tx tx = userAppContext.tx()) {

				// node should not be found
				assertNull(StructrApp.getInstance(userContext).nodeQuery(TestOne.class).getFirst());

				tx.success();
			}

			// test with foreign user, expect failure, node should not be found
			try (final Tx tx = ownerAppContext.tx()) {

				// make node visible to user
				t1.grant(Permission.read, user);

				tx.success();
			}

			// try to grant read permissions in user context, should fail because user doesn't have access control permission
			try (final Tx tx = userAppContext.tx()) {

				try {
					final TestOne t = StructrApp.getInstance(userContext).nodeQuery(TestOne.class).getFirst();
					t.grant(Permission.read, user);

					fail("Non-owner should not be allowed to change permissions on object");

				} catch (FrameworkException fex) {

					// expect status 403 forbidden
					assertEquals(fex.getStatus(), 403);

				}

				tx.success();
			}

			// try to grant read permissions in owner context, should succeed (?)
			try (final Tx tx = ownerAppContext.tx()) {

				// important lesson here: the context under which the node is constructed defines the security context
				final TestOne t = StructrApp.getInstance(ownerContext).nodeQuery(TestOne.class).getFirst();
				t.grant(Permission.accessControl, user);
				tx.success();
			}

			// test with foreign user, expect failure
			try (final Tx tx = userAppContext.tx()) {

				final TestOne t = StructrApp.getInstance(userContext).nodeQuery(TestOne.class).getFirst();

				// node should be found because it's public
				assertNotNull(t);

				// setProperty should fail because of missing write permissions
				try {

					t.setProperty(TestOne.aString, "aString");
					fail("setProperty should not be allowed for non-owner on publicly visible nodes");

				} catch (FrameworkException fex) {

					// expect status 403 forbidden
					assertEquals(fex.getStatus(), 403);
				}

				tx.success();
			}

			// grant write
			try (final Tx tx = app.tx()) {

				// make t1 visible to public users explicitely
				t1.setProperty(GraphObject.visibleToPublicUsers, true);

				tx.success();
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");

		}

	}















	public static void clearResourceAccess() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final ResourceAccess access : app.nodeQuery(ResourceAccess.class).getAsList()) {
				app.delete(access);
			}

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
		}
	}

}
