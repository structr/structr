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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.AccessMode;
import org.structr.common.PathHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.AuthHelper;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.auth.exception.UnauthorizedException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Person;
import org.structr.core.entity.Principal;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SuperUser;
import org.structr.core.property.PropertyKey;
import org.structr.web.resource.RegistrationResource;
import org.structr.web.servlet.HtmlServlet;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class UiAuthenticator implements Authenticator {

	private static final Logger logger       = Logger.getLogger(UiAuthenticator.class.getName());

	protected boolean examined = false;
	protected static boolean userAutoCreate;
	protected static boolean userAutoLogin;
	protected static Class   userClass;

	private enum Method { GET, PUT, POST, DELETE, OPTIONS }
	private static final Map<String, Method> methods = new LinkedHashMap();

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

	//~--- methods --------------------------------------------------------

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
	public boolean hasExaminedRequest() {

		return examined;

	}

	@Override
	public void setUserAutoCreate(final boolean userAutoCreate, final Class userClass) {

		UiAuthenticator.userAutoCreate = userAutoCreate;
		UiAuthenticator.userClass      = userClass;
	}

	@Override
	public void setUserAutoLogin(boolean userAutoLogin, Class userClass) {

		UiAuthenticator.userAutoLogin = userAutoLogin;
		UiAuthenticator.userClass     = userClass;
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

		if  (user != null) {


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

	@Override
	public void doLogout(HttpServletRequest request) {

		try {
			Principal user = getUser(request, false);
			if (user != null) {

				final String sessionId = request.getRequestedSessionId();

				if (sessionId != null) {

					AuthHelper.clearSession(sessionId);
					user.removeSessionId(sessionId);

				}
			}

			HttpSession session = request.getSession(false);

			if (session != null) {
				session.invalidate();
			}

			request.logout();

		} catch (ServletException | FrameworkException ex) {

			logger.log(Level.WARNING, "Error while logging out user", ex);

		}
	}

	/**
	 * This method checks all configured external authentication services.
	 *
	 * @param request
	 * @param response
	 * @return
	 */
	protected static Principal checkExternalAuthentication(final HttpServletRequest request, final HttpServletResponse response) {

		String path = PathHelper.clean(request.getPathInfo());
		String[] uriParts = PathHelper.getParts(path);

		logger.log(Level.FINE, "Checking external authentication ...");

		if (uriParts == null || uriParts.length != 3 || !("oauth".equals(uriParts[0]))) {

			logger.log(Level.FINE, "Incorrect URI parts for OAuth process, need /oauth/<name>/<action>");
			return null;
		}

		String name   = uriParts[1];
		String action = uriParts[2];

		// Try to getValue an OAuth2 server for the given name
		StructrOAuthClient oauthServer = StructrOAuthClient.getServer(name);

		if (oauthServer == null) {

			logger.log(Level.FINE, "No OAuth2 authentication server configured for {0}", path);
			return null;

		}

		if ("login".equals(action)) {

			try {

				response.sendRedirect(oauthServer.getEndUserAuthorizationRequestUri(request));
				return null;

			} catch (Exception ex) {

				logger.log(Level.SEVERE, "Could not send redirect to authorization server", ex);
			}

		} else if ("auth".equals(action)) {

			String accessToken = oauthServer.getAccessToken(request);
			SecurityContext superUserContext = SecurityContext.getSuperUserInstance();

			if (accessToken != null) {

				logger.log(Level.FINE, "Got access token {0}", accessToken);
				//securityContext.setAttribute("OAuthAccessToken", accessToken);

				String value = oauthServer.getCredential(request);
				logger.log(Level.FINE, "Got credential value: {0}", new Object[] { value });

				if (value != null) {

					PropertyKey credentialKey = oauthServer.getCredentialKey();

					Principal user = AuthHelper.getPrincipalForCredential(credentialKey, value);

					if (user == null && userAutoCreate) {

						user = RegistrationResource.createUser(superUserContext, credentialKey, value, true, userClass);

					}

					if (user != null) {

						final String sessionId = UiAuthenticator.getSessionId(request);
						AuthHelper.clearSession(sessionId);
						user.addSessionId(sessionId);
						HtmlServlet.setNoCacheHeaders(response);
						try {

							logger.log(Level.FINE, "Response status: {0}", response.getStatus());

							response.sendRedirect(oauthServer.getReturnUri());

						} catch (IOException ex) {

							logger.log(Level.SEVERE, "Could not redirect to {0}: {1}", new Object[]{oauthServer.getReturnUri(), ex});

						}
						return user;
					}
				}
			}
		}

		try {

			response.sendRedirect(oauthServer.getErrorUri());

		} catch (IOException ex) {

			logger.log(Level.SEVERE, "Could not redirect to {0}: {1}", new Object[]{ oauthServer.getReturnUri(), ex });

		}

		return null;

	}

	protected static Principal checkSessionAuthentication(HttpServletRequest request) {

		String sessionIdFromRequest = request.getRequestedSessionId();

		if (sessionIdFromRequest == null) {

			// create session id
			request.getSession(true);
			return null;

		}

		Principal user = AuthHelper.getPrincipalForSessionId(sessionIdFromRequest);

		if (user != null) {

			return user;

		}

		return null;

	}

	public static void writeUnauthorized(HttpServletResponse response) throws IOException {

		response.setHeader("WWW-Authenticate", "BASIC realm=\"Restricted Access\"");
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

	}

	public static void writeNotFound(HttpServletResponse response) throws IOException {

		response.sendError(HttpServletResponse.SC_NOT_FOUND);

	}

	public static void writeInternalServerError(HttpServletResponse response) {

		try {

			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

		} catch (IOException ignore) {}

	}

	private static String getSessionId(final HttpServletRequest request) {

		String existingSessionId = request.getRequestedSessionId();

		if (existingSessionId == null) {

			HttpSession session = request.getSession(true);

			logger.log(Level.INFO, "Created new HTTP session: {0}", session.toString());

			return session.getId();

		}

		return existingSessionId;

	}

	@Override
	public boolean getUserAutoCreate() {
		return userAutoCreate;
	}

	@Override
	public boolean getUserAutoLogin() {
		return userAutoLogin;
	}

	@Override
	public Class getUserClass() {
		return userClass;
	}

	@Override
	public Principal getUser(final HttpServletRequest request, final boolean tryLogin) throws FrameworkException {

		// First, check session (JSESSIONID cookie)
		Principal user = checkSessionAuthentication(request);

		if (user == null) {

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
		}

		return user;

	}

}
