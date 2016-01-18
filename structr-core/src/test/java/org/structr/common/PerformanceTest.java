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

import org.structr.common.error.FrameworkException;

//~--- JDK imports ------------------------------------------------------------

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.relationship.NodeHasLocation;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;

//~--- classes ----------------------------------------------------------------

/**
 * Test performance of node and relationship creation
 *
 * All tests are executed in superuser context
 *
 *
 */
public class PerformanceTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(PerformanceTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {
		super.test00DbAvailable();
	}
	
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
	public void test01PerformanceOfNodeCreation() {

		try {

			int number                = 1000;
			long t0                   = System.nanoTime();
			List<GenericNode> nodes   = createTestNodes(GenericNode.class, number);
			long t1                   = System.nanoTime();

			assertTrue(nodes.size() == number);

			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			Double time                 = (t1 - t0) / 1000000000.0;
			Double rate                 = number / ((t1 - t0) / 1000000000.0);

			logger.log(Level.INFO, "Created {0} nodes in {1} seconds ({2} per s)", new Object[] { number, decimalFormat.format(time), decimalFormat.format(rate) });
			assertTrue(rate > 10);

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

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
	public void test02PerformanceOfRelationshipCreation() {

		try {

			int number                      = 1000;
			long t0                         = System.nanoTime();
			List<NodeHasLocation> rels = createTestRelationships(NodeHasLocation.class, number);
			long t1                         = System.nanoTime();

			assertTrue(rels.size() == number);

			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			Double time                 = (t1 - t0) / 1000000000.0;
			Double rate                 = number / ((t1 - t0) / 1000000000.0);

			logger.log(Level.INFO, "Created {0} relationships in {1} seconds ({2} per s)", new Object[] { number, decimalFormat.format(time), decimalFormat.format(rate) });
			assertTrue(rate > 50);

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
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
	public void test03ReadPerformance() {

		try {

			int number = 1000;
			int loop   = 1000;
			createTestNodes(TestOne.class, number);

			long t0                   = System.nanoTime();
			
			for (int i=0; i<loop; i++) {
			
				try (final Tx tx = app.tx()) {

					final List<TestOne> res = app.nodeQuery(TestOne.class).getAsList();

					for (final TestOne t : res) {

						for (final PropertyKey key : t.getPropertyKeys(PropertyView.Ui)) {
							t.getProperty(key);
						}
					}

					tx.success();
				}
			}
			
			long t1                   = System.nanoTime();


			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			Double time                 = (t1 - t0) / 1000000000.0;
			Double rate                 = number * loop / ((t1 - t0) / 1000000000.0);

			logger.log(Level.INFO, "Read {0} nodes in {1} seconds ({2} per s)", new Object[] { number, decimalFormat.format(time), decimalFormat.format(rate) });
			assertTrue(rate > 10000);

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

}