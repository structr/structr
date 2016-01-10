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
import java.util.logging.Level;
import java.util.logging.Logger;


import org.structr.core.GraphObject;
import org.structr.core.entity.TestOne;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;

//~--- classes ----------------------------------------------------------------

/**
 *
 *
 */
public class GraphComparatorTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(GraphComparatorTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {
		super.test00DbAvailable();
	}
	
	public void test01CompareAscending() {

		try {

			TestOne a = createTestNode(TestOne.class);
			TestOne b = createTestNode(TestOne.class);
			
			try (final Tx tx = app.tx()) {

				GraphObjectComparator comp = new GraphObjectComparator(TestOne.anInt, GraphObjectComparator.ASCENDING);

				try {
					comp.compare(null, null);
					fail("Should have raised an NullPointerException");

				} catch (NullPointerException e) {}

				try {
					comp.compare(a, null);
					fail("Should have raised an NullPointerException");

				} catch (NullPointerException e) {}

				try {
					comp.compare(null, b);
					fail("Should have raised an NullPointerException");

				} catch (NullPointerException e) {}

				try {
					comp.compare(null, b);
					fail("Should have raised an NullPointerException");

				} catch (NullPointerException e) {}

				// a: null
				// b: null
				// a == b => 0
				assertEquals(0, comp.compare(a, b));

				// a: 0
				// b: null
				// a < b => -1
				setPropertyTx(a, TestOne.anInt, 0);
				assertEquals(-1, comp.compare(a, b));

				// a: null
				// b: 0
				// a > b => 1
				setPropertyTx(a, TestOne.anInt, null);
				setPropertyTx(b, TestOne.anInt, 0);
				assertEquals(1, comp.compare(a, b));

				// a: 1
				// b: 2
				// a < b => -1
				setPropertyTx(a, TestOne.anInt, 1);
				setPropertyTx(b, TestOne.anInt, 2);
				assertEquals(-1, comp.compare(a, b));

				// a: 2
				// b: 1
				// a > b => 1
				setPropertyTx(a, TestOne.anInt, 2);
				setPropertyTx(b, TestOne.anInt, 1);
				assertEquals(1, comp.compare(a, b));
			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}
	
	public void test01CompareDescending() {

		try {

			TestOne a = createTestNode(TestOne.class);
			TestOne b = createTestNode(TestOne.class);
			
			GraphObjectComparator comp = new GraphObjectComparator(TestOne.anInt, GraphObjectComparator.DESCENDING);

			try {
				comp.compare(null, null);
				fail("Should have raised an NullPointerException");
				
			} catch (NullPointerException e) {}

			try {
				comp.compare(a, null);
				fail("Should have raised an NullPointerException");
				
			} catch (NullPointerException e) {}

			try {
				comp.compare(null, b);
				fail("Should have raised an NullPointerException");
				
			} catch (NullPointerException e) {}

			try {
				comp.compare(null, b);
				fail("Should have raised an NullPointerException");
				
			} catch (NullPointerException e) {}

			try (final Tx tx = app.tx()) {
				
				// a: null
				// b: null
				// a == b => 0
				assertEquals(0, comp.compare(a, b));
			}

			// a: 0
			// b: null
			// a > b => 1
			setPropertyTx(a, TestOne.anInt, 0);
			
			try (final Tx tx = app.tx()) {
				
				assertEquals(1, comp.compare(a, b));
			}

			// a: null
			// b: 0
			// a < b => -1
			setPropertyTx(a, TestOne.anInt, null);
			setPropertyTx(b, TestOne.anInt, 0);
			
			try (final Tx tx = app.tx()) {
				
				assertEquals(-1, comp.compare(a, b));
			}
			
			// a: 1
			// b: 2
			// a > b => 1
			setPropertyTx(a, TestOne.anInt, 1);
			setPropertyTx(b, TestOne.anInt, 2);
			
			try (final Tx tx = app.tx()) {
				
				assertEquals(1, comp.compare(a, b));
			}

			// a: 2
			// b: 1
			// a < b => -1
			setPropertyTx(a, TestOne.anInt, 2);
			setPropertyTx(b, TestOne.anInt, 1);
			
			try (final Tx tx = app.tx()) {
				
				assertEquals(-1, comp.compare(a, b));
			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}
		
	private void setPropertyTx(final GraphObject obj, final PropertyKey key, final Object value) {
		
		try (final Tx tx = app.tx()) {
			
			obj.setProperty(key, value);
			tx.success();
			
		} catch (FrameworkException ex) {
		}
	}
}
