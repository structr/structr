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

import static junit.framework.TestCase.assertEquals;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.entity.TestFour;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.OneFourOneToOne;
import org.structr.core.graph.Tx;

/**
 *
 *
 */
public class StringPropertyTest extends StructrTest {
	
	public void test() {
		
		try {

			final Property<String> instance = TestFour.stringProperty;
			final TestFour testEntity        = createTestNode(TestFour.class);
			
			assertNotNull(testEntity);

			// store string in the test entitiy
			final String value = "This is a test!";

			try (final Tx tx = app.tx()) {

				instance.setProperty(securityContext, testEntity, value);
				tx.success();
			}

			try (final Tx tx = app.tx()) {
				
				// check value from database
				assertEquals(value, instance.getProperty(securityContext, testEntity, true));
			}
			
		} catch (FrameworkException fex) {
			
			fail("Unable to store array");
		}
	}
	
	public void testSimpleSearchOnNode() {
		
		try {

			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<String> key = TestFour.stringProperty;
			
			properties.put(key, "test");
			
			final TestFour testEntity     = createTestNode(TestFour.class, properties);
			
			assertNotNull(testEntity);
			
			try (final Tx tx = app.tx()) {

				// check value from database
				assertEquals("test", testEntity.getProperty(key));
				
				Result<TestFour> result = app.nodeQuery(TestFour.class).and(key, "test").getResult();

				assertEquals(result.size(), 1);
				assertEquals(result.get(0), testEntity);
			}
		
		} catch (FrameworkException fex) {
			
			fail("Unable to store array");
		}
		
	}
	
	public void testSimpleSearchOnRelationship() {
		
		try {

			final TestOne testOne        = createTestNode(TestOne.class);
			final TestFour testFour      = createTestNode(TestFour.class);
			final Property<String> key   = OneFourOneToOne.stringProperty;
			
			assertNotNull(testOne);
			assertNotNull(testFour);
			
			final OneFourOneToOne testEntity = createTestRelationship(testOne, testFour, OneFourOneToOne.class);
			
			assertNotNull(testEntity);

			try (final Tx tx = app.tx()) {
				
				testEntity.setProperty(key, "test");
				tx.success();
			}

			try (final Tx tx = app.tx()) {
				
				// check value from database
				assertEquals("test", testEntity.getProperty(key));

				Result<OneFourOneToOne> result = app.relationshipQuery(OneFourOneToOne.class).and(key, "test").getResult();

				assertEquals(result.size(), 1);
				assertEquals(result.get(0), testEntity);
			}
		
		} catch (FrameworkException fex) {
			
			fail("Unable to store array");
		}
	}
}
