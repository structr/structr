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
import org.structr.common.AccessMode;
import org.structr.core.Services;
import org.structr.core.auth.exception.UnauthorizedException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Person;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import static org.structr.web.auth.HttpAuthenticator.checkSessionAuthentication;

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
	public SecurityContext initializeAndExamineRequest(HttpServletRequest request, HttpServletResponse response) throws FrameworkException {

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
		
		examined = true;
		return securityContext;
		
	}

	@Override
	public void checkResourceAccess(HttpServletRequest request, String rawResourceSignature, String propertyView)
		throws FrameworkException {
		
		ResourceAccess resourceAccess = ResourceAccess.findGrant(rawResourceSignature);
		
		Method method       = methods.get(request.getMethod());

		Principal user = getUser(request, true);
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
	public Principal doLogin(HttpServletRequest request,String emailOrUsername, String password) throws AuthenticationException {

		Principal user = AuthHelper.getPrincipalForPassword(Person.email, emailOrUsername, password);

		if (user != null) {

			final String sessionIdFromRequest = request.getRequestedSessionId();
			
			// Websocket connects don't have a session
			if (sessionIdFromRequest != null) {
			
				final Principal principal         = user;

				try {
					
					Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction() {

						@Override
						public Object execute() throws FrameworkException {
							// store session id in user object
							principal.setProperty(Principal.sessionId, sessionIdFromRequest);
							return null;
						}
					});

				} catch (Exception ex) {

					logger.log(Level.SEVERE, null, ex);

				}
			}

		}

		return user;

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Principal getUser(HttpServletRequest request, final boolean tryLogin) throws FrameworkException {

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
