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

import org.apache.commons.lang.ArrayUtils;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.TestFour;

/**
 *
 * @author Christian Morgner
 */
public class ArrayPropertyTest extends StructrTest {
	
	public void testArrayProperty() {
		
		Property<String[]> instance = TestFour.stringArrayProperty;
		TestFour testEntity         = null;
		
		try {
			testEntity = createTestNode(TestFour.class);
			
		} catch (FrameworkException fex) {
			
			fail("Unable to create test node.");
		}
		
		assertNotNull(testEntity);
		
		
		// store a string array in the test entitiy
		String[] arr = new String[] { "one", "two", "three", "four", "five" };
		
		try {
			instance.setProperty(securityContext, testEntity, arr);
			
		} catch (FrameworkException fex) {
			
			fail("Unable to store array");
		}
		
		String[] newArr = instance.getProperty(securityContext, testEntity, true);
		
		assertTrue(ArrayUtils.isEquals(arr, newArr));
	}
}
