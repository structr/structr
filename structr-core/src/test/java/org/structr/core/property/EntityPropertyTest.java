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

import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.TestThree;
import org.structr.core.entity.TestSix;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;

/**
 *
 * @author Christian Morgner
 */
public class EntityPropertyTest extends StructrTest {

	public void testOneToOne() throws Exception {
		
		EntityProperty<TestThree> instance = TestSix.oneToOneTestThree;
		TestSix testSix1                   = null;
		TestSix testSix2                   = null;
		TestThree testThree1               = null;
		TestThree testThree2               = null;
		
		try {
			
			testSix1 = createTestNode(TestSix.class);
			testSix1.setProperty(AbstractNode.name, "testSix1");
			
			testSix2 = createTestNode(TestSix.class);
			testSix2.setProperty(AbstractNode.name, "testSix2");
			
			testThree1 = createTestNode(TestThree.class);
			testThree1.setProperty(AbstractNode.name, "testThree1");

			testThree2 = createTestNode(TestThree.class);
			testThree2.setProperty(AbstractNode.name, "testThree2");
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			fail("Unable to create test nodes");
		}
		
		assertNotNull(testSix1);
		assertNotNull(testSix2);
		assertNotNull(testThree1);
		assertNotNull(testThree2);

		/**
		 * We test the following here:
		 * A -> B
		 * C -> D
		 * 
		 * then connect A -> D, so C and B should not
		 * be related any more
		 */
		
		// create two connections
		instance.setProperty(securityContext, testSix1, testThree1);
		instance.setProperty(securityContext, testSix2, testThree2);
		
		// verify connections
		TestThree vrfy1 = instance.getProperty(securityContext, testSix1, true);
		TestThree vrfy2 = instance.getProperty(securityContext, testSix2, true);
		
		assertTrue(vrfy1 != null && vrfy1.equals(testThree1));
		assertTrue(vrfy2 != null && vrfy2.equals(testThree2));
		
		
		
		// connect testSix1 and testThree2
		instance.setProperty(securityContext, testSix1, testThree2);

		// verify connection
		TestThree vrfy3 = instance.getProperty(securityContext, testSix1, true);
		assertTrue(vrfy3 != null && vrfy3.equals(testThree2));

		
		// testSix2 should not have a testThree associated
		TestThree vrfy4 = instance.getProperty(securityContext, testSix2, true);
		assertNull(vrfy4);
		
//		
//		
//		
//		
//		System.out.println("#####################################################################################################");
//		System.out.println("testSix1:");
//		
//		// test
//		for (AbstractRelationship rel : testSix1.getRelationships()) {
//		
//			System.out.println(rel.getStartNode().getProperty(AbstractNode.name) + " -[" + rel.getType() + "]->" + rel.getEndNode().getProperty(AbstractNode.name));
//		}
//		
//		System.out.println("#####################################################################################################");
//		System.out.println("testSix2:");
//		
//		// test
//		for (AbstractRelationship rel : testSix2.getRelationships()) {
//		
//			System.out.println(rel.getStartNode().getProperty(AbstractNode.name) + " -[" + rel.getType() + "]->" + rel.getEndNode().getProperty(AbstractNode.name));
//		}
//		
	}

	public void testManyToOne() throws Exception {
		
		EntityProperty<TestThree> instance = TestSix.manyToOneTestThree;
		TestSix testSix1                   = null;
		TestSix testSix2                   = null;
		TestThree testThree1               = null;
		TestThree testThree2               = null;
		
		try {
			
			testSix1 = createTestNode(TestSix.class);
			testSix2 = createTestNode(TestSix.class);
			
			testThree1 = createTestNode(TestThree.class);
			testThree2 = createTestNode(TestThree.class);
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			fail("Unable to create test nodes");
		}
		
		assertNotNull(testSix1);
		assertNotNull(testSix2);
		assertNotNull(testThree1);
		assertNotNull(testThree2);

		/**
		 * We test the following here:
		 * A -> B
		 * C -> D
		 * 
		 * then connect A -> D, so C and B should not
		 * be related any more
		 */
		
		// create two connections
		instance.setProperty(securityContext, testSix1, testThree1);
		instance.setProperty(securityContext, testSix2, testThree2);
		
		// verify connections
		TestThree vrfy1 = instance.getProperty(securityContext, testSix1, true);
		TestThree vrfy2 = instance.getProperty(securityContext, testSix2, true);
		
		assertTrue(vrfy1 != null && vrfy1.equals(testThree1));
		assertTrue(vrfy2 != null && vrfy2.equals(testThree2));
		
		
		
		// connect testSix1 and testThree2
		instance.setProperty(securityContext, testSix1, testThree2);

		// verify connection
		TestThree vrfy3 = instance.getProperty(securityContext, testSix1, true);

		assertTrue(vrfy3 != null && vrfy3.equals(testThree2));
		
		// testSix2 should have the initally assigned testThree associated
		TestThree vrfy4 = instance.getProperty(securityContext, testSix2, true);
		assertEquals(testThree2, vrfy4);
	}
	
	/**
	 * Test of typeName method, of class EntityProperty.
	 */
	public void testTypeName() {

		EntityProperty instance = TestSix.oneToOneTestThree;
		String expResult = "Object";
		String result = instance.typeName();
		assertEquals(expResult, result);
	}

	/**
	 * Test of databaseConverter method, of class EntityProperty.
	 */
	public void testDatabaseConverter() {

		EntityProperty instance = TestSix.oneToOneTestThree;
		PropertyConverter expResult = null;
		PropertyConverter result = instance.databaseConverter(securityContext, null);
		assertEquals(expResult, result);
	}

	/**
	 * Test of inputConverter method, of class EntityProperty.
	 */
	public void testInputConverter() {

		EntityProperty instance = TestSix.oneToOneTestThree;
		PropertyConverter result = instance.inputConverter(securityContext);
		
		assertTrue(result != null);
	}

	/**
	 * Test of relatedType method, of class EntityProperty.
	 */
	public void testRelatedType() {

		EntityProperty instance = TestSix.oneToOneTestThree;
		Class expResult = TestThree.class;
		Class result = instance.relatedType();
		assertEquals(expResult, result);
	}

	/**
	 * Test of isCollection method, of class EntityProperty.
	 */
	public void testIsCollection() {

		EntityProperty instance = TestSix.oneToOneTestThree;
		boolean expResult = false;
		boolean result = instance.isCollection();
		assertEquals(expResult, result);
	}

	/**
	 * Test of getNotion method, of class EntityProperty.
	 */
	public void testGetNotion() {

		EntityProperty instance = TestSix.oneToOneTestThree;
		Notion result = instance.getNotion();
		
		assertTrue(result != null && result instanceof ObjectNotion);
	}

	/**
	 * Test of isOneToMany method, of class EntityProperty.
	 */
	public void testIsManyToOne() {

		EntityProperty instance = TestSix.oneToOneTestThree;
		boolean expResult = false;
		boolean result = instance.isManyToOne();
		assertEquals(expResult, result);
	}
}
