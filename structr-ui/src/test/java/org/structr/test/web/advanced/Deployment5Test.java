/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.test.web.advanced;

import java.util.function.Function;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Group;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.SchemaGrant;
import org.structr.core.entity.SchemaNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.web.entity.User;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.Test;

public class Deployment5Test extends DeploymentTestBase {

	@Test
	public void test51SchemaGrantsRoundtrip() {

		/*
		 * This method verifies that schema-based permissions survive an export/import deployment
		 * roundtrip even if the UUID of the group changes. The test simulates the deployment of
		 * an application from one server to another with differen groups.
		 */

		// setup
		try (final Tx tx = app.tx()) {

			// Create a group with name "SchemaAccess" and allow access to all nodes of type "MailTemplate"
			final SchemaNode schemaNode = app.nodeQuery(SchemaNode.class).andName("MailTemplate").getFirst();
			final Group group           = app.create(Group.class, "SchemaAccess");
			final User user             = app.create(User.class, "tester");

			group.addMember(securityContext, user);

			// create schema grant object
			app.create(SchemaGrant.class,
				new NodeAttribute<>(SchemaGrant.schemaNode,  schemaNode),
				new NodeAttribute<>(SchemaGrant.principal,   group),
				new NodeAttribute<>(SchemaGrant.allowRead,   true),
				new NodeAttribute<>(SchemaGrant.allowWrite,  true),
				new NodeAttribute<>(SchemaGrant.allowDelete, true)
			);

			// create MailTemplate instances
			app.create(MailTemplate.class, "TEMPLATE1");
			app.create(MailTemplate.class, "TEMPLATE2");

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test1: verify that user is allowed to access MailTemplates
		try (final Tx tx = app.tx()) {

			final User user                   = app.nodeQuery(User.class).andName("tester").getFirst();
			final SecurityContext userContext = SecurityContext.getInstance(user, AccessMode.Backend);

			for (final MailTemplate template : app.nodeQuery(MailTemplate.class).getAsList()) {

				assertTrue("User should have read access to all mail templates", template.isGranted(Permission.read, userContext));
				assertTrue("User should have write access to all mail templates", template.isGranted(Permission.write, userContext));
				assertTrue("User should have delete access to all mail templates", template.isGranted(Permission.delete, userContext));
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// deployment export, clean database, create new group with same name but different ID, deployment import
		doImportExportRoundtrip(true, true, new Function() {

			@Override
			public Object apply(final Object o) {

				try (final Tx tx = app.tx()) {

					final Group group = app.create(Group.class, "SchemaAccess");
					final User user   = app.create(User.class, "tester");

					group.addMember(securityContext, user);

					tx.success();

				} catch (FrameworkException fex) {
					fex.printStackTrace();
					fail("Unexpected exception.");
				}

				return null;
			}
		});

		// test2: verify that new user is allowed to access MailTemplates
		try (final Tx tx = app.tx()) {

			final User user                   = app.nodeQuery(User.class).andName("tester").getFirst();
			final SecurityContext userContext = SecurityContext.getInstance(user, AccessMode.Backend);

			for (final MailTemplate template : app.nodeQuery(MailTemplate.class).getAsList()) {

				assertTrue("User should have read access to all mail templates", template.isGranted(Permission.read, userContext));
				assertTrue("User should have write access to all mail templates", template.isGranted(Permission.write, userContext));
				assertTrue("User should have delete access to all mail templates", template.isGranted(Permission.delete, userContext));
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}
}
