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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.graph.Cardinality;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.api.util.Iterables;
import org.structr.common.AccessControllable;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.*;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractSchemaNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.SchemaGrantTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;
import org.structr.schema.export.StructrSchema;
import org.structr.test.core.entity.TestEight;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.AssertJUnit.*;

/**
 *
 */
public class SystemTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(SystemTest.class);

	@Test
	public void testCallbacksWithSuperUserContext() {

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		try {
			testCallbacks(securityContext);
		} catch (FrameworkException fex) {
			fail("Unexpected exception");
		}
	}

	@Test
	public void testCallbacksWithNormalContext() {

		try {

			NodeInterface person = this.createTestNode(StructrTraits.USER);

			final SecurityContext securityContext = SecurityContext.getInstance(person.as(Principal.class), null, AccessMode.Backend);
			testCallbacks(securityContext);

		} catch (FrameworkException fex) {

			logger.warn("", fex);
		}
	}

	@Test
	public void testCallbackOrder() {

		final PropertyKey<Integer> testProperty = Traits.of("TestEight").key("testProperty");

		try {

// ##################################### test creation callbacks

			TestEight test = null;

			try (final Tx tx = app.tx()) {

				test = app.create("TestEight", new NodeAttribute(testProperty, 123)).as(TestEight.class);
				tx.success();
			}

			// only the creation methods should have been called now!
			assertTrue("onCreationTimestamp should be != 0", test.getOnCreationTimestamp() != 0L);
			assertEquals("onModificationTimestamp should be == 0", 0L, test.getOnModificationTimestamp());
			assertEquals("onDeletionTimestamp should be == 0", 0L, test.getOnDeletionTimestamp());

			// only the creation methods should have been called now!
			assertTrue("afterCreationTimestamp should be != 0", test.getAfterCreationTimestamp() != 0L);
			assertEquals("afterModificationTimestamp should be == 0", 0L, test.getAfterModificationTimestamp());


// ##################################### test modification callbacks


			// reset timestamps
			test.resetTimestamps();

			try (final Tx tx = app.tx()) {
				test.setProperty(testProperty, 234);
				tx.success();
			}

			// only the modification methods should have been called now!
			assertEquals("onCreationTimestamp should be == 0", 0L, test.getOnCreationTimestamp());
			assertTrue("onModificationTimestamp should be != 0", test.getOnModificationTimestamp() != 0L);
			assertEquals("onDeletionTimestamp should be == 0", 0L, test.getOnDeletionTimestamp());

			// only the modification methods should have been called now!
			assertEquals("afterCreationTimestamp should be == 0", 0L, test.getAfterCreationTimestamp());
			assertTrue("afterModificationTimestamp should be != 0", test.getAfterModificationTimestamp() != 0L);


// ##################################### test non-modifying set operation

			// reset timestamps
			test.resetTimestamps();

			try (final Tx tx = app.tx()) {
				test.setProperty(testProperty, 234);
				tx.success();
			}

			// only the creation methods should have been called now!
			assertEquals("onCreationTimestamp should be == 0", 0L, test.getOnCreationTimestamp());
			assertEquals("onModificationTimestamp should be == 0", 0L, test.getOnModificationTimestamp());
			assertEquals("onDeletionTimestamp should be == 0", 0L, test.getOnDeletionTimestamp());

			// only the creation methods should have been called now!
			assertEquals("afterCreationTimestamp should be == 0", 0L, test.getAfterCreationTimestamp());
			assertEquals("afterModificationTimestamp should be == 0", 0L, test.getAfterModificationTimestamp());



// ##################################### test deletion

			// reset timestamps
			test.resetTimestamps();

			try (final Tx tx = app.tx()) {
				app.delete(test);
				tx.success();
			}

			// only the creation methods should have been called now!
			assertEquals("onCreationTimestamp should be == 0", 0L, test.getOnCreationTimestamp());
			assertEquals("onModificationTimestamp should be == 0", 0L, test.getOnModificationTimestamp());
			assertTrue("onDeletionTimestamp should be != 0", test.getOnDeletionTimestamp() != 0L);

			// only the creation methods should have been called now!
			assertEquals("afterCreationTimestamp should be == 0", 0L, test.getAfterCreationTimestamp());
			assertEquals("afterModificationTimestamp should be == 0", 0L, test.getAfterModificationTimestamp());



		} catch (FrameworkException ex) {

			logger.error("Error", ex);
			fail("Unexpected exception.");
		}
	}

	private void testCallbacks(final SecurityContext securityContext) throws FrameworkException {

		NodeInterface entity = null;
		Integer zero = 0;
		Integer one  = 1;

		try (final Tx tx = app.tx()) {

			entity = app.create("TestFive");
			tx.success();

		} catch (Throwable t) {

			logger.warn("", t);
		}

		assertNotNull("Entity should have been created", entity);

		// creation assertions
		try (final Tx tx = app.tx()) {

			assertEquals("modifiedInBeforeCreation should have a value of 1: ", one, entity.getProperty(Traits.of("TestFive").key("modifiedInBeforeCreation")));
			assertEquals("modifiedInAfterCreation should have a value of 1:  ", one, entity.getProperty(Traits.of("TestFive").key("modifiedInAfterCreation")));

			// modification assertions
			assertEquals("modifiedInBeforeModification should have a value of 0: ", zero, entity.getProperty(Traits.of("TestFive").key("modifiedInBeforeModification")));
			assertEquals("modifiedInAfterModification should have a value of 0:  ", zero, entity.getProperty(Traits.of("TestFive").key("modifiedInAfterModification")));

			tx.success();
		}


		// 2nd part of the test: modify node
		try (final Tx tx = app.tx()) {

			final NodeInterface finalEntity = entity;

			finalEntity.setProperty(Traits.of("TestFive").key("integerProperty"), 123);
			tx.success();

		} catch (Throwable t) {
			logger.warn("", t);
		}

		try (final Tx tx = app.tx()) {

			// creation assertions
			assertEquals("modifiedInBeforeCreation should have a value of 1: ", one, entity.getProperty(Traits.of("TestFive").key("modifiedInBeforeCreation")));
			assertEquals("modifiedInAfterCreation should have a value of 1:  ", one, entity.getProperty(Traits.of("TestFive").key("modifiedInAfterCreation")));

			// modification assertions
			assertEquals("modifiedInBeforeModification should have a value of 1: ", one, entity.getProperty(Traits.of("TestFive").key("modifiedInBeforeModification")));
			assertEquals("modifiedInAfterModification should have a value of 1:  ", one, entity.getProperty(Traits.of("TestFive").key("modifiedInAfterModification")));
		}
	}

	/**
	 * disabled, failing test to check for (existing, confirmed) flaw in parallel node instantiation)
	 */
	@Test
	public void testFlawedParallelInstantiation() {

		final int nodeCount    = 1000;
		String testTypeName    = "CreateTest";
		NodeInterface testType = null;

		// setup: create dynamic type with onCreate() method
		try (final Tx tx = app.tx()) {

			testType = createTestNode(StructrTraits.SCHEMA_NODE, testTypeName);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}


		assertNotNull("Type CreateTest should have been created", testType);

		// second step: create 1000 test nodes
		try (final Tx tx = app.tx()) {

			createTestNodes(testTypeName, nodeCount);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final Traits propertyTraits = Traits.of(StructrTraits.SCHEMA_PROPERTY);
			final Traits methodTraits   = Traits.of(StructrTraits.SCHEMA_METHOD);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(propertyTraits.key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   testType),
				new NodeAttribute<>(propertyTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),           "testCount"),
				new NodeAttribute<>(propertyTraits.key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "Integer")
			);

			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(methodTraits.key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), testType),
				new NodeAttribute<>(propertyTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "onCreate"),
				new NodeAttribute<>(methodTraits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY),      "set(this, 'testCount', size(find('CreateTest')))")
			);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		NodeInterface node = null;

		// third step: create a single node in a separate transaction
		try (final Tx tx = app.tx()) {

			node = createTestNode(testTypeName, "Tester");

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// fourth step: check property value
		try (final Tx tx = app.tx()) {

			final Integer testCount = node.getProperty(Traits.of(testTypeName).key("testCount"));

			assertEquals("Invalid node count, check parallel instantiation!", (int)nodeCount+1, (int)testCount);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testRollbackOnError () {

		final ActionContext ctx = new ActionContext(securityContext, null);

		/**
		 * first the old scripting style
		 */
		NodeInterface testNode = null;

		try (final Tx tx = app.tx()) {

			testNode = createTestNode("TestOne");
			testNode.setProperty(Traits.of("TestOne").key("aString"), "InitialString");
			testNode.setProperty(Traits.of("TestOne").key("anInt"), 42);

			tx.success();

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

		try (final Tx tx = app.tx()) {

			Scripting.replaceVariables(ctx, testNode, "${ ( set(this, 'aString', 'NewString'), set(this, 'anInt', 'NOT_AN_INTEGER') ) }", "testRoolbackOnError");
			fail("StructrScript: setting anInt to 'NOT_AN_INTEGER' should cause an Exception");

			tx.success();

		} catch (FrameworkException expected) { }


		try {

			try (final Tx tx = app.tx()) {

				assertEquals("Property value should still have initial value!", "InitialString", testNode.getProperty(Traits.of("TestOne").key("aString")));

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}
	}

	@Test
	public void testConstraintsConcurrently() {

		/**
		 * This test concurrently creates 1000 nodes in
		 * batches of 10, with 10 threads simultaneously.
		 */

		try (final Tx tx = app.tx()) {

			final Traits nodeTraits     = Traits.of(StructrTraits.SCHEMA_NODE);
			final Traits propertyTraits = Traits.of(StructrTraits.SCHEMA_PROPERTY);

			app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute(nodeTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Item"),
				new NodeAttribute(nodeTraits.key(AbstractSchemaNodeTraitDefinition.SCHEMA_PROPERTIES_PROPERTY),
					Arrays.asList(app.create(
						StructrTraits.SCHEMA_PROPERTY,
						new NodeAttribute(propertyTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name"),
						new NodeAttribute(propertyTraits.key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
						new NodeAttribute(propertyTraits.key(SchemaPropertyTraitDefinition.UNIQUE_PROPERTY), true),
						new NodeAttribute(propertyTraits.key(SchemaPropertyTraitDefinition.INDEXED_PROPERTY), true)
					)
				))
			);

			tx.success();

		} catch (FrameworkException ex) {
			fail("Error creating schema node");
		}

		final String itemType = "Item";

		assertNotNull("Error creating schema node", itemType);

		final Runnable worker = new Runnable() {

			@Override
			public void run() {

				int i = 0;

				while (i < 1000) {

					try (final Tx tx = app.tx()) {

						for (int j=0; j<10 && i<1000; j++) {

							app.create(itemType, "Item" + StringUtils.leftPad(Integer.toString(i++), 5, "0"));
						}

						tx.success();

					} catch (FrameworkException expected) {
					}
				}
			}
		};

		final ExecutorService service = Executors.newFixedThreadPool(10);
		final List<Future> futures    = new LinkedList<>();

		for (int i=0; i<10; i++) {

			futures.add(service.submit(worker));
		}

		// wait for result of async. operations
		for (final Future future : futures) {

			try {
				future.get();

			} catch (Throwable t) {

				logger.warn("", t);
				fail("Unexpected exception");
			}
		}


		try (final Tx tx = app.tx()) {

			final List<NodeInterface> items = app.nodeQuery(itemType).sort(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList();
			int i                           = 0;

			assertEquals("Invalid concurrent constraint test result", 1000, items.size());

			for (final NodeInterface item : items) {

				assertEquals("Invalid name detected", "Item" + StringUtils.leftPad(Integer.toString(i++), 5, "0"), item.getName());
			}

			tx.success();

		} catch (FrameworkException ex) {
			fail("Unexpected exception");
		}

		service.shutdownNow();
	}

	@Test
	public void testTransactionIsolation() {

		// Tests the transaction isolation of the underlying database layer.

		// Create a node and use many different threads to set a property on
		// it in a transaction. Observe the property value to check that the
		// threads do not interfere with each other.

		try {

			final NodeInterface test       = createTestNode("TestOne");
			final ExecutorService executor = Executors.newCachedThreadPool();
			final List<TestRunner> tests   = new LinkedList<>();
			final List<Future> futures     = new LinkedList<>();

			// create and run test runners
			for (int i=0; i<25; i++) {

				final TestRunner runner = new TestRunner(app, test);

				futures.add(executor.submit(runner));
				tests.add(runner);
			}

			// wait for termination
			for (final Future future : futures) {
				future.get();
				System.out.print(".");
			}

			System.out.println();

			// check for success
			for (final TestRunner runner : tests) {
				assertTrue("Could not validate transaction isolation", runner.success());
			}

			executor.shutdownNow();

		} catch (Throwable fex) {
			fail("Unexpected exception");
		}
	}

	@Test
	public void testTransactionIsolationWithFailures() {

		// Tests the transaction isolation of the underlying database layer.

		// Create a node and use ten different threads to set a property on
		// it in a transaction. Observe the property value to check that the
		// threads do not interfere with each other.

		try {

			final NodeInterface test            = createTestNode("TestOne");
			final ExecutorService executor      = Executors.newCachedThreadPool();
			final List<FailingTestRunner> tests = new LinkedList<>();
			final List<Future> futures          = new LinkedList<>();

			// create and run test runners
			for (int i=0; i<25; i++) {

				final FailingTestRunner runner = new FailingTestRunner(app, test);

				futures.add(executor.submit(runner));
				tests.add(runner);
			}

			// wait for termination
			for (final Future future : futures) {
				future.get();
				System.out.print(".");
			}

			System.out.println();

			// check for success
			for (final FailingTestRunner runner : tests) {
				assertTrue("Could not validate transaction isolation", runner.success());
			}

			executor.shutdownNow();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testEnsureOneToOneCardinality() {

		Principal tester1 = null;
		Principal tester2 = null;

		// setup
		try (final Tx tx = app.tx()) {

			tester1 = app.create(StructrTraits.USER, "tester1").as(Principal.class);
			tester2 = app.create(StructrTraits.USER, "tester2").as(Principal.class);

			JsonSchema schema         = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType("Item");

			type.relate(type, "NEXT", Cardinality.OneToOne, "prev", "next");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		final SecurityContext tester1Context = SecurityContext.getInstance(tester1, AccessMode.Backend);
		final SecurityContext tester2Context = SecurityContext.getInstance(tester2, AccessMode.Backend);
		final String type                    = "Item";
		final App tester1App                 = StructrApp.getInstance(tester1Context);
		final App tester2App                 = StructrApp.getInstance(tester2Context);
		final PropertyKey next               = Traits.of(type).key("next");

		try (final Tx tx = tester1App.tx()) {

			final NodeInterface item1 = tester1App.create(type,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "item1"),
				new NodeAttribute<>(next, tester1App.create(type,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "item2"),
					new NodeAttribute<>(next, tester1App.create(type,
						new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "item3")
					))
				))
			);

			// make item1 visible to tester2
			item1.as(AccessControllable.class).grant(Permission.read, tester2);
			item1.as(AccessControllable.class).grant(Permission.write, tester2);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		// test setProperty with different user (should delete the previous relationship)
		try (final Tx tx = tester2App.tx()) {

			final NodeInterface item1 = tester2App.nodeQuery(type).andName("item1").getFirst();

			assertNotNull("Item 1 should be visible to tester2", item1);

			item1.setProperty(next, tester2App.create(type, "item4"));

			tx.success();

		} catch (FrameworkException t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		// check result
		try (final Tx tx = app.tx()) {

			final NodeInterface item1              = app.nodeQuery(type).andName("item1").getFirst();
			final List<RelationshipInterface> rels = Iterables.toList(item1.getOutgoingRelationships());

			for (final RelationshipInterface rel : rels) {
				System.out.println(rel.getType() + ": " + rel.getSourceNodeId() + " -> " + rel.getTargetNodeId());
			}

			assertEquals("OneToOne.ensureCardinality is not wirking correctly", 1, rels.size());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testEnsureOneToManyCardinality() {

		Principal tester1 = null;
		Principal tester2 = null;

		// setup
		try (final Tx tx = app.tx()) {

			tester1 = app.create(StructrTraits.USER, "tester1").as(Principal.class);
			tester2 = app.create(StructrTraits.USER, "tester2").as(Principal.class);

			JsonSchema schema         = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType("Item");

			type.relate(type, "NEXT", Cardinality.OneToMany, "prev", "next");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		final SecurityContext tester1Context = SecurityContext.getInstance(tester1, AccessMode.Backend);
		final SecurityContext tester2Context = SecurityContext.getInstance(tester2, AccessMode.Backend);
		final String itemType                = "Item";
		final App tester1App                 = StructrApp.getInstance(tester1Context);
		final App tester2App                 = StructrApp.getInstance(tester2Context);
		final PropertyKey prev               = Traits.of(itemType).key("prev");

		try (final Tx tx = tester1App.tx()) {

			final NodeInterface item1 = tester1App.create(itemType,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "item1"),
				new NodeAttribute<>(prev, tester1App.create(itemType,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "item2"),
					new NodeAttribute<>(prev, tester1App.create(itemType,
						new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "item3")
					))
				))
			);

			// make item1 visible to tester2
			item1.as(AccessControllable.class).grant(Permission.read, tester2);
			item1.as(AccessControllable.class).grant(Permission.write, tester2);

			tx.success();

		} catch (FrameworkException t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		// test setProperty with different user (should delete the previous relationship)
		try (final Tx tx = tester2App.tx()) {

			final NodeInterface item1 = tester2App.nodeQuery(itemType).andName("item1").getFirst();

			assertNotNull("Item 1 should be visible to tester2", item1);

			item1.setProperty(prev, tester2App.create(itemType, "item4"));

			tx.success();

		} catch (FrameworkException t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		// check result
		try (final Tx tx = app.tx()) {

			final NodeInterface item1                  = app.nodeQuery(itemType).andName("item1").getFirst();
			final List<RelationshipInterface> rels     = Iterables.toList(item1.getIncomingRelationships());
			final List<RelationshipInterface> filtered = new LinkedList<>();

			for (final RelationshipInterface rel : rels) {

				if ("ItemNEXTItem".equals(rel.getType())) {
					filtered.add(rel);
				}
			}

			assertEquals("OneToMany.ensureCardinality is not wirking correctly", 1, filtered.size());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testEnsureManyToOneCardinality() {

		Principal tester1 = null;
		Principal tester2 = null;

		// setup
		try (final Tx tx = app.tx()) {

			tester1 = app.create(StructrTraits.USER, "tester1").as(Principal.class);
			tester2 = app.create(StructrTraits.USER, "tester2").as(Principal.class);

			JsonSchema schema         = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType("Item");

			type.relate(type, "NEXT", Cardinality.ManyToOne, "prev", "next");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		final SecurityContext tester1Context = SecurityContext.getInstance(tester1, AccessMode.Backend);
		final SecurityContext tester2Context = SecurityContext.getInstance(tester2, AccessMode.Backend);
		final String type                    = "Item";
		final App tester1App                 = StructrApp.getInstance(tester1Context);
		final App tester2App                 = StructrApp.getInstance(tester2Context);
		final PropertyKey next               = Traits.of(type).key("next");

		try (final Tx tx = tester1App.tx()) {

			final NodeInterface item1 = tester1App.create(type,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "item1"),
				new NodeAttribute<>(next, tester1App.create(type,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "item2"),
					new NodeAttribute<>(next, tester1App.create(type,
						new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "item3")
					))
				))
			);

			// make item1 visible to tester2
			item1.as(AccessControllable.class).grant(Permission.read, tester2);
			item1.as(AccessControllable.class).grant(Permission.write, tester2);

			tx.success();

		} catch (FrameworkException t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		// test setProperty with different user (should delete the previous relationship)
		try (final Tx tx = tester2App.tx()) {

			final NodeInterface item1 = tester2App.nodeQuery(type).andName("item1").getFirst();

			assertNotNull("Item 1 should be visible to tester2", item1);

			item1.setProperty(next, tester2App.create(type, "item4"));

			tx.success();

		} catch (FrameworkException t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		// check result
		try (final Tx tx = app.tx()) {

			final NodeInterface item1              = app.nodeQuery(type).andName("item1").getFirst();
			final List<RelationshipInterface> rels = Iterables.toList(item1.getOutgoingRelationships());

			for (final RelationshipInterface rel : rels) {
				System.out.println(rel.getType() + ": " + rel.getSourceNodeId() + " -> " + rel.getTargetNodeId());
			}

			assertEquals("ManyToOne.ensureCardinality is not wirking correctly", 1, rels.size());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testEnsureCardinalityPerformance() {

		final List<NodeInterface> list = new LinkedList<>();
		final int num            = 1000;

		// test setup, create a supernode with 10000 relationships
		try (final Tx tx = app.tx()) {

			System.out.println("Creating supernode with " + num + " relationships.");

			list.add(createTestNode("TestOne",
				new NodeAttribute<>(Traits.of("TestOne").key("manyToManyTestSixs"), createTestNodes("TestSix", num))
			));

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// actual test: test performance of node association on supernode
		try (final Tx tx = app.tx()) {

			for (int i=0; i<10; i++) {

				final long t0 = System.currentTimeMillis();
				createTestNode("TestSix", new NodeAttribute<>(Traits.of("TestSix").key("manyToManyTestOnes"), list));
				final long t1 = System.currentTimeMillis();

				System.out.println((t1 - t0) + "ms");

			}

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testPasswordAndHashSecurity() {

		// actual test: test performance of node association on supernode
		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.PRINCIPAL).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "tester"),
				new NodeAttribute<>(Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "password")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception");
		}

		// actual test: test performance of node association on supernode
		try (final Tx tx = app.tx()) {

			final Principal user = app.nodeQuery(StructrTraits.USER).getFirst().as(Principal.class);

			assertEquals("Password hash IS NOT SECURE!", Principal.HIDDEN, user.getProperty(Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.PASSWORD_PROPERTY)));
			assertEquals("Password salt IS NOT SECURE!", Principal.HIDDEN, user.getProperty(Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.SALT_PROPERTY)));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception");
		}
	}

	@Test
	public void testGrantFunctionPerformance() {

		// test setup, create a supernode with 10000 relationships
		try (final Tx tx = app.tx()) {

			// create test group
			app.create(StructrTraits.GROUP, "group");

			JsonSchema schema = StructrSchema.createFromDatabase(app);
			schema.addType("GrantTest").addMethod("onCreation", "grant(first(find('Group')), this, 'read')");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final String type = "GrantTest";

		final long t0 = System.currentTimeMillis();
		try (final Tx tx = app.tx()) {

			for (int i=0; i<1000; i++) {
				app.create(type, "test" + i);
			}

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final long t1 = System.currentTimeMillis();
		System.out.println((t1-t0) + " ms");
	}

	@Test
	public void testConcurrentIdenticalRelationshipCreation() {

		final ExecutorService service = Executors.newCachedThreadPool();

		for (int i=0; i<1000; i++) {

			try {
				final NodeInterface source          = createTestNode("TestSix");
				final NodeInterface target          = createTestNode("TestOne");

				final Future one = service.submit(new RelationshipCreator(source, target));
				final Future two = service.submit(new RelationshipCreator(source, target));

				// wait for completion
				one.get();
				two.get();

				try (final Tx tx = app.tx()) {

					// check for a single relationship since all three parts of
					// both relationships are equal => only one should be created
					final List<NodeInterface> list = Iterables.toList((Iterable)source.getProperty(Traits.of("TestSix").key("oneToManyTestOnes")));

					assertEquals("Invalid concurrent identical relationship creation result", 1, list.size());

					tx.success();
				}

			} catch (ExecutionException | InterruptedException | FrameworkException fex) {
				// success
			}
		}

		service.shutdownNow();
	}

	@Test
	public void testDeletionCallback() {

		/**
		 * This test concurrently creates 1000 nodes in
		 * batches of 10, with 10 threads simultaneously.
		 */

		try (final Tx tx = app.tx()) {

			final NodeInterface deleteTestNode = app.create(StructrTraits.SCHEMA_NODE, "DeleteTest");
			final NodeInterface onDelete     = app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "onDelete"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), deleteTestNode),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "log('deleted')")
			);

			tx.success();

		} catch (FrameworkException ex) {
			fail("Error creating schema node");
		}
	}

	@Test
	public void testFetchByIdCache() {

		try (final Tx tx = app.tx()) {

			app.nodeQuery(StructrTraits.GROUP).getAsList();
			app.nodeQuery(StructrTraits.GROUP).andName("test").getAsList();

			tx.success();

		} catch (FrameworkException ex) {
			fail("Error creating schema node");
		}
	}

	@Test
	public void testBaseUrlInOnSave() {

		/*
		This test verifies that access to the baseUrl keyword in a
		scripting context does not produce a NullPointerException.
		*/

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final JsonType contact        = sourceSchema.addType("Contact");

			contact.addTrait(StructrTraits.PRINCIPAL);
			//contact.setExtends(sourceSchema.getType(StructrTraits.PRINCIPAL));
			contact.addMethod("onModification", "log(baseUrl)");

			StructrSchema.extendDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (Exception t) {
			fail("Unexpected exception.");
		}

		final String type = "Contact";

		try (final Tx tx = StructrApp.getInstance().tx()) {

			app.create(type, "test");

			tx.success();

		} catch (Exception t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final GraphObject node = app.nodeQuery(type).getFirst();

			node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "new name");

			tx.success();

		} catch (Exception t) {
			fail("Unexpected exception.");
		}

	}

	@Test
	public void testCallPrivileged() {

		Principal tester = null;

		try (final Tx tx = StructrApp.getInstance().tx()) {

			tester = createTestNode(StructrTraits.USER, "tester").as(Principal.class);

			// create global schema method that creates another object
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),   "globalTestMethod"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "(log('Before create in globalTestMethod'),create('Test2', 'name', 'test2'),log('After create in globalTestMethod'))")
			);

			tx.success();

		} catch (Exception t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final JsonType test1          = sourceSchema.addType("Test1");
			final JsonType test2          = sourceSchema.addType("Test2");

			test1.addMethod("onCreation", "call_privileged('globalTestMethod')");

			StructrSchema.extendDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (Exception t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String type                 = "Test1";
		final SecurityContext userContext = SecurityContext.getInstance(tester, AccessMode.Backend);

		try (final Tx tx = StructrApp.getInstance(userContext).tx()) {

			app.create(type, "test1");

			tx.success();

		} catch (Exception t) {
			System.out.println(t.getMessage());
			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testIsDeletedFlag() {

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			final JsonObjectType test1 = sourceSchema.addType("Test1");
			final JsonObjectType test2 = sourceSchema.addType("Test2");

			test1.relate(test2, "TEST", Cardinality.OneToOne, "source", "target");

			StructrSchema.extendDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (Exception t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String type1 = "Test1";
		final String type2 = "Test2";

		try (final Tx tx = app.tx()) {

			final NodeInterface test1 = app.create(type1, "test1");
			app.create(type2,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "test2"),
				new NodeAttribute<>(Traits.of(type2).key("source"), test1)
			);

			tx.success();

		} catch (Exception t) {
			System.out.println(t.getMessage());
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface node               = app.nodeQuery(type1).getFirst();
			final List<RelationshipInterface> rels = Iterables.toList(node.getRelationships());
			final Node n                           = node.getNode();

			app.delete(node);

			assertTrue("TransactionCommand.isDeleted() does not work properly", TransactionCommand.isDeleted(n));

			for (final RelationshipInterface rel : rels) {
				final Relationship r = rel.getRelationship();
				assertTrue("TransactionCommand.isDeleted() does not work properly", TransactionCommand.isDeleted(r));
			}

			tx.success();

		} catch (Exception t) {
			System.out.println(t.getMessage());
			t.printStackTrace();
			fail("Unexpected exception.");
		}

	}

	@Test
	public void testConfigurationFile() {

		File tmpConfig = null;

		try {

			tmpConfig = File.createTempFile("structr", "test");

			try (final PrintWriter writer = new PrintWriter(tmpConfig)) {

				writer.println("application.TITLE = Structr Test Title");
				writer.println("application.menu.main = Pages,Code,Schema,Flows,Data");
				writer.println("configured.services = NodeService AgentService CronService SchemaService LogService HttpService FtpService SSHService MailService");
				writer.println("database.driver.mode = remote");
				writer.println("database.connection.url = localhost:7687");
				writer.println("HttpService.servlets = JsonRestServlet HtmlServlet WebSocketServlet CsvServlet UploadServlet ProxyServlet GraphQLServlet FlowServlet");
				writer.println("security.twofactorauthentication.level = 0");
				writer.println("application.schema.automigration = true");
				writer.println("mail.maxEmails = 50");
				writer.println("non.Existing.KEY = 12345b");
				writer.println("nodeextender.log = true");
				writer.println("DATABASE.cAchE.NOde.SIZE = 112233");

				writer.println("thisIsACaseSensitiveCustomKey = CustomValue");
				writer.println("thisIsACaseSensitiveKey.cronExpression = * * * * * *");

			}

			Settings.loadConfiguration(tmpConfig.getAbsolutePath());

		} catch (IOException ioex) {

			fail("Unexpected exception");
		}

		// check configuration

		assertEquals("Invalid configuration setting result", "Structr Test Title", Settings.ApplicationTitle.getValue());
		assertEquals("Invalid configuration setting result", "NodeService AgentService CronService SchemaService LogService HttpService FtpService SSHService MailService", Settings.Services.getValue());
		assertEquals("Invalid configuration setting result", "JsonRestServlet HtmlServlet WebSocketServlet CsvServlet UploadServlet ProxyServlet GraphQLServlet FlowServlet", Settings.Servlets.getValue());
		assertEquals("Invalid configuration setting result", Integer.valueOf(0), Settings.TwoFactorLevel.getValue());
		assertEquals("Invalid configuration setting result", Boolean.valueOf(true), Settings.SchemaAutoMigration.getValue());
		assertEquals("Invalid configuration setting result", Boolean.valueOf(true), Settings.LogSchemaOutput.getValue());
		assertEquals("Invalid configuration setting result", Integer.valueOf(112233), Settings.NodeCacheSize.getValue());

		// config setting will not be found
		assertNull("Invalid configuration setting result", Settings.getBooleanSetting("deployment.export.exportfileuuids"));
		assertNull("Invalid configuration setting result", Settings.getIntegerSetting("mail", "maxemails"));
		assertNull("Invalid configuration setting result", Settings.getStringSetting("non", "existing", "key"));

		// test two custom entries in settings - those should remain untouched (ie not lower-cased)
		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);

			assertEquals("Invalid configuration setting result", "CustomValue", Scripting.evaluate(actionContext, null, "${config('thisIsACaseSensitiveCustomKey')}", "testReadConfig1"));
			assertEquals("Invalid configuration setting result", "* * * * * *", Scripting.evaluate(actionContext, null, "${config('thisIsACaseSensitiveKey.cronExpression')}", "testReadConfig2"));

			tx.success();

		} catch(UnlicensedScriptException |FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		tmpConfig.deleteOnExit();

		Settings.LogSchemaOutput.setValue(false);
	}

	@Test
	public void testOverlappingTransactions() {

		final ExecutorService service = Executors.newCachedThreadPool();

		// setup
		try (final Tx tx = StructrApp.getInstance().tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			final JsonObjectType test = sourceSchema.addType("Test");

			test.addStringProperty("test1");
			test.addStringProperty("test2");

			StructrSchema.extendDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (Exception t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String type      = "Test";
		final PropertyKey key1 = Traits.of(type).key("test1");
		final PropertyKey key2 = Traits.of(type).key("test2");

		// setup
		try (final Tx tx = StructrApp.getInstance().tx()) {

			app.create(type, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Test"), new NodeAttribute<>(key1, "test.key1"), new NodeAttribute<>(key2, "test.key2"));

			tx.success();

		} catch (Exception t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// create two parallel transactions, on that starts first but takes longer
		// and a second one that starts later but finishes first.
		service.submit(() -> {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				final GraphObject node1 = app.nodeQuery(type).andName("Test").getFirst();

				node1.setProperty(key1, "key1.value after thread1");

				// wait before committing transaction
				Thread.sleep(2000);

				tx.success();

			} catch (Exception t) { t.printStackTrace(); }
		});

		service.submit(() -> {

			// wait before executing transaction
			try { Thread.sleep(200); } catch (Throwable t) {}

			try (final Tx tx = StructrApp.getInstance().tx()) {

				final GraphObject node1 = app.nodeQuery(type).andName("Test").getFirst();

				node1.setProperty(key2, "key2.value after thread2");

				tx.success();

			} catch (Exception t) { t.printStackTrace(); }
		});

		try {

			service.awaitTermination(10, TimeUnit.SECONDS);

		} catch (Throwable t) {
			t.printStackTrace();
		}

		service.shutdown();

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final GraphObject node1 = app.nodeQuery(type).andName("Test").getFirst();

			assertEquals("Invalid result for interleaving transactions, transaction isolation violated.", "key1.value after thread1", node1.getProperty(key1));
			assertEquals("Invalid result for interleaving transactions, transaction isolation violated.", "key2.value after thread2", node1.getProperty(key2));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	@Test
	public void testSchemaGrantAutoDelete() {

		// setup - schema type
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			// add test type
			schema.addType("Project");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// setup 2 - schema grant
		try (final Tx tx = app.tx()) {

			final Group testGroup1       = app.create(StructrTraits.GROUP, "Group1").as(Group.class);
			final Group testGroup2       = app.create(StructrTraits.GROUP, "Group2").as(Group.class);
			final Group testGroup3       = app.create(StructrTraits.GROUP, "Group3").as(Group.class);

			// create group hierarchy
			testGroup1.addMember(securityContext, testGroup2);
			testGroup2.addMember(securityContext, testGroup3);

			final Principal user = app.create(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "user"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "password")
			).as(Principal.class);

			testGroup3.addMember(securityContext, user);

			final NodeInterface projectNode = app.nodeQuery(StructrTraits.SCHEMA_NODE).andName("Project").getFirst();

			// create grant
			app.create(StructrTraits.SCHEMA_GRANT,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key(SchemaGrantTraitDefinition.SCHEMA_NODE_PROPERTY),          projectNode),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("principal"),           testGroup1),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("allowRead"),           true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("allowWrite"),          true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("allowDelete"),         true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("allowAccessControl"),  true)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test - delete groups => should work and remove schema grant as well
		try (final Tx tx = app.tx()) {

			app.deleteAllNodesOfType(StructrTraits.GROUP);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// verify that no groups and no schema grants exist
		try (final Tx tx = app.tx()) {

			assertEquals("Schema permissions should be removed automatically when principal or schema node are removed", 0, app.nodeQuery(StructrTraits.SCHEMA_GRANT).getAsList().size());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// setup schema grant again
		try (final Tx tx = app.tx()) {

			final NodeInterface testGroup1  = app.create(StructrTraits.GROUP, "Group1");
			final NodeInterface projectNode = app.nodeQuery(StructrTraits.SCHEMA_NODE).andName("Project").getFirst();

			app.create(StructrTraits.SCHEMA_GRANT,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key(SchemaGrantTraitDefinition.SCHEMA_NODE_PROPERTY),          projectNode),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("principal"),           testGroup1),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("allowRead"),           true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("allowWrite"),          true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("allowDelete"),         true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("allowAccessControl"),  true)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test - delete schema node
		try (final Tx tx = app.tx()) {

			app.delete(app.nodeQuery(StructrTraits.SCHEMA_NODE).andName("Project").getFirst());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// verify that no groups and no schema grants exist
		try (final Tx tx = app.tx()) {

			assertEquals("Schema permissions should be removed automatically when principal or schema node are removed", 0, app.nodeQuery(StructrTraits.SCHEMA_GRANT).getAsList().size());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testNumberOfRelationshipsEqualToFetchSize() {

		final int num = 100;

		// setup: create groups
		try (final Tx tx = app.tx()) {

			final NodeInterface root = app.create(StructrTraits.GROUP, "root");
			final PropertyKey key    = Traits.of(StructrTraits.GROUP).key(PrincipalTraitDefinition.GROUPS_PROPERTY);

			for (int i=0; i<num; i++) {

				app.create(StructrTraits.GROUP,
					new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "child" + i),
					new NodeAttribute<>(key, Arrays.asList(root))
				);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		Settings.FetchSize.setValue(num);

		// test: load all groups
		try (final Tx tx = app.tx()) {

			final Group root = app.nodeQuery(StructrTraits.GROUP).andName("root").getFirst().as(Group.class);
			int count        = 0;

			for (final Principal p : root.getMembers()) {

				assertTrue("RelationshipQuery returns too many results", count++ < num);
			}

			System.out.println("count: " + count);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		Settings.FetchSize.setValue(Settings.FetchSize.getDefaultValue());
	}

	//@Test
	public void testConcurrentDeleteAndFetch() {

		final AtomicBoolean error = new AtomicBoolean(false);
		final List<String> msgs   = new LinkedList<>();
		int num                   = 0;

		for (int i=0; i<10; i++) {

			// setup: create groups
			try (final Tx tx = app.tx()) {

				for (int j=0; j<10; j++) {

					app.create(StructrTraits.GROUP, StructrTraits.GROUP + StringUtils.leftPad(String.valueOf(num++), 5));
				}

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception.");
			}
		}

		// start a worker thread that deletes groups in batches of 500
		final Thread deleter = new Thread(() -> {

			try {
				boolean run = true;

				while (run) {

					int count = 0;
					run = false;

					synchronized (System.out) {
						System.out.println("Deleter: fetching objects...");
					}

					try (final Tx tx = app.tx()) {

						for (final NodeInterface group : app.nodeQuery(StructrTraits.GROUP).pageSize(5).getAsList()) {

							app.delete(group);
							run = true;
							count++;
						}

						tx.success();

					} catch (FrameworkException fex) {
						fex.printStackTrace();
						fail("Unexpected exception.");
					}

					synchronized (System.out) {
						System.out.println("Deleter: deleted " + count + " objects");
					}
				}

			} catch (Throwable t) {

				t.printStackTrace();

				msgs.add(t.getMessage());
				error.set(true);
			}

		}, "Deleter");

		// start a worker thread that counts all groups until there are no more groups (or timeout occurs)
		final Thread reader = new Thread(() -> {

			try {

				final long start = System.currentTimeMillis();
				boolean run      = true;

				// run for 10 seconds max
				while (run && System.currentTimeMillis() < start + 100000) {

					int count = 0;
					run = false;

					synchronized (System.out) {
						System.out.println("Reader:  fetching objects...");
					}

					try (final Tx tx = app.tx()) {

						for (final NodeInterface group : app.nodeQuery(StructrTraits.GROUP).getResultStream()) {

							run = true;
							count++;
						}

						tx.success();

					} catch (FrameworkException fex) {
						fex.printStackTrace();
					}

					synchronized (System.out) {
						System.out.println("Reader:  found " + count + " objects");
					}
				}

			} catch (Throwable t) {

				t.printStackTrace();

				msgs.add(t.getMessage());
				error.set(true);
			}

		}, "Reader");

		reader.start();
		deleter.start();

		try {

			reader.join();
			deleter.join();

		} catch (InterruptedException t) {
			t.printStackTrace();
		}

		if (error.get()) {

			System.out.println(msgs);
		}

		assertFalse("Reading and deleting nodes simultaneously causes an error", error.get());
	}

	@Test
	public void testFetchWaitDelete() {

		// setup: create groups
		try (final Tx tx = app.tx()) {

			for (int i=0; i<100; i++) {

				app.create(StructrTraits.GROUP, StructrTraits.GROUP + StringUtils.leftPad(String.valueOf(i), 5));
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final Thread reader = new Thread(() -> {

			try (final Tx tx = app.tx()) {

				System.out.println(Thread.currentThread().getName() + ": fetching groups");

				final List<NodeInterface> groups = app.nodeQuery(StructrTraits.GROUP).getAsList();

				for (int i=0; i<10; i++) {

					System.out.println(Thread.currentThread().getName() + ": waiting: " + i);
					Thread.sleep(1000);
				}

				for (final NodeInterface g : groups) {
					System.out.println(Thread.currentThread().getName() + ": " + g.getName());
				}

				tx.success();

			} catch (Throwable t) {
				t.printStackTrace();
			}

			System.out.println(Thread.currentThread().getName() + ": transaction closed");

		}, "Reader");

		reader.start();

		try { Thread.sleep(1000); } catch (Throwable t) {}

		for (int i=0; i<10; i++) {

			try (final Tx tx = app.tx()) {

				System.out.println(Thread.currentThread().getName() + ": fetching single group");

				final NodeInterface group = app.nodeQuery(StructrTraits.GROUP).getFirst();

				System.out.println(Thread.currentThread().getName() + ": deleting group");

				app.delete(group);

				System.out.println(Thread.currentThread().getName() + ": group deleted");

				tx.success();

			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		System.out.println(Thread.currentThread().getName() + ": transaction closed");

		try {

			System.out.println(Thread.currentThread().getName() + ": waiting for reader");

			reader.join();

		} catch (InterruptedException t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testIdenticalRelationshipTypeFilterPerformance() {

		logger.info("Schema setup..");

		// setup 1: create schema
		try (final Tx tx = app.tx()) {

			final JsonSchema schema     = StructrSchema.createFromDatabase(app);
			final JsonObjectType left   = schema.addType("Left");
			final JsonObjectType middle = schema.addType("Middle");
			final JsonObjectType right  = schema.addType("Right");

			left.relate(middle, "TEST", Cardinality.ManyToOne, "lefts", "middle");
			right.relate(middle, "TEST", Cardinality.ManyToOne, "rights", "middle");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		final String left   = "Left";
		final String middle = "Middle";
		final String right  = "Right";

		final PropertyKey leftToMiddle  = Traits.of(left).key(  "middle");
		final PropertyKey rightToMiddle = Traits.of(right).key( "middle");
		final PropertyKey middleToLeft  = Traits.of(middle).key("lefts");

		logger.info("Creating data..");

		// setup 2: create data
		try (final Tx tx = app.tx()) {

			final NodeInterface middle1 = app.create(middle, "Middle");

			for (int i=0; i<2000; i++) {

				app.create(right, new NodeAttribute<>(rightToMiddle, middle1));
			}

			app.create(left, new NodeAttribute<>(leftToMiddle, middle1));
			app.create(left, new NodeAttribute<>(leftToMiddle, middle1));

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		logger.info("Testing..");

		FlushCachesCommand.flushAll();

		// test: check performance
		try (final Tx tx = app.tx()) {

			final GraphObject m = app.nodeQuery(middle).getFirst();
			final long t0       = System.currentTimeMillis();
			final List list     = Iterables.toList((Iterable)m.getProperty(middleToLeft));
			final long t1       = System.currentTimeMillis();
			final long dt       = t1 - t0;

			assertEquals("Related nodes are not filtered correctly", 2, list.size());
			assertTrue("Related node filtering by target label: performance is too low", dt < 200);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testRollbackFunction() {

		// test setup, create some nodes and expect rollback even with tx.success()!
		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.GROUP, "group1");
			app.create(StructrTraits.GROUP, "group2");
			app.create(StructrTraits.GROUP, "group3");

			Actions.execute(securityContext, null, "${rollback_transaction()}", "testRollbackFunction");

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// assert that no groups exist!
		try (final Tx tx = app.tx()) {

			final List<NodeInterface> groups = app.nodeQuery(StructrTraits.GROUP).getAsList();

			assertTrue(groups.isEmpty());

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

	}

	// ----- nested classes -----
	private static class TestRunner implements Runnable {

		private boolean success    = true;
		private NodeInterface test = null;
		private App app            = null;

		public TestRunner(final App app, final NodeInterface test) {
			this.app  = app;
			this.test = test;
		}

		public boolean success() {
			return success;
		}

		@Override
		public void run() {

			final String name = Thread.currentThread().getName();

			try (final Tx tx = app.tx()) {

				// set property on node
				test.setProperty(Traits.of("TestOne").key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

				for (int i=0; i<100; i++) {

					// wait some time
					try { Thread.sleep(1); } catch (Throwable t) {}

					// check if the given name is still there
					final String testName = test.getProperty(Traits.of("TestOne").key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
					if (!name.equals(testName)) {

						success = false;
					}
				}

				tx.success();

			} catch (Throwable t) {
				success = false;
			}
		}

	}

	private static class FailingTestRunner implements Runnable {

		private boolean success    = true;
		private NodeInterface test = null;
		private App app            = null;

		public FailingTestRunner(final App app, final NodeInterface test) {
			this.app  = app;
			this.test = test;
		}

		public boolean success() {
			return success;
		}

		@Override
		public void run() {

			final String name = Thread.currentThread().getName();

			try (final Tx tx = app.tx()) {

				// set property on node
				test.setProperty(Traits.of("TestOne").key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

				for (int i=0; i<100; i++) {

					// wait some time
					try { Thread.sleep(1); } catch (Throwable t) {}

					// check if the given name is still there
					final String testName = test.getProperty(Traits.of("TestOne").key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
					if (!name.equals(testName)) {

						success = false;
					}
				}

				// make Transactions fail randomly
				if (Math.random() <= 0.5) {
					tx.success();
				}

			} catch (Throwable t) {
				success = false;
			}
		}

	}

	private static class RelationshipCreator implements Runnable {

		private NodeInterface source = null;
		private NodeInterface target = null;

		public RelationshipCreator(final NodeInterface source, final NodeInterface  target) {
			this.source = source;
			this.target = target;
		}

		@Override
		public void run() {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				final List<NodeInterface> list = new LinkedList<>();
				list.add(target);

				source.setProperty(Traits.of("TestSix").key("oneToManyTestOnes"), list);

				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("", fex);
			}
		}
	}
}
