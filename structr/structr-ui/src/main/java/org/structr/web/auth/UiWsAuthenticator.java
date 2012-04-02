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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.AuthHelper;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.User;

//~--- JDK imports ------------------------------------------------------------

import javax.servlet.http.HttpServletRequest;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class UiWsAuthenticator implements Authenticator {

	@Override
	public void examineRequest(SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {}

	@Override
	public User doLogin(SecurityContext securityContext, HttpServletRequest request, String userName, String password) throws AuthenticationException {

		String errorMsg = null;
		User user       = AuthHelper.getUserForUsernameAndPassword(securityContext, userName, password);

		if (errorMsg != null) {

			throw new AuthenticationException(errorMsg);

		}

		return user;
	}

	@Override
	public void doLogout(SecurityContext securityContext, HttpServletRequest request) {}

	//~--- get methods ----------------------------------------------------

	@Override
	public User getUser(SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {
		return null;    // In WS context, there's no classic HTTP request/session/servlet context available
	}
}
