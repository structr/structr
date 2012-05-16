/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.auth;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.AuthHelper;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.Principal;

//~--- JDK imports ------------------------------------------------------------

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class UiAuthenticator implements Authenticator {

	@Override
	public void initializeAndExamineRequest(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response) throws FrameworkException {
		getUser(securityContext, request, response);
	}

	@Override
	public void examineRequest(SecurityContext securityContext, HttpServletRequest request, String uriPart) throws FrameworkException { }

	@Override
	public Principal doLogin(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response, String userName, String password) throws AuthenticationException {

		String errorMsg = null;
		Principal user       = AuthHelper.getUserForUsernameAndPassword(securityContext, userName, password);

		if (errorMsg != null) {

			throw new AuthenticationException(errorMsg);

		}

		return user;
	}

	@Override
	public void doLogout(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response) {}

	//~--- get methods ----------------------------------------------------

	@Override
	public Principal getUser(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response) throws FrameworkException {

		String userName = request.getHeader("X-User");
		String password = request.getHeader("X-Password");
		String token    = request.getHeader("X-StructrSessionToken");
		Principal user       = null;

		// Try to authorize with a session token first
		if (token != null) {

			user = AuthHelper.getUserForToken(token);

		} else if ((userName != null) && (password != null)) {

			user = AuthHelper.getUserForUsernameAndPassword(securityContext, userName, password);

		}

		if (user != null) {

			securityContext.setUser(user);

		}

		return user;
	}
}
