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
package org.structr.web.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.AuthHelper;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.auth.exception.UnauthorizedException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Person;
import org.structr.core.entity.Principal;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SuperUser;
import static org.structr.web.auth.HttpAuthenticator.checkSessionAuthentication;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class UiAuthenticator extends HttpAuthenticator {

	private enum Method { GET, PUT, POST, DELETE, OPTIONS }
	private static final Map<String, Method> methods = new LinkedHashMap();

	private static final Logger logger       = Logger.getLogger(HttpAuthenticator.class.getName());

	// HTTP methods
	static {

		methods.put("GET", Method.GET);
		methods.put("PUT", Method.PUT);
		methods.put("POST", Method.POST);
		methods.put("DELETE", Method.DELETE);
		methods.put("OPTIONS", Method.OPTIONS);

	}

	// access flags
	public static final long FORBIDDEN		= 0;
	public static final long AUTH_USER_GET		= 1;
	public static final long AUTH_USER_PUT		= 2;
	public static final long AUTH_USER_POST		= 4;
	public static final long AUTH_USER_DELETE	= 8;

	public static final long NON_AUTH_USER_GET	= 16;
	public static final long NON_AUTH_USER_PUT	= 32;
	public static final long NON_AUTH_USER_POST	= 64;
	public static final long NON_AUTH_USER_DELETE	= 128;

	public static final long AUTH_USER_OPTIONS	= 256;
	public static final long NON_AUTH_USER_OPTIONS	= 512;

	/**
	 * Examine request and try to find a user.
	 *
	 * First, check session id, then try external (OAuth) authentication,
	 * finally, check standard login by credentials.
	 *
	 * @param request
	 * @param response
	 * @return
	 * @throws FrameworkException
	 */
	@Override
	public SecurityContext initializeAndExamineRequest(final HttpServletRequest request, final HttpServletResponse response) throws FrameworkException {

		SecurityContext securityContext;

		Principal user = checkSessionAuthentication(request);

		if (user == null) {

			user = checkExternalAuthentication(request, response);

		}

		if (user == null) {

			user = getUser(request, true);

		}

		if (user == null) {

			// If no user could be determined, assume frontend access
			securityContext = SecurityContext.getInstance(user, request, AccessMode.Frontend);

		} else {


			if (user instanceof SuperUser) {

				securityContext = SecurityContext.getSuperUserInstance(request);

			} else {

				securityContext = SecurityContext.getInstance(user, request, AccessMode.Backend);

			}

		}

		securityContext.setAuthenticator(this);

		// test for cross site resource sharing
		String origin = request.getHeader("Origin");
		if (!StringUtils.isBlank(origin)) {

			 // allow cross site resource sharing (read only)
			response.setHeader("Access-Control-Allow-Origin", origin);
			response.setHeader("Access-Control-Allow-Methods", "GET,PUT,POST");
			response.setHeader("Access-Control-Allow-Headers", "Content-Type");

		 }

		examined = true;
		return securityContext;

	}

	@Override
	public void checkResourceAccess(final HttpServletRequest request, final String rawResourceSignature, final String propertyView)
		throws FrameworkException {

		ResourceAccess resourceAccess = ResourceAccess.findGrant(rawResourceSignature);

		Method method       = methods.get(request.getMethod());

		Principal user = getUser(request, true);
		boolean validUser = (user != null);

		// super user is always authenticated
		if (validUser && (user instanceof SuperUser || user.getProperty(Principal.isAdmin))) {
			return;
		}

		// no grants => no access rights
		if (resourceAccess == null) {

			throw new UnauthorizedException("Forbidden");

		} else {

			switch (method) {

				case GET :

					if (!validUser && resourceAccess.hasFlag(NON_AUTH_USER_GET)) {

						return;

					}

					if (validUser && resourceAccess.hasFlag(AUTH_USER_GET)) {

						return;

					}

					break;

				case PUT :

					if (!validUser && resourceAccess.hasFlag(NON_AUTH_USER_PUT)) {

						return;

					}

					if (validUser && resourceAccess.hasFlag(AUTH_USER_PUT)) {

						return;

					}

					break;

				case POST :

					if (!validUser && resourceAccess.hasFlag(NON_AUTH_USER_POST)) {

						return;

					}

					if (validUser && resourceAccess.hasFlag(AUTH_USER_POST)) {

						return;

					}

					break;

				case DELETE :

					if (!validUser && resourceAccess.hasFlag(NON_AUTH_USER_DELETE)) {

						return;

					}

					if (validUser && resourceAccess.hasFlag(AUTH_USER_DELETE)) {

						return;

					}

					break;

				case OPTIONS :

					if (!validUser && resourceAccess.hasFlag(NON_AUTH_USER_OPTIONS)) {

						return;

					}

					if (validUser && resourceAccess.hasFlag(AUTH_USER_OPTIONS)) {

						return;

					}

					break;
			}
		}

		throw new UnauthorizedException("Forbidden");

	}

	@Override
	public Principal doLogin(final HttpServletRequest request, final String emailOrUsername, final String password) throws AuthenticationException {

		Principal user = AuthHelper.getPrincipalForPassword(Person.eMail, emailOrUsername, password);

		if (user != null) {


			String sessionIdFromRequest = null;
			try {
				sessionIdFromRequest = request.getRequestedSessionId();
			} catch (UnsupportedOperationException uoe) {
				// ignore
			}

			// Websocket connects don't have a session
			if (sessionIdFromRequest != null) {

				AuthHelper.clearSession(sessionIdFromRequest);
				user.addSessionId(sessionIdFromRequest);
			}

		}

		return user;

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Principal getUser(final HttpServletRequest request, final boolean tryLogin) throws FrameworkException {

		// First, check session (JSESSIONID cookie)
		Principal user = checkSessionAuthentication(request);

		if (user != null) {

			return user;
		}

		// Second, check X-Headers
		String userName = request.getHeader("X-User");
		String password = request.getHeader("X-Password");
		String token    = request.getHeader("X-StructrSessionToken");

		// Try to authorize with a session token first
		if (token != null) {

			user = AuthHelper.getPrincipalForSessionId(token);

		} else if ((userName != null) && (password != null)) {

			if (tryLogin) {

				user = AuthHelper.getPrincipalForPassword(AbstractNode.name, userName, password);

			}
		}

		return user;

	}

}
