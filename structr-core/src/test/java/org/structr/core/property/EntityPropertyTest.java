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

import static junit.framework.Assert.fail;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.TestThree;
import org.structr.core.entity.TestSix;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.notion.Notion;
import org.structr.core.notion.ObjectNotion;

/**
 *
 * @author Christian Morgner
 */
public class EntityPropertyTest extends StructrTest {

	public void testOneToOne() throws Exception {
		
		final EntityProperty<TestThree> instance = TestSix.oneToOneTestThree;
		final TestSix testSix1                   = createTestNode(TestSix.class);
		final TestSix testSix2                   = createTestNode(TestSix.class);
		final TestThree testThree1               = createTestNode(TestThree.class);
		final TestThree testThree2               = createTestNode(TestThree.class);
		
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					testSix1.setProperty(AbstractNode.name, "testSix1");
					testSix2.setProperty(AbstractNode.name, "testSix2");
					testThree1.setProperty(AbstractNode.name, "testThree1");
					testThree2.setProperty(AbstractNode.name, "testThree2");

					return null;
				}

			});
			
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
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
					
					instance.setProperty(securityContext, testSix1, testThree1);
					instance.setProperty(securityContext, testSix2, testThree2);

					return null;
				}

			});
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			fail("Unable to link test nodes");
		}
		
		// verify connections
		TestThree vrfy1 = instance.getProperty(securityContext, testSix1, true);
		TestThree vrfy2 = instance.getProperty(securityContext, testSix2, true);
		
		assertTrue(vrfy1 != null && vrfy1.equals(testThree1));
		assertTrue(vrfy2 != null && vrfy2.equals(testThree2));
		
		
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
		
					// connect testSix1 and testThree2
					instance.setProperty(securityContext, testSix1, testThree2);

					return null;
				}

			});
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			fail("Unable to link test nodes");
		}
		

		// verify connection
		TestThree vrfy3 = instance.getProperty(securityContext, testSix1, true);
		assertTrue(vrfy3 != null && vrfy3.equals(testThree2));

		
		// testSix2 should not have a testThree associated
		TestThree vrfy4 = instance.getProperty(securityContext, testSix2, true);
		assertNull(vrfy4);
	}

	public void testManyToOne() throws Exception {
		
		final EntityProperty<TestThree> instance = TestSix.manyToOneTestThree;
		final TestSix testSix1                         = createTestNode(TestSix.class);
		final TestSix testSix2                         = createTestNode(TestSix.class);
		final TestThree testThree1                     = createTestNode(TestThree.class);
		final TestThree testThree2                     = createTestNode(TestThree.class);
		
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
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
					
					// create two connections
					instance.setProperty(securityContext, testSix1, testThree1);
					instance.setProperty(securityContext, testSix2, testThree2);

					return null;
				}

			});
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			fail("Unable to link test nodes");
		}
		
		// verify connections
		TestThree vrfy1 = instance.getProperty(securityContext, testSix1, true);
		TestThree vrfy2 = instance.getProperty(securityContext, testSix2, true);
		
		assertTrue(vrfy1 != null && vrfy1.equals(testThree1));
		assertTrue(vrfy2 != null && vrfy2.equals(testThree2));
		
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
					
					// connect testSix1 and testThree2
					instance.setProperty(securityContext, testSix1, testThree2);

					return null;
				}

			});
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			fail("Unable to link test nodes");
		}

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
