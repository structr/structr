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
package org.structr.core.auth;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.AbstractUser;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.Actions;

//~--- classes ----------------------------------------------------------------

/**
 * Utility class for authentication
 *
 *
 */
public class AuthHelper {

	public static final String STANDARD_ERROR_MSG = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
	private static final Logger logger             = Logger.getLogger(AuthHelper.class.getName());

	//~--- get methods ----------------------------------------------------

	/**
	 * Find a {@link Principal} for the given credential
	 *
	 * @param key
	 * @param value
	 * @return principal
	 */
	public static <T> Principal getPrincipalForCredential(final PropertyKey<T> key, final T value) {

		if (value == null) {
			return null;
		}

		final App app = StructrApp.getInstance();



		final Query<Principal> query = app.nodeQuery(Principal.class).and(key, value);

		try {
			return query.getFirst();

		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Error while searching for principal", fex);
		}

		return null;
	}

	/**
	 * Find a {@link Principal} with matching password and given key or name
	 *
	 * @param key
	 * @param value
	 * @param password
	 * @return principal
	 * @throws AuthenticationException
	 */
	public static Principal getPrincipalForPassword(final PropertyKey<String> key, final String value, final String password) throws AuthenticationException {

		String errorMsg = null;
		Principal principal  = null;

		// FIXME: this might be slow, because the the property file needs to be read each time
		final String superuserName = StructrApp.getConfigurationValue(Services.SUPERUSER_USERNAME);
		final String superUserPwd  = StructrApp.getConfigurationValue(Services.SUPERUSER_PASSWORD);

		if (superuserName.equals(value) && superUserPwd.equals(password)) {

			// logger.log(Level.INFO, "############# Authenticated as superadmin! ############");

			principal = new SuperUser();

		} else {

			try {

				principal = StructrApp.getInstance().nodeQuery(Principal.class).and().or(key, value).or(AbstractUser.name, value).getFirst();

				if (principal == null) {

					logger.log(Level.INFO, "No principal found for {0} {1}", new Object[]{ key.dbName(), value });

					errorMsg = STANDARD_ERROR_MSG;

				} else {

					if (principal.getProperty(Principal.blocked)) {

						logger.log(Level.INFO, "Principal {0} is blocked", principal);

						errorMsg = STANDARD_ERROR_MSG;

					}

					if (StringUtils.isEmpty(password)) {

						logger.log(Level.INFO, "Empty password for principal {0}", principal);

						errorMsg = "Empty password, should never happen here!";

					} else {

						// let Principal decide how to check password
						if (!principal.isValidPassword(password)) {

							errorMsg = STANDARD_ERROR_MSG;
						}

					}

				}

			} catch (FrameworkException fex) {

				fex.printStackTrace();

			}

		}

		if (errorMsg != null) {

			throw new AuthenticationException(errorMsg);
		}

		return principal;

	}

	/**
	 * Find a {@link Principal} for the given session id
	 *
	 * @param sessionId
	 * @return principal
	 */
	public static Principal getPrincipalForSessionId(final String sessionId) {
		return getPrincipalForCredential(Principal.sessionIds, new String[]{ sessionId });
	}

	/**
	 * Calculate a SHA-512 hash of the given password string.
	 *
	 * If salt is given, use salt.
	 *
	 * @param password
	 * @param salt
	 * @return hash
	 */
	public static String getHash(final String password, final String salt) {

		if (StringUtils.isEmpty(salt)) {

			return getSimpleHash(password);

		}

		return DigestUtils.sha512Hex(DigestUtils.sha512Hex(password).concat(salt));

	}

	/**
	 * Calculate a SHA-512 hash without salt
	 *
	 * @param password
	 * @return simple hash
	 * @deprecated Use
	 * {@link AuthHelper#getHash(java.lang.String, java.lang.String)}
	 * instead
	 */
	@Deprecated
	public static String getSimpleHash(final String password) {

		return DigestUtils.sha512Hex(password);

	}

	public static void doLogin(final HttpServletRequest request, final Principal user) throws FrameworkException {

		HttpSession session = request.getSession(false);

		if (session == null) {
			session = newSession(request);
		}

		// We need a session to login a user
		if (session != null) {

			AuthHelper.clearSession(session.getId());
			user.addSessionId(session.getId());

			Actions.call(Actions.NOTIFICATION_LOGIN, user);
		}
	}

	public static void doLogout(final HttpServletRequest request, final Principal user) throws FrameworkException {

		final HttpSession session = request.getSession(false);

		// We need a session to logout a user
		if (session != null) {

			AuthHelper.clearSession(session.getId());

			Actions.call(Actions.NOTIFICATION_LOGOUT, user);

			try { request.logout(); request.changeSessionId(); } catch (Throwable t) {}

		}
	}

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

	public static Principal checkSessionAuthentication(final HttpServletRequest request) throws FrameworkException {

		String requestedSessionId = request.getRequestedSessionId();
		HttpSession session       = request.getSession(false);
		boolean sessionValid      = false;

		if (requestedSessionId == null) {

			// No session id requested => create new session
			AuthHelper.newSession(request);

			// we just created a totally new session, there can't
			// be a user with this session ID, so don't search.
			return null;

		} else {

			// Existing session id, check if we have an existing session
			if (session != null) {

				if (session.getId().equals(requestedSessionId)) {

					if (AuthHelper.isSessionTimedOut(session)) {

						sessionValid = false;

						// remove invalid session ID
						AuthHelper.clearSession(requestedSessionId);

					} else {

						sessionValid = true;
					}

				}

			} else {

				// No existing session, create new
				session = AuthHelper.newSession(request);

				// remove session ID without session
				AuthHelper.clearSession(requestedSessionId);

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
