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

import static junit.framework.Assert.assertTrue;
import org.apache.commons.lang.ArrayUtils;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.TestFour;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;

/**
 *
 * @author Christian Morgner
 */
public class ArrayPropertyTest extends StructrTest {
	
	public void testArrayProperty() {
		
		
		try {
			final Property<String[]> instance = TestFour.stringArrayProperty;
			final TestFour testEntity         = createTestNode(TestFour.class);
			
			assertNotNull(testEntity);

			// store a string array in the test entitiy
			final String[] arr = new String[] { "one", "two", "three", "four", "five" };

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {
					
					instance.setProperty(securityContext, testEntity, arr);
					return null;
				}
				
			});
			
			String[] newArr = instance.getProperty(securityContext, testEntity, true);

			assertTrue(ArrayUtils.isEquals(arr, newArr));
			
		} catch (FrameworkException fex) {
			
			fail("Unable to store array");
		}
	}
}
