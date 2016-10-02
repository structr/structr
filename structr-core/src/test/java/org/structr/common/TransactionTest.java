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
package org.structr.common;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSix;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class TransactionTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(TransactionTest.class.getName());

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
	}

	public void testTransactionIsolation() {

		// Tests the transaction isolation of the underlying database layer.

		// Create a node and use ten different threads to set a property on
		// it in a transaction. Observe the property value to check that the
		// threads do not interfere with each other.

		try {

			final TestOne test             = createTestNode(TestOne.class);
			final ExecutorService executor = Executors.newCachedThreadPool();
			final List<TestRunner> tests   = new LinkedList<>();
			final List<Future> futures     = new LinkedList<>();

			// create and run test runners
			for (int i=0; i<10; i++) {

				final TestRunner runner = new TestRunner(app, test);

				futures.add(executor.submit(runner));
				tests.add(runner);
			}

			// wait for termination
			for (final Future future : futures) {
				future.get();
			}

			// check for success
			for (final TestRunner runner : tests) {
				assertTrue("Could not validate transaction isolation", runner.success());
			}

		} catch (Throwable fex) {
			fail("Unexpected exception");
		}
	}

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

		} catch (ExecutionException | InterruptedException | FrameworkException fex) {
			fex.printStackTrace();
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

				for (int i=0; i<10; i++) {

					// wait some time
					try { Thread.sleep((long)(Math.random() * 10L) + 10); } catch (Throwable t) {}

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
				fex.printStackTrace();
			}
		}
	}
}
