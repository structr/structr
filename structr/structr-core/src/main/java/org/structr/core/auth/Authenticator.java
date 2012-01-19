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

package org.structr.core.auth;

import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.core.entity.User;

/**
 * An authenticator interface that defines how the system
 * can obtain a pricipal from a HttpServletRequest.
 *
 * @author Christian Morgner
 */
public interface Authenticator {

	public void setSecurityContext(SecurityContext securityContext);

	/**
	 *
	 * Tries to authenticate the given HttpServletRequest.
	 *
	 * @param request the request to authenticate
	 * @param userName the (optional) username
	 * @param password the (optional) password
	 * 
	 * @return the user that was just logged in
	 * @throws AuthenticationException
	 */
	public User doLogin(HttpServletRequest request, String userName, String password) throws AuthenticationException;

	/**
	 * Logs the given request out.
	 *
	 * @param request the request to log out
	 */
	public void doLogout(HttpServletRequest request);

	/**
	 * Returns the user that is currently logged into the system,
	 * or null if the session is not authenticated.
	 *
	 * @param request the request
	 * @return the logged-in user or null
	 */
	public User getUser(HttpServletRequest request);
}
