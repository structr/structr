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
package org.structr.common;

import org.structr.common.error.FrameworkException;
import org.structr.common.property.PropertyMap;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.Person;
import org.structr.core.entity.TestFive;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;

/**
 *
 * @author Christian Morgner
 */
public class CallbackTest extends StructrTest {
	
	public void testCallbacksWithSuperUserContext() {
		
		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		testCallbacks(securityContext);
	}
	
	public void testCallbacksWithNormalContext() {
		
		try {
			
			Person person = this.createTestNode(Person.class);
			
			final SecurityContext securityContext = SecurityContext.getInstance(person, AccessMode.Backend);
			testCallbacks(securityContext);
			
		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
		}
	}
	
	private void testCallbacks(final SecurityContext securityContext) {
		
		final PropertyMap properties = new PropertyMap();
		
		TestFive entity = null;
		Integer zero = 0;
		Integer one  = 1;
		
		// set type of new entity
		properties.put(GraphObject.type, TestFive.class.getSimpleName());

		try {

			entity = Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<TestFive>() {

				@Override
				public TestFive execute() throws FrameworkException {

					return (TestFive)Services.command(securityContext, CreateNodeCommand.class).execute(properties);
				}


			});
			
			
		} catch (Throwable t) {
			
			t.printStackTrace();
		}

		assertNotNull("Entity should have been created", entity);
		
		// creation assertions
		assertEquals("modifiedInBeforeCreation should have a value of 1: ", one, entity.getProperty(TestFive.modifiedInBeforeCreation));
		assertEquals("modifiedInAfterCreation should have a value of 1:  ", one, entity.getProperty(TestFive.modifiedInAfterCreation));

		// modification assertions
		assertEquals("modifiedInBeforeModification should have a value of 0: ", zero, entity.getProperty(TestFive.modifiedInBeforeModification));
		assertEquals("modifiedInAfterModification should have a value of 0:  ", zero, entity.getProperty(TestFive.modifiedInAfterModification));
		
		
		
		// 2nd part of the test: modify node
		try {
			entity.setProperty(TestFive.intProperty, 123);
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		
		// creation assertions
		assertEquals("modifiedInBeforeCreation should have a value of 1: ", one, entity.getProperty(TestFive.modifiedInBeforeCreation));
		assertEquals("modifiedInAfterCreation should have a value of 1:  ", one, entity.getProperty(TestFive.modifiedInAfterCreation));

		// modification assertions
		assertEquals("modifiedInBeforeModification should have a value of 1: ", one, entity.getProperty(TestFive.modifiedInBeforeModification));
		assertEquals("modifiedInAfterModification should have a value of 1:  ", one, entity.getProperty(TestFive.modifiedInAfterModification));
		
	}
}
