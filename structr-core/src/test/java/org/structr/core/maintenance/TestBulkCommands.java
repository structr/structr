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
 * @author Christian Morgner
 */
public class TestBulkCommands extends StructrTest {

	public void testBulkCreateLabelsCommand() {

		try {
			
			final GraphDatabaseService graphDb = app.getGraphDatabaseService();
			final Set<Label> expectedLabels    = new LinkedHashSet<>();
			
			expectedLabels.add(DynamicLabel.label("Group"));
			expectedLabels.add(DynamicLabel.label("Principal"));
			expectedLabels.add(DynamicLabel.label("AccessControllable"));
			
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
					
					assertEquals(3, labels.size());
					assertTrue(expectedLabels.containsAll(labels));
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

				// check nodes, we should find 100 TestOnes here, and none TestTwos
				assertEquals(0, app.nodeQuery(TestOne.class).getResult().size());
			}

			// test rebuild index
			app.command(BulkRebuildIndexCommand.class).execute(new LinkedHashMap<String, Object>());
			
			// nodes should now be visible to Structr
			try (final Tx tx = app.tx()) {

				// check nodes, we should find 100 TestOnes here, and none TestTwos
				assertEquals(100, app.nodeQuery(TestOne.class).getResult().size());
			}

		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	public void testBulkSetNodePropertiesCommand() {
		
		final Integer one = Integer.valueOf(1);
		
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
