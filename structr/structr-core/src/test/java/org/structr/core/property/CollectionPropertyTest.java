/*
 *  Copyright (C) 2012 Axel Morgner
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
package org.structr.core.property;

import java.util.LinkedList;
import java.util.List;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSix;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;

/**
 *
 * @author Christian Morgner
 */
public class CollectionPropertyTest extends StructrTest {

	public void testManyToMany() throws Exception {
		
		CollectionProperty instance = TestSix.manyToManyTestOnes;
		TestSix testSix1            = null;
		TestSix testSix2            = null;
		TestOne testOne1            = null;
		TestOne testOne2            = null;
		
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
		List<TestOne> testOnesToSet = new LinkedList<TestOne>();
		testOnesToSet.add(testOne1);
		testOnesToSet.add(testOne2);

		instance.setProperty(securityContext, testSix1, testOnesToSet);
		instance.setProperty(securityContext, testSix2, testOnesToSet);
		
		List<TestOne> testOnesFromTestSix1 = instance.getProperty(securityContext, testSix1, true);
		List<TestOne> testOnesFromTestSix2 = instance.getProperty(securityContext, testSix2, true);

		assertNotNull(testOnesFromTestSix1);
		assertNotNull(testOnesFromTestSix2);
		
		// both entities should have the two related nodes
		assertEquals(2, testOnesFromTestSix1.size());
		assertEquals(2, testOnesFromTestSix2.size());
	}

	public void testOneToMany() throws Exception {
		
		CollectionProperty instance = TestSix.oneToManyTestOnes;
		TestSix testSix1            = null;
		TestSix testSix2            = null;
		TestOne testOne1            = null;
		TestOne testOne2            = null;
		
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

		List<TestOne> testOnesToSet = new LinkedList<TestOne>();
		testOnesToSet.add(testOne1);
		testOnesToSet.add(testOne2);

		// set two TestOne entities on textSix1
		instance.setProperty(securityContext, testSix1, testOnesToSet);
		
		// verfiy that the relationships in testSix1 have in fact been created
		List vrfy1 = instance.getProperty(securityContext, testSix1, true);
		assertTrue(vrfy1 != null && vrfy1.size() == 2);
		
		// set two TestOne entities on textSix1, should remove the previously created rels
		instance.setProperty(securityContext, testSix2, testOnesToSet);
		
		// verfiy that the relationships in testSix2 have in fact been created
		List vrfy2 = instance.getProperty(securityContext, testSix2, true);
		assertTrue(vrfy2 != null && vrfy2.size() == 2);
		
		// verfiy that the relationships in testSix1 have been removed by the previous call
		List vrfy3 = instance.getProperty(securityContext, testSix1, true);
		assertTrue(vrfy3 != null && vrfy3.size() == 0);
	}
	
	
	/**
	 * Test of typeName method, of class CollectionProperty.
	 */
	public void testTypeName() {

		CollectionProperty instance = TestSix.manyToManyTestOnes;
		String expResult = "Object";
		String result = instance.typeName();
		assertEquals(expResult, result);
	}

	/**
	 * Test of databaseConverter method, of class CollectionProperty.
	 */
	public void testDatabaseConverter() {

		CollectionProperty instance = TestSix.manyToManyTestOnes;
		PropertyConverter expResult = null;
		PropertyConverter result = instance.databaseConverter(securityContext, null);
		assertEquals(expResult, result);
	}

	/**
	 * Test of inputConverter method, of class CollectionProperty.
	 */
	public void testInputConverter() {

		CollectionProperty instance = TestSix.manyToManyTestOnes;
		PropertyConverter result = instance.inputConverter(securityContext);
		
		assertTrue(result != null);
	}

	/**
	 * Test of relatedType method, of class CollectionProperty.
	 */
	public void testRelatedType() {

		CollectionProperty instance = TestSix.manyToManyTestOnes;
		Class expResult = TestOne.class;
		Class result = instance.relatedType();
		assertEquals(expResult, result);
	}

	/**
	 * Test of isCollection method, of class CollectionProperty.
	 */
	public void testIsCollection() {

		CollectionProperty instance = TestSix.manyToManyTestOnes;
		boolean expResult = true;
		boolean result = instance.isCollection();
		assertEquals(expResult, result);
	}

	/**
	 * Test of getNotion method, of class CollectionProperty.
	 */
	public void testGetNotion() {

		CollectionProperty instance = TestSix.manyToManyTestOnes;
		Notion result = instance.getNotion();
		
		assertTrue(result != null && result instanceof ObjectNotion);
	}

	/**
	 * Test of isOneToMany method, of class CollectionProperty.
	 */
	public void testIsOneToMany() {

		CollectionProperty instance = TestSix.manyToManyTestOnes;
		boolean expResult = false;
		boolean result = instance.isOneToMany();
		assertEquals(expResult, result);
	}
}
