/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Group;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.TestEight;
import org.structr.core.entity.TestFive;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSix;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.IntProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.json.JsonSchema;

/**
 *
 */
public class SystemTest extends StructrTest {

	private static final Logger logger         = LoggerFactory.getLogger(SystemTest.class);

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

			Principal person = this.createTestNode(Principal.class);

			final SecurityContext securityContext = SecurityContext.getInstance(person, null, AccessMode.Backend);
			testCallbacks(securityContext);

		} catch (FrameworkException fex) {

			logger.warn("", fex);
		}
	}

	@Test
	public void testCallbackOrder() {

		try {

// ##################################### test creation callbacks

			TestEight test = null;

			try (final Tx tx = app.tx()) {

				test = app.create(TestEight.class, new NodeAttribute(TestEight.testProperty, 123));
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
				test.setProperty(TestEight.testProperty, 234);
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
				test.setProperty(TestEight.testProperty, 234);
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

		TestFive entity = null;
		Integer zero = 0;
		Integer one  = 1;

		try (final Tx tx = app.tx()) {

			entity = app.create(TestFive.class);
			tx.success();

		} catch (Throwable t) {

			logger.warn("", t);
		}

		assertNotNull("Entity should have been created", entity);

		// creation assertions
		try (final Tx tx = app.tx()) {

			assertEquals("modifiedInBeforeCreation should have a value of 1: ", one, entity.getProperty(TestFive.modifiedInBeforeCreation));
			assertEquals("modifiedInAfterCreation should have a value of 1:  ", one, entity.getProperty(TestFive.modifiedInAfterCreation));

			// modification assertions
			assertEquals("modifiedInBeforeModification should have a value of 0: ", zero, entity.getProperty(TestFive.modifiedInBeforeModification));
			assertEquals("modifiedInAfterModification should have a value of 0:  ", zero, entity.getProperty(TestFive.modifiedInAfterModification));
		}


		// 2nd part of the test: modify node
		try (final Tx tx = app.tx()) {

			final TestFive finalEntity = entity;

			finalEntity.setProperty(TestFive.intProperty, 123);
			tx.success();

		} catch (Throwable t) {
			logger.warn("", t);
		}

		try (final Tx tx = app.tx()) {

			// creation assertions
			assertEquals("modifiedInBeforeCreation should have a value of 1: ", one, entity.getProperty(TestFive.modifiedInBeforeCreation));
			assertEquals("modifiedInAfterCreation should have a value of 1:  ", one, entity.getProperty(TestFive.modifiedInAfterCreation));

			// modification assertions
			assertEquals("modifiedInBeforeModification should have a value of 1: ", one, entity.getProperty(TestFive.modifiedInBeforeModification));
			assertEquals("modifiedInAfterModification should have a value of 1:  ", one, entity.getProperty(TestFive.modifiedInAfterModification));
		}
	}

	/**
	 * disabled, failing test to check for (existing, confirmed) flaw in parallel node instantiation)
	 */
	@Test
	public void testFlawedParallelInstantiation() {

		final int nodeCount       = 1000;
		SchemaNode createTestType = null;

		// setup: create dynamic type with onCreate() method
		try (final Tx tx = app.tx()) {

			createTestType = createTestNode(SchemaNode.class, "CreateTest");

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		Class testType = StructrApp.getConfiguration().getNodeEntityClass("CreateTest");

		assertNotNull("Type CreateTest should have been created", testType);

		// second step: create 1000 test nodes
		try (final Tx tx = app.tx()) {

			createTestNodes(testType, nodeCount);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			createTestType.setProperty(new StringProperty("_testCount"), "Integer");
			createTestType.setProperty(new StringProperty("___onCreate"), "set(this, 'testCount', size(find('CreateTest')))");

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		testType = StructrApp.getConfiguration().getNodeEntityClass("CreateTest");
		NodeInterface node = null;

		// third step: create a single node in a separate transaction
		try (final Tx tx = app.tx()) {

			node = createTestNode(testType, "Tester");

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// fourth step: check property value
		try (final Tx tx = app.tx()) {

			final Integer testCount = node.getProperty(new IntProperty("testCount"));

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
		TestOne testNode = null;

		try (final Tx tx = app.tx()) {

			testNode = createTestNode(TestOne.class);
			testNode.setProperty(TestOne.aString, "InitialString");
			testNode.setProperty(TestOne.anInt, 42);

			tx.success();

		} catch (FrameworkException ex) {

			logger.warn("", ex);
			fail("Unexpected exception");

		}

		try (final Tx tx = app.tx()) {

			Scripting.replaceVariables(ctx, testNode, "${ ( set(this, 'aString', 'NewString'), set(this, 'anInt', 'NOT_AN_INTEGER') ) }");
			fail("StructrScript: setting anInt to 'NOT_AN_INTEGER' should cause an Exception");

			tx.success();

		} catch (FrameworkException expected) { }


		try {

			try (final Tx tx = app.tx()) {

				assertEquals("Property value should still have initial value!", "InitialString", testNode.getProperty(TestOne.aString));

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

			app.create(SchemaNode.class,
				new NodeAttribute(SchemaNode.name, "Item"),
				new NodeAttribute(SchemaNode.schemaProperties,
					Arrays.asList(app.create(
						SchemaProperty.class,
						new NodeAttribute(SchemaProperty.name, "name"),
						new NodeAttribute(SchemaProperty.propertyType, "String"),
						new NodeAttribute(SchemaProperty.unique, true),
						new NodeAttribute(SchemaProperty.indexed, true)
					)
				))
			);

			tx.success();

		} catch (FrameworkException ex) {
			fail("Error creating schema node");
		}

		final Class itemType = StructrApp.getConfiguration().getNodeEntityClass("Item");

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

			final List<NodeInterface> items = app.nodeQuery(itemType).sort(AbstractNode.name).getAsList();
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

			final TestOne test             = createTestNode(TestOne.class);
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

		Settings.CypherDebugLogging.setValue(true);

		// Tests the transaction isolation of the underlying database layer.

		// Create a node and use ten different threads to set a property on
		// it in a transaction. Observe the property value to check that the
		// threads do not interfere with each other.

		try {

			final TestOne test                  = createTestNode(TestOne.class);
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

		Settings.CypherDebugLogging.setValue(false);
	}

	@Test
	public void testEnsureCardinalityPerformance() {

		final List<TestOne> list = new LinkedList<>();
		final int num            = 1000;

		// test setup, create a supernode with 10000 relationships
		try (final Tx tx = app.tx()) {

			System.out.println("Creating supernode with " + num + " relationships.");

			list.add(createTestNode(TestOne.class,
				new NodeAttribute<>(TestOne.manyToManyTestSixs, createTestNodes(TestSix.class, num))
			));

			tx.success();

		} catch (Throwable fex) {
			fail("Unexpected exception");
		}

		// actual test: test performance of node association on supernode
		try (final Tx tx = app.tx()) {

			for (int i=0; i<10; i++) {

				final long t0 = System.currentTimeMillis();
				createTestNode(TestSix.class, new NodeAttribute<>(TestSix.manyToManyTestOnes, list));
				final long t1 = System.currentTimeMillis();

				System.out.println((t1 - t0) + "ms");

			}

			tx.success();

		} catch (Throwable fex) {
			fail("Unexpected exception");
		}
	}

	@Test
	public void testPasswordAndHashSecurity() {

		final Class userType                  = StructrApp.getConfiguration().getNodeEntityClass("Principal");
		final PropertyKey<String> passwordKey = StructrApp.key(userType, "password");
		final PropertyKey<String> saltKey     = StructrApp.key(userType, "salt");

		// actual test: test performance of node association on supernode
		try (final Tx tx = app.tx()) {

			app.create(Principal.class,
				new NodeAttribute<>(Principal.name, "tester"),
				new NodeAttribute<>(passwordKey, "password")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception");
		}

		// actual test: test performance of node association on supernode
		try (final Tx tx = app.tx()) {

			final Principal user = app.nodeQuery(Principal.class).getFirst();

			assertEquals("Password hash IS NOT SECURE!", Principal.HIDDEN, user.getProperty(passwordKey));
			assertEquals("Password salt IS NOT SECURE!", Principal.HIDDEN, user.getProperty(saltKey));

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
			app.create(Group.class, "group");

			JsonSchema schema = StructrSchema.createFromDatabase(app);
			schema.addType("GrantTest").addMethod("onCreation", "grant(first(find('Group')), this, 'read')", "");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fail("Unexpected exception");
		}

		final Class type = StructrApp.getConfiguration().getNodeEntityClass("GrantTest");

		try (final Tx tx = app.tx()) {

			final long t0 = System.currentTimeMillis();
			for (int i=0; i<10000; i++) {
				app.create(type, "test" + i);
			}
			final long t1 = System.currentTimeMillis();

			System.out.println((t1-t0) + " ms");

			tx.success();

		} catch (Throwable fex) {
			fail("Unexpected exception");
		}
	}

	@Test
	public void testConcurrentIdenticalRelationshipCreation() {

		try {
			final ExecutorService service = Executors.newCachedThreadPool();
			final TestSix source          = createTestNode(TestSix.class);
			final TestOne target          = createTestNode(TestOne.class);

			final Future one = service.submit(new RelationshipCreator(source, target));
			final Future two = service.submit(new RelationshipCreator(source, target));

			// wait for completion
			one.get();
			two.get();

			try (final Tx tx = app.tx()) {

				// check for a single relationship since all three parts of
				// both relationships are equal => only one should be created
				final List<TestOne> list = source.getProperty(TestSix.oneToManyTestOnes);

				assertEquals("Invalid concurrent identical relationship creation result", 1, list.size());

				tx.success();
			}

			service.shutdownNow();

		} catch (ExecutionException | InterruptedException | FrameworkException fex) {
			logger.warn("", fex);
		}
	}

	@Test
	public void testDeletionCallback() {

		/**
		 * This test concurrently creates 1000 nodes in
		 * batches of 10, with 10 threads simultaneously.
		 */

		try (final Tx tx = app.tx()) {

			final SchemaNode deleteTestNode = app.create(SchemaNode.class, "DeleteTest");
			final SchemaMethod onDelete     = app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.name, "onDelete"),
				new NodeAttribute<>(SchemaMethod.schemaNode, deleteTestNode),
				new NodeAttribute<>(SchemaMethod.source, "log('deleted')")
			);

			tx.success();

		} catch (FrameworkException ex) {
			fail("Error creating schema node");
		}
	}


	private static class TestRunner implements Runnable {

		private boolean success = true;
		private TestOne test    = null;
		private App app         = null;

		public TestRunner(final App app, final TestOne test) {
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
				test.setProperty(TestOne.name, name);

				for (int i=0; i<100; i++) {

					// wait some time
					try { Thread.sleep(1); } catch (Throwable t) {}

					// check if the given name is still there
					final String testName = test.getProperty(TestOne.name);
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

		private boolean success = true;
		private TestOne test    = null;
		private App app         = null;

		public FailingTestRunner(final App app, final TestOne test) {
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
				test.setProperty(TestOne.name, name);

				for (int i=0; i<100; i++) {

					// wait some time
					try { Thread.sleep(1); } catch (Throwable t) {}

					// check if the given name is still there
					final String testName = test.getProperty(TestOne.name);
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

		private TestSix source = null;
		private TestOne target = null;

		public RelationshipCreator(final TestSix source, final TestOne  target) {
			this.source = source;
			this.target = target;
		}

		@Override
		public void run() {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				final List<TestOne> list = new LinkedList<>();
				list.add(target);

				source.setProperty(TestSix.oneToManyTestOnes, list);

				tx.success();

			} catch (FrameworkException fex) {
				logger.warn("", fex);
			}
		}
	}
}
