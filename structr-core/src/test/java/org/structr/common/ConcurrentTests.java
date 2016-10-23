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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.TestOne;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.IntProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;

/**
 *
 *
 */
public class ConcurrentTests extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(ConcurrentTests.class.getName());

	@Test
	public void testConcurrentValidation() {

		final int count = 100;

		try (final Tx tx = app.tx()) {

			app.create(SchemaNode.class,
				new NodeAttribute(SchemaNode.name, "Item"),
				new NodeAttribute(new StringProperty("_name"), "+String!")
			);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}


		final Class type = StructrApp.getConfiguration().getNodeEntityClass("Item");
		assertNotNull(type);


		final PropertyKey name = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, "name", false);
		assertNotNull(name);

		final Runnable tester = new Runnable() {

			@Override
			public void run() {

				for (int i=0; i<count; i++) {

					// testing must be done in an isolated transaction
					try (final Tx tx = app.tx()) {

						app.create(type, "Item" + i);

						tx.success();

					} catch (FrameworkException fex) {}

				}
			}
		};

		// submit three test instances
		final ExecutorService executor = Executors.newCachedThreadPool();
		final Future f1                = executor.submit(tester);
		final Future f2                = executor.submit(tester);
		final Future f3                = executor.submit(tester);

		try {
			f1.get();
			f2.get();
			f3.get();

		} catch (Throwable ex) {}


		List<GraphObject> result = null;

		try (final Tx tx = app.tx()) {

			result = app.nodeQuery(type).getAsList();


			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}


		// verify that only count entities have been created.
		assertEquals("Invalid concurrent validation result", count, result.size());
	}

	@Test
	public void testConcurrentValidationOnDynamicProperty() {

		final int count = 100;

		try (final Tx tx = app.tx()) {

			app.create(SchemaNode.class,
				new NodeAttribute(SchemaNode.name, "Item"),
				new NodeAttribute(new StringProperty("_testXYZ"), "+String!")
			);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}


		final Class type = StructrApp.getConfiguration().getNodeEntityClass("Item");
		assertNotNull(type);


		final PropertyKey testXYZ = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, "testXYZ", false);
		assertNotNull(testXYZ);

		final Runnable tester = new Runnable() {

			@Override
			public void run() {

				for (int i=0; i<count; i++) {

					// testing must be done in an isolated transaction
					try (final Tx tx = app.tx()) {

						app.create(type, new NodeAttribute(testXYZ, "Item" + i));

						tx.success();

					} catch (FrameworkException fex) {}

				}
			}
		};

		// submit three test instances
		final ExecutorService executor = Executors.newCachedThreadPool();
		final Future f1                = executor.submit(tester);
		final Future f2                = executor.submit(tester);
		final Future f3                = executor.submit(tester);

		try {
			f1.get();
			f2.get();
			f3.get();

		} catch (Throwable ex) {}


		List<GraphObject> result = null;

		try (final Tx tx = app.tx()) {

			result = app.nodeQuery(type).getAsList();

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}


		// verify that only count entities have been created.
		assertEquals("Invalid concurrent validation result", count, result.size());
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

			createTestType.setProperty(new StringProperty("testCount"), "Integer");
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
	public void testConcurrentModificationIsolation() {

		final ExecutorService service = Executors.newCachedThreadPool();
		final List<Future> futures    = new LinkedList<>();
		final int count               = 20;
		final int num                 = 20;


		// setup: create dynamic type with onCreate() method
		try (final Tx tx = app.tx()) {

			final SchemaNode createTestType = createTestNode(SchemaNode.class, "CreateTest");
			final SchemaProperty prop       = createTestNode(SchemaProperty.class,
				new NodeAttribute(SchemaProperty.schemaNode, createTestType),
				new NodeAttribute(SchemaProperty.name, "name"),
				new NodeAttribute(SchemaProperty.propertyType, "String"),
				new NodeAttribute(SchemaProperty.unique, true),
				new NodeAttribute(SchemaProperty.notNull, true)
			);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}


		final Class type = StructrApp.getConfiguration().getNodeEntityClass("CreateTest");

		for (int i=0; i<count; i++) {

			futures.add(service.submit(new Worker<>(app, type, num)));
		}

		for (final Future future : futures) {
			try { future.get(); } catch (Throwable t) {}
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> tests = app.nodeQuery(type).getAsList();
			int i                           = 0;

			assertEquals("Invalid result size for concurrent modification test", count*num, tests.size());

			for (final NodeInterface one : tests) {

				final String name = "Test" + StringUtils.leftPad(Integer.toString(i++), 3, "0");
				assertEquals("Invalid detail result for concurrent modification test", name, one.getName());
				assertEquals("Invalid detail result for concurrent modification test", one.getName(), one.getProperty(TestOne.aString));
			}

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	private static class Worker<T> implements Runnable {

		private static final AtomicInteger counter = new AtomicInteger();
		private Class<NodeInterface> type          = null;
		private App app                            = null;
		private int num                            = 0;

		public Worker(final App app, final Class<NodeInterface> type, final int num) {
			this.type = type;
			this.num  = num;
			this.app  = app;
		}

		@Override
		public void run() {

			for (int i=0; i<num; i++) {

				final String name = "Test" + StringUtils.leftPad(Integer.toString(counter.getAndIncrement()), 3, "0");

				try (final Tx tx = app.tx()) {

					app.create(type, name).setProperty(TestOne.aString, name);
					tx.success();

				} catch (FrameworkException fex) {}

				try (final Tx tx = app.tx()) {

					final NodeInterface object = app.nodeQuery(type).sort(AbstractNode.name).getFirst();
					object.setProperty(AbstractNode.name, name);

					tx.success();

				} catch (FrameworkException fex) {}
			}
		}
	}
}
