/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import java.util.LinkedList;
import java.util.List;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSix;

/**
 *
 * @author Christian Morgner
 */
public class CollectionPropertyTest extends StructrTest {

	public void testManyToMany() throws Exception {
		
		Property<List<TestOne>> instance = TestSix.manyToManyTestOnes;
		TestSix testSix1  = null;
		TestSix testSix2  = null;
		TestOne testOne1  = null;
		TestOne testOne2  = null;
		
		try {
			
			testSix1 = createTestNode(TestSix.class);
			testSix2 = createTestNode(TestSix.class);
			
			testOne1 = createTestNode(TestOne.class);
			testOne2 = createTestNode(TestOne.class);
			
		} catch (FrameworkException fex) {
			
			fail("Unable to create test nodes");
		}
		
		assertNotNull(testSix1);
		assertNotNull(testSix2);
		assertNotNull(testOne1);
		assertNotNull(testOne2);

		// set two TestOne entities on both TestSix entities
		List<TestOne> twoTestOnesList = new LinkedList<>();
		twoTestOnesList.add(testOne1);
		twoTestOnesList.add(testOne2);

		try {
			app.beginTx();
			instance.setProperty(securityContext, testSix1, twoTestOnesList);
			instance.setProperty(securityContext, testSix2, twoTestOnesList);
			app.commitTx();

		} finally {

			app.finishTx();
		}
		
		List<TestOne> testOnesFromTestSix1 = instance.getProperty(securityContext, testSix1, true);
		List<TestOne> testOnesFromTestSix2 = instance.getProperty(securityContext, testSix2, true);

		assertNotNull(testOnesFromTestSix1);
		assertNotNull(testOnesFromTestSix2);
		
		// both entities should have the two related nodes
		assertEquals(2, testOnesFromTestSix1.size());
		assertEquals(2, testOnesFromTestSix2.size());
		
		// create new list with one TestOne entity
		List<TestOne> oneTestOneList = new LinkedList<>();
		oneTestOneList.add(testOne1);

		try {
			app.beginTx();
			// set list with one TestOne node as related nodes
			instance.setProperty(securityContext, testSix1, oneTestOneList);
			app.commitTx();

		} finally {

			app.finishTx();
		}

		List<TestOne> oneTestOnesFromTestSix1 = instance.getProperty(securityContext, testSix1, true);
		
		// entity should have exactly one related node
		assertNotNull(oneTestOnesFromTestSix1);
		assertEquals(1, oneTestOnesFromTestSix1.size());
		
		assertEquals(oneTestOnesFromTestSix1.get(0).getUuid(), testOne1.getUuid());
		
	}
	
	public void testCascadingDelete() {
		
		
	}
	
	/**
	 * Test of typeName method, of class CollectionProperty.
	 */
	public void testTypeName() {

		Property<List<TestOne>> instance = TestSix.manyToManyTestOnes;
		String expResult = "Object";
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
