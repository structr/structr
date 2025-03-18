/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.rest.auth;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.session.Session;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.rest.service.HttpService;

import java.time.Instant;
import java.util.Date;

/**
 * Utility class for session handling
 *
 *
 */
public class SessionHelper {

	public static final String SESSION_IS_NEW     = "SESSION_IS_NEW";

	private static final Logger logger = LoggerFactory.getLogger(SessionHelper.class.getName());

	public static boolean isSessionTimedOut(final HttpSession session) {

		if (session == null) {
			return true;
		}

		final long now = (new Date()).getTime();

		try {

			final long lastAccessed  = session.getLastAccessedTime();
			final int sessionTimeout = Services.getGlobalSessionTimeout();

			if (sessionTimeout > 0 && now > lastAccessed + sessionTimeout * 1000) {

				logger.debug("Session {} timed out, last accessed at {}", new Object[]{session.getId(), Instant.ofEpochMilli(lastAccessed).toString()});
				return true;
			}

			return false;

		} catch (IllegalStateException ise) {

			return true;
		}
	}

	public static HttpSession getSessionBySessionId(final String sessionId) throws FrameworkException {

		return getSessionFromCache(sessionId);
	}

	public static void newSession(final HttpServletRequest request) {

		if (request.getSession(true) == null) {

			if (request instanceof JettyServerUpgradeRequest) {
				logger.debug("Requested to create a new session on a Websocket request, aborting");
				return;
			}

			request.changeSessionId();
		}

		final HttpSession session = request.getSession(false);
		if (session != null) {

			logger.debug("Created new session " + session.getId());

		} else {

			logger.warn("Request still has no valid session");
		}
	}

	/**
	 * Make sure the given sessionId is not set for any user.
	 *
	 * @param sessionId
	 */
	public static void clearSession(final String sessionId) {

		if (StringUtils.isBlank(sessionId)) {
			return;
		}

		final App app                            = StructrApp.getInstance();
		final PropertyKey<String[]> sessionIdKey = Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.SESSION_IDS_PROPERTY);
		final Query<NodeInterface> query         = app.nodeQuery(StructrTraits.PRINCIPAL).and(sessionIdKey, new String[]{sessionId}).disableSorting();

		try {

			for (final NodeInterface p : query.getAsList()) {

				p.as(Principal.class).removeSessionId(sessionId);
			}

		} catch (Exception fex) {

			logger.warn("Error while removing sessionId " + sessionId + " from all principals", fex);
		}

