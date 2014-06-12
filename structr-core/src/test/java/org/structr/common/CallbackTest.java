/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.TestEight;
import org.structr.core.entity.TestFive;
import org.structr.core.entity.TestUser;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;

/**
 *
 * @author Christian Morgner
 */
public class CallbackTest extends StructrTest {
	
	public void testCallbacksWithSuperUserContext() {
		
		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		try {
			testCallbacks(securityContext);
		} catch (FrameworkException fex) {
			fail("Unexpected exception");
		}
	}
	
	public void testCallbacksWithNormalContext() {
		
		try {
			
			TestUser person = this.createTestNode(TestUser.class);
			
			final SecurityContext securityContext = SecurityContext.getInstance(person, null, AccessMode.Backend);
			testCallbacks(securityContext);
			
		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
		}
	}
	
	public void testCallbackOrder() {
		
		try {

// ##################################### test creation callbacks
			
			TestEight test = null;
			
			try (final Tx tx = app.tx()) {

				test = app.create(TestEight.class, new NodeAttribute(TestEight.testProperty, 123));
				tx.success();
			}
			
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
			
			try (final Tx tx = app.tx()) {
				test.setProperty(TestEight.testProperty, 234);
				tx.success();
			}

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
			
			try (final Tx tx = app.tx()) {
				test.setProperty(TestEight.testProperty, 234);
				tx.success();
			}

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
			
			try (final Tx tx = app.tx()) {
				app.delete(test);
				tx.success();
			}

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
	
	private void testCallbacks(final SecurityContext securityContext) throws FrameworkException {
		
		TestFive entity = null;
		Integer zero = 0;
		Integer one  = 1;
		
		try (final Tx tx = app.tx()) {

			entity = app.create(TestFive.class);
			tx.success();
			
		} catch (Throwable t) {
			
			t.printStackTrace();
		}

		assertNotNull("Entity should have been created", entity);
		
		// creation assertions
		try (final Tx tx = app.tx()) {
			
			assertEquals("modifiedInBeforeCreation should have a value of 1: ", one, entity.getProperty(TestFive.modifiedInBeforeCreation));
			assertEquals("modifiedInAfterCreation should have a value of 1:  ", one, entity.getProperty(TestFive.modifiedInAfterCreation));

			// modification assertions
			assertEquals("modifiedInBeforeModification should have a value of 0: ", zero, entity.getProperty(TestFive.modifiedInBeforeModification));
			assertEquals("modifiedInAfterModification should have a value of 0:  ", zero, entity.getProperty(TestFive.modifiedInAfterModification));
		}
		
		
		// 2nd part of the test: modify node
		try (final Tx tx = app.tx()) {

			final TestFive finalEntity = entity;

			finalEntity.setProperty(TestFive.intProperty, 123);
			tx.success();
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
		
		try (final Tx tx = app.tx()) {

			// creation assertions
			assertEquals("modifiedInBeforeCreation should have a value of 1: ", one, entity.getProperty(TestFive.modifiedInBeforeCreation));
			assertEquals("modifiedInAfterCreation should have a value of 1:  ", one, entity.getProperty(TestFive.modifiedInAfterCreation));

			// modification assertions
			assertEquals("modifiedInBeforeModification should have a value of 1: ", one, entity.getProperty(TestFive.modifiedInBeforeModification));
			assertEquals("modifiedInAfterModification should have a value of 1:  ", one, entity.getProperty(TestFive.modifiedInAfterModification));
		}
	}
}
