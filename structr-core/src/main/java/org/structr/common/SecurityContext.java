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

import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.auth.Authenticator;
import org.structr.core.entity.*;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.neo4j.graphdb.Node;

//~--- classes ----------------------------------------------------------------

/**
 * Encapsulates the current user and access path and provides methods
 * to query permission flags for a given node. This is the place where
 * HttpServletRequest and Authenticator get together.
 *
 * @author Christian Morgner
 */
public class SecurityContext {

	private static final Logger logger                   = Logger.getLogger(SecurityContext.class.getName());
	private static final Map<String, Long> resourceFlags = new LinkedHashMap<String, Long>();

	//~--- fields ---------------------------------------------------------

	private Map<Long, AbstractNode> cache = null;
	private AccessMode accessMode         = AccessMode.Frontend;
	private Map<String, Object> attrs     = Collections.synchronizedMap(new LinkedHashMap<String, Object>());
	private Authenticator authenticator   = null;
	private Principal cachedUser          = null;
	private HttpServletRequest request    = null;

	//~--- constructors ---------------------------------------------------

	private SecurityContext() {}

	/*
	 * Alternative constructor for stateful context, e.g. WebSocket
	 */
	private SecurityContext(Principal user, AccessMode accessMode) {

		this.cachedUser = user;
		this.accessMode = accessMode;

		cache = new ConcurrentHashMap<Long, AbstractNode>();
	}

	/*
	 * Alternative constructor for stateful context, e.g. WebSocket
	 */
	private SecurityContext(Principal user, HttpServletRequest request, AccessMode accessMode) {

		this.cachedUser = user;
		this.accessMode = accessMode;
		this.request    = request;

		initRequestBasedCache(request);
	}

	private SecurityContext(HttpServletRequest request) {

		this.request    = request;

		initRequestBasedCache(request);
	}

	//~--- methods --------------------------------------------------------

	private void initRequestBasedCache(HttpServletRequest request) {

		// request-based caching
		if (request != null && request.getServletContext() != null) {
			cache = (Map<Long, AbstractNode>)request.getServletContext().getAttribute("NODE_CACHE");
		}
		
		if (cache == null) {

			cache = new ConcurrentHashMap<Long, AbstractNode>();
			
			if (request != null && request.getServletContext() != null) {
				request.getServletContext().setAttribute("NODE_CACHE", cache);
			}
		}

	}
	
	/**
	 * Call this method after the request this context was
	 * created for is finished and the resources can be freed.
	 */
	public void cleanUp() {
		
		if (cache != null) {
			cache.clear();
		}
	}
	
	public AbstractNode lookup(Node node) {
		return cache.get(node.getId());
	}
	
	public void store(AbstractNode node) {
		
		Node dbNode = node.getNode();
		if (dbNode != null) {
			
			cache.put(dbNode.getId(), node);
		}
	}
	
	public static void clearResourceFlag(final String resource, long flag) {

		String name     = EntityContext.normalizeEntityName(resource);
		Long flagObject = resourceFlags.get(name);
		long flags      = 0;

		if (flagObject != null) {

			flags = flagObject.longValue();
		}

		flags &= ~flag;

		resourceFlags.put(name, flags);

	}

	public void removeForbiddenNodes(List<? extends GraphObject> nodes, final boolean includeDeletedAndHidden, final boolean publicOnly) {

		boolean readableByUser = false;

		for (Iterator<? extends GraphObject> it = nodes.iterator(); it.hasNext(); ) {

			GraphObject obj = it.next();

			if (obj instanceof AbstractNode) {

				AbstractNode n = (AbstractNode) obj;

				readableByUser = isAllowed(n, Permission.read);

				if (!(readableByUser && (includeDeletedAndHidden || !n.isDeleted()) && (n.isVisibleToPublicUsers() || !publicOnly))) {

					it.remove();
				}

			}

		}

	}

	//~--- get methods ----------------------------------------------------

	public static SecurityContext getSuperUserInstance(HttpServletRequest request) {
		return new SuperUserSecurityContext(request);
	}
	
