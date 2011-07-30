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

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;

//~--- classes ----------------------------------------------------------------

/**
 * Encapsulates the current user and access path and provides methods
 * to query permission flags for a given node.
 *
 *
 * @author Christian Morgner
 */
public class SecurityContext {

	private AccessMode accessMode = AccessMode.Frontend;

	//~--- constructors ---------------------------------------------------

	public SecurityContext() {}

	//~--- get methods ----------------------------------------------------

	private User getUser() {
		return CurrentSession.getUser();
	}

	public boolean isAllowed(AbstractNode node, Permission permission) {

		switch (accessMode) {

			case Backend :
				return isAllowedInBackend(node, permission);

			case Frontend :
				return isAllowedInFrontend(node, permission);
		}

		return (false);
	}

	public boolean isVisible(AbstractNode node) {

		if (node == null) {
			return (false);
		}

		User user = getUser();

		if (user instanceof SuperUser) {

			// Super user may always see it
			return true;
		}

		// check hidden flag (see STRUCTR-12)
		if (node.isHidden()) {
			return false;
		}

		boolean visibleByTime = false;

		// check visibility period of time (see STRUCTR-13)
		Date visStartDate       = node.getVisibilityStartDate();
		long effectiveStartDate = 0L;
		Date createdDate        = node.getCreatedDate();

		if (createdDate != null) {
			effectiveStartDate = Math.max(createdDate.getTime(), 0L);
		}

		// if no start date for visibility is given,
		// take the maximum of 0 and creation date.
		visStartDate = ((visStartDate == null)
				? new Date(effectiveStartDate)
				: visStartDate);

		// if no end date for visibility is given,
		// take the Long.MAX_VALUE
		Date visEndDate = node.getVisibilityEndDate();

		visEndDate = ((visEndDate == null)
			      ? new Date(Long.MAX_VALUE)
			      : visEndDate);

		Date now = new Date();

		visibleByTime = (now.after(visStartDate) && now.before(visEndDate));

		if (user == null) {

			// No logged-in user
			if (node.isPublic()) {
				return visibleByTime;
			} else {
				return false;
			}
		} else {

			// Logged-in users
			if (node.isVisibleToAuthenticatedUsers()) {
				return visibleByTime;
			} else {
				return false;
			}
		}
	}

	// ----- private methods -----
	private boolean isAllowedInBackend(AbstractNode node, Permission permission) {

		User user = getUser();

		if (node == null) {
			return false;
		}

		switch (permission) {

			case AccessControl :
				if (user == null) {
					return false;
				}

				// superuser
				if (user instanceof SuperUser) {
					return true;
				}

				// node itself
				if (node.equals(user)) {
					return true;
				}

				StructrRelationship r = null;

				// owner has always access control
				if (user.equals(node.getOwnerNode())) {
					return true;
				}

				r = node.getSecurityRelationship(user);

				if ((r != null) && r.isAllowed(StructrRelationship.ACCESS_CONTROL_KEY)) {
					return true;
				}

				return false;

			case CreateNode :
				return node.hasPermission(StructrRelationship.CREATE_SUBNODE_KEY, user);

			case CreateRelationship :
				return node.hasPermission(StructrRelationship.ADD_RELATIONSHIP_KEY, user);

			case DeleteNode :
				return node.hasPermission(StructrRelationship.DELETE_NODE_KEY, user);

			case DeleteRelationship :
				return node.hasPermission(StructrRelationship.REMOVE_RELATIONSHIP_KEY, user);

			case EditProperty :
				return node.hasPermission(StructrRelationship.EDIT_PROPERTIES_KEY, user);

			case Execute :
				return node.hasPermission(StructrRelationship.EXECUTE_KEY, user);

			case Read :
				return node.hasPermission(StructrRelationship.READ_KEY, user);

			case ShowTree :
				return node.hasPermission(StructrRelationship.SHOW_TREE_KEY, user);

			case Write :
				return node.hasPermission(StructrRelationship.WRITE_KEY, user);
		}

		return (false);
	}

	private boolean isAllowedInFrontend(AbstractNode node, Permission permission) {

		if (node == null) {
			return false;
		}

		return isAllowedInBackend(node, permission);
	}

	//~--- set methods ----------------------------------------------------

	public void setAccessMode(AccessMode accessMode) {
		this.accessMode = accessMode;
	}
}
