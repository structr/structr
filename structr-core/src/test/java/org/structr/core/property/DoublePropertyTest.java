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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.TestFour;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.OneFourOneToOne;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.graph.search.SearchRelationshipCommand;

/**
 *
 * @author Christian Morgner
 */
public class DoublePropertyTest extends StructrTest {
	
	public void test() {
		
		try {
			final Property<Double> instance = TestFour.doubleProperty;
			final TestFour testEntity        = createTestNode(TestFour.class);
			
			assertNotNull(testEntity);

			// store double in the test entitiy
			final Double value = 3.141592653589793238;

			try {
				app.beginTx();
				instance.setProperty(securityContext, testEntity, value);
				app.commitTx();

			} finally {

				app.finishTx();
			}

			// check value from database
			assertEquals(value, instance.getProperty(securityContext, testEntity, true));
			
		} catch (FrameworkException fex) {
			
			fail("Unable to store array");
		}
	}
	
	public void testSimpleSearchOnNode() {
		
		try {
			final PropertyMap properties  = new PropertyMap();
			final PropertyKey<Double> key = TestFour.doubleProperty;
			
			properties.put(key, 3.141592653589793238);
			
			final TestFour testEntity     = createTestNode(TestFour.class, properties);
			
			assertNotNull(testEntity);

			// check value from database
			assertEquals(3.141592653589793238, testEntity.getProperty(key));
			
			
			Result<TestFour> result = Services.command(securityContext, SearchNodeCommand.class).execute(
				Search.andExactType(TestFour.class),
				Search.andExactProperty(securityContext, key, 3.141592653589793238)
			);
			
			assertEquals(result.size(), 1);
			assertEquals(result.get(0), testEntity);
		
		} catch (FrameworkException fex) {
			
			fail("Unable to store array");
		}
		
	}
	
	public void testSimpleSearchOnRelationship() {
		
		try {
			final TestOne testOne        = createTestNode(TestOne.class);
			final TestFour testFour      = createTestNode(TestFour.class);
			final Property<Double> key = OneFourOneToOne.doubleProperty;
			
			assertNotNull(testOne);
			assertNotNull(testFour);
			
			final OneFourOneToOne testEntity = createTestRelationship(testOne, testFour, OneFourOneToOne.class);
			
			assertNotNull(testEntity);

			try {
				app.beginTx();
				testEntity.setProperty(key, 3.141592653589793238);
				app.commitTx();

			} finally {

				app.finishTx();
			}
			
			// check value from database
			assertEquals(3.141592653589793238, testEntity.getProperty(key));
			
			Result<TestFour> result = Services.command(securityContext, SearchRelationshipCommand.class).execute(
				Search.andExactRelType(OneFourOneToOne.class),
				Search.andExactProperty(securityContext, key, 3.141592653589793238)
			);
			
			assertEquals(result.size(), 1);
			assertEquals(result.get(0), testEntity);
		
		} catch (FrameworkException fex) {
			
			fail("Unable to store array");
		}
	}
}
