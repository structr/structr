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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.relationship.NodeHasLocation;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;

/**
 *
 */
public class PerformanceTest extends StructrTest {

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

		try {

			try (final Tx tx = app.tx()) {

				final long t1 = System.currentTimeMillis();

				for (int i=0; i<number; i++) {

					nodes.add(app.create(TestOne.class,
						new NodeAttribute(TestOne.name, "TestOne" + i),
						new NodeAttribute(TestOne.aBoolean, true),
						new NodeAttribute(TestOne.aDate, new Date()),
						new NodeAttribute(TestOne.aDouble, 1.234),
						new NodeAttribute(TestOne.aLong, 12345L),
						new NodeAttribute(TestOne.anEnum, TestOne.Status.One),
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
		assertTrue("Creation rate of nodes too low, expected > 100, was " + rate, rate > 50);
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

			int number                 = 1000;
			long t0                    = System.nanoTime();
			List<NodeHasLocation> rels = createTestRelationships(NodeHasLocation.class, number);
			long t1                    = System.nanoTime();

			assertTrue(rels.size() == number);

			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			Double time                 = (t1 - t0) / 1000000000.0;
			Double rate                 = number / ((t1 - t0) / 1000000000.0);

			logger.info("Created {} relationships in {} seconds ({} per s)", new Object[] { number, decimalFormat.format(time), decimalFormat.format(rate) });
			assertTrue(rate > 50);

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
	public void testReadPerformance() {

		try {

			int number = 1000;
			int loop   = 1000;
			createTestNodes(TestOne.class, number);

			long t0                   = System.nanoTime();

			for (int i=0; i<loop; i++) {

				try (final Tx tx = app.tx()) {

					final List<TestOne> res = app.nodeQuery(TestOne.class).getAsList();

					for (final TestOne t : res) {

						final String name = t.getProperty(AbstractNode.name);
					}

					tx.success();
				}
			}

			long t1                   = System.nanoTime();


			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			double time                 = (t1 - t0) / 1000000000.0;
			double rate                 = number * loop / ((t1 - t0) / 1000000000.0);

			logger.info("Read {}x {} nodes in {} seconds ({} per s)", new Object[] { loop, number, decimalFormat.format(time), decimalFormat.format(rate) });
			assertTrue("Invalid read performance result", rate > 50000);

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}
}
