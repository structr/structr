/*
 *  Copyright (C) 2012 Axel Morgner, structr <structr@structr.org>
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


import java.util.logging.Level;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.AuthHelper;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.User;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class UiRestAuthenticator implements Authenticator {

	private static final Logger logger = Logger.getLogger(UiRestAuthenticator.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void examineRequest(SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {
		getUser(securityContext, request);
	}

	@Override
	public User doLogin(SecurityContext securityContext, HttpServletRequest request, String userName, String password) throws AuthenticationException {

		try {
			return getUser(securityContext, request);
		} catch (Throwable t) {
			logger.log(Level.WARNING, t.getMessage());
		}

		return null;
	}

	@Override
	public void doLogout(SecurityContext securityContext, HttpServletRequest request) {}

	//~--- get methods ----------------------------------------------------

	@Override
	public User getUser(SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {

		String userName = request.getHeader("X-User");
		String password = request.getHeader("X-Password");
		String token    = request.getHeader("X-StructrSessionToken");
		User user       = null;

		// Try to authorize with a session token first
		if (token != null) {

			user = AuthHelper.getUserForToken(token);

		} else if ((userName != null) && (password != null)) {

			user = AuthHelper.getUserForUsernameAndPassword(securityContext, userName, password);

		}

		return user;
	}
}
