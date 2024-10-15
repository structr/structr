/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.auth;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.entity.SuperUser;



/**
 * An authenticator implementation for structr which always returns a superuser or superuser context.
 *
 *
 */
public class SuperUserAuthenticator implements Authenticator {

	private static final SuperUser superUser = new SuperUser();

	@Override
	public SecurityContext initializeAndExamineRequest(final HttpServletRequest request, final HttpServletResponse response) throws FrameworkException {
		return SecurityContext.getSuperUserInstance(request);
	}

	@Override
	public void checkResourceAccess(final SecurityContext securityContext, final HttpServletRequest request, final String resourceSignature, final String propertyView) throws FrameworkException {
	}

	@Override
	public PrincipalInterface doLogin(final HttpServletRequest request, final String userName, final String password) throws AuthenticationException {
		return superUser;
	}

	@Override
	public void doLogout(final HttpServletRequest request) {
	}

	@Override
	public PrincipalInterface getUser(final HttpServletRequest request, final boolean tryLogin) throws FrameworkException {
		return superUser;
	}

	@Override
	public boolean hasExaminedRequest() {
		return false;
	}

	@Override
	public Class getUserClass() {
		return null;
	}
}
