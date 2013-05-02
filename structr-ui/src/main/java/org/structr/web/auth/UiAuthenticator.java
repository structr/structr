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


package org.structr.web.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.AuthHelper;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.ResourceAccess;

//~--- JDK imports ------------------------------------------------------------

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.structr.core.auth.exception.UnauthorizedException;
import org.structr.core.entity.SuperUser;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class UiAuthenticator extends HttpAuthenticator {
	
	private enum Method { GET, PUT, POST, DELETE }
	private static final Map<String, Method> methods = new LinkedHashMap<String, Method>();

	private static final Logger logger       = Logger.getLogger(HttpAuthenticator.class.getName());

	// HTTP methods
	static {

		methods.put("GET", Method.GET);
		methods.put("PUT", Method.PUT);
		methods.put("POST", Method.POST);
		methods.put("DELETE", Method.DELETE);

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
	
	@Override
	public void initializeAndExamineRequest(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response) throws FrameworkException {

		getUser(securityContext, request, response, true);

	}

	@Override
	public void examineRequest(SecurityContext securityContext, HttpServletRequest request, String rawResourceSignature, ResourceAccess resourceAccess, String propertyView)
		throws FrameworkException {
		
		Method method       = methods.get(request.getMethod());

		Principal user = securityContext.getUser(true);
		boolean validUser = (user != null);
		
		// super user is always authenticated
		if (validUser && user instanceof SuperUser) {
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

			}
		}

		throw new UnauthorizedException("Forbidden");

	}
	
	@Override
	public Principal doLogin(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response, String userName, String password) throws AuthenticationException {

		Principal user = AuthHelper.getUserForUsernameAndPassword(SecurityContext.getSuperUserInstance(), userName, password);

		if (user != null) {

			HttpSession session = request.getSession(false);
			
			// Websocket connects don't have a session
			if (session != null) {
			
				String sessionIdFromRequest = session.getId();

				try {
					// store session id in user object
					user.setProperty(Principal.sessionId, sessionIdFromRequest);

					//request.login(userName, password);
					//request.authenticate(response);

				} catch (Exception ex) {

					logger.log(Level.SEVERE, null, ex);

				}
			}

			securityContext.setUser(user);

		}

		return user;

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Principal getUser(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response, final boolean tryLogin) throws FrameworkException {

		// First, check session (JSESSIONID cookie)
		Principal user = checkSessionAuthentication(request, response);
		
		if (user != null) {
			
			securityContext.setUser(user);
			return user;
		}
		
		// Second, check X-Headers
		String userName = request.getHeader("X-User");
		String password = request.getHeader("X-Password");
		String token    = request.getHeader("X-StructrSessionToken");

		// Try to authorize with a session token first
		if (token != null) {

			user = AuthHelper.getUserForSessionId(token);
			
		} else if ((userName != null) && (password != null)) {

			if (tryLogin) {
				
				user = AuthHelper.getUserForUsernameAndPassword(SecurityContext.getSuperUserInstance(request, response), userName, password);
				
			}
		}

		if (user != null) {

			securityContext.setUser(user);

		}
		
		return user;

	}

}
