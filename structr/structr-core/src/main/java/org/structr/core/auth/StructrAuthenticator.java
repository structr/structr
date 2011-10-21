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

import org.apache.commons.codec.digest.DigestUtils;

import org.structr.common.SecurityContext;
import org.structr.context.SessionMonitor;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.node.FindUserCommand;
import org.structr.ui.page.LoginPage;
import org.structr.ui.page.StructrPage;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

//~--- classes ----------------------------------------------------------------

/**
 * The default authenticator for structr.
 *
 * @author Christian Morgner
 */
public class StructrAuthenticator implements Authenticator {

	private static final String STANDARD_ERROR_MSG =
		"Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
	public static final String USERNAME_KEY  = "username";
	public static final String USER_NODE_KEY = "userNode";
	private static final Logger logger       = Logger.getLogger(StructrAuthenticator.class.getName());

	//~--- fields ---------------------------------------------------------

	private SecurityContext securityContext = null;

	//~--- methods --------------------------------------------------------

	@Override
	public User doLogin(HttpServletRequest request, String userName, String password)
		throws AuthenticationException {

		String errorMsg = null;
		User user       = null;

		if (LoginPage.SUPERADMIN_USERNAME_KEY.equals(userName)
			&& StructrPage.SUPERADMIN_PASSWORD_KEY.equals(password)) {

			logger.log(Level.INFO,
				   "############# Logged in as superadmin! ############");
			user = new SuperUser();

		} else {

			Command findUser = Services.command(securityContext,
				FindUserCommand.class);

			user = (User) findUser.execute(userName);

			if (user == null) {

				logger.log(Level.INFO,
					   "No user found for name {0}",
					   user);
				errorMsg = STANDARD_ERROR_MSG;

			} else {

				if (user.isBlocked()) {

					logger.log(Level.INFO,
						   "User {0} is blocked",
						   user);
					errorMsg = STANDARD_ERROR_MSG;
				}

				if (password == null) {

					logger.log(Level.INFO,
						   "Password for user {0} is null",
						   user);
					errorMsg = "You should enter a password.";
				}

				String encryptedPasswordValue = DigestUtils.sha512Hex(password);
				if (!encryptedPasswordValue.equals(user.getEncryptedPassword())) {

					logger.log(Level.INFO,
						   "Wrong password for user {0}",
						   user);
					errorMsg = STANDARD_ERROR_MSG;
				}
			}
		}

		if (errorMsg != null) {
			throw new AuthenticationException(errorMsg);
		}

		HttpSession session = request.getSession();

		session.setAttribute(USER_NODE_KEY,
				     user);
		session.setAttribute(USERNAME_KEY,
				     userName);

		long sessionId = SessionMonitor.registerUserSession(securityContext,
			session);

		SessionMonitor.logActivity(securityContext,
					   sessionId,
					   "Login");

		// Mark this session with the internal session id
		session.setAttribute(SessionMonitor.SESSION_ID,
				     sessionId);

		return user;
	}

	@Override
	public void doLogout(HttpServletRequest request) {

		HttpSession session = request.getSession();
		Long sessionIdValue = (Long) session.getAttribute(SessionMonitor.SESSION_ID);

		if (sessionIdValue != null) {

			long sessionId = sessionIdValue.longValue();

			SessionMonitor.logActivity(securityContext,
						   sessionId,
						   "Logout");
		}

		session.removeAttribute(USER_NODE_KEY);
		session.removeAttribute(USERNAME_KEY);
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public User getUser(HttpServletRequest request) {
		return (User) request.getSession().getAttribute(USER_NODE_KEY);
	}

	//~--- set methods ----------------------------------------------------

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
}
