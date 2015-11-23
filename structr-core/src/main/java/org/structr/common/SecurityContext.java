/**
 * Copyright (C) 2010-2015 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.helpers.collection.LruMap;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.auth.Authenticator;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaHelper;

/**
 * Encapsulates the current user and access path and provides methods to query
 * permission flags for a given node. This is the place where HttpServletRequest
 * and Authenticator get together.
 *
 *
 */
public class SecurityContext {

	public static final String LOCALE_KEY = "locale";

	private static final Logger logger                   = Logger.getLogger(SecurityContext.class.getName());
	private static final Map<String, Long> resourceFlags = new LruMap<>(10000);
	private static final Pattern customViewPattern       = Pattern.compile(".*properties=([a-zA-Z_,]+)");
	private boolean doTransactionNotifications           = true;
	private boolean dontModifyAccessTime                 = false;

	//~--- fields ---------------------------------------------------------
	private final Map<String, QueryRange> ranges = new ConcurrentHashMap<>();
	private final Map<String, Object> attrs      = Collections.synchronizedMap(new LinkedHashMap<String, Object>());
	private AccessMode accessMode                = AccessMode.Frontend;
	private Authenticator authenticator          = null;
	private Principal cachedUser                 = null;
	private HttpServletRequest request           = null;
	private HttpServletResponse response         = null;
	private Set<String> customView               = null;
	private String cachedUserName                = null;
	private String cachedUserId                  = null;

	//~--- constructors ---------------------------------------------------
	private SecurityContext() {
	}

	/*
	 * Alternative constructor for stateful context, e.g. WebSocket
	 */
	private SecurityContext(Principal user, AccessMode accessMode) {

		this.cachedUser     = user;
		this.accessMode     = accessMode;
	}

	/*
	 * Alternative constructor for stateful context, e.g. WebSocket
	 */
	private SecurityContext(Principal user, HttpServletRequest request, AccessMode accessMode) {

		this(request);

		this.cachedUser = user;
		this.accessMode = accessMode;
	}

	private SecurityContext(HttpServletRequest request) {

		this.request = request;

		initializeCustomView(request);
		initializeQueryRanges(request);
		initializeHttpParameters(request);
	}

	private void initializeHttpParameters(final HttpServletRequest request) {

		if (request != null) {

			try {

				if ("disabled".equals(request.getHeader("Structr-Websocket-Broadcast"))) {
					this.doTransactionNotifications = false;
				}

			} catch (Throwable t) {}

		}
	}

	private void initializeCustomView(final HttpServletRequest request) {

		// check for custom view attributes
		if (request != null) {

			try {
				final String acceptedContentType = request.getHeader("Accept");
				if (acceptedContentType != null && acceptedContentType.startsWith("application/json")) {

					final Matcher matcher = customViewPattern.matcher(acceptedContentType);
					if (matcher.matches()) {

						customView = new LinkedHashSet<>();

						final String properties = matcher.group(1);
						final String[] parts = properties.split("[,]+");
						for (final String part : parts) {

							final String p = part.trim();
							if (p.length() > 0) {

								customView.add(p);
							}
						}
					}
				}

			} catch (Throwable ignore) {
			}
		}

	}

	private void initializeQueryRanges(final HttpServletRequest request) {

		if (request != null) {

			final String rangeSource = request.getHeader("Range");
			if (rangeSource != null) {

				final String[] rangeParts = rangeSource.split("[;]+");
				final int rangeCount      = rangeParts.length;

				for (int i = 0; i < rangeCount; i++) {

					final String[] parts = rangeParts[i].split("[=]+");
					if (parts.length == 2) {

						final String identifier = parts[0].trim();
						final String valueRange = parts[1].trim();

						if (StringUtils.isNotBlank(identifier) && StringUtils.isNotBlank(valueRange)) {

							if (valueRange.contains(",")) {

								logger.log(Level.WARNING, "Unsupported Range header specification {0}, multiple ranges are not supported.", valueRange);

							} else {

								final String[] valueParts = valueRange.split("[-]+");
								if (valueParts.length == 2) {

									String startString = valueParts[0].trim();
									String endString = valueParts[1].trim();

									// remove optional total size indicator
									if (endString.contains("/")) {
										endString = endString.substring(0, endString.indexOf("/"));
									}

									try {

										final int start    = Integer.parseInt(startString);
										final int end      = Integer.parseInt(endString);

										ranges.put(identifier, new QueryRange(start, end));

									} catch (Throwable t) {

										t.printStackTrace();
									}
								}
							}

						}
					}
				}
			}
		}
	}

