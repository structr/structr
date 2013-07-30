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
package org.structr.common;

import java.util.logging.Level;
import java.util.logging.Logger;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyMap;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Person;
import org.structr.core.entity.TestEight;
import org.structr.core.entity.TestFive;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.DeleteNodeCommand;
import org.structr.core.graph.NodeAttribute;
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
			
			final SecurityContext securityContext = SecurityContext.getInstance(person, null, AccessMode.Backend);
			testCallbacks(securityContext);
			
		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
		}
	}
	
	public void testCallbackOrder() {
		
		try {

// ##################################### test creation callbacks
			
			final TestEight test = (TestEight)Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					return (TestEight)Services.command(securityContext, CreateNodeCommand.class).execute(
						new NodeAttribute(AbstractNode.type, TestEight.class.getSimpleName()),
						new NodeAttribute(TestEight.testProperty, 123)
					);
				}
				
			});
			
			// only the creation methods should have been called now!
			assertTrue("onCreationTimestamp should be != 0", test.getOnCreationTimestamp() != 0L);
			assertEquals("onModificationTimestamp should be == 0", 0L, test.getOnModificationTimestamp());
			assertEquals("onDeletionTimestamp should be == 0", 0L, test.getOnDeletionTimestamp());
			
			// only the creation methods should have been called now!
			assertTrue("afterCreationTimestamp should be != 0", test.getAfterCreationTimestamp() != 0L);
			assertEquals("afterModificationTimestamp should be == 0", 0L, test.getAfterModificationTimestamp());


// ##################################### test modification callbacks
			
			
			// reset timestamps
			test.resetTimestamps();
			
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					test.setProperty(TestEight.testProperty, 234);
					
					return null;
				}
				
			});
			
			// only the modification methods should have been called now!
			assertEquals("onCreationTimestamp should be == 0", 0L, test.getOnCreationTimestamp());
			assertTrue("onModificationTimestamp should be != 0", test.getOnModificationTimestamp() != 0L);
			assertEquals("onDeletionTimestamp should be == 0", 0L, test.getOnDeletionTimestamp());
			
			// only the modification methods should have been called now!
			assertEquals("afterCreationTimestamp should be == 0", 0L, test.getAfterCreationTimestamp());
			assertTrue("afterModificationTimestamp should be != 0", test.getAfterModificationTimestamp() != 0L);

			
// ##################################### test non-modifying set operation
			
			// reset timestamps
			test.resetTimestamps();
			
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					test.setProperty(TestEight.testProperty, 234);
					
					return null;
				}
				
			});
			
			// only the creation methods should have been called now!
			assertEquals("onCreationTimestamp should be == 0", 0L, test.getOnCreationTimestamp());
			assertEquals("onModificationTimestamp should be == 0", 0L, test.getOnModificationTimestamp());
			assertEquals("onDeletionTimestamp should be == 0", 0L, test.getOnDeletionTimestamp());
			
			// only the creation methods should have been called now!
			assertEquals("afterCreationTimestamp should be == 0", 0L, test.getAfterCreationTimestamp());
			assertEquals("afterModificationTimestamp should be == 0", 0L, test.getAfterModificationTimestamp());

			
			
// ##################################### test deletion
			
			// reset timestamps
			test.resetTimestamps();
			
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					Services.command(securityContext, DeleteNodeCommand.class).execute(test);
					
					return null;
				}
				
			});
			
			// only the creation methods should have been called now!
			assertEquals("onCreationTimestamp should be == 0", 0L, test.getOnCreationTimestamp());
			assertEquals("onModificationTimestamp should be == 0", 0L, test.getOnModificationTimestamp());
			assertTrue("onDeletionTimestamp should be != 0", test.getOnDeletionTimestamp() != 0L);
			
			// only the creation methods should have been called now!
			assertEquals("afterCreationTimestamp should be == 0", 0L, test.getAfterCreationTimestamp());
			assertEquals("afterModificationTimestamp should be == 0", 0L, test.getAfterModificationTimestamp());
			
			
			
		} catch (FrameworkException ex) {
			
			Logger.getLogger(CallbackTest.class.getName()).log(Level.SEVERE, null, ex);
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
			final TestFive finalEntity = entity;
			
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					finalEntity.setProperty(TestFive.intProperty, 123);
					
					return null;
				}
			});
			
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
