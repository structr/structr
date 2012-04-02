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

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.AuthenticatorCommand;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

//~--- classes ----------------------------------------------------------------

/**
 * Encapsulates the current user and access path and provides methods
 * to query permission flags for a given node. This is the place where
 * HttpServletRequest and Authenticator get together.
 *
 *
 * @author Christian Morgner
 */
public class SecurityContext {

	private static final Logger logger = Logger.getLogger(SecurityContext.class.getName());

	//~--- fields ---------------------------------------------------------

	private AccessMode accessMode       = AccessMode.Frontend;
	private Map<String, Object> attrs   = null;
	private Authenticator authenticator = null;
	private HttpServletRequest request  = null;
	private User user                   = null;

	//~--- constructors ---------------------------------------------------

	private SecurityContext(ServletConfig config, HttpServletRequest request, AccessMode accessMode) {

		this.attrs      = Collections.synchronizedMap(new LinkedHashMap<String, Object>());
		this.accessMode = accessMode;
		this.request    = request;

		// the authenticator does not have a security context
		try {
			this.authenticator = (Authenticator) Services.command(null, AuthenticatorCommand.class).execute(config);
		} catch (Throwable t) {
			logger.log(Level.SEVERE, "Could not instantiate security context!");
		}
	}
	
	/*
	 * Alternative constructor for stateful context, e.g. WebSocket
	 */
	private SecurityContext(User user, AccessMode accessMode) {

		this.user = user;
		this.accessMode = accessMode;
	}
	
	//~--- methods --------------------------------------------------------

	public void examineRequest(HttpServletRequest request) throws FrameworkException {
		this.authenticator.examineRequest(this, request);
	}

	public User doLogin(String userName, String password) throws AuthenticationException {
		return authenticator.doLogin(this, request, userName, password);
	}

	public void doLogout() {
		authenticator.doLogout(this, request);
	}

	//~--- get methods ----------------------------------------------------

	public static SecurityContext getSuperUserInstance() {
		return new SuperUserSecurityContext();
	}

	public static SecurityContext getInstance(ServletConfig config, HttpServletRequest request, AccessMode accessMode) throws FrameworkException {
		return new SecurityContext(config, request, accessMode);
	}
	
	public static SecurityContext getInstance(User user, AccessMode accessMode) throws FrameworkException {
		return new SecurityContext(user, accessMode);
	}
	
	public HttpSession getSession() {
		return request.getSession();
	}

	public User getUser() {

		try {

			if (user == null) {

				user = authenticator.getUser(this, request);

			}

		} catch (FrameworkException ex) {
			logger.log(Level.WARNING, "No user found", ex);
		}

		return user;
	}

	public String getUserName() {

		user = getUser();

		if (user != null) {

			return user.getName();

		}

		return null;
	}

	public AccessMode getAccessMode() {
		return (accessMode);
	}

	public StringBuilder getBaseURI() {

		StringBuilder uriBuilder = new StringBuilder(200);

		uriBuilder.append(request.getScheme());
		uriBuilder.append("://");
		uriBuilder.append(request.getServerName());
		uriBuilder.append(":");
		uriBuilder.append(request.getServerPort());
		uriBuilder.append(request.getContextPath());
		uriBuilder.append(request.getServletPath());
		uriBuilder.append("/");

		return uriBuilder;
	}

	public Object getAttribute(String key) {
		return attrs.get(key);
	}

	public boolean isSuperUser() {

		user = getUser();

		return ((user != null) && (user instanceof SuperUser));
	}

	public boolean isAllowed(AccessControllable node, Permission permission) {

		if (isSuperUser()) {

			return true;

		}

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

			user = getUser();

			logger.log(Level.FINEST, "Returning {0} for user {1}, access mode {2}, node {3}, permission {4}", new Object[] { isAllowed, (user != null)
				? user.getName()
				: "null", accessMode, node, permission });

		}

