/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.auth.exception.UnauthorizedException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SuperUser;

//~--- classes ----------------------------------------------------------------

/**
 *
 *
 */
public class RestAuthenticator implements Authenticator {

	private static final Logger logger       = LoggerFactory.getLogger(RestAuthenticator.class.getName());

	protected boolean examined = false;
	protected static boolean userAutoCreate;
	protected static boolean userAutoLogin;
	protected static Class   userClass;

	private enum Method { GET, PUT, POST, DELETE, HEAD, OPTIONS }
	private static final Map<String, Method> methods = new LinkedHashMap();

	// HTTP methods
	static {

		methods.put("GET", Method.GET);
		methods.put("PUT", Method.PUT);
		methods.put("POST", Method.POST);
		methods.put("HEAD", Method.HEAD);
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

	public static final long AUTH_USER_HEAD		= 1024;
	public static final long NON_AUTH_USER_HEAD	= 2048;

	//~--- methods --------------------------------------------------------

	/**
	 * Examine request and try to find a user.
	 *
	 * First, check session id, then try external (OAuth) authentication,
	 * finally, check standard login by credentials.
	 *
	 * @param request
	 * @param response
	 * @return security context
	 * @throws FrameworkException
	 */
	@Override
	public SecurityContext initializeAndExamineRequest(final HttpServletRequest request, final HttpServletResponse response) throws FrameworkException {

		SecurityContext securityContext;

		Principal user = SessionHelper.checkSessionAuthentication(request);

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

				SessionHelper.clearInvalidSessions(user);

			}

		}

		securityContext.setAuthenticator(this);

		// Check CORS settings (Cross-origin resource sharing, see http://en.wikipedia.org/wiki/Cross-origin_resource_sharing)
		final String origin = request.getHeader("Origin");
		if (!StringUtils.isBlank(origin)) {

			final Services services = Services.getInstance();

			response.setHeader("Access-Control-Allow-Origin", origin);

			 // allow cross site resource sharing (read only)
			final String maxAge = services.getConfigurationValue(Services.ACCESS_CONTROL_MAX_AGE);
			if (StringUtils.isNotBlank(maxAge)) {
				response.setHeader("Access-Control-MaxAge", maxAge);
			}

			final String allowMethods = services.getConfigurationValue(Services.ACCESS_CONTROL_ALLOW_METHODS);
			if (StringUtils.isNotBlank(allowMethods)) {
				response.setHeader("Access-Control-Allow-Methods", allowMethods);
			}

			final String allowHeaders = services.getConfigurationValue(Services.ACCESS_CONTROL_ALLOW_HEADERS);
			if (StringUtils.isNotBlank(allowHeaders)) {
				response.setHeader("Access-Control-Allow-Headers", allowHeaders);
			}

			final String allowCredentials = services.getConfigurationValue(Services.ACCESS_CONTROL_ALLOW_CREDENTIALS);
			if (StringUtils.isNotBlank(allowCredentials)) {
				response.setHeader("Access-Control-Allow-Credentials", allowCredentials);
			}

			final String exposeHeaders = services.getConfigurationValue(Services.ACCESS_CONTROL_EXPOSE_HEADERS);
			if (StringUtils.isNotBlank(exposeHeaders)) {
				response.setHeader("Access-Control-Expose-Headers", exposeHeaders);
			}
		 }

		examined = true;
		return securityContext;

	}

	@Override
	public boolean hasExaminedRequest() {

		return examined;

	}

	@Override
	public void setUserAutoCreate(final boolean userAutoCreate) {

		RestAuthenticator.userAutoCreate = userAutoCreate;
	}

	@Override
	public void setUserAutoLogin(final boolean userAutoLogin) {

		RestAuthenticator.userAutoLogin = userAutoLogin;
	}

	@Override
	public void checkResourceAccess(final SecurityContext securityContext, final HttpServletRequest request, final String rawResourceSignature, final String propertyView)
		throws FrameworkException {

		final ResourceAccess resourceAccess = ResourceAccess.findGrant(securityContext, rawResourceSignature);
		final Method method                 = methods.get(request.getMethod());
		final Principal user                = getUser(request, true);
		final boolean validUser             = (user != null);

		// super user is always authenticated
		if (validUser && (user instanceof SuperUser || user.getProperty(Principal.isAdmin))) {
			return;
		}

		// no grants => no access rights
		if (resourceAccess == null) {

			logger.info("No resource access grant found for signature {}.", rawResourceSignature);

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

				case HEAD :

					if (!validUser && resourceAccess.hasFlag(NON_AUTH_USER_HEAD)) {

						return;

					}

					if (validUser && resourceAccess.hasFlag(AUTH_USER_HEAD)) {

						return;

					}

					break;
			}
		}

		logger.info("Resource access grant found for signature {}, but method {} not allowed for {}.", new Object[] { rawResourceSignature, method, validUser ? "authenticated users" : "public users" } );

		throw new UnauthorizedException("Forbidden");

	}

	@Override
	public Principal doLogin(final HttpServletRequest request, final String emailOrUsername, final String password) throws AuthenticationException, FrameworkException {

		final Principal user = AuthHelper.getPrincipalForPassword(Principal.eMail, emailOrUsername, password);

		SessionHelper.clearInvalidSessions(user);

		return user;
	}

	@Override
	public void doLogout(final HttpServletRequest request) {

		try {
			final Principal user = getUser(request, false);
			if (user != null) {

				AuthHelper.doLogout(request, user);
			}

			final HttpSession session = request.getSession(false);
			if (session != null) {

				session.invalidate();
			}

		} catch (IllegalStateException | FrameworkException ex) {

			logger.warn("Error while logging out user", ex);
		}
	}

	public static void writeUnauthorized(final HttpServletResponse response) throws IOException {

		response.setHeader("WWW-Authenticate", "BASIC realm=\"Restricted Access\"");
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

	}

	public static void writeNotFound(final HttpServletResponse response) throws IOException {

		response.sendError(HttpServletResponse.SC_NOT_FOUND);

	}

	public static void writeInternalServerError(final HttpServletResponse response) {

		try {

			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

		} catch (IOException ignore) {}

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

		Principal user = null;

		// First, check session (JSESSIONID cookie)
		final HttpSession session = request.getSession(false);

		if (session != null) {

			user = AuthHelper.getPrincipalForSessionId(session.getId());

		}

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
