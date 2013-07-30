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
package org.structr.rest.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;

/**
 * An authenticator implementation for structr which always returns a superuser or superuser context.
 * 
 * @author Christian Morgner
 */
public class SuperUserAuthenticator implements Authenticator {
	
	private static final SuperUser superUser = new SuperUser();

	@Override
	public SecurityContext initializeAndExamineRequest(HttpServletRequest request, HttpServletResponse response) throws FrameworkException {
		return SecurityContext.getSuperUserInstance(request);
	}

	@Override
	public void checkResourceAccess(HttpServletRequest request, String resourceSignature, String propertyView) throws FrameworkException {
	}

	@Override
	public Principal doLogin(HttpServletRequest request, String userName, String password) throws AuthenticationException {
		return superUser;
	}

	@Override
	public void doLogout(HttpServletRequest request) {
	}

	@Override
	public Principal getUser(HttpServletRequest request, final boolean tryLogin) throws FrameworkException {
		return superUser;
	}

	@Override
	public boolean hasExaminedRequest() {
		return false;
	}
}
