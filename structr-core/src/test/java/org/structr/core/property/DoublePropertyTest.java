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
import org.structr.core.entity.TestFour;

/**
 *
 * @author Christian Morgner
 */
public class DoublePropertyTest extends StructrTest {
	
	public void test() {
		
		Property<Double> instance = TestFour.doubleProperty;
		TestFour testEntity        = null;
		
		try {
			testEntity = createTestNode(TestFour.class);
			
		} catch (FrameworkException fex) {
			
			fail("Unable to create test node.");
		}
		
		assertNotNull(testEntity);
		
		// store double in the test entitiy
		Double value = 3.141592653589793238;
		
		try {
			instance.setProperty(securityContext, testEntity, value);
			
		} catch (FrameworkException fex) {
			
			fail("Unable to store array");
		}

		// check value from database
		assertEquals(value, instance.getProperty(securityContext, testEntity, true));
	}
}
