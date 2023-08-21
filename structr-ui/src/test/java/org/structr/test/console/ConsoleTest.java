/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.test.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.console.Console;
import org.structr.console.Console.ConsoleMode;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.testng.AssertJUnit.*;

public class ConsoleTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(ConsoleTest.class.getName());

	@Test
	public void testSwitchModes() {

		try {

			final Console console = new Console(securityContext, ConsoleMode.JavaScript, Collections.emptyMap());

			assertEquals("Invalid console execution result", "Mode set to 'StructrScript'.\r\n", console.runForTest("Console.setMode('" + ConsoleMode.StructrScript.name() + "')"));
			assertEquals("Invalid console execution result", "Mode set to 'Cypher'.\r\n",        console.runForTest("Console.setMode('" + ConsoleMode.Cypher.name() + "')"));
			assertEquals("Invalid console execution result", "Mode set to 'JavaScript'.\r\n",    console.runForTest("Console.setMode('" + ConsoleMode.JavaScript.name() + "')"));
			assertEquals("Invalid console execution result", "Mode set to 'AdminShell'. Type 'help' to get a list of commands.\r\n", console.runForTest("Console.setMode('" + ConsoleMode.AdminShell.name() + "')"));
			assertEquals("Invalid console execution result", "Mode set to 'REST'. Type 'help' to get a list of commands.\r\n", console.runForTest("Console.setMode('" + ConsoleMode.REST.name() + "')"));


		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
			logger.warn("", fex);
		}
	}

	@Test
	public void testUserCommand() {

		final Console console = new Console(securityContext, ConsoleMode.JavaScript, Collections.emptyMap());
		Principal admin       = null;

		try {

			assertEquals("Invalid console execution result", "Mode set to 'AdminShell'. Type 'help' to get a list of commands.\r\n", console.runForTest("Console.setMode('" + ConsoleMode.AdminShell.name() + "')"));
			assertEquals("Invalid console execution result", "\r\n", console.runForTest("user list"));

			// create a user
			assertEquals("Invalid console execution result", "User created.\r\n", console.runForTest("user add tester tester@test.de"));
			assertEquals("Invalid console execution result", "User created.\r\n", console.runForTest("user add admin admin@localhost isAdmin"));
			assertEquals("Invalid console execution result", "User created.\r\n", console.runForTest("user add root isAdmin"));

			// check success
			try (final Tx tx = app.tx()) {

				final User user = app.nodeQuery(User.class).andName("tester").sort(AbstractNode.name).getFirst();

				assertNotNull("Invalid console execution result", user);
				assertEquals("Invalid console execution result", "tester",         user.getProperty(User.name));
				assertEquals("Invalid console execution result", "tester@test.de", user.getEMail());
				assertEquals("Invalid console execution result", Boolean.FALSE,    (Boolean)user.isAdmin());

				tx.success();
			}

			// check list
			assertEquals("Invalid console execution result", "admin, root, tester\r\n", console.runForTest("user list"));

			// delete user
			assertEquals("Invalid console execution result", "User deleted.\r\n", console.runForTest("user delete tester"));

			// check list
			assertEquals("Invalid console execution result", "admin, root\r\n", console.runForTest("user list"));

			// check "root" user
			try (final Tx tx = app.tx()) {

				final User root = app.nodeQuery(User.class).andName("root").getFirst();

				assertNotNull("Invalid console execution result", root);
				assertEquals("Invalid console execution result", "root",           root.getProperty(User.name));
				assertEquals("Invalid console execution result", Boolean.TRUE,     (Boolean)root.isAdmin());

				tx.success();
			}

			// make check "admin" user
			try (final Tx tx = app.tx()) {

				admin = app.nodeQuery(User.class).andName("admin").getFirst();

				assertNotNull("Invalid console execution result", admin);
				assertEquals("Invalid console execution result", "admin",           admin.getProperty(User.name));
				assertEquals("Invalid console execution result", "admin@localhost", admin.getEMail());
				assertEquals("Invalid console execution result", Boolean.TRUE,      (Boolean)admin.isAdmin());

				final Folder folder = app.create(Folder.class, "folder");
				folder.setProperties(folder.getSecurityContext(), new PropertyMap(Folder.owner, admin));

				tx.success();
			}

			final String idHash = admin.getUuid().substring(7, 11);

			// delete user without confirmation
			assertEquals("Invalid console execution result", "User 'admin' has owned nodes, please confirm deletion with 'user delete admin " + idHash + "'.\r\n", console.runForTest("user delete admin"));

			// delete user with confirmation
			assertEquals("Invalid console execution result", "User deleted.\r\n", console.runForTest("user delete admin " + idHash));

			// check list
			assertEquals("Invalid console execution result", "root\r\n", console.runForTest("user list"));

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testRebuildCommand() {

		final Console console = new Console(securityContext, ConsoleMode.JavaScript, Collections.emptyMap());
		final int nodeCount      = 2401;
		final int relCount       = 2817;
		final int typedNodeCount = 594;

		final String fullIndexRebuildOutput =
			"Node type not set or no entity class found. Starting (re-)indexing all nodes\r\n" +
			"RebuildNodeIndex: 1000 objects processed\r\n" +
			"RebuildNodeIndex: 2000 objects processed\r\n" +
			"RebuildNodeIndex: " + nodeCount + " objects processed\r\n" +
			"RebuildNodeIndex: " + nodeCount + " objects processed\r\n" +
			"Done with (re-)indexing " + nodeCount + " nodes\r\n" +
			"Relationship type not set, starting (re-)indexing all relationships\r\n" +
			"RebuildRelIndex: 1000 objects processed\r\n" +
			"RebuildRelIndex: 2000 objects processed\r\n" +
			"RebuildRelIndex: " + relCount + " objects processed\r\n" +
			"RebuildRelIndex: " + relCount + " objects processed\r\n" +
			"Done with (re-)indexing " + relCount + " relationships\r\n";

		final String nodeIndexRebuildOutput =
			"Node type not set or no entity class found. Starting (re-)indexing all nodes\r\n" +
			"RebuildNodeIndex: 1000 objects processed\r\n" +
			"RebuildNodeIndex: 2000 objects processed\r\n" +
			"RebuildNodeIndex: " + nodeCount + " objects processed\r\n" +
			"RebuildNodeIndex: " + nodeCount + " objects processed\r\n" +
			"Done with (re-)indexing " + nodeCount + " nodes\r\n";

		final String relIndexRebuildOutput =
			"Relationship type not set, starting (re-)indexing all relationships\r\n" +
			"RebuildRelIndex: 1000 objects processed\r\n" +
			"RebuildRelIndex: 2000 objects processed\r\n" +
			"RebuildRelIndex: " + relCount + " objects processed\r\n" +
			"RebuildRelIndex: " + relCount + " objects processed\r\n" +
			"Done with (re-)indexing " + relCount + " relationships\r\n";

		final String typedNodeIndexRebuildOutput =
			"Starting (re-)indexing all nodes of type ResourceAccess\r\n" +
			"RebuildNodeIndex: " + typedNodeCount + " objects processed\r\n" +
			"RebuildNodeIndex: " + typedNodeCount + " objects processed\r\n" +
			"Done with (re-)indexing " + typedNodeCount + " nodes\r\n";

		final String typedRelationshipIndexRebuildOutput =
			"Starting (re-)indexing all relationships of type ResourceAccess\r\n" +
			"RebuildRelIndex: 0 objects processed\r\n" +
			"Done with (re-)indexing 0 relationships\r\n";

		final String createNodeUuidsOutput =
			"Start setting UUID on all nodes\r\n" +
			"SetNodeUuid: 1000 objects processed\r\n" +
			"SetNodeUuid: 2000 objects processed\r\n" +
			"SetNodeUuid: " + nodeCount + " objects processed\r\n" +
			"SetNodeUuid: " + nodeCount + " objects processed\r\n" +
			"Done with setting UUID on " + nodeCount + " nodes\r\n";

		final String createNodeUuidsOnUserOutput =
			"Start setting UUID on nodes of type User\r\n" +
			"SetNodeUuid: 0 objects processed\r\n" +
			"Done with setting UUID on 0 nodes\r\n";

		final String createRelUuidsOnUserOutput =
			"Start setting UUID on rels of type User\r\n" +
			"SetRelationshipUuid: 0 objects processed\r\n" +
			"Done with setting UUID on 0 relationships\r\n";

		final String createRelUuidsOutput =
			"Start setting UUID on all rels\r\n" +
			"SetRelationshipUuid: 1000 objects processed\r\n" +
			"SetRelationshipUuid: 2000 objects processed\r\n" +
			"SetRelationshipUuid: " + relCount + " objects processed\r\n" +
			"SetRelationshipUuid: " + relCount + " objects processed\r\n" +
			"Done with setting UUID on " + relCount + " relationships\r\n";

		final String createLabelsOutput =
			"Node type not set or no entity class found. Starting creation of labels for all nodes.\r\n" +
			"CreateLabels: " + nodeCount + " objects processed\r\n" +
			"CreateLabels: " + nodeCount + " objects processed\r\n" +
			"Done with creating labels on " + nodeCount + " nodes\r\n";

		final String createUserLabelsOutput =
			"Starting creation of labels for all nodes of type User\r\n" +
			"CreateLabels: 0 objects processed\r\n" +
			"Done with creating labels on 0 nodes\r\n";

		try {

			assertEquals("Invalid console execution result", "Mode set to 'AdminShell'. Type 'help' to get a list of commands.\r\n", console.runForTest("Console.setMode('" + ConsoleMode.AdminShell.name() + "')"));

			// test syntax parser
			assertEquals("Invalid console execution result", "Please specify what to initialize.\r\n", console.runForTest("init"));
			assertEquals("Invalid console execution result", "Index type must be specified before the 'index' keyword.\r\n", console.runForTest("init index node"));
			assertEquals("Invalid console execution result", "Entity type must be specified before the 'ids' keyword.\r\n", console.runForTest("init ids node"));
			assertEquals("Invalid console execution result", "Syntax error, too many parameters.\r\n", console.runForTest("init ids ids"));
			assertEquals("Invalid console execution result", "Please specify what to initialize.\r\n", console.runForTest("init node node"));
			assertEquals("Invalid console execution result", "Please specify what to initialize.\r\n", console.runForTest("init rel node"));
			assertEquals("Invalid console execution result", "Please specify what to initialize.\r\n", console.runForTest("init rel rel"));
			assertEquals("Invalid console execution result", "Please specify what to initialize.\r\n", console.runForTest("init relationship node"));
			assertEquals("Invalid console execution result", "Please specify what to initialize.\r\n", console.runForTest("init relationship rel"));
			assertEquals("Invalid console execution result", "Please specify what to initialize.\r\n", console.runForTest("init relationship relationship"));
			assertEquals("Invalid console execution result", "Unknown init mode 'for'.\r\n", console.runForTest("init for index"));
			assertEquals("Invalid console execution result", "Unknown init mode 'for'.\r\n", console.runForTest("init for for"));

			assertEquals("Invalid console execution result", "Unknown init mode 'test'.\r\n", console.runForTest("init test"));
			assertEquals("Invalid console execution result", "Unknown init mode 'test'.\r\n", console.runForTest("init test index"));
			assertEquals("Invalid console execution result", "Syntax error, please specify something like 'init node index for User'.\r\n", console.runForTest("init node ids test"));
			assertEquals("Invalid console execution result", "Syntax error, please specify something like 'init node index for User'.\r\n", console.runForTest("init index abc"));
			assertEquals("Invalid console execution result", "Syntax error, too many parameters.\r\n", console.runForTest("init index index"));
			assertEquals("Invalid console execution result", "Missing type specification, please specify something like 'init node index for User'.\r\n", console.runForTest("init index for"));
			assertEquals("Invalid console execution result", "Missing type specification, please specify something like 'init node index for User'.\r\n", console.runForTest("init node index for"));
			assertEquals("Invalid console execution result", "Missing type specification, please specify something like 'init node index for User'.\r\n", console.runForTest("init relationship index for"));
			assertEquals("Invalid console execution result", "Missing type specification, please specify something like 'init node index for User'.\r\n", console.runForTest("init rel index for"));
			assertEquals("Invalid console execution result", "Syntax error, too many parameters.\r\n", console.runForTest("init index for User test test"));
			assertEquals("Invalid console execution result", "Syntax error, too many parameters.\r\n", console.runForTest("init node index for User test"));
			assertEquals("Invalid console execution result", "Syntax error, too many parameters.\r\n", console.runForTest("init node index for User test test"));
			assertEquals("Invalid console execution result", "Syntax error, too many parameters.\r\n", console.runForTest("init rel index for User test"));
			assertEquals("Invalid console execution result", "Syntax error, too many parameters.\r\n", console.runForTest("init rel index for User test test"));
			assertEquals("Invalid console execution result", "Syntax error, too many parameters.\r\n", console.runForTest("init relationship index for User test"));
			assertEquals("Invalid console execution result", "Syntax error, too many parameters.\r\n", console.runForTest("init relationship index for User test test"));

			// test actual command execution
			assertEquals("Invalid console execution result", fullIndexRebuildOutput, console.runForTest("init index"));
			assertEquals("Invalid console execution result", nodeIndexRebuildOutput, console.runForTest("init node index"));
			assertEquals("Invalid console execution result", relIndexRebuildOutput, console.runForTest("init relationship index"));
			assertEquals("Invalid console execution result", relIndexRebuildOutput, console.runForTest("init rel index"));

			assertEquals("Invalid console execution result", typedNodeIndexRebuildOutput, console.runForTest("init node index for ResourceAccess"));
			assertEquals("Invalid console execution result", typedRelationshipIndexRebuildOutput, console.runForTest("init relationship index for ResourceAccess"));
			assertEquals("Invalid console execution result", typedRelationshipIndexRebuildOutput, console.runForTest("init rel index for ResourceAccess"));

			assertEquals("Invalid console execution result", createNodeUuidsOutput, console.runForTest("init ids"));
			assertEquals("Invalid console execution result", createNodeUuidsOutput, console.runForTest("init node ids"));
			assertEquals("Invalid console execution result", createRelUuidsOutput, console.runForTest("init relationship ids"));
			assertEquals("Invalid console execution result", createRelUuidsOutput, console.runForTest("init rel ids"));
			assertEquals("Invalid console execution result", createNodeUuidsOnUserOutput, console.runForTest("init ids for User"));
			assertEquals("Invalid console execution result", createNodeUuidsOnUserOutput, console.runForTest("init node ids for User"));
			assertEquals("Invalid console execution result", createRelUuidsOnUserOutput, console.runForTest("init rel ids for User"));
			assertEquals("Invalid console execution result", createRelUuidsOnUserOutput, console.runForTest("init relationship ids for User"));

			System.out.println(console.runForTest("init labels for User"));

			assertEquals("Invalid console execution result", createLabelsOutput, console.runForTest("init labels"));
			assertEquals("Invalid console execution result", createLabelsOutput, console.runForTest("init node labels"));
			assertEquals("Invalid console execution result", createUserLabelsOutput, console.runForTest("init labels for User"));
			assertEquals("Invalid console execution result", createUserLabelsOutput, console.runForTest("init node labels for User"));
			assertEquals("Invalid console execution result", "Cannot set labels on relationships.\r\n", console.runForTest("init relationship labels"));
			assertEquals("Invalid console execution result", "Cannot set labels on relationships.\r\n", console.runForTest("init rel labels"));
			assertEquals("Invalid console execution result", "Cannot set labels on relationships.\r\n", console.runForTest("init rel labels for User"));
			assertEquals("Invalid console execution result", "Cannot set labels on relationships.\r\n", console.runForTest("init relationship labels for User"));
			assertEquals("Invalid console execution result", "Unknown init mode 'test'.\r\n", console.runForTest("init test relationship labels"));
			assertEquals("Invalid console execution result", "Unknown init mode 'blah'.\r\n", console.runForTest("init rel blah labels"));
			assertEquals("Invalid console execution result", "Cannot set labels on relationships.\r\n", console.runForTest("init rel labels for test User"));
			assertEquals("Invalid console execution result", "Unknown init mode 'test'.\r\n", console.runForTest("init test relationship test labels for User"));


		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
			logger.warn("", fex);
		}
	}
}
