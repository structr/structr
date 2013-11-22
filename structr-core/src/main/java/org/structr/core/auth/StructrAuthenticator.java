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


package org.structr.core.auth;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

//import org.structr.context.SessionMonitor;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.Principal;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;

//~--- classes ----------------------------------------------------------------

/**
 * The default authenticator for structr.
 *
 * @author Christian Morgner
 */
public class StructrAuthenticator implements Authenticator {

	public static final String USERNAME_KEY = "username";

	// public static final String USER_NODE_KEY = "userNode";
	private static final Logger logger = Logger.getLogger(StructrAuthenticator.class.getName());
	
	private boolean examined = false;

	//~--- methods --------------------------------------------------------

	@Override
	public void setUserAutoCreate(final boolean userAutoCreate, final Class userClass) {}
	
	@Override
	public boolean getUserAutoCreate() {
		return false;
	}

	@Override
	public Class getUserClass() {
		return null;
	}

	@Override
	public SecurityContext initializeAndExamineRequest(HttpServletRequest request, HttpServletResponse response) throws FrameworkException {
		return null;
	}

	@Override
	public void checkResourceAccess(HttpServletRequest request, String resourceSignature, String propertyView)
		throws FrameworkException {}

	@Override
	public Principal doLogin(HttpServletRequest request, String userName, String password) throws AuthenticationException {

		Principal user = AuthHelper.getPrincipalForPassword(AbstractNode.name, userName, password);

		try {

			HttpSession session = request.getSession();

			session.setAttribute(USERNAME_KEY, userName);

		} catch (Exception e) {

			logger.log(Level.INFO, "Could not register session");

		}

		examined = true;
		
		return user;

	}

	@Override
	public void doLogout(HttpServletRequest request) {

		HttpSession session = request.getSession();

		session.removeAttribute(USERNAME_KEY);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Principal getUser(HttpServletRequest request, final boolean tryLogin) throws FrameworkException {

		final String userName = (String) request.getSession().getAttribute(USERNAME_KEY);
		final App app         = StructrApp.getInstance();
		
		return app.nodeQuery(Principal.class).andName(userName).getFirst();
	}

	@Override
	public boolean hasExaminedRequest() {
		
		return examined;
		
	}

}
