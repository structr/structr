/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.test.core.maintenance;

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
import java.util.stream.Collectors;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.Transaction;
import org.structr.api.graph.Label;
import org.structr.api.graph.Node;
import org.structr.api.util.Iterables;
import org.structr.test.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Group;
import org.structr.test.core.entity.TestEleven;
import org.structr.test.core.entity.TestOne;
import org.structr.test.core.entity.TestTwo;
import org.structr.core.graph.BulkCreateLabelsCommand;
import org.structr.core.graph.BulkRebuildIndexCommand;
import org.structr.core.graph.BulkSetNodePropertiesCommand;
import org.structr.core.graph.SyncCommand;
import org.structr.core.graph.Tx;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

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
			cleanDatabaseAndSchema();

			// test import
			app.command(SyncCommand.class).execute(toMap("mode", "import", "file", EXPORT_FILENAME));

			try (final Tx tx = app.tx()) {
				assertEquals(100, app.nodeQuery(TestOne.class).getAsList().size());
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

			cleanDatabaseAndSchema();

			// test import
			app.command(SyncCommand.class).execute(toMap("mode", "import", "file", EXPORT_FILENAME, "batchSize", 20L));

			try (final Tx tx = app.tx()) {
				assertEquals(100, app.nodeQuery(TestOne.class).getAsList().size());
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
			final String tenantIdentifier    = app.getDatabaseService().getTenantIdentifier();
			int labelCount                   = 7;

			// one additional label
			if (tenantIdentifier != null) {
				labelCount += 1;
			}

			try (final Tx tx = app.tx()) {

				for (final TestEleven node : testNodes) {

					Iterable<Label> labels = node.getNode().getLabels();

					assertEquals(labelCount, Iterables.count(labels));

					for (final Label label : labels) {
						System.out.print(label.name() + " ");
					}
					System.out.println();

					final Set<String> names = Iterables.toSet(labels).stream().map(Label::name).collect(Collectors.toSet());

					assertEquals("Number of labels must be 7", labelCount, names.size());
					assertTrue("Set of labels must contain AbstractNode",       names.contains("AbstractNode"));
					assertTrue("Set of labels must contain NodeInterface",      names.contains("NodeInterface"));
					assertTrue("Set of labels must contain AccessControllable", names.contains("AccessControllable"));
					assertTrue("Set of labels must contain CMISInfo",           names.contains("CMISInfo"));
					assertTrue("Set of labels must contain CMISItemInfo",       names.contains("CMISItemInfo"));
					assertTrue("Set of labels must contain TestOne",            names.contains("TestOne"));
					assertTrue("Set of labels must contain TestEleven",         names.contains("TestEleven"));

					if (tenantIdentifier != null) {
						assertTrue("Set of labels must contain custom tenant identifier if set", names.contains(tenantIdentifier));
					}
				}

				tx.success();
			}


			// test export
			app.command(SyncCommand.class).execute(toMap("mode", "export", "file", EXPORT_FILENAME));

			final Path exportFile = Paths.get(EXPORT_FILENAME);

 			assertTrue("Export file doesn't exist!", Files.exists(exportFile));

			// clear database
			cleanDatabaseAndSchema();

			// test import
			app.command(SyncCommand.class).execute(toMap("mode", "import", "file", EXPORT_FILENAME));

			final DatabaseService db = app.getDatabaseService();


			try (final Tx tx = app.tx()) {

				final List<TestEleven> result = app.nodeQuery(TestEleven.class).getAsList();
				assertEquals(10, result.size());

				for (final TestEleven node : result) {

					Iterable<Label> labels = node.getNode().getLabels();
					final Set<Label> set   = new HashSet<>(Iterables.toList(labels));

					assertEquals(labelCount, set.size());

					assertTrue("First label has to be AbstractNode",       set.contains(db.forName(Label.class, "AbstractNode")));
					assertTrue("Second label has to be NodeInterface",     set.contains(db.forName(Label.class, "NodeInterface")));
					assertTrue("Third label has to be AccessControllable", set.contains(db.forName(Label.class, "AccessControllable")));
					assertTrue("Fourth label has to be CMISInfo",          set.contains(db.forName(Label.class, "CMISInfo")));
					assertTrue("Firth label has to be CMISItemInfo",       set.contains(db.forName(Label.class, "CMISItemInfo")));
					assertTrue("Sixth label has to be TestEleven",         set.contains(db.forName(Label.class, "TestEleven")));
					assertTrue("Seventh label has to be TestOne",          set.contains(db.forName(Label.class, "TestOne")));

					if (tenantIdentifier != null) {
						assertTrue("Set of labels must contain custom tenant identifier if set", set.contains(db.forName(Label.class, tenantIdentifier)));
					}

				}

				tx.success();
			}

			// clean-up after test
			Files.delete(exportFile);

		} catch (Exception ex) {
			ex.printStackTrace();
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
			expectedLabels.add(graphDb.forName(Label.class, "AbstractNode"));
			expectedLabels.add(graphDb.forName(Label.class, "NodeInterface"));
			expectedLabels.add(graphDb.forName(Label.class, "CMISInfo"));
			expectedLabels.add(graphDb.forName(Label.class, "CMISItemInfo"));

			if (graphDb.getTenantIdentifier() != null) {
				expectedLabels.add(graphDb.forName(Label.class, graphDb.getTenantIdentifier()));
			}

			// intentionally create raw Neo4j transaction and create nodes in there
			try (Transaction tx = graphDb.beginTx()) {

				for (int i=0; i<100; i++) {

					final Node test = graphDb.createNode("Group", Collections.EMPTY_SET, Collections.EMPTY_MAP);

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
				assertEquals(0, app.nodeQuery(Group.class).getAsList().size());
			}
			 */

			// test rebuild index and create labels
			//app.command(BulkRebuildIndexCommand.class).execute(new LinkedHashMap<>());
			app.command(BulkCreateLabelsCommand.class).execute(new LinkedHashMap<>());

			// nodes should now be visible to Structr
			try (final Tx tx = app.tx()) {

				// check nodes, we should find 100 Groups here
				assertEquals(100, app.nodeQuery(Group.class).getAsList().size());

				// check nodes
				for (final Group group : app.nodeQuery(Group.class)) {

					final Set<Label> labels = Iterables.toSet(group.getNode().getLabels());

					assertEquals("Invalid number of labels", expectedLabels.size(), labels.size());
					assertTrue("Invalid labels found", labels.containsAll(expectedLabels));
				}

				tx.success();
			}

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

	}

	@Test
	public void testBulkRebuildIndexCommand() {

		try {

			// nodes should not be found yet..
			try (final Tx tx = app.tx()) {

				createTestNodes(TestOne.class, 100);
				tx.success();
			}

			// test rebuild index
			app.command(BulkRebuildIndexCommand.class).execute(new LinkedHashMap<>());

			// nodes should now be visible to Structr
			try (final Tx tx = app.tx()) {

				// check nodes, we should find 100 TestOnes here, and none TestTwos
				assertEquals(100, app.nodeQuery(TestOne.class).getAsList().size());
				tx.success();
			}

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

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
				assertEquals(  0, app.nodeQuery(TestTwo.class).getAsList().size());
				assertEquals(100, app.nodeQuery(TestOne.class).getAsList().size());

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
				assertEquals(  0, app.nodeQuery(TestOne.class).getAsList().size());
				assertEquals(100, app.nodeQuery(TestTwo.class).getAsList().size());
			}

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}
}
