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

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class HttpAuthenticator implements Authenticator {

	private static final String SESSION_USER = "sessionUser";

	@Override
	public void examineRequest(SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {}

	@Override
	public User doLogin(SecurityContext securityContext, HttpServletRequest request, String userName, String password) throws AuthenticationException {

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
	public void doLogout(SecurityContext securityContext, HttpServletRequest request) {

		try {

			request.logout();
			securityContext.setUser(null);

		} catch (ServletException ex) {
			Logger.getLogger(HttpAuthenticator.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public User getUser(SecurityContext securityContext, HttpServletRequest request) {

		User user = (User) request.getSession().getAttribute(SESSION_USER);

		if (user == null) {

			// TODO: Implement HTTP basic auth
		}

		return user;
	}
}
