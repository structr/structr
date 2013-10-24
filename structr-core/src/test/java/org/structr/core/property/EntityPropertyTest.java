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

import static junit.framework.Assert.assertNotNull;
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
		
		final TestSix a   = createTestNode(TestSix.class);
		final TestSix c   = createTestNode(TestSix.class);
		final TestThree b = createTestNode(TestThree.class);
		final TestThree d = createTestNode(TestThree.class);
		
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					a.setProperty(AbstractNode.name, "a");
					c.setProperty(AbstractNode.name, "c");
					b.setProperty(AbstractNode.name, "b");
					d.setProperty(AbstractNode.name, "d");

					return null;
				}

			});
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			fail("Unable to create test nodes");
		}
		
		assertNotNull(a);
		assertNotNull(c);
		assertNotNull(b);
		assertNotNull(d);

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
					
					a.setProperty(TestSix.oneToOneTestThree, b);
					c.setProperty(TestSix.oneToOneTestThree, d);

					return null;
				}

			});
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			fail("Unable to link test nodes");
		}
		
		// verify connections
		TestThree verifyB = a.getProperty(TestSix.oneToOneTestThree);
		TestThree verifyD = c.getProperty(TestSix.oneToOneTestThree);
		
		assertTrue(verifyB != null && verifyB.equals(b));
		assertTrue(verifyD != null && verifyD.equals(d));
		
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
		
					// connect a and d
					a.setProperty(TestSix.oneToOneTestThree, d);

					return null;
				}

			});
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			fail("Unable to link test nodes");
		}
		

		// verify connection
		TestThree verifyD2 = a.getProperty(TestSix.oneToOneTestThree);
		assertTrue(verifyD2 != null && verifyD2.equals(d));

		// testSix2 should not have a testThree associated
		TestThree vrfy4 = c.getProperty(TestSix.oneToOneTestThree);
		assertNull(vrfy4);
	}

	public void testOneToMany() throws Exception {
		
		final TestSix a   = createTestNode(TestSix.class);
		final TestSix c   = createTestNode(TestSix.class);
		final TestThree b = createTestNode(TestThree.class);
		final TestThree d = createTestNode(TestThree.class);
				
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					a.setProperty(AbstractNode.name, "a");
					c.setProperty(AbstractNode.name, "c");
					b.setProperty(AbstractNode.name, "b");
					d.setProperty(AbstractNode.name, "d");

					return null;
				}

			});
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			fail("Unable to create test nodes");
		}

		assertNotNull(a);
		assertNotNull(b);
		assertNotNull(c);
		assertNotNull(d);

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
					a.setProperty(TestSix.oneToManyTestThrees, b);
					c.setProperty(TestSix.oneToManyTestThrees, d);

					return null;
				}

			});
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			fail("Unable to link test nodes");
		}
		
		// verify connections
		TestThree verifyB = a.getProperty(TestSix.oneToManyTestThrees);
		TestThree verifyD = c.getProperty(TestSix.oneToManyTestThrees);
		
		assertTrue(verifyB != null && verifyB.equals(b));
		assertTrue(verifyD != null && verifyD.equals(d));
		
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
					
					// connect testSix1 and testThree2
					a.setProperty(TestSix.oneToManyTestThrees, d);

					return null;
				}

			});
			
		} catch (FrameworkException fex) {
		
			fex.printStackTrace();
			
			fail("Unable to link test nodes");
		}

		// verify connection
		TestThree verifyD2 = a.getProperty(TestSix.oneToManyTestThrees);

		assertTrue(verifyD2 != null && verifyD2.equals(d));
		
		// testSix2 should have the initally assigned testThree associated
		TestThree vrfy4 = c.getProperty(TestSix.oneToManyTestThrees);
		assertEquals(d, vrfy4);
	}

	/**
	 * Test of typeName method, of class EntityProperty.
	 */
	public void testTypeName() {

		End instance = TestSix.oneToOneTestThree;
		String expResult = "Object";
		String result = instance.typeName();
		assertEquals(expResult, result);
	}

	/**
	 * Test of databaseConverter method, of class EntityProperty.
	 */
	public void testDatabaseConverter() {

		End instance = TestSix.oneToOneTestThree;
		PropertyConverter expResult = null;
		PropertyConverter result = instance.databaseConverter(securityContext, null);
		assertEquals(expResult, result);
	}

	/**
	 * Test of inputConverter method, of class EntityProperty.
	 */
	public void testInputConverter() {

		End instance = TestSix.oneToOneTestThree;
		PropertyConverter result = instance.inputConverter(securityContext);
		
		assertTrue(result != null);
	}

	/**
	 * Test of relatedType method, of class EntityProperty.
	 */
	public void testRelatedType() {

		End instance = TestSix.oneToOneTestThree;
		Class expResult = TestThree.class;
		Class result = instance.relatedType();
		assertEquals(expResult, result);
	}

	/**
	 * Test of isCollection method, of class EntityProperty.
	 */
	public void testIsCollection() {

		End instance = TestSix.oneToOneTestThree;
		boolean expResult = false;
		boolean result = instance.isCollection();
		assertEquals(expResult, result);
	}

	/**
	 * Test of getNotion method, of class EntityProperty.
	 */
	public void testGetNotion() {

		End instance = TestSix.oneToOneTestThree;
		Notion result = instance.getNotion();
		
		assertTrue(result != null && result instanceof ObjectNotion);
	}

	/**
	 * Test of isManyToOne method, of class EntityProperty.
	 */
	public void testIsManyToOne() {

		End instance = TestSix.oneToOneTestThree;
		boolean expResult = false;
		boolean result = instance.isManyToOne();
		assertEquals(expResult, result);
	}
}
