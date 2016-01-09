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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.Iterables;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Group;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestTwo;
import org.structr.core.graph.BulkCreateLabelsCommand;
import org.structr.core.graph.BulkRebuildIndexCommand;
import org.structr.core.graph.BulkSetNodePropertiesCommand;
import org.structr.core.graph.Tx;

/**
 *
 *
 */
public class TestBulkCommands extends StructrTest {

	public void testBulkCreateLabelsCommand() {

		try {

			final GraphDatabaseService graphDb = app.getGraphDatabaseService();
			final Set<Label> expectedLabels    = new LinkedHashSet<>();

			expectedLabels.add(DynamicLabel.label("Principal"));
			expectedLabels.add(DynamicLabel.label("Group"));
			expectedLabels.add(DynamicLabel.label("AccessControllable"));
			expectedLabels.add(DynamicLabel.label("AbstractUser"));
			expectedLabels.add(DynamicLabel.label("AbstractNode"));
			expectedLabels.add(DynamicLabel.label("NodeInterface"));
			expectedLabels.add(DynamicLabel.label("CMISInfo"));
			expectedLabels.add(DynamicLabel.label("CMISItemInfo"));

			// intentionally create raw Neo4j transaction and create nodes in there
			try (Transaction tx = graphDb.beginTx()) {

				for (int i=0; i<100; i++) {

					final Node test = graphDb.createNode();

					// set ID and type so that the rebuild index command identifies it as a Structr node.
					test.setProperty("type", "Group");
					test.setProperty("id", UUID.randomUUID().toString().replace("-", ""));
				}

				// this is important.... :)
				tx.success();
			}

			// nodes should not be found yet..
			try (final Tx tx = app.tx()) {

				// check nodes, we should find 100 Groups here
				assertEquals(0, app.nodeQuery(Group.class).getResult().size());
			}

			// test rebuild index and create labels
			app.command(BulkRebuildIndexCommand.class).execute(new LinkedHashMap<String, Object>());
			app.command(BulkCreateLabelsCommand.class).execute(new LinkedHashMap<String, Object>());

			// nodes should now be visible to Structr
			try (final Tx tx = app.tx()) {

				// check nodes, we should find 100 Groups here
				assertEquals(100, app.nodeQuery(Group.class).getResult().size());

				// check nodes
				for (final Group group : app.nodeQuery(Group.class)) {

					final Set<Label> labels = Iterables.toSet(group.getNode().getLabels());

					assertEquals(8, labels.size());
					assertTrue(labels.containsAll(expectedLabels));
				}


			}

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

	}

	public void testBulkRebuildIndexCommand() {

		try {

			final GraphDatabaseService graphDb = app.getGraphDatabaseService();

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
			app.command(BulkRebuildIndexCommand.class).execute(new LinkedHashMap<String, Object>());

			// nodes should now be visible to Structr
			try (final Tx tx = app.tx()) {

				// check nodes, we should find 100 TestOnes here, and none TestTwos
				assertEquals(100, app.nodeQuery(TestOne.class).getResult().size());
				tx.success();
			}

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

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

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}
}
