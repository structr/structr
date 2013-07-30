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

import java.util.LinkedList;
import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;

//import org.structr.context.SessionMonitor;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.graph.search.SearchAttribute;

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

		String userName  = (String) request.getSession().getAttribute(USERNAME_KEY);
		
		List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();
		attrs.add(Search.andExactTypeAndSubtypes(Principal.class));
		attrs.add(Search.andExactName(userName));
		
		Result userList = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(attrs);
		Principal user  = null;
		
		if (!userList.isEmpty()) {
			user = (Principal) userList.get(0);
		}

		return user;

	}

	@Override
	public boolean hasExaminedRequest() {
		
		return examined;
		
	}

}