		// also remove short session id if the current session id is the long session id
		final String shortSessionId = getShortSessionId(sessionId);
		if (!sessionId.equals(shortSessionId)) {

			clearSession(shortSessionId);
		}
	}

	/**
	 * Remove old sessionIds of the given user.
	 *
	 * @param user
	 */
	public static void clearInvalidSessions(final Principal user) {

		logger.info("Clearing invalid sessions for user {} ({})", user.getName(), user.getUuid());

		final String[] sessionIds = user.getSessionIds();

		if (sessionIds != null && sessionIds.length > 0) {

			final SessionCache sessionCache = getDefaultSessionCache();

			for (final String sessionId : sessionIds) {

				Session session = getSessionFromCache(sessionCache, sessionId);

				if (session == null || SessionHelper.isSessionTimedOut(session)) {
					SessionHelper.clearSession(sessionId);
					SessionHelper.invalidateSession(sessionId);
				}
			}
		}
	}

	/**
	 * Remove all sessionIds of the given user.
	 *
	 * @param user
	 */
	public static void clearAllSessions(final Principal user) {

		if (!user.getTraits().contains(StructrTraits.GROUP)) {

			logger.info("Clearing all sessions for user {} ({})", user.getName(), user.getUuid());

			final String[] sessionIds = user.getSessionIds();

			if (sessionIds != null && sessionIds.length > 0) {

				for (final String sessionId : sessionIds) {

					SessionHelper.clearSession(sessionId);
					SessionHelper.invalidateSession(sessionId);
				}
			}
		}
	}

	/**
	 * Remove all sessionIds for all users.
	 *
	 */
	public static void clearAllSessions() {

		logger.info("Clearing all session ids for all users");

		try (final Tx tx = StructrApp.getInstance().tx(false, false, false)) {

			for (final NodeInterface user : StructrApp.getInstance().nodeQuery(StructrTraits.PRINCIPAL).getAsList()) {
				clearAllSessions(user.as(Principal.class));
			}

			tx.success();

		} catch (final FrameworkException ex) {

			logger.warn("Removing all session ids failed: {}", ex);
		}
	}

	public static void invalidateSession(final String sessionId) {

		if (sessionId != null) {

			try {

				getDefaultSessionCache().delete(sessionId);

			} catch (final Exception ex) {

				logger.warn("Invalidating session failed: {}", sessionId);
			}
		}
	}

	public static Principal checkSessionAuthentication(final HttpServletRequest request) throws FrameworkException {

		String requestedSessionId = request.getRequestedSessionId();
		String sessionId          = null;

		logger.debug("0. Requested session id: " + requestedSessionId + ", request says is valid? " + request.isRequestedSessionIdValid());

		//HttpSession session       = request.getSession(false);
		boolean isNotTimedOut      = false;

		if (requestedSessionId == null) {

			logger.debug("1b. Empty requested session id, creating a new one.");

			// No session id requested => create new session
			SessionHelper.newSession(request);

			// Store info in request that session is new => saves us a lookup later
			request.setAttribute(SESSION_IS_NEW, true);

			// we just created a totally new session, there can't
			// be a user with this session ID, so don't search.
			return null;

		} else {

			requestedSessionId = getShortSessionId(requestedSessionId);

			// Existing session id, check if we have an existing session
			if (request.getSession(false) != null) {

				logger.debug("1a. Requested session id without worker id suffix: " + requestedSessionId);

				sessionId = request.getSession(false).getId();
				logger.debug("2a. Current session id: " + sessionId);

				if (sessionId.equals(requestedSessionId)) {

					logger.debug("3a. Current session id equals requested session id");

				} else {

					logger.debug("3b. Current session id does not equal requested session id.");
				}

			} else {

				logger.debug("2b. Current session is null.");

				// Try to find session in session cache
				if (getSessionBySessionId(requestedSessionId) == null) {

					// Not found, create new
					SessionHelper.newSession(request);
					logger.debug("3a. Created new session");

					// remove session ID without session
					SessionHelper.clearSession(requestedSessionId);
					logger.debug("4. Cleared unknown session " + requestedSessionId);

					// we just created a totally new session, there can't
					// be a user with this session ID, so don't search.
					return null;

				} else  {
					logger.debug("3b. Session with requested id " + requestedSessionId + " found, continuing.");

					sessionId = requestedSessionId;

				}
			}

			if (SessionHelper.isSessionTimedOut(request.getSession(false))) {

				isNotTimedOut = false;

				// invalidate session
				SessionHelper.invalidateSession(sessionId);

				// remove invalid session ID
				SessionHelper.clearSession(sessionId);

				logger.debug("4a. Cleared timed-out session " + sessionId);

				SessionHelper.newSession(request);
				// we just created a totally new session, there can't
				// be a user with this session ID, so don't search.
				return null;

			} else {

				logger.debug("4b. Session " + sessionId + " is not timed-out.");

				isNotTimedOut = true;
			}
		}

		final Principal user = AuthHelper.getPrincipalForSessionId(sessionId);

		if (isNotTimedOut) {

			//logger.debug("Valid session found: {}, last accessed {}, authenticated with user {}", new Object[]{session, session.getLastAccessedTime(), user});
			return user;

		} else {

			if (user != null) {

				//logger.info("Timed-out session: {}, last accessed {}, authenticated with user {}", new Object[]{session, (session != null ? session.getLastAccessedTime() : ""), user});
				logger.debug("Logging out user {}", new Object[]{user});
				AuthHelper.doLogout(request, user);
				try { request.logout(); } catch (Throwable t) {}
			}

			SessionHelper.newSession(request);

			return null;
		}
	}

	public static String getShortSessionId(final String sessionId) {
		return StringUtils.substringBeforeLast(sessionId, ".");
	}

	private static SessionCache getDefaultSessionCache() {
		return Services.getInstance().getService(HttpService.class, "default").getSessionCache();
	}

	private static Session getSessionFromCache(final String sessionId) {

		return getSessionFromCache(getDefaultSessionCache(), sessionId);
	}

	private static Session getSessionFromCache(final SessionCache sessionCache, final String sessionId) {

		try {

			return sessionCache.get(sessionId);

		} catch (final Exception ex) {
			logger.debug("Unable to retrieve session " + sessionId + " from session cache:", ex);
		}

		return null;
	}
}