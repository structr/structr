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



package org.structr.rest.auth;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.node.FindNodeCommand;
import org.structr.rest.servlet.JsonRestServlet;

//~--- JDK imports ------------------------------------------------------------

import javax.servlet.http.HttpServletRequest;

//~--- classes ----------------------------------------------------------------

/**
 * An authenticator implementation for {@see JsonRestServlet}.
 *
 * @author Christian Morgner
 */
public class RestAuthenticator implements Authenticator {

	@Override
	public void examineRequest(SecurityContext securityContext, HttpServletRequest request) throws FrameworkException {}

	@Override
	public User doLogin(SecurityContext securityContext, HttpServletRequest request, String userName, String password) throws AuthenticationException {
		return null;
	}

	@Override
	public void doLogout(SecurityContext securityContext, HttpServletRequest request) {}

	//~--- get methods ----------------------------------------------------

	@Override
	public User getUser(SecurityContext securityContext, HttpServletRequest request) {

		String userHeader = request.getHeader("X-User");
		User user         = null;

		try {

			if (userHeader != null) {

				long userId           = Long.parseLong(userHeader);
				AbstractNode userNode = (AbstractNode) Services.command(securityContext, FindNodeCommand.class).execute(userId);

				if ((userNode != null) && (userNode instanceof User)) {

					user = (User) userNode;

				}

			}

		} catch (Throwable t) {}

		return user;
	}
}
