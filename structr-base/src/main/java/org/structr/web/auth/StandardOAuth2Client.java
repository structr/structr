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

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard OAuth2 client implementation for providers that work with default configuration.
 * Handles Google, GitHub, Facebook, LinkedIn, and other standard OAuth2 providers.
 *
 * Uses provider defaults from OAuth2ProviderRegistry for user info endpoints and scopes.
 * These can be overridden via configuration if needed.
 */
public class StandardOAuth2Client extends AbstractOAuth2Client {

	private static final Logger logger = LoggerFactory.getLogger(StandardOAuth2Client.class);

	/**
	 * Constructor using provider configuration.
	 *
	 * @param request The HTTP servlet request
	 * @param provider The provider name
	 * @param providerConfig The provider configuration with defaults
	 */
	public StandardOAuth2Client(final HttpServletRequest request, final String provider, final OAuth2ProviderRegistry.ProviderConfig providerConfig) {
		super(request, provider, providerConfig.getApi(), providerConfig);
	}
}