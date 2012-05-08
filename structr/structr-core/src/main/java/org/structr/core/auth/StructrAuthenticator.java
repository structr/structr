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


import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
//import org.structr.context.SessionMonitor;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.User;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

//~--- classes ----------------------------------------------------------------

/**
 * The default authenticator for structr.
 *
 * @author Christian Morgner
 */
public class StructrAuthenticator implements Authenticator {

	public static final String USERNAME_KEY  = "username";
	public static final String USER_NODE_KEY = "userNode";
	private static final Logger logger       = Logger.getLogger(StructrAuthenticator.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void examineRequest(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response) throws FrameworkException {}

	@Override
	public User doLogin(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response, String userName, String password) throws AuthenticationException {

		String errorMsg = null;
		User user       = AuthHelper.getUserForUsernameAndPassword(securityContext, userName, password);

		if (errorMsg != null) {

			throw new AuthenticationException(errorMsg);

		}

		try {

			HttpSession session = request.getSession();

			session.setAttribute(USER_NODE_KEY, user);
			session.setAttribute(USERNAME_KEY, userName);

//			long sessionId = SessionMonitor.registerUserSession(securityContext, session);
//
//			SessionMonitor.logActivity(securityContext, sessionId, "Login");
//
//			// Mark this session with the internal session id
//			session.setAttribute(SessionMonitor.SESSION_ID, sessionId);

		} catch (Exception e) {
			logger.log(Level.INFO, "Could not register session");
		}

		return user;
	}

	@Override
	public void doLogout(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response) {

		HttpSession session = request.getSession();
//		Long sessionIdValue = (Long) session.getAttribute(SessionMonitor.SESSION_ID);
//
//		if (sessionIdValue != null) {
//
//			long sessionId = sessionIdValue.longValue();
//
//			SessionMonitor.logActivity(securityContext, sessionId, "Logout");
//
//		}

		session.removeAttribute(USER_NODE_KEY);
		session.removeAttribute(USERNAME_KEY);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public User getUser(SecurityContext securityContext, HttpServletRequest request, HttpServletResponse response) {
		return (User) request.getSession().getAttribute(USER_NODE_KEY);
	}
}
