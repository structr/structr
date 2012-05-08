/*
 *  Copyright (C) 2011 Axel Morgner
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

import java.util.Date;
import junit.framework.TestCase;
import org.structr.core.entity.Principal;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class SecurityContextTest extends TestCase {

	private SecurityContext frontendUserSecurityContext = null;

	public SecurityContextTest(String testName) {
		super(testName);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// initialize security contexts
		this.frontendUserSecurityContext = SecurityContext.getSuperUserInstance();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Test of isAllowed method, of class SecurityContext.
	 */
	public void testIsAllowed() {
	}

	/**
	 * Test of isVisible method, of class SecurityContext.
	 */
	public void testIsVisible() {

		/*
		ALLOWED_KEY;
		DENIED_KEY;
		READ_KEY;
		SHOW_TREE_KEY;
		WRITE_KEY;
		EXECUTE_KEY;
		CREATE_SUBNODE_KEY;
		DELETE_NODE_KEY;
		EDIT_PROPERTIES_KEY;
		ADD_RELATIONSHIP_KEY;
		REMOVE_RELATIONSHIP_KEY;
		ACCESS_CONTROL_KEY;
		*/


		AccessControllable publicNode =		new MockAccessControllable( true, false,  true, AbstractRelationship.Permission.read.name());
		AccessControllable privateNode =	new MockAccessControllable(false,  true,  true, AbstractRelationship.Permission.read.name());
		AccessControllable publicHiddenNode =	new MockAccessControllable( true,  true,  true, AbstractRelationship.Permission.read.name());


		assertTrue(frontendUserSecurityContext.isVisible(publicNode));

//		assertFalse(frontendUserSecurityContext.isVisible(privateNode));

//		assertFalse(frontendUserSecurityContext.isVisible(publicHiddenNode));
	}

	/**
	 * Test of setAccessMode method, of class SecurityContext.
	 */
	public void testSetAccessMode() {
	}

	/**
	 * Test of getAccessMode method, of class SecurityContext.
	 */
	public void testGetAccessMode() {
	}

	// ----- private methods -----
	private static class MockAccessControllable implements AccessControllable {

		private boolean visibleToAuthenticatedUsersFlag = false;
		private String[] permissions = null;
		private boolean publicFlag = false;
		private boolean hiddenFlag = false;

		public MockAccessControllable(boolean publicFlag, boolean hiddenFlag, boolean visibleToAuthenticatedUsersFlag, String... permissions) {

			this.publicFlag = publicFlag;
			this.hiddenFlag = hiddenFlag;
			this.visibleToAuthenticatedUsersFlag = visibleToAuthenticatedUsersFlag;
			this.permissions = permissions;
		}

		@Override
		public User getOwnerNode() {
			return(null);
		}

		@Override
		public boolean hasPermission(String permission, Principal principal) {

			if(principal == null) {
				return(false);
			}

			if (principal instanceof SuperUser) {
				return true;
			}

			// check preset permissions
			for(String presetPermission : permissions) {
				if(presetPermission.equals(permission)) {
					return(true);
				}
			}

			return(false);
		}

		@Override
		public AbstractRelationship getSecurityRelationship(Principal principal) {
			return(null);
		}

		@Override
		public boolean isVisibleToPublicUsers() {
			return(publicFlag);
		}

		@Override
		public boolean isVisibleToAuthenticatedUsers() {
			return(visibleToAuthenticatedUsersFlag);
		}

		@Override
		public boolean isNotHidden() {
			return(!hiddenFlag);
		}

		@Override
		public boolean isHidden() {
			return(hiddenFlag);

		}

		@Override
		public Date getVisibilityStartDate() {
			return(null);
		}

		@Override
		public Date getVisibilityEndDate() {
			return(null);
		}

		@Override
		public Date getCreatedDate() {
			return(new Date(0));
		}

		@Override
		public Date getLastModifiedDate() {
			return(null);
		}
	}
}
