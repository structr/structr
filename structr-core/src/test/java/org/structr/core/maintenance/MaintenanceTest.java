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
package org.structr.core.maintenance;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.Transaction;
import org.structr.api.graph.Label;
import org.structr.api.graph.Node;
import org.structr.api.util.Iterables;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.entity.Group;
import org.structr.core.entity.TestEleven;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestTwo;
import org.structr.core.graph.BulkCreateLabelsCommand;
import org.structr.core.graph.BulkSetNodePropertiesCommand;
import org.structr.core.graph.SyncCommand;
import org.structr.core.graph.Tx;

/**
 *
 *
 */
public class MaintenanceTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(MaintenanceTest.class.getName());

	private final static String EXPORT_FILENAME = "___structr-test-export___.zip";

	@Test
	public void testSyncCommandParameters() {

		try {

			// test failure with non-existing mode
			try {

				// test failure with wrong mode
				app.command(SyncCommand.class).execute(toMap("mode", "non-existing-mode", "file", EXPORT_FILENAME));
				fail("Using SyncCommand with a non-existing mode should throw an exception.");

			} catch (FrameworkException fex) {

				// status: 400
				assertEquals(400, fex.getStatus());
				assertEquals("Please specify sync mode (import|export).", fex.getMessage());
			}

			// test failure with omitted file name
			try {

				// test failure with wrong mode
				app.command(SyncCommand.class).execute(toMap("mode", "export"));
				fail("Using SyncCommand without file parameter should throw an exception.");

			} catch (FrameworkException fex) {

				// status: 400
				assertEquals(400, fex.getStatus());
				assertEquals("Please specify sync file.", fex.getMessage());
			}

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testSyncCommandBasicExportImport() {

		try {
			// create test nodes
			createTestNodes(TestOne.class, 100);

			// test export
			app.command(SyncCommand.class).execute(toMap("mode", "export", "file", EXPORT_FILENAME));

			final Path exportFile = Paths.get(EXPORT_FILENAME);

 			assertTrue("Export file doesn't exist!", Files.exists(exportFile));

			// clear database
			cleanDatabase();

			// test import
			app.command(SyncCommand.class).execute(toMap("mode", "import", "file", EXPORT_FILENAME));

			try (final Tx tx = app.tx()) {
				assertEquals(100, app.nodeQuery(TestOne.class).getResult().size());
			}

			// clean-up after test
			Files.delete(exportFile);

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception.");
		}

	}

	@Test
	public void testSyncCommandBasicExportImportSmallBatchSize() {

		try {
			// create test nodes
			createTestNodes(TestOne.class, 100);

			// test export
			app.command(SyncCommand.class).execute(toMap("mode", "export", "file", EXPORT_FILENAME));

			final Path exportFile = Paths.get(EXPORT_FILENAME);

 			assertTrue("Export file doesn't exist!", Files.exists(exportFile));

			cleanDatabase();

			// test import
			app.command(SyncCommand.class).execute(toMap("mode", "import", "file", EXPORT_FILENAME, "batchSize", 20L));

			try (final Tx tx = app.tx()) {
				assertEquals(100, app.nodeQuery(TestOne.class).getResult().size());
			}

			// clean-up after test
			Files.delete(exportFile);

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testSyncCommandInheritance() {

		try {
			// create test nodes
			final List<TestEleven> testNodes = createTestNodes(TestEleven.class, 10);

			try (final Tx tx = app.tx()) {

				for (final TestEleven node : testNodes) {

					Iterable<Label> labels = node.getNode().getLabels();

					assertEquals(7, Iterables.count(labels));

					for (final Label label : labels) {
						System.out.print(label.name() + " ");
					}
					System.out.println();

					assertEquals("First label has to be AbstractNode",       Iterables.toList(labels).get(0).name(), "AbstractNode");
					assertEquals("Second label has to be NodeInterface",     Iterables.toList(labels).get(1).name(), "NodeInterface");
					assertEquals("Third label has to be AccessControllable", Iterables.toList(labels).get(2).name(), "AccessControllable");
					assertEquals("Fourth label has to be CMISInfo",          Iterables.toList(labels).get(3).name(), "CMISInfo");
					assertEquals("Firth label has to be CMISItemInfo",       Iterables.toList(labels).get(4).name(), "CMISItemInfo");
					assertEquals("Sixth label has to be TestOne",            Iterables.toList(labels).get(5).name(), "TestOne");
					assertEquals("Seventh label has to be TestEleven",       Iterables.toList(labels).get(6).name(), "TestEleven");
				}

				tx.success();
			}


			// test export
			app.command(SyncCommand.class).execute(toMap("mode", "export", "file", EXPORT_FILENAME));

			final Path exportFile = Paths.get(EXPORT_FILENAME);

 			assertTrue("Export file doesn't exist!", Files.exists(exportFile));

			// stop existing and start new database
			stopSystem();
			startSystem(Collections.emptyMap());

			// test import
			app.command(SyncCommand.class).execute(toMap("mode", "import", "file", EXPORT_FILENAME));

			final DatabaseService db = app.getDatabaseService();


			try (final Tx tx = app.tx()) {

				final Result<TestEleven> result = app.nodeQuery(TestEleven.class).getResult();
				assertEquals(10, result.size());

				for (final TestEleven node : result.getResults()) {

					Iterable<Label> labels = node.getNode().getLabels();
					final Set<Label> set   = new HashSet<>(Iterables.toList(labels));

					assertEquals(7, set.size());

					assertTrue("First label has to be AbstractNode",       set.contains(db.forName(Label.class, "AbstractNode")));
					assertTrue("Second label has to be NodeInterface",     set.contains(db.forName(Label.class, "NodeInterface")));
					assertTrue("Third label has to be AccessControllable", set.contains(db.forName(Label.class, "AccessControllable")));
					assertTrue("Fourth label has to be CMISInfo",          set.contains(db.forName(Label.class, "CMISInfo")));
					assertTrue("Firth label has to be CMISItemInfo",       set.contains(db.forName(Label.class, "CMISItemInfo")));
					assertTrue("Sixth label has to be TestEleven",         set.contains(db.forName(Label.class, "TestEleven")));
					assertTrue("Seventh label has to be TestOne",          set.contains(db.forName(Label.class, "TestOne")));

				}

				tx.success();
			}

			// clean-up after test
			Files.delete(exportFile);

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testBulkCreateLabelsCommand() {

		try {

			final DatabaseService graphDb   = app.getDatabaseService();
			final Set<Label> expectedLabels = new LinkedHashSet<>();

			expectedLabels.add(graphDb.forName(Label.class, "Principal"));
			expectedLabels.add(graphDb.forName(Label.class, "Group"));
			expectedLabels.add(graphDb.forName(Label.class, "AccessControllable"));
			expectedLabels.add(graphDb.forName(Label.class, "AbstractUser"));
			expectedLabels.add(graphDb.forName(Label.class, "AbstractNode"));
			expectedLabels.add(graphDb.forName(Label.class, "NodeInterface"));
			expectedLabels.add(graphDb.forName(Label.class, "CMISInfo"));
			expectedLabels.add(graphDb.forName(Label.class, "CMISItemInfo"));

			// intentionally create raw Neo4j transaction and create nodes in there
			try (Transaction tx = graphDb.beginTx()) {

				for (int i=0; i<100; i++) {

					final Node test = graphDb.createNode(Collections.EMPTY_SET, Collections.EMPTY_MAP);

					// set ID and type so that the rebuild index command identifies it as a Structr node.
					test.setProperty("type", "Group");
					test.setProperty("id", UUID.randomUUID().toString().replace("-", ""));
				}

				// this is important.... :)
				tx.success();
			}

			/*
			 * This test will fail with the new Neo4j 3.0 Bolt interface, because
			 * there is no separation between a (Lucene-based) index and the
			 * database values any more. Nodes are selected by their 'type'
			 * property and will always be found even if NOT created using Structr
			 * methods.
			// nodes should not be found yet..
			try (final Tx tx = app.tx()) {

				// check nodes, we should find no Groups here
				assertEquals(0, app.nodeQuery(Group.class).getResult().size());
			}
			 */

			// test rebuild index and create labels
			//app.command(BulkRebuildIndexCommand.class).execute(new LinkedHashMap<>());
			app.command(BulkCreateLabelsCommand.class).execute(new LinkedHashMap<>());

			// nodes should now be visible to Structr
			try (final Tx tx = app.tx()) {

				// check nodes, we should find 100 Groups here
				assertEquals(100, app.nodeQuery(Group.class).getResult().size());

				// check nodes
				for (final Group group : app.nodeQuery(Group.class)) {

					final Set<Label> labels = Iterables.toSet(group.getNode().getLabels());

					assertEquals("Invalid number of labels", 8, labels.size());
					assertTrue("Invalid labels found", labels.containsAll(expectedLabels));
				}

				tx.success();
			}

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

	}

	/*
	 * This test will fail with the new Neo4j 3.0 Bolt interface, because
	 * there is no separation between a (Lucene-based) index and the
	 * database values any more. Nodes are selected by their 'type'
	 * property and will always be found even if NOT created using Structr
	 * methods.
	public void testBulkRebuildIndexCommand() {

		try {

			final DatabaseService graphDb = app.getDatabaseService();

			// intentionally create raw Neo4j transaction and create nodes in there
			try (Transaction tx = graphDb.beginTx()) {

				for (int i=0; i<100; i++) {

					final Node test = graphDb.createNode();

					// set ID and type so that the rebuild index command identifies it as a Structr node.
					test.setProperty("type", "TestOne");
					test.setProperty("id", UUID.randomUUID().toString().replace("-", ""));
				}

				// this is important.... :)
				tx.success();
			}

			// nodes should not be found yet..
			try (final Tx tx = app.tx()) {

				// check nodes, we should find 0 TestOnes here, and none TestTwos
				assertEquals(0, app.nodeQuery(TestOne.class).getResult().size());
				tx.success();
			}

			// test rebuild index
			app.command(BulkRebuildIndexCommand.class).execute(new LinkedHashMap<>());

			// nodes should now be visible to Structr
			try (final Tx tx = app.tx()) {

				// check nodes, we should find 100 TestOnes here, and none TestTwos
				assertEquals(100, app.nodeQuery(TestOne.class).getResult().size());
				tx.success();
			}

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}
	*/

	@Test
	public void testBulkSetNodePropertiesCommand() {

		final Integer one = 1;

		try {

			// create test nodes first
			createTestNodes(TestOne.class, 100);

			try {

				// test failure with wrong type
				app.command(BulkSetNodePropertiesCommand.class).execute(toMap("type", "NonExistingType"));
				fail("Using BulkSetNodePropertiesCommand with a non-existing type should throw an exception.");

			} catch (FrameworkException fex) {

				// status: 422
				assertEquals(422, fex.getStatus());
				assertEquals("Invalid type NonExistingType", fex.getMessage());
			}

			try {

				// test failure without type
				app.command(BulkSetNodePropertiesCommand.class).execute(toMap("anInt", 1));
				fail("Using BulkSetNodePropertiesCommand without a type property should throw an exception.");

			} catch (FrameworkException fex) {

				// status: 422
				assertEquals(422, fex.getStatus());
				assertEquals("Type must not be empty", fex.getMessage());
			}

			// test success
			app.command(BulkSetNodePropertiesCommand.class).execute(toMap("type", "TestOne", "anInt", 1, "aString", "one"));

			try (final Tx tx = app.tx()) {

				// check nodes, we should find 100 TestOnes here, and none TestTwos
				assertEquals(  0, app.nodeQuery(TestTwo.class).getResult().size());
				assertEquals(100, app.nodeQuery(TestOne.class).getResult().size());

				// check nodes
				for (final TestOne test : app.nodeQuery(TestOne.class)) {

					assertEquals(one, test.getProperty(TestOne.anInt));
					assertEquals("one", test.getProperty(TestOne.aString));
				}
			}

			// advanced: modify type
			app.command(BulkSetNodePropertiesCommand.class).execute(toMap("type", "TestOne", "newType", "TestTwo"));

			try (final Tx tx = app.tx()) {

				// check nodes, we should find 100 TestTwos here, and none TestOnes
				assertEquals(  0, app.nodeQuery(TestOne.class).getResult().size());
				assertEquals(100, app.nodeQuery(TestTwo.class).getResult().size());
			}

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}
}