	public static SecurityContext getSuperUserInstance() {
		return new SuperUserSecurityContext();

	}

	public static SecurityContext getInstance(Principal user, AccessMode accessMode) throws FrameworkException {
		return new SecurityContext(user, accessMode);

	}

	public static SecurityContext getInstance(Principal user, HttpServletRequest request, AccessMode accessMode) throws FrameworkException {
		return new SecurityContext(user, request, accessMode);

	}

	public HttpSession getSession() {

		return request.getSession();

	}

	public HttpServletRequest getRequest() {

		return request;

	}

	public Principal getUser(final boolean tryLogin) {

		// If we've got a user, return it! Easiest and fastest!!
		if (cachedUser != null) {
			
			return cachedUser;
			
		}
		
		if (authenticator == null) {
			
			return null;
			
		}
		
		if (authenticator.hasExaminedRequest()) {
			
			// If the authenticator has already examined the request,
			// we assume that we will not get new information.
			// Otherwise, the cachedUser would have been != null
			// and we would not land here.
			return null;
			
		}

		try {

			cachedUser = authenticator.getUser(request, tryLogin);

		} catch (Throwable t) {

			logger.log(Level.WARNING, "No user found");

		}

		return cachedUser;

	}

	public AccessMode getAccessMode() {

		return accessMode;

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

	public static long getResourceFlags(String resource) {

		String name     = EntityContext.normalizeEntityName(resource);
		Long flagObject = resourceFlags.get(name);
		long flags      = 0;

		if (flagObject != null) {

			flags = flagObject.longValue();
		} else {

			logger.log(Level.FINE, "No resource flag set for {0}", resource);
		}

		return flags;

	}

	public static boolean hasFlag(String resourceSignature, long flag) {

		return (getResourceFlags(resourceSignature) & flag) == flag;

	}

	public boolean isSuperUser() {

		Principal user = getUser(false);

		return ((user != null) && (user instanceof SuperUser));

	}

	public boolean isAllowed(AccessControllable node, Permission permission) {

		if (node == null) {

			return false;
		}

		if (isSuperUser()) {

			return true;
		}

		Principal user = getUser(false);

		if (user == null) {

			return false;
		}

		Principal owner = node.getOwnerNode();
		
		// owner is always allowed to do anything with its nodes
		if (user.equals(node) || user.equals(owner) || user.getParents().contains(owner)) {

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

		logger.log(Level.FINEST, "Returning {0} for user {1}, access mode {2}, node {3}, permission {4}", new Object[] { isAllowed, user.getProperty(AbstractNode.name), accessMode, node, permission });

		return isAllowed;

	}

	public boolean isVisible(AccessControllable node) {

		switch (accessMode) {

			case Backend :
				return isVisibleInBackend(node);

			case Frontend :
				return isVisibleInFrontend(node);

			default :
				return false;

		}

	}

	public boolean isReadable(final AbstractNode node, final boolean includeDeletedAndHidden, final boolean publicOnly) {

		/**
		 * The if-clauses in the following lines have been split
		 * for performance reasons.
		 */

		// deleted and hidden nodes will only be returned if we are told to do so
		if ((node.isDeleted() || node.isHidden()) && !includeDeletedAndHidden) {

			return false;
		}

		// visibleToPublic overrides anything else
		// Publicly visible nodes will always be returned
		if (node.isVisibleToPublicUsers()) {

			return true;
		}

		// Next check is only for non-public nodes, because
		// public nodes are already added one step above.
		if (publicOnly) {

			return false;
		}

		// Ask for user only if node is visible for authenticated users
		if (node.isVisibleToAuthenticatedUsers() && getUser(false) != null) {
			
			return true;
		}

		// Ask the security context
		if (isAllowed(node, Permission.read)) {

			return true;
		}

		return false;
	}
	
	// ----- private methods -----
	private boolean isVisibleInBackend(AccessControllable node) {

		// no node, nothing to see here..
		if (node == null) {

			return false;
		}

		// fetch user
		Principal user = getUser(false);

		// anonymous users may not see any nodes in backend
		if (user == null) {

			return false;
		}

		// SuperUser may always see the node
		if (user instanceof SuperUser) {

			return true;
		}

		// users with read permissions may see the node
		if (isAllowedInBackend(node, Permission.read)) {

			return true;
		}

		// no match, node is not visible
		return false;
	}

	/**
	 * Indicates whether the given node is visible for a frontend
	 * request. This method should be used to explicetely check
	 * visibility of the requested root element, like e.g. a page,
	 * a partial or a file/image to download.
	 * 
	 * It should *not* be used to check accessibility of child
	 * nodes because it might send a 401 along with a request for
	 * basic authentication.
	 * 
	 * For those, use {@link SecurityContext#isReadable(org.structr.core.entity.AbstractNode, boolean, boolean)}
	 *
	 * @param node
	 * @return
	 */
	private boolean isVisibleInFrontend(AccessControllable node) {

		if (node == null) {

			return false;
		}

		// check hidden flag
		if (node.isHidden()) {

			return false;
		}

		// Fetch already logged-in user, if present (don't try to login)
		Principal user = getUser(false);
		
		if (user != null) {

			Principal owner = node.getOwnerNode();

			// owner is always allowed to do anything with its nodes
			if (user.equals(node) || user.equals(owner) || user.getParents().contains(owner)) {

				return true;
			}
		
		}

		// Public nodes are visible to non-auth users only
		if (node.isVisibleToPublicUsers() && user == null) {

			return true;
		}

		// Ask for user only if node is visible for authenticated users
		if (node.isVisibleToAuthenticatedUsers()) {

			if (user != null) {

				return true;
			}
		}
		
		if (isAllowedInFrontend(node, Permission.read)) {

			return true;
		}

		return false;

	}

	private boolean isAllowedInBackend(AccessControllable node, Permission permission) {

		Principal user = getUser(false);

		return node.isGranted(permission, user);

	}

	private boolean isAllowedInFrontend(AccessControllable node, Permission permission) {

                Principal user = getUser(false);

		return node.isGranted(permission, user);
	}

	//~--- set methods ----------------------------------------------------

	public void setRequest(HttpServletRequest request) {

		this.request = request;

	}

	public static void setResourceFlag(final String resource, long flag) {

		String name     = EntityContext.normalizeEntityName(resource);
		Long flagObject = resourceFlags.get(name);
		long flags      = 0;

		if (flagObject != null) {

			flags = flagObject.longValue();
		}

		flags |= flag;

		resourceFlags.put(name, flags);

	}

	public void setAttribute(String key, Object value) {

		attrs.put(key, value);

	}

	public void setAccessMode(AccessMode accessMode) {

		this.accessMode = accessMode;

	}

	public Authenticator getAuthenticator() {
		return authenticator;
	}
	
	public void setAuthenticator(final Authenticator authenticator) {
		this.authenticator = authenticator;
	}
	
	//~--- inner classes --------------------------------------------------

	// ----- nested classes -----
	private static class SuperUserSecurityContext extends SecurityContext {
		
		public SuperUserSecurityContext(HttpServletRequest request) {
			super(request);
		}
		
		public SuperUserSecurityContext() {
		}

		//~--- get methods --------------------------------------------

		@Override
		public HttpSession getSession() {

			throw new IllegalStateException("Trying to access session in SuperUserSecurityContext!");

		}

		@Override
		public Principal getUser(final boolean tryLogin) {

			return new SuperUser();

		}

		@Override
		public AccessMode getAccessMode() {

			return (AccessMode.Backend);

		}

		@Override
		public boolean isReadable(final AbstractNode node, final boolean includeDeletedAndHidden, final boolean publicOnly) {
		
			return true;
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
	
		@Override
		public AbstractNode lookup(Node node) {
			return null;
		}
		
		@Override
		public void store(AbstractNode node) {
		}

		
	}

}