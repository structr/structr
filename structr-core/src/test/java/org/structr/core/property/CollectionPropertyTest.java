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
package org.structr.core.property;

import java.util.LinkedList;
import java.util.List;


import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSix;
import org.structr.core.graph.Tx;

/**
 *
 *
 */
public class CollectionPropertyTest extends StructrTest {

	public void testOneToMany() throws Exception {

		TestOne testOne                   = null;
		List<TestSix> testSixs            = null;
		List<TestSix> testSixs2           = null;
		int index                         = 0;

		List<Integer> index1              = new LinkedList();
		List<Integer> index2              = new LinkedList();

		try (final Tx tx = app.tx()) {

			testOne        = createTestNode(TestOne.class);
			testSixs       = createTestNodes(TestSix.class, 20);

			for (final TestSix testSix : testSixs) {
				int i = index++;
				testSix.setProperty(TestSix.index, i);
				System.out.println(i + " ");
				index1.add(i);
			}

			testOne.setProperty(TestOne.manyToManyTestSixs, testSixs);

			tx.success();

		}

		try (final Tx tx = app.tx()) {

			testSixs2 = testOne.getProperty(TestOne.manyToManyTestSixs);

			for (final TestSix testSix : testSixs2) {
				int i = testSix.getProperty(TestSix.index);
				System.out.println(i + " ");
				index2.add(i);
			}

			assertEquals(index1, index2);

			tx.success();

		}


	}

	public void testManyToMany() throws Exception {

		try {

			Property<List<TestOne>> instance = TestSix.manyToManyTestOnes;
			TestSix testSix1  = null;
			TestSix testSix2  = null;
			TestOne testOne1  = null;
			TestOne testOne2  = null;

			testSix1 = createTestNode(TestSix.class);
			testSix2 = createTestNode(TestSix.class);

			testOne1 = createTestNode(TestOne.class);
			testOne2 = createTestNode(TestOne.class);

			assertNotNull(testSix1);
			assertNotNull(testSix2);
			assertNotNull(testOne1);
			assertNotNull(testOne2);

			// set two TestOne entities on both TestSix entities
			List<TestOne> twoTestOnesList = new LinkedList<>();
			twoTestOnesList.add(testOne1);
			twoTestOnesList.add(testOne2);

			try (final Tx tx = app.tx()) {

				instance.setProperty(securityContext, testSix1, twoTestOnesList);
				instance.setProperty(securityContext, testSix2, twoTestOnesList);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<TestOne> testOnesFromTestSix1 = instance.getProperty(securityContext, testSix1, true);
				List<TestOne> testOnesFromTestSix2 = instance.getProperty(securityContext, testSix2, true);

				assertNotNull(testOnesFromTestSix1);
				assertNotNull(testOnesFromTestSix2);

				// both entities should have the two related nodes
				assertEquals(2, testOnesFromTestSix1.size());
				assertEquals(2, testOnesFromTestSix2.size());
			}

			// create new list with one TestOne entity
			List<TestOne> oneTestOneList = new LinkedList<>();
			oneTestOneList.add(testOne1);

			try (final Tx tx = app.tx()) {

				// set list with one TestOne node as related nodes
				instance.setProperty(securityContext, testSix1, oneTestOneList);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<TestOne> oneTestOnesFromTestSix1 = instance.getProperty(securityContext, testSix1, true);

				// entity should have exactly one related node
				assertNotNull(oneTestOnesFromTestSix1);
				assertEquals(1, oneTestOnesFromTestSix1.size());

				assertEquals(oneTestOnesFromTestSix1.get(0).getUuid(), testOne1.getUuid());
			}

		} catch (FrameworkException fex) {

		}

	}

	/**
	 * Test of typeName method, of class CollectionProperty.
	 */
	public void testTypeName() {

		Property<List<TestOne>> instance = TestSix.manyToManyTestOnes;
		String expResult = "collection";
		String result = instance.typeName();
		assertEquals(expResult, result);
	}

	/**
	 * Test of databaseConverter method, of class CollectionProperty.
	 */
	public void testDatabaseConverter() {

		Property<List<TestOne>> instance = TestSix.manyToManyTestOnes;
		PropertyConverter expResult = null;
		PropertyConverter result = instance.databaseConverter(securityContext, null);
		assertEquals(expResult, result);
	}

	/**
	 * Test of inputConverter method, of class CollectionProperty.
	 */
	public void testInputConverter() {

		Property<List<TestOne>> instance = TestSix.manyToManyTestOnes;
		PropertyConverter result = instance.inputConverter(securityContext);

		assertTrue(result != null);
	}

	/**
	 * Test of relatedType method, of class CollectionProperty.
	 */
	public void testRelatedType() {

		Property<List<TestOne>> instance = TestSix.manyToManyTestOnes;
		Class expResult = TestOne.class;
		Class result = instance.relatedType();
		assertEquals(expResult, result);
	}

	/**
	 * Test of isCollection method, of class CollectionProperty.
	 */
	public void testIsCollection() {

		Property<List<TestOne>> instance = TestSix.manyToManyTestOnes;
		boolean expResult = true;
		boolean result = instance.isCollection();
		assertEquals(expResult, result);
	}
}