	public static void clearResourceFlag(final String resource, long flag) {

		final String name     = SchemaHelper.normalizeEntityName(resource);
		final Long flagObject = resourceFlags.get(name);
		long flags            = 0;

		if (flagObject != null) {

			flags = flagObject;
		}

		flags &= ~flag;

		resourceFlags.put(name, flags);

	}

	public void removeForbiddenNodes(List<? extends GraphObject> nodes, final boolean includeDeletedAndHidden, final boolean publicOnly) {

		boolean readableByUser = false;

		for (Iterator<? extends GraphObject> it = nodes.iterator(); it.hasNext();) {

			GraphObject obj = it.next();

			if (obj instanceof AbstractNode) {

				AbstractNode n = (AbstractNode) obj;

				readableByUser = n.isGranted(Permission.read, this);

				if (!(readableByUser && (includeDeletedAndHidden || !n.isDeleted()) && (n.isVisibleToPublicUsers() || !publicOnly))) {

					it.remove();
				}

			}

		}

	}

	public static SecurityContext getSuperUserInstance(HttpServletRequest request) {
		return new SuperUserSecurityContext(request);
	}

	public static SecurityContext getSuperUserInstance() {
		return new SuperUserSecurityContext();

	}

	public static SecurityContext getInstance(Principal user, AccessMode accessMode) {
		return new SecurityContext(user, accessMode);

	}

	public static SecurityContext getInstance(Principal user, HttpServletRequest request, AccessMode accessMode) {
		return new SecurityContext(user, request, accessMode);

	}

	public HttpSession getSession() {

		if (request != null) {

			final HttpSession session = request.getSession(false);
			if (session != null) {

				session.setMaxInactiveInterval(Services.getGlobalSessionTimeout());
			}

			return session;

		}

		return null;

	}

	public HttpServletRequest getRequest() {

		return request;

	}

	public HttpServletResponse getResponse() {

		return response;

	}

	public String getCachedUserId() {
		return cachedUserId;
	}

	public String getCachedUserName() {
		return cachedUserName;
	}

	public Principal getCachedUser() {
		return cachedUser;
	}

	public Principal getUser(final boolean tryLogin) {

		// If we've got a user, return it! Easiest and fastest!!
		if (cachedUser != null) {

			// update caches, we can safely assume a transaction context here
			if (cachedUserId == null) {
				this.cachedUserId   = cachedUser.getUuid();
			}

			// update caches, we can safely assume a transaction context here
			if (cachedUserName == null) {
				this.cachedUserName = cachedUser.getName();
			}

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
			if (cachedUser != null) {

				cachedUserId   = cachedUser.getUuid();
				cachedUserName = cachedUser.getName();
			}


		} catch (Throwable t) {

			logger.log(Level.WARNING, "No user found");

		}

		return cachedUser;

	}

	public AccessMode getAccessMode() {

		return accessMode;

	}

