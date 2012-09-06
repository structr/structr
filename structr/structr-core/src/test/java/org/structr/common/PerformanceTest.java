/*
 *  Copyright (C) 2010-2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.common;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Test performance of node and relationship creation
 *
 * All tests are executed in superuser context
 *
 * @author Axel Morgner
 */
public class PerformanceTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(PerformanceTest.class.getName());

	//~--- methods --------------------------------------------------------

	public void testPerformanceOfNodeCreation() {

		try {

			int number               = 2000;
			long t0                  = System.nanoTime();
			List<AbstractNode> nodes = createTestNodes("UnknownTestType", number);
			long t1                  = System.nanoTime();

			assertTrue(nodes.size() == number);

			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			Double time                 = (t1 - t0) / 1000000000.0;
			Double rate                 = number / ((t1 - t0) / 1000000000.0);

			logger.log(Level.INFO, "Created {0} nodes in {1} seconds ({2} per s)", new Object[] { number, decimalFormat.format(time), decimalFormat.format(rate) });
			assertTrue(rate > 100);

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, null, ex);

		}

	}

	public void testPerformanceOfRelationshipCreation() {

		try {

			int number                      = 1000;
			long t0                         = System.nanoTime();
			List<AbstractRelationship> rels = createTestRelationships(RelType.UNDEFINED, number);
			long t1                         = System.nanoTime();

			assertTrue(rels.size() == number);

			DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
			Double time                 = (t1 - t0) / 1000000000.0;
			Double rate                 = number / ((t1 - t0) / 1000000000.0);

			logger.log(Level.INFO, "Created {0} relationships in {1} seconds ({2} per s)", new Object[] { number, decimalFormat.format(time), decimalFormat.format(rate) });
			assertTrue(rate > 100);

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, null, ex);

		}

	}

}
