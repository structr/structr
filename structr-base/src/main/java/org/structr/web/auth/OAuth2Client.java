/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.web.auth;

import com.github.scribejava.core.model.OAuth2AccessToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;

import java.util.Map;

/**
 * Interface for OAuth2 client implementations.
 */
public interface OAuth2Client {

	/**
	 * Gets the authorization URL for the OAuth2 flow.
	 *
	 * @param state The state parameter for CSRF protection
	 * @return The authorization URL
	 */
	String getAuthorizationURL(final String state);

	/**
	 * Exchanges authorization code for access token.
	 *
	 * @param authorizationReplyCode The authorization code from the provider
	 * @return The access token
	 */
	OAuth2AccessToken getAccessToken(final String authorizationReplyCode);

	/**
	 * Retrieves user credentials from the provider using the access token.
	 *
	 * @param accessToken The OAuth access token
	 * @return The credential value (typically email)
	 */
	String getClientCredentials(final OAuth2AccessToken accessToken);

	/**
	 * Gets the return URI to redirect to after successful authentication.
	 *
	 * @return The return URI
	 */
	String getReturnURI();

	/**
	 * Gets the error URI to redirect to after failed authentication.
	 *
	 * @return The error URI
	 */
	String getErrorURI();

	/**
	 * Gets the logout URI.
	 *
	 * @return The logout URI
	 */
	String getLogoutURI();

	/**
	 * Invokes the onOAuthLogin method on the user if it exists.
	 *
	 * @param user The authenticated user
	 * @throws FrameworkException If method invocation fails
	 */
	void invokeOnLoginMethod(final Principal user) throws FrameworkException;

	/**
	 * Gets the credential key used to identify users (e.g., "email").
	 *
	 * @return The credential key
	 */
	String getCredentialKey();

	/**
	 * Initializes an auto-created user with provider-specific data.
	 *
	 * @param user The newly created user
	 */
	void initializeAutoCreatedUser(final Principal user);

	/**
	 * Gets the full user info retrieved from the OAuth provider.
	 *
	 * @return Map of user information
	 */
	Map<String, Object> getUserInfo();
}