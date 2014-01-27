/**
 * Copyright (C) 2010-2014 Structr, c/o Morgner UG (haftungsbeschränkt) <structr@structr.org>
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.rest.auth;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;

//~--- JDK imports ------------------------------------------------------------

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.AccessMode;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.search.SearchUserCommand;
import org.structr.rest.servlet.JsonRestServlet;

//~--- classes ----------------------------------------------------------------

/**
 * An authenticator implementation for {@see JsonRestServlet}.
 *
 * @author Christian Morgner
 */
public class RestAuthenticator implements Authenticator {
	
	@Override
	public SecurityContext initializeAndExamineRequest(HttpServletRequest request, HttpServletResponse response) throws FrameworkException {
		
		Principal user = getUser(request, true);
		
		if (user instanceof SuperUser) {
			
			return SecurityContext.getSuperUserInstance(request);
			
		} else {
			
			return SecurityContext.getInstance(user, request, AccessMode.Backend);
			
		}
	}

	@Override
	public void checkResourceAccess(HttpServletRequest request, String resourceSignature, String propertyView) throws FrameworkException { }

	@Override
	public Principal doLogin(HttpServletRequest request, String userName, String password) throws AuthenticationException {
		return null;
	}

	@Override
	public void doLogout(HttpServletRequest request) {}

	//~--- get methods ----------------------------------------------------

	@Override
	public Principal getUser(HttpServletRequest request, final boolean tryLogin) {

		String userHeader = request.getHeader("X-User");
		Principal user         = null;

		try {

			if (userHeader != null) {

				long userId           = Long.parseLong(userHeader);
				AbstractNode userNode = (AbstractNode) StructrApp.getInstance().command(SearchUserCommand.class).execute(userId);

				if ((userNode != null) && (userNode instanceof Principal)) {

					user = (Principal) userNode;

				}

			}

		} catch (Throwable t) {}
		
		return user;
	}

	@Override
	public boolean hasExaminedRequest() {
		
		return false;
		
	}

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
	
}
