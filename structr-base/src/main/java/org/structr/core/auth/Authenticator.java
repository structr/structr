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
import org.structr.core.entity.Principal;
import org.structr.core.traits.Traits;

/**
 * An authenticator interface that defines how the system can obtain a principal
 * from a Request.
 *
 *
 *
 */
public interface Authenticator {

	/*
	 * Indicate that the authenticator has already examined the request
	 */
	public boolean hasExaminedRequest();

	/**
	 * Return user class
	 * @return userClass
	 */
	public Traits getUserClass();

	/**
	 * Initializes the authenticator with data from the given request.
	 *
	 * @param request
	 * @param response
	 * @return securityContext
	 * @throws FrameworkException
	 */
	public SecurityContext initializeAndExamineRequest(final HttpServletRequest request, HttpServletResponse response) throws FrameworkException;

	/**
	 *
	 * @param securityContext
	 * @param request
	 * @param resourceSignature
	 * @param propertyView
	 * @throws FrameworkException
	 */
	public void checkResourceAccess(final SecurityContext securityContext, final HttpServletRequest request, final String resourceSignature, final String propertyView) throws FrameworkException;

	/**
	 *
	 * Tries to authenticate the given Request.
	 *
	 * @param request the request to authenticate
	 * @param emailOrUsername the (optional) email/username
	 * @param password the (optional) password
	 *
	 * @return the user that was just logged in
	 * @throws AuthenticationException
	 * @throws FrameworkException
	 */
	public Principal doLogin(final HttpServletRequest request, final String emailOrUsername, final String password) throws AuthenticationException, FrameworkException;

	/**
	 * Logs the given request out.
	 *
	 * @param request the request to log out
	 */
	public void doLogout(final HttpServletRequest request);

	/**
	 * Returns the user that is currently logged into the system,
	 * or null if the session is not authenticated.
	 *
	 * @param request the request
	 * @param tryLogin if true, try to login the user
	 * @return the logged-in user or null
	 * @throws FrameworkException
	 */
	public Principal getUser(final HttpServletRequest request, final boolean tryLogin) throws FrameworkException;
}
