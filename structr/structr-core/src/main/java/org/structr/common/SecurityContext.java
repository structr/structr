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
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Encapsulates the current user and access path and provides methods
 * to query permission flags for a given node.
 *
 *
 * @author Christian Morgner
 */
public class SecurityContext {

	private static final Logger logger = Logger.getLogger(SecurityContext.class.getName());

	//~--- fields ---------------------------------------------------------

	private AccessMode accessMode = AccessMode.Frontend;

	//~--- constructors ---------------------------------------------------

	public SecurityContext() {}

	//~--- get methods ----------------------------------------------------

	private User getUser() {
		return CurrentSession.getUser();
	}

	public AccessMode getAccessMode() {
		return(accessMode);
	}

	public boolean isAllowed(AbstractNode node, Permission permission) {

		boolean isAllowed = false;

		switch (accessMode) {

			case Backend :
				isAllowed = isAllowedInBackend(node, permission);

				break;

			case Frontend :
				isAllowed = isAllowedInFrontend(node, permission);

				break;
		}

		if (node != null) {

			User user = getUser();

			logger.log(Level.FINEST, "Returning {0} for user {1}, access mode {2}, node {3}, permission {4}",
				   new Object[] { isAllowed, (user != null)
				? user.getName()
				: "null", accessMode, node.getId(), permission });
		}

		return isAllowed;
	}

	public boolean isVisible(AbstractNode node) {

		boolean ret = false;

		switch (accessMode) {

			case Backend :
				ret = isVisibleInBackend(node);

				break;

			case Frontend :
				ret = isVisibleInFrontend(node);

				break;
		}

		if (node != null) {

			User user = getUser();

			logger.log(Level.FINEST, "Returning {0} for user {1}, access mode {2}, node {3}",
				   new Object[] { ret, (user != null)
						       ? user.getName()
						       : "null", accessMode, node.getId() });
		}

		return (ret);
	}

	// ----- private methods -----
	private boolean isVisibleInBackend(AbstractNode node) {

		// no node, nothing to see here..
		if (node == null) {
			return (false);
		}

		// fetch user
		User user = getUser();

		// anonymous users may not see any nodes in backend
		if (user == null) {
			return (false);
		}

		// SuperUser may always see the node
		if (user instanceof SuperUser) {
			return true;
		}

		// non-backend users are not allowed here
		if (!user.isBackendUser()) {
			return (false);
		}

		// users with read permissions may see the node
		if (isAllowedInBackend(node, Permission.Read)) {
			return (true);
		}

		// no match, node is not visible
		return (false);
	}

	/**
	 * Indicates whether the given node is visible for a frontend
	 * request. This method ignores the user.
	 *
	 * @param node
	 * @return
	 */
	private boolean isVisibleInFrontend(AbstractNode node) {

		if (node == null) {
			return false;
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

		// public nodes are always visible (constrained by time)
		if(node.isPublic()) {

			return visibleByTime;
		}

		// fetch user
		User user = getUser();

		if (user != null) {

			// SuperUser
			if (user instanceof SuperUser) {
				return (true);
			}

			// frontend user
			if (user.isFrontendUser()) {

				return node.hasPermission(StructrRelationship.READ_KEY, user);
			}
		}

		return (false);
	}

	private boolean isAllowedInBackend(AbstractNode node, Permission permission) {

		User user = getUser();

		if (node == null) {
			return false;
		}

		// SuperUser has all permissions
		if ((user != null) && (user instanceof SuperUser)) {
			return true;
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

		User user = getUser();

		if ((user != null) && (user instanceof SuperUser)) {
			return true;
		}

		switch (permission) {

			case Read :
				return isVisibleInFrontend(node); // Read permission in frontend is equivalent to visibility
				//return node.hasPermission(StructrRelationship.READ_KEY, user);

			default :
				return false;
		}
	}

	//~--- set methods ----------------------------------------------------

	public void setAccessMode(AccessMode accessMode) {
		this.accessMode = accessMode;
	}
}
