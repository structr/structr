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
package org.structr.rest.auth;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.rest.service.HttpService;

/**
 * Utility class for session handling
 *
 *
 */
public class SessionHelper {

	public static final String STANDARD_ERROR_MSG = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
	private static final Logger logger             = Logger.getLogger(SessionHelper.class.getName());


	public static boolean isSessionTimedOut(final HttpSession session) {

		if (session == null) {
			return true;
		}

		final long now = (new Date()).getTime();

		try {

			final long lastAccessed = session.getLastAccessedTime();

			if (now > lastAccessed + Services.getGlobalSessionTimeout() * 1000) {

				logger.log(Level.INFO, "Session {0} timed out, last accessed at {1}", new Object[]{ session, lastAccessed});
				return true;
			}

			return false;

		} catch (IllegalStateException ise) {

			return true;
		}

	}

	public static HttpSession newSession(final HttpServletRequest request) {

		HttpSession session = request.getSession(true);
		if (session == null) {

			// try again
			session = request.getSession(true);

		}

		if (session != null) {

			session.setMaxInactiveInterval(Services.getGlobalSessionTimeout());

		} else {

			logger.log(Level.SEVERE, "Unable to create new session after two attempts");

		}

		return session;

	}

	/**
	 * Make sure the given sessionId is not set for any user.
	 *
	 * @param sessionId
	 */
	public static void clearSession(final String sessionId) {

		final App app = StructrApp.getInstance();
		final Query<Principal> query = app.nodeQuery(Principal.class).and(Principal.sessionIds, new String[]{ sessionId });

		try {
			List<Principal> principals = query.getAsList();

			for (Principal p : principals) {

				p.removeSessionId(sessionId);

			}

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Error while removing sessionId " + sessionId + " from all principals", fex);
			fex.printStackTrace();
		}

	}

	/**
	 * Remove old sessionIds of the given user
	 *
	 * @param user
	 */
	public static void clearOldSessions(final Principal user) {

		final HashSessionManager sessionManager = Services.getInstance().getService(HttpService.class).getHashSessionManager();

		final String[] sessionIds = user.getProperty(Principal.sessionIds);

		if (sessionIds != null && sessionIds.length > 0) {

			for (String sessionId : user.getProperty(Principal.sessionIds)) {
				HttpSession session = sessionManager.getSession(sessionId);

				if (session == null || SessionHelper.isSessionTimedOut(session)) {
					SessionHelper.clearSession(sessionId);
				}

			}

		}

	}

	public static Principal checkSessionAuthentication(final HttpServletRequest request) throws FrameworkException {

		String requestedSessionId = request.getRequestedSessionId();
		HttpSession session       = request.getSession(false);
		boolean sessionValid      = false;

		if (requestedSessionId == null) {

			// No session id requested => create new session
			SessionHelper.newSession(request);

			// we just created a totally new session, there can't
			// be a user with this session ID, so don't search.
			return null;

		} else {

			// Existing session id, check if we have an existing session
			if (session != null) {

				if (session.getId().equals(requestedSessionId)) {

					if (SessionHelper.isSessionTimedOut(session)) {

						sessionValid = false;

						// remove invalid session ID
						SessionHelper.clearSession(requestedSessionId);

					} else {

						sessionValid = true;
					}

				}

			} else {

				// No existing session, create new
				session = SessionHelper.newSession(request);

				// remove session ID without session
				SessionHelper.clearSession(requestedSessionId);

			}

		}

		if (sessionValid) {

			final Principal user = AuthHelper.getPrincipalForSessionId(session.getId());
			logger.log(Level.FINE, "Valid session found: {0}, last accessed {1}, authenticated with user {2}", new Object[] { session, session.getLastAccessedTime(), user });

			return user;


		} else {

			final Principal user = AuthHelper.getPrincipalForSessionId(requestedSessionId);

			logger.log(Level.FINE, "Invalid session: {0}, last accessed {1}, authenticated with user {2}", new Object[] { session, (session != null ? session.getLastAccessedTime() : ""), user });

			if (user != null) {

				AuthHelper.doLogout(request, user);
			}

			try { request.logout(); request.changeSessionId(); } catch (Throwable t) {}

		}


		return null;

	}
}
