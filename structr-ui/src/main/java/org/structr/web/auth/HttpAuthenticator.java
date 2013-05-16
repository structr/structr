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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.AuthHelper;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.ResourceAccess;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;
import java.io.PrintWriter;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.structr.common.PathHelper;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.web.entity.User;
import org.structr.web.resource.RegistrationResource;
import org.structr.web.servlet.HtmlServlet;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class HttpAuthenticator implements Authenticator {

	private static final Logger logger       = Logger.getLogger(HttpAuthenticator.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeAndExamineRequest(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response) throws FrameworkException {
	
		Principal user = checkSessionAuthentication(request, response);
		
		if (user != null) {

			securityContext.setUser(user);
			return;

		}
		
		user = checkExternalAuthentication(securityContext, request, response);
		
		if (user != null) {
			
			securityContext.setUser(user);

		}
		
	
	}

	@Override
	public void examineRequest(SecurityContext securityContext, HttpServletRequest request, String resourceSignature, ResourceAccess resourceAccess, String propertyView)
		throws FrameworkException {
	
		logger.log(Level.FINE, "Got session? ", request.getSession(false));
		logger.log(Level.FINE, "User principal: ", request.getUserPrincipal());
	
	}

	@Override
	public Principal doLogin(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response, String userName, String password) throws AuthenticationException {

		Principal user = AuthHelper.getUserForUsernameAndPassword(SecurityContext.getSuperUserInstance(), userName, password);

		if (user != null) {

			securityContext.setUser(user);
			
			String sessionIdFromRequest = request.getRequestedSessionId();

			try {

				// store session id in user object
				user.setProperty(Principal.sessionId, sessionIdFromRequest);

			} catch (Exception ex) {

				logger.log(Level.SEVERE, null, ex);

			}

		}

		return user;

	}

	@Override
	public void doLogout(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response) {
	
		try {

			Principal user = securityContext.getUser(false);
			
			if (user != null) {
				
				user.setProperty(Principal.sessionId, null);

			}

			request.getSession(false).invalidate();
			request.logout();
			securityContext.setUser(null);

		} catch (Exception ex) {

			logger.log(Level.WARNING, "Error while logging out user", ex);

		}

	
	}

	protected static Principal checkExternalAuthentication(final SecurityContext securityContext, final HttpServletRequest request, final HttpServletResponse response) {
		
		String path = PathHelper.clean(request.getPathInfo());
		String[] uriParts = PathHelper.getParts(path);
		
		logger.log(Level.INFO, "Checking external authentication ...");
		
		if (uriParts == null || uriParts.length != 3 || !("oauth".equals(uriParts[0]))) {
			
			logger.log(Level.WARNING, "Incorrect URI parts for OAuth process, need /oauth/<name>/<action>");
			return null;
		}
		
		String name   = uriParts[1];
		String action = uriParts[2];
		
		// Try to get an OAuth2 server for the given name
		OAuth2Server oauthServer = OAuth2Server.getServer(name);
		
		if (oauthServer == null) {
			
			logger.log(Level.INFO, "No OAuth2 authentication server configured for {0}", path);
			return null;
			
		}
		
		if ("login".equals(action)) {
		
			try {

				response.sendRedirect(oauthServer.getEndUserAuthorizationRequest(request).getLocationUri());
				return null;

			} catch (Exception ex) {

				logger.log(Level.SEVERE, "Could not send redirect to authorization server", ex);
			}
			
		} else if ("auth".equals(action)) {
			
			String accessToken = oauthServer.getAccessToken(request);
			
			if (accessToken != null) {
				
				logger.log(Level.INFO, "Got access token {0}", accessToken);
				//securityContext.setAttribute("OAuthAccessToken", accessToken);
				
				String email = oauthServer.getEmail(request);
				logger.log(Level.INFO, "Got email: {0}", new Object[] { email });

				if (email != null) {

					Principal user = HttpAuthenticator.getUserForEmail(securityContext, email);

					if (user == null) {

						user = RegistrationResource.createUser(securityContext, email);

					}

					if (user != null) {

						try {

							user.setProperty(Principal.sessionId, HttpAuthenticator.getSessionId(request));
							securityContext.setUser(user);
							
							HtmlServlet.setNoCacheHeaders(response);
							
							try {
								
								logger.log(Level.INFO, "Response status: {0}", response.getStatus());
								
								response.sendRedirect(oauthServer.getReturnUri());
								
							} catch (Exception ex) {
								
								logger.log(Level.SEVERE, "Could not redirect to {0}: {1}", new Object[]{ oauthServer.getReturnUri(), ex });
								
							}

							return user;

						} catch (FrameworkException ex) {

							logger.log(Level.SEVERE, "Could not set session id for user {0}", user.toString());

						}

					}

				}
					
			}
			
		}

		try {

			response.sendRedirect(oauthServer.getErrorUri());

		} catch (Exception ex) {

			logger.log(Level.SEVERE, "Could not redirect to {0}: {1}", new Object[]{ oauthServer.getReturnUri(), ex });

		}
		
		return null;
		
	}
		
	protected static Principal checkSessionAuthentication(HttpServletRequest request, HttpServletResponse response) {

		String sessionIdFromRequest = request.getRequestedSessionId();
		
		if (sessionIdFromRequest == null) {
			
			return null;
			
		}

		Principal user = AuthHelper.getUserForSessionId(sessionIdFromRequest);

		if (user != null) {

			return user;

		}

		return null;

	}

	private Principal checkBasicAuthentication(HttpServletRequest request, HttpServletResponse response) {

		Principal user;
		String auth = request.getHeader("Authorization");
		
		try {
			if (auth == null) {

				sendBasicAuthResponse(response);

				return null;

			}

			if (!auth.toUpperCase().startsWith("BASIC ")) {

				sendBasicAuthResponse(response);

				return null;

			}

			String[] userAndPass = getUsernameAndPassword(request);

			try {

				if ((userAndPass == null) || (userAndPass.length != 2)) {

					writeUnauthorized(response);
				}

				user = AuthHelper.getUserForUsernameAndPassword(SecurityContext.getSuperUserInstance(), userAndPass[0], userAndPass[1]);

			} catch (Exception ex) {

				sendBasicAuthResponse(response);

				return null;

			}

			return user;
			
		} catch (IllegalStateException ise) {
			
			logger.log(Level.WARNING, "Error while sending basic auth response, stream might be already closed, sending anyway.");
			
		}
		
		return null;

	}

	public void sendBasicAuthResponse(HttpServletResponse response) {

		try {

			writeUnauthorized(response);

		} catch (IOException ex) {

			//logger.log(Level.SEVERE, null, ex);
			writeInternalServerError(response);

		}

	}

	public static void writeUnauthorized(HttpServletResponse response) throws IOException {

		response.setHeader("WWW-Authenticate", "BASIC realm=\"Restricted Access\"");
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED);

	}

	public static void writeContent(String content, HttpServletResponse response) throws IOException {

		try {
			
			response.setStatus(HttpServletResponse.SC_OK);
			response.setCharacterEncoding("UTF-8");

			PrintWriter writer = response.getWriter();
			writer.append(content);
			writer.flush();
			writer.close();

		} catch (IllegalStateException ise) {
			
			logger.log(Level.WARNING, "Could not write to output stream", ise.getMessage());
			
		}

	}

	public static void writeNotFound(HttpServletResponse response) throws IOException {

		response.sendError(HttpServletResponse.SC_NOT_FOUND);

	}

	public static void writeInternalServerError(HttpServletResponse response) {

		try {

			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

		} catch (Exception ignore) {}

	}

	//~--- get methods ----------------------------------------------------

	public static String[] getUsernameAndPassword(final HttpServletRequest request) {

		String auth = request.getHeader("Authorization");

		if (auth == null) {

			return null;
		}

		String usernameAndPassword = new String(Base64.decodeBase64(auth.substring(6)));

		logger.log(Level.FINE, "Decoded user and pass: {0}", usernameAndPassword);

		String[] userAndPass = StringUtils.split(usernameAndPassword, ":");

		return userAndPass;

	}

	@Override
	public Principal getUser(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response, final boolean tryLogin) throws FrameworkException {

		// First, check session (JSESSIONID cookie)
		Principal user = checkSessionAuthentication(request, response);
		
		if (user != null) {
			
			securityContext.setUser(user);
			return user;
		}

		// Second, try basic auth, if requested
		if (tryLogin && user == null) {

			user = checkBasicAuthentication(request, response);
		}
		
		return user;

	}
	
	public static Principal getUserForEmail(final SecurityContext securityContext, final String email) {
		
		Principal user = null;
		
		Result result = Result.EMPTY_RESULT;
		try {
			
			result = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(
				Search.andExactTypeAndSubtypes(Principal.class.getSimpleName()),
				Search.andExactProperty(User.email, email));

		} catch (FrameworkException ex) {
			
			logger.log(Level.SEVERE, null, ex);

		}

		if (!result.isEmpty()) {

			user = (User) result.get(0);

		}
		
		return user;
	}
	
	public static String getSessionId(final HttpServletRequest request) {
		
		String existingSessionId = request.getRequestedSessionId();
		
		if (existingSessionId == null) {
		
			HttpSession session = request.getSession(true);

			logger.log(Level.INFO, "Created new HTTP session: {0}", session.toString());
			
			return session.getId();
		
		}
		
		return existingSessionId;
		
	}
	
}
