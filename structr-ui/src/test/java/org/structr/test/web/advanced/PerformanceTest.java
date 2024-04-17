/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.test.web.advanced;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.GenericRelationship;
import org.structr.core.graph.*;
import org.structr.test.web.IndexingTest;
import org.structr.test.web.entity.TestFive;
import org.structr.test.web.entity.TestOne;
import org.structr.test.web.entity.TestTwo;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import org.structr.api.config.Settings;

import static org.testng.AssertJUnit.*;

/**
 *
 */
public class PerformanceTest extends IndexingTest {

	private static final Logger logger = LoggerFactory.getLogger(PerformanceTest.class);

	/**
	 * Tests basic throughput of node creation operations
	 *
	 * Note that this is just a very rought test as performance is heavily
	 * depending on hardware and setup (cache parameters etc.)
	 *
	 * The assumed rate is low so if this test fails, there may be issues
	 * with the test setup.
	 *
	 * If the test passes, one can expect structr to create nodes with typical performance.
	 */
	@Test
	public void testPerformanceOfNodeCreation() {

		final List<TestOne> nodes = new LinkedList<>();
		final long number         = 1000;

		// start measuring
		final long t0 = System.currentTimeMillis();
		final App app = StructrApp.getInstance(setupSecurityContext());

		try {

			try (final Tx tx = app.tx()) {

				final long t1 = System.currentTimeMillis();

				for (int i=0; i<number; i++) {

					nodes.add(app.create(TestOne.class,
						new NodeAttribute(TestOne.name, "TestOne" + i),
						new NodeAttribute(TestOne.aDate, new Date()),
						new NodeAttribute(TestOne.aDouble, 1.234),
						new NodeAttribute(TestOne.aLong, 12345L),
						new NodeAttribute(TestOne.anInt, 123)
					));
				}

				final long t2 = System.currentTimeMillis();
				System.out.println((t2 - t1) + " ms");

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

		final long t1 = System.currentTimeMillis();

		assertTrue(nodes.size() == number);

		DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		Double time                 = (t1 - t0) / 1000.0;
		Double rate                 = number / ((t1 - t0) / 1000.0);

		logger.info("Created {} nodes in {} seconds ({} per s)", number, decimalFormat.format(time), decimalFormat.format(rate) );
		assertTrue("Creation rate of nodes too low, expected > 30, was " + rate, rate > 30);
	}

	/**
	 * Tests basic throughput of relationship creation operations
	 *
	 * Note that this is just a very rought test as performance is heavily
	 * depending on hardware and setup (cache parameters etc.)
	 *
	 * The assumed rate is low so if this test fails, there may be issues
	 * with the test setup.
	 *
	 * If the test passes, one can expect structr to create relationship with typical performance.
	 */
	@Test
	public void testPerformanceOfRelationshipCreation() {

		try {

			int expected                   = 1000;
			final App app                  = StructrApp.getInstance(setupSecurityContext());
			final List<GenericNode> nodes  = new ArrayList<>(createNodes(app, GenericNode.class, expected + 1));
			List<GenericRelationship> rels = new LinkedList<>();
			long t0                        = System.nanoTime();

 			try (final Tx tx = app.tx()) {

				for (int i=0 ;i<expected; i++) {

					final GenericNode n1 = nodes.get(i);
					final GenericNode n2 = nodes.get(i+1);

					rels.add(app.create(n1, n2, GenericRelationship.class));
				}

				tx.success();
			}

			long t1                       = System.nanoTime();

			assertEquals("Invalid relationship creation result", expected, rels.size());

			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			Double time                 = (t1 - t0) / 1000000000.0;
			Double rate                 = expected / ((t1 - t0) / 1000000000.0);

			logger.info("Created {} relationships in {} seconds ({} per s)", new Object[] { expected, decimalFormat.format(time), decimalFormat.format(rate) });
			assertTrue(rate > 50);

		} catch (FrameworkException ex) {

			ex.printStackTrace();

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
	}

	/**
	 * Tests basic throughput of reading node properties.
	 *
	 * Note that this is just a very rought test as performance is heavily
	 * depending on hardware and setup (cache parameters etc.)
	 *
	 * The assumed rate is low so if this test fails, there may be issues
	 * with the test setup.
	 *
	 * If the test passes, one can expect structr to read nodes with typical performance.
	 */
	@Test
	public void testReadPerformance() {

		try {

			final App app = StructrApp.getInstance(setupSecurityContext());
			int number    = 1000;
			int loop      = 300;

			logger.info("Creating {} nodes..", number);

			createNodes(app, TestOne.class, number);

			long t0 = System.nanoTime();

			logger.info("Flushing caches..");

			FlushCachesCommand.flushAll();

			logger.info("Reading {} x {} nodes..", loop, number);

			for (int i=0; i<loop; i++) {

				try (final Tx tx = app.tx()) {

					for (final TestOne t : app.nodeQuery(TestOne.class).getResultStream()) {

						final String name = t.getProperty(AbstractNode.name);
					}

					tx.success();
				}
			}

			long t1 = System.nanoTime();


			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			double time                 = (t1 - t0) / 1000000000.0;
			double rate                 = number * loop / ((t1 - t0) / 1000000000.0);

			logger.info("Read {}x {} nodes in {} seconds ({} per s)", loop, number, decimalFormat.format(time), decimalFormat.format(rate));
			assertTrue("Invalid read performance result", rate > 10000);

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
	}

	/**
	 * Tests basic throughput of reading node properties.
	 *
	 * Note that this is just a very rought test as performance is heavily
	 * depending on hardware and setup (cache parameters etc.)
	 *
	 * The assumed rate is low so if this test fails, there may be issues
	 * with the test setup.
	 *
	 * If the test passes, one can expect structr to read nodes with typical performance.
	 */
	@Test
	public void testReadPerformanceWithPrefetching() {

		try {

			final App app = StructrApp.getInstance(setupSecurityContext());
			int number    = 1000;
			int loop      = 10;

			try (final Tx tx = app.tx()) {

				for (int i=0; i<number; i++) {

					app.create(TestFive.class,
						new NodeAttribute<>(TestFive.name, "TestFive" + i),
						new NodeAttribute<>(TestFive.testTwo, app.create(TestTwo.class, "TestTwo" + i))
					);
				}

				tx.success();

			} catch (Throwable t) {
				t.printStackTrace();
			}

			long t0 = System.nanoTime();

			// flush caches
			FlushCachesCommand.flushAll();

			for (int i=0; i<loop; i++) {

				try (final Tx tx = app.tx()) {

					for (final TestTwo t : app.nodeQuery(TestTwo.class).getAsList()) {

						t.getName();

						final List<TestFive> list = Iterables.toList(t.getProperty(TestTwo.testFives));
						for (final TestFive f : list) {

							f.getName();
						}

						assertEquals("Invalid collection size", 1, list.size());
					}

					tx.success();
				}
			}

			long t1                   = System.nanoTime();


			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			double time                 = (t1 - t0) / 1000000000.0;
			double rate                 = number * loop / ((t1 - t0) / 1000000000.0);

			logger.info("Read {}x {} nodes with relationship in {} seconds ({} per s)", new Object[] { loop, number, decimalFormat.format(time), decimalFormat.format(rate) });
			assertTrue("Invalid read performance result", rate > 2000);

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
	}

	@Test
	public void testPerformanceOfNodeDeletion() {

		final App app                   = StructrApp.getInstance(setupSecurityContext());
		final List<NodeInterface> nodes = new LinkedList<>();
		final int number                = 1000;

		try {

			try (final Tx tx = app.tx()) {

				nodes.addAll(createNodes(app, TestOne.class, number));
				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

		// start measuring
		final long t0 = System.currentTimeMillis();

		Settings.CypherDebugLogging.setValue(true);

		try {

			try (final Tx tx = app.tx()) {

				final DeleteNodeCommand command = app.command(DeleteNodeCommand.class);

				for (final NodeInterface node : nodes) {

					command.execute(node);
				}

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		final long t1 = System.currentTimeMillis();

		DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
		Double time                 = (t1 - t0) / 1000.0;
		Double rate                 = number / ((t1 - t0) / 1000.0);

		logger.info("Deleted {} nodes in {} seconds ({} per s)", number, decimalFormat.format(time), decimalFormat.format(rate) );
		assertTrue("Deletion rate of nodes too low, expected > 40, was " + rate, rate > 40);
	}

	// ----- private methods -----
	private SecurityContext setupSecurityContext() {

		final App app = StructrApp.getInstance();
		User user     = null;

		try (final Tx tx = app.tx()) {

			user = app.create(User.class,
				new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),      true)
			);

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		return SecurityContext.getInstance(user, AccessMode.Backend);
	}

	private <T extends NodeInterface> List<T> createNodes(final App app, final Class<T> type, final int number) throws FrameworkException {

		final List<T> nodes = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			for (int i = 0; i < number; i++) {

				nodes.add(app.create(type));
			}

			tx.success();
		}

		return nodes;

	}
}