		return isAllowed;
	}

	public boolean isVisible(AccessControllable node) {

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

			user = getUser();

			logger.log(Level.FINEST, "Returning {0} for user {1}, access mode {2}, node {3}", new Object[] { ret, (user != null)
				? user.getName()
				: "null", accessMode, node });

		}

		return (ret);
	}

	// ----- private methods -----
	private boolean isVisibleInBackend(AccessControllable node) {

		// no node, nothing to see here..
		if (node == null) {

			return (false);

		}

		// fetch user
		user = getUser();

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
	private boolean isVisibleInFrontend(AccessControllable node) {

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
		if (node.isVisibleToPublicUsers()) {

			return visibleByTime;

		}

		// fetch user
		user = getUser();

		if (user != null) {

			// SuperUser
			if (user instanceof SuperUser) {

				return (true);

			}

			// frontend user
			if (user.isFrontendUser()) {

				return node.hasPermission(AbstractRelationship.Permission.read.name(), user);

			}
		}

		return (false);
	}

	private boolean isAllowedInBackend(AccessControllable node, Permission permission) {

		user = getUser();

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

				AbstractRelationship r = null;

				// owner has always access control
				if (user.equals(node.getOwnerNode())) {

					return true;

				}

				r = node.getSecurityRelationship(user);

				if ((r != null) && r.isAllowed(AbstractRelationship.Permission.accessControl.name())) {

					return true;

				}

				return false;

			case CreateNode :
				return node.hasPermission(AbstractRelationship.Permission.createNode.name(), user);

			case CreateRelationship :
				return node.hasPermission(AbstractRelationship.Permission.addRelationship.name(), user);

			case DeleteNode :
				return node.hasPermission(AbstractRelationship.Permission.deleteNode.name(), user);

			case DeleteRelationship :
				return node.hasPermission(AbstractRelationship.Permission.removeRelationship.name(), user);

			case EditProperty :
				return node.hasPermission(AbstractRelationship.Permission.editProperties.name(), user);

			case Execute :
				return node.hasPermission(AbstractRelationship.Permission.execute.name(), user);

			case Read :
				return node.hasPermission(AbstractRelationship.Permission.read.name(), user);

			case ShowTree :
				return node.hasPermission(AbstractRelationship.Permission.showTree.name(), user);

			case Write :
				return node.hasPermission(AbstractRelationship.Permission.write.name(), user);

		}

		return (false);
	}

	private boolean isAllowedInFrontend(AccessControllable node, Permission permission) {

		if (node == null) {

			return false;

		}

		user = getUser();

		if ((user != null) && (user instanceof SuperUser)) {

			return true;

		}

		switch (permission) {

			case Read :
				return isVisibleInFrontend(node);    // Read permission in frontend is equivalent to visibility

			// return node.hasPermission(AbstractRelationship.READ_KEY, user);
			default :
				return false;

		}
	}

	//~--- set methods ----------------------------------------------------

	public void setAttribute(String key, Object value) {
		attrs.put(key, value);
	}

	public void setAccessMode(AccessMode accessMode) {
		this.accessMode = accessMode;
	}

	//~--- inner classes --------------------------------------------------

	// ----- nested classes -----
	private static class SuperUserSecurityContext extends SecurityContext {

		public SuperUserSecurityContext() {
			super(null, null, null);
		}

		//~--- get methods --------------------------------------------

		@Override
		public HttpSession getSession() {
			throw new IllegalStateException("Trying to access session in SuperUserSecurityContext!");
		}

		@Override
		public User getUser() {
			return new SuperUser();
		}

		@Override
		public AccessMode getAccessMode() {
			return (AccessMode.Backend);
		}

		@Override
		public boolean isAllowed(AccessControllable node, Permission permission) {
			return true;
		}

		@Override
		public boolean isVisible(AccessControllable node) {
			return true;
		}

		@Override
		public boolean isSuperUser() {
			return true;
		}
	}
}
