/*
 *  Copyright (C) 2011 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.auth;

import java.io.IOException;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.AuthHelper;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.User;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class HttpAuthenticator implements Authenticator {

	private static final String SESSION_USER = "sessionUser";
	private static final Logger logger = Logger.getLogger(HttpAuthenticator.class.getName());

	@Override
	public void examineRequest(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response) throws FrameworkException {}

	@Override
	public User doLogin(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response, String userName, String password) throws AuthenticationException {

		User user = AuthHelper.getUserForUsernameAndPassword(securityContext, userName, password);

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

	//~--- get methods ----------------------------------------------------

	@Override
	public User getUser(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response) {

		User user = (User) request.getSession().getAttribute(SESSION_USER);

		if (user == null) {

			user = checkBasicAuthentication(request, response);
		}

		return user;
	}
	
	private User checkBasicAuthentication(HttpServletRequest request, HttpServletResponse response) {

		User user;
		String auth = request.getHeader("Authorization");

		if (auth == null) {

			sendBasicAuthResponse(response);
			return null;

		}

		if (!auth.toUpperCase().startsWith("BASIC ")) {

			sendBasicAuthResponse(response);
			return null;

		}

		String usernameAndPassword = new String(Base64.decodeBase64(auth.substring(6)));

		logger.log(Level.INFO, "Decoded user and pass: {0}", usernameAndPassword);

		String[] userAndPass = StringUtils.split(usernameAndPassword, ":");

		try {
			user = AuthHelper.getUserForUsernameAndPassword(SecurityContext.getSuperUserInstance(), userAndPass[0], userAndPass[1]);
		} catch (AuthenticationException ex) {
			sendBasicAuthResponse(response);
			return null;
		}

		return user;
	}

	private void sendBasicAuthResponse(HttpServletResponse response) {

		response.setHeader("WWW-Authenticate", "BASIC realm=\"structr\"");
		try {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
	}
}
