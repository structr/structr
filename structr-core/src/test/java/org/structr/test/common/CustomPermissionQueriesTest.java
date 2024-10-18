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
package org.structr.test.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseFeature;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

/**
 * Test access control based on custom queries.
 */
public class CustomPermissionQueriesTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(CustomPermissionQueriesTest.class.getName());

	@Test
	public void test01SimplePermissionResolutionRead() {

		// don't run tests that depend on Cypher being available in the backend
		if (Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.QueryLanguage, "application/x-cypher-query")) {

			PrincipalInterface user1                      = null;
			Class type1                          = null;

			try (final Tx tx = app.tx()) {

				// create a test user
				user1 = app.create(User.class, "user1");

				final SchemaNode t1 = app.create(SchemaNode.class, "Type1");

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception");
			}

			assertNotNull("User should have been created", user1);

			try (final Tx tx = app.tx()) {

				type1 = StructrApp.getConfiguration().getNodeEntityClass("Type1");

				assertNotNull("Node type Type1 should exist.", type1);

				final NodeInterface instance1 = app.create(type1, "instance1OfType1");

				assertNotNull("Instance of type Type1 should exist", instance1);

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception");
			}

			// check access for user1 on instance1
			final App userApp = StructrApp.getInstance(SecurityContext.getInstance(user1, AccessMode.Backend));
			try (final Tx tx = userApp.tx()) {

				assertNull("User1 should NOT be able to find instance of type Type1", userApp.nodeQuery(type1).getFirst());

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception");
			}

			// set custom permission query on user
			try (final Tx tx = userApp.tx()) {

				// query returns always true if user exists
				user1.setProperty(StructrApp.key(User.class, "customPermissionQueryRead"), "MATCH (p:Principal:" + randomTenantId + "{id: $principalUuid}) RETURN p IS NOT NULL");

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception");
			}

			// check access for user1 on instance1
			try (final Tx tx = userApp.tx()) {

				assertNotNull("User1 should be able to find instance of type Type1", userApp.nodeQuery(type1).getFirst());

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception");
			}

			// set custom permission query on user
			try (final Tx tx = userApp.tx()) {

				// query returns always false if user exists
				user1.setProperty(StructrApp.key(User.class, "customPermissionQueryRead"), "MATCH (p:Principal:" + randomTenantId + "{id: $principalUuid}) RETURN p IS NULL");

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception");
			}

			// check access for user1 on instance1
			try (final Tx tx = userApp.tx()) {

				assertNull("User1 should NOT be able to find instance of type Type1", userApp.nodeQuery(type1).getFirst());

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception");
			}
		}
	}

	@Test
	public void test02SimplePermissionResolutionWrite() {

		// don't run tests that depend on Cypher being available in the backend
		if (Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.QueryLanguage, "application/x-cypher-query")) {

			PrincipalInterface user1                      = null;
			Class type1                          = null;

			try (final Tx tx = app.tx()) {

				// create a test user
				user1 = app.create(User.class, "user1");

				final SchemaNode t1 = app.create(SchemaNode.class, "Type1");

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception");
			}

			assertNotNull("User should have been created", user1);

			try (final Tx tx = app.tx()) {

				type1 = StructrApp.getConfiguration().getNodeEntityClass("Type1");

				assertNotNull("Node type Type1 should exist.", type1);

				final NodeInterface instance1 = app.create(type1, "instance1OfType1");

				assertNotNull("Instance of type Type1 should exist", instance1);

				// make instance1 visible to user1
				instance1.grant(Permission.read, user1);

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception");
			}

			// check access for user1 on instance1
			final App userApp = StructrApp.getInstance(SecurityContext.getInstance(user1, AccessMode.Backend));
			try (final Tx tx = userApp.tx()) {

				userApp.nodeQuery(type1).getFirst().setProperty(GraphObject.visibleToPublicUsers, true);

				tx.success();

			} catch (FrameworkException fex) {
				assertEquals("User1 should NOT be able to modify instance of type Type1", 403, fex.getStatus());
			}

			// set custom permission query on user
			try (final Tx tx = userApp.tx()) {

				// query returns always true if user exists
				user1.setProperty(StructrApp.key(User.class, "customPermissionQueryWrite"), "MATCH (p:Principal:" + randomTenantId + "{id: $principalUuid}) RETURN p IS NOT NULL");

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception");
			}

			// check access for user1 on instance1
			try (final Tx tx = userApp.tx()) {

				userApp.nodeQuery(type1).getFirst().setProperty(GraphObject.visibleToPublicUsers, true);

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception");
			}

			// set custom permission query on user
			try (final Tx tx = userApp.tx()) {

				// query returns always false if user exists
				user1.setProperty(StructrApp.key(User.class, "customPermissionQueryRead"), "MATCH (p:Principal:" + randomTenantId + "{id: $principalUuid}) RETURN p IS NULL");

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception");
			}

			// check access for user1 on instance1
			try (final Tx tx = userApp.tx()) {

				userApp.nodeQuery(type1).getFirst().setProperty(GraphObject.visibleToPublicUsers, true);

				tx.success();

			} catch (FrameworkException fex) {
				assertEquals("User1 should NOT be able to modify instance of type Type1", 403, fex.getStatus());
			}
		}
	}

	// ----- private methods -----
	public static void clearResourceAccess() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final ResourceAccess access : app.nodeQuery(ResourceAccess.class).getAsList()) {
				app.delete(access);
			}

			tx.success();

		} catch (Throwable t) {

			logger.warn("Unable to clear resource access permissions", t);
		}
	}
}