	public StringBuilder getBaseURI() {

		final StringBuilder uriBuilder = new StringBuilder(200);

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

		final String name     = SchemaHelper.normalizeEntityName(resource);
		final Long flagObject = resourceFlags.get(name);
		long flags            = 0;

		if (flagObject != null) {

			flags = flagObject;
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

		return ((user != null) && (user instanceof SuperUser || user.getProperty(Principal.isAdmin)));

	}

	public boolean isVisible(AccessControllable node) {

		switch (accessMode) {

			case Backend:
				return isVisibleInBackend(node);

			case Frontend:
				return isVisibleInFrontend(node);

			default:
				return false;

		}

	}

	public boolean isReadable(final NodeInterface node, final boolean includeDeletedAndHidden, final boolean publicOnly) {

		/**
		 * The if-clauses in the following lines have been split for
		 * performance reasons.
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

		return node.isGranted(Permission.read, this);
	}

	// ----- private methods -----
	private boolean isVisibleInBackend(AccessControllable node) {

		if (isVisibleInFrontend(node)) {

			return true;

		}

		// no node, nothing to see here..
		if (node == null) {

			return false;
		}

		// fetch user
		final Principal user = getUser(false);

		// anonymous users may not see any nodes in backend
		if (user == null) {

			return false;
		}

		// SuperUser may always see the node
		if (user instanceof SuperUser) {

			return true;
		}

		return node.isGranted(Permission.read, this);
	}

	/**
	 * Indicates whether the given node is visible for a frontend request.
	 * This method should be used to explicetely check visibility of the
	 * requested root element, like e.g. a page, a partial or a file/image
	 * to download.
	 *
	 * It should *not* be used to check accessibility of child nodes because
	 * it might send a 401 along with a request for basic authentication.
	 *
	 * For those, use
	 * {@link SecurityContext#isReadable(org.structr.core.entity.AbstractNode, boolean, boolean)}
	 *
	 * @param node
	 * @return isVisible
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
		final Principal user = getUser(false);

		if (user != null) {

			final Principal owner = node.getOwnerNode();

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

		return node.isGranted(Permission.read, this);

	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}

	public static void setResourceFlag(final String resource, long flag) {

		final String name     = SchemaHelper.normalizeEntityName(resource);
		final Long flagObject = resourceFlags.get(name);
		long flags            = 0;

		if (flagObject != null) {

			flags = flagObject;
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

	public void clearCustomView() {
		customView = new LinkedHashSet<>();
	}

	public void setCustomView(final String... properties) {

		customView = new LinkedHashSet<>();

		for (final String prop : properties) {
			customView.add(prop);
		}

	}

	public Authenticator getAuthenticator() {
		return authenticator;
	}

	public void setAuthenticator(final Authenticator authenticator) {
		this.authenticator = authenticator;
	}

	public boolean hasCustomView() {
		return customView != null && !customView.isEmpty();
	}

	public Set<String> getCustomView() {
		return customView;
	}

	public QueryRange getRange(final String key) {
		return ranges.get(key);
	}

	/**
	 * Determine the effective locale for this request.
	 *
	 * Priority 1: URL parameter "locale" Priority 2: User locale  3: Browser locale  4: Default locale
	 *
	 * @return locale
	 */
	public Locale getEffectiveLocale() {

		Locale locale = Locale.getDefault();
		boolean userHasLocaleString = false;

		if (cachedUser != null) {

			final String userLocaleString = cachedUser.getProperty(Principal.locale);

			if (userLocaleString != null) {
				userHasLocaleString = true;

				try {
					locale = LocaleUtils.toLocale(userLocaleString);
				} catch (IllegalArgumentException e) {
					locale = Locale.forLanguageTag(userLocaleString);
				}
			}

		}

		if (request != null) {

			if (!userHasLocaleString) {
				locale = request.getLocale();
			}

			// Overwrite locale if requested by URL parameter
			String requestedLocaleString = request.getParameter(LOCALE_KEY);
			if (StringUtils.isNotBlank(requestedLocaleString)) {
				try {
					locale = LocaleUtils.toLocale(requestedLocaleString);
				} catch (IllegalArgumentException e) {
					locale = Locale.forLanguageTag(requestedLocaleString);
				}
			}

		}

		return locale;
	}

	public boolean isDoTransactionNotifications() {
		return doTransactionNotifications;
	}

	public void setDoTransactionNotifications(boolean doTransactionNotifications) {
		this.doTransactionNotifications = doTransactionNotifications;
	}

	public boolean dontModifyAccessTime() {
		return dontModifyAccessTime;
	}

	public void preventModificationOfAccessTime() {
		dontModifyAccessTime = true;
	}

	// ----- nested classes -----
	private static class SuperUserSecurityContext extends SecurityContext {

		private static final SuperUser superUser = new SuperUser();

		public SuperUserSecurityContext(HttpServletRequest request) {
			super(request);
		}

		public SuperUserSecurityContext() {
		}

		//~--- get methods --------------------------------------------

		@Override
		public Principal getUser(final boolean tryLogin) {

			return new SuperUser();

		}

		@Override
		public Principal getCachedUser() {
			return superUser;
		}

		@Override
		public String getCachedUserId() {
			return "00000000000000000000000000000000";
		}

		@Override
		public String getCachedUserName() {
			return "superadmin";
		}

		@Override
		public AccessMode getAccessMode() {

			return (AccessMode.Backend);

		}

		@Override
		public boolean isReadable(final NodeInterface node, final boolean includeDeletedAndHidden, final boolean publicOnly) {

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
