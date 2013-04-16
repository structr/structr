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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class HttpAuthenticator implements Authenticator {

	private static final String SESSION_USER = "sessionUser";
	private static final Logger logger       = Logger.getLogger(HttpAuthenticator.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeAndExamineRequest(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response) throws FrameworkException {}

	@Override
	public void examineRequest(SecurityContext securityContext, HttpServletRequest request, String resourceSignature, ResourceAccess resourceAccess, String propertyView)
		throws FrameworkException {}

	@Override
	public Principal doLogin(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response, String userName, String password) throws AuthenticationException {

		Principal user = AuthHelper.getUserForUsernameAndPassword(securityContext, userName, password);

		if (user != null) {

			request.getSession().setAttribute(SESSION_USER, user);
			securityContext.setUser(user);

			try {

				request.login(userName, password);

			} catch (ServletException ex) {

				Logger.getLogger(HttpAuthenticator.class.getName()).log(Level.SEVERE, null, ex);

			}

		}

		return user;

	}

	@Override
	public void doLogout(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response) {

		try {

			request.logout();
			securityContext.setUser(null);

		} catch (ServletException ex) {

			Logger.getLogger(HttpAuthenticator.class.getName()).log(Level.SEVERE, null, ex);

		}

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
			
			logger.log(Level.WARNING, "Error while appending to writer", ise);
			
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
	public Principal getUser(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response) {

		Principal user = (Principal) request.getSession().getAttribute(SESSION_USER);

		if (user == null) {

			user = checkBasicAuthentication(request, response);
		}

		return user;

	}

}
