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
package org.structr.test.core.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.Transaction;
import org.structr.api.graph.Node;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.*;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.testng.AssertJUnit.*;

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
			createTestNodes("TestOne", 100);

			// test export
			app.command(SyncCommand.class).execute(toMap("mode", "export", "file", EXPORT_FILENAME));

			final Path exportFile = Paths.get(EXPORT_FILENAME);

 			assertTrue("Export file doesn't exist!", Files.exists(exportFile));

			// clear database
			cleanDatabaseAndSchema();

			// test import
			app.command(SyncCommand.class).execute(toMap("mode", "import", "file", EXPORT_FILENAME));

			try (final Tx tx = app.tx()) {
				assertEquals(100, app.nodeQuery("TestOne").getAsList().size());
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
	public void testSyncCommandBasicExportImportSmallBatchSize() {

		try {
			// create test nodes
			createTestNodes("TestOne", 100);

			// test export
			app.command(SyncCommand.class).execute(toMap("mode", "export", "file", EXPORT_FILENAME));

			final Path exportFile = Paths.get(EXPORT_FILENAME);

 			assertTrue("Export file doesn't exist!", Files.exists(exportFile));

			cleanDatabaseAndSchema();

			// test import
			app.command(SyncCommand.class).execute(toMap("mode", "import", "file", EXPORT_FILENAME, "batchSize", 20L));

			try (final Tx tx = app.tx()) {
				assertEquals(100, app.nodeQuery("TestOne").getAsList().size());
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
	public void testSyncCommandInheritance() {

		try {
			// create test nodes
			final List<NodeInterface> testNodes = createTestNodes("TestEleven", 10);
			final String tenantIdentifier    = app.getDatabaseService().getTenantIdentifier();
			int labelCount                   = Traits.of("TestEleven").getLabels().size();

			// one additional label
			if (tenantIdentifier != null) {
				labelCount += 1;
			}

			try (final Tx tx = app.tx()) {

				for (final NodeInterface node : testNodes) {

					final List<String> labels = Iterables.toList(node.getNode().getLabels());

					assertEquals(labelCount, Iterables.count(labels));

					for (final String label : labels) {
						System.out.print(label + " ");
					}
					System.out.println();

					assertEquals("Number of labels must be " + labelCount, labelCount,      labels.size());
					assertTrue("Set of labels must contain PropertyContainer",  labels.contains(StructrTraits.PROPERTY_CONTAINER));
					assertTrue("Set of labels must contain GraphObject",        labels.contains(StructrTraits.GRAPH_OBJECT));
					assertTrue("Set of labels must contain NodeInterface",      labels.contains(StructrTraits.NODE_INTERFACE));
					assertTrue("Set of labels must contain AccessControllable", labels.contains(StructrTraits.ACCESS_CONTROLLABLE));
					assertTrue("Set of labels must contain TestOne",            labels.contains("TestOne"));
					assertTrue("Set of labels must contain TestEleven",         labels.contains("TestEleven"));

					if (tenantIdentifier != null) {
						assertTrue("Set of labels must contain custom tenant identifier if set", labels.contains(tenantIdentifier));
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

			try (final Tx tx = app.tx()) {

				final List<NodeInterface> result = app.nodeQuery("TestEleven").getAsList();
				assertEquals(10, result.size());

				for (final NodeInterface node : result) {

					Iterable<String> labels = node.getNode().getLabels();
					final Set<String> set   = new HashSet<>(Iterables.toList(labels));

					assertEquals(labelCount, set.size());

					assertTrue("Set of labels must contain PropertyContainer",  set.contains(StructrTraits.PROPERTY_CONTAINER));
					assertTrue("Set of labels must contain GraphObject",        set.contains(StructrTraits.GRAPH_OBJECT));
					assertTrue("Set of labels must contain NodeInterface",      set.contains(StructrTraits.NODE_INTERFACE));
					assertTrue("Set of labels must contain AccessControllable", set.contains(StructrTraits.ACCESS_CONTROLLABLE));
					assertTrue("Set of labels must contain TestEleven",         set.contains("TestEleven"));
					assertTrue("Set of labels must contain TestOne",            set.contains("TestOne"));

					if (tenantIdentifier != null) {
						assertTrue("Set of labels must contain custom tenant identifier if set", set.contains(tenantIdentifier));
					}

				}

				tx.success();
			}

			// clean-up after test
			Files.delete(exportFile);

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testBulkAddUUIDsCommand() {

		try {

			final DatabaseService graphDb    = app.getDatabaseService();
			final Set<String> expectedLabels = new LinkedHashSet<>();

			expectedLabels.add(StructrTraits.PRINCIPAL);
			expectedLabels.add(StructrTraits.GROUP);
			expectedLabels.add(StructrTraits.ACCESS_CONTROLLABLE);
			expectedLabels.add(StructrTraits.GRAPH_OBJECT);
			expectedLabels.add(StructrTraits.PROPERTY_CONTAINER);
			expectedLabels.add(StructrTraits.NODE_INTERFACE);

			if (graphDb.getTenantIdentifier() != null) {
				expectedLabels.add(graphDb.getTenantIdentifier());
			}

			// intentionally create raw Neo4j transaction and create nodes in there
			try (Transaction tx = graphDb.beginTx()) {

				for (int i=0; i<3000; i++) {

					// Create nodes with one label and the type Group, and no properties
					graphDb.createNode(StructrTraits.GROUP, Set.of(StructrTraits.GROUP, StructrTraits.NODE_INTERFACE), Map.of());
				}

				tx.success();
			}

			// test set UUIDs command
			final long count = app.command(BulkSetUuidCommand.class).executeWithCount(Map.of("type", StructrTraits.GROUP));

			// ensure at least 3000 nodes have been touched
			assertTrue(count >= 3000);

			// nodes should now be visible to Structr
			try (final Tx tx = app.tx()) {

				// check nodes, we should find 3000 Groups here
				assertEquals(3000, app.nodeQuery(StructrTraits.GROUP).getAsList().size());

				// check nodes
				for (final NodeInterface group : app.nodeQuery(StructrTraits.GROUP).getResultStream()) {

					final Set<String> labels = Iterables.toSet(group.getNode().getLabels());
					assertNotNull("No UUID was set by BulkSetUUIDCommand", group.getUuid());
				}

				tx.success();
			}

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testBulkCreateLabelsCommand() {

		try {

			final DatabaseService graphDb    = app.getDatabaseService();
			final Set<String> expectedLabels = new TreeSet<>();

			expectedLabels.add(StructrTraits.PROPERTY_CONTAINER);
			expectedLabels.add(StructrTraits.GRAPH_OBJECT);
			expectedLabels.add(StructrTraits.NODE_INTERFACE);
			expectedLabels.add(StructrTraits.ACCESS_CONTROLLABLE);
			expectedLabels.add(StructrTraits.PRINCIPAL);
			expectedLabels.add(StructrTraits.GROUP);

			if (graphDb.getTenantIdentifier() != null) {
				expectedLabels.add(graphDb.getTenantIdentifier());
			}

			// intentionally create raw Neo4j transaction and create nodes in there
			try (Transaction tx = graphDb.beginTx()) {

				for (int i=0; i<3000; i++) {

					final Node test = graphDb.createNode(StructrTraits.GROUP, Collections.EMPTY_SET, Collections.EMPTY_MAP);

					// set ID and type so that the rebuild index command identifies it as a Structr node.
					test.setProperty("type", StructrTraits.GROUP);
					test.setProperty("id", UUID.randomUUID().toString().replace("-", ""));
				}

				// this is important.... :)
				tx.success();
			}

			// test rebuild index and create labels
			final long count = app.command(BulkCreateLabelsCommand.class).executeWithCount(new LinkedHashMap<>());

			assertTrue(count >= 3000);

			// nodes should now be visible to Structr
			try (final Tx tx = app.tx()) {

				// check nodes, we should find 100 Groups here
				assertEquals(3000, app.nodeQuery(StructrTraits.GROUP).getAsList().size());

				// check nodes
				for (final NodeInterface group : app.nodeQuery(StructrTraits.GROUP).getResultStream()) {

					final Set<String> labels = new TreeSet<>(Iterables.toSet(group.getNode().getLabels()));

					assertEquals("Invalid labels", expectedLabels, labels);
				}

				tx.success();
			}

		} catch (Throwable fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

	}

	@Test
	public void testBulkRebuildIndexCommand() {

		try {

			// nodes should not be found yet..
			try (final Tx tx = app.tx()) {

				createTestNodes("TestOne", 3000);
				tx.success();
			}

			// test rebuild index
			final long count = app.command(BulkRebuildIndexCommand.class).executeWithCount(new LinkedHashMap<>());

			// Ensure count of processed nodes is greater or equal than 3000
			assertTrue(count >= 3000);

			// nodes should now be visible to Structr
			try (final Tx tx = app.tx()) {

				// check nodes, we should find 100 TestOnes here, and none TestTwos
				assertEquals(3000, app.nodeQuery("TestOne").getAsList().size());
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
			createTestNodes("TestOne", 3000);

			try {

				// test failure with wrong type
				app.command(BulkSetNodePropertiesCommand.class).executeWithCount(toMap("type", "NonExistingType"));
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
			final long count = app.command(BulkSetNodePropertiesCommand.class).executeWithCount(toMap("type", "TestOne", "anInt", 1, "aString", "one"));

			// ensure that at least 3000 nodes were affected by the bulk command
			assertTrue(count >= 3000);

			try (final Tx tx = app.tx()) {

				// check nodes, we should find 100 TestOnes here, and none TestTwos
				assertEquals(  0, app.nodeQuery("TestTwo").getAsList().size());
				assertEquals(3000, app.nodeQuery("TestOne").getAsList().size());

				// check nodes
				for (final NodeInterface test : app.nodeQuery("TestOne").getResultStream()) {

					assertEquals(one, test.getProperty(Traits.of("TestOne").key("anInt")));
					assertEquals("one", test.getProperty(Traits.of("TestOne").key("aString")));
				}
			}

			// advanced: modify type
			app.command(BulkSetNodePropertiesCommand.class).execute(toMap("type", "TestOne", "newType", "TestTwo"));

			try (final Tx tx = app.tx()) {

				// check nodes, we should find 100 TestTwos here, and none TestOnes
				assertEquals(  0, app.nodeQuery("TestOne").getAsList().size());
				assertEquals(3000, app.nodeQuery("TestTwo").getAsList().size());

				tx.success();
			}

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testClearDatabaseCommand() {

		// setup 1: create test types
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.addType("Test123");
			schema.addType("OneTwoThreeFour");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final String test1 = "Test123";
		final String test2 = "OneTwoThreeFour";

		// setup 2: create test nodes
		try (final Tx tx = app.tx()) {

			createTestNodes(test1, 100);
			createTestNodes(test2, 100);

			app.create(StructrTraits.GROUP, "Group1");

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test 1: clear database
		try (final Tx tx = app.tx()) {

			app.command(ClearDatabase.class).execute();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test 2: verify that nothing except the initial schema is there..
		try (final Tx tx = app.tx()) {

			assertNull("Database was not cleaned correctly by ClearDatabase command", app.nodeQuery(StructrTraits.SCHEMA_NODE).andName("Test123").getFirst());
			assertNull("Database was not cleaned correctly by ClearDatabase command", app.nodeQuery(StructrTraits.SCHEMA_NODE).andName("OneTwoTreeFour").getFirst());

			assertNull("Database was not cleaned correctly by ClearDatabase command", app.nodeQuery(test1).getFirst());
			assertNull("Database was not cleaned correctly by ClearDatabase command", app.nodeQuery(test2).getFirst());
			assertNull("Database was not cleaned correctly by ClearDatabase command", app.nodeQuery(StructrTraits.GROUP).getFirst());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

	}
}
