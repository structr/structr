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

import java.lang.String;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import org.apache.commons.lang.ArrayUtils;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.TestFour;

/**
 *
 * @author Christian Morgner
 */
public class ArrayPropertyTest extends StructrTest {
	
	public void testStringArrayProperty() {
		
		
		try {
			final Property<String[]> instance = TestFour.stringArrayProperty;
			final TestFour testEntity         = createTestNode(TestFour.class);
			
			assertNotNull(testEntity);

			// store a string array in the test entitiy
			final String[] arr = new String[] { "one", "two", "three", "four", "five" };

			app.beginTx();
			instance.setProperty(securityContext, testEntity, arr);
			app.commitTx();
			
			String[] newArr = instance.getProperty(securityContext, testEntity, true);

			assertTrue(ArrayUtils.isEquals(arr, newArr));
			
		} catch (FrameworkException fex) {
			
			fail("Unable to store array");
		}
	}
	
	/* FIXME: how can we search for arrays?
	public void testSimpleSearchOnNode() {
		
		try {
			final PropertyMap properties    = new PropertyMap();
			final PropertyKey<String[]> key = TestFour.stringArrayProperty;

			// store a string array in the test entitiy
			final String[] arr = new String[] { "one", "two", "three", "four", "five" };
			
			properties.put(key, arr);
			
			final TestFour testEntity     = createTestNode(TestFour.class, properties);
			
			assertNotNull(testEntity);

			// check value from database
			assertEquals(arr, testEntity.getProperty(key));
			
			
			Result<TestFour> result = Services.command(securityContext, SearchNodeCommand.class).execute(
				Search.andExactType(TestFour.class),
				Search.andExactProperty(securityContext, key, arr)
			);
			
			assertEquals(1, result.size());
			assertEquals(result.get(0), testEntity);
		
		} catch (FrameworkException fex) {
			
			fail("Unable to store array");
		}
		
	}
	
	public void testSimpleSearchOnRelationship() {
		
		try {
			final TestOne testOne        = createTestNode(TestOne.class);
			final TestFour testFour      = createTestNode(TestFour.class);
			final Property<String[]> key = TestRelationship.stringArrayProperty;

			// store a string array in the test entitiy
			final String[] arr = new String[] { "one", "two", "three", "four", "five" };
			
			assertNotNull(testOne);
			assertNotNull(testFour);
			
			final TestRelationship testEntity = (TestRelationship)createTestRelationship(testOne, testFour, RelType.IS_AT);
			
			assertNotNull(testEntity);

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
					
					// set property
					testEntity.setProperty(key, arr);
					
					return null;
				}
				
			});
			
			// check value from database
			assertEquals(arr, testEntity.getProperty(key));	
			
			Result<TestFour> result = Services.command(securityContext, SearchRelationshipCommand.class).execute(
				Search.andExactRelType(EntityContext.getNamedRelation(TestRelationship.Relation.test_relationships.name())),
				Search.andExactProperty(securityContext, key, arr)
			);
			
			assertEquals(result.size(), 1);
			assertEquals(result.get(0), testEntity);
		
		} catch (FrameworkException fex) {
			
			fail("Unable to store array");
		}
	}
	*/
}
