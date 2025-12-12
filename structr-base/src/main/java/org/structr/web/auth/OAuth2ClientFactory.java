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
 * Factory for creating OAuth2 client instances.
 *
 * Simply delegates to OAuth2ProviderRegistry to look up provider configuration
 * and create the appropriate client (standard or custom).
 */
public class OAuth2ClientFactory {

	private static final Logger logger = LoggerFactory.getLogger(OAuth2ClientFactory.class);

	/**
	 * Creates an OAuth2 client for the specified provider.
	 *
	 * @param provider The provider name (e.g., "google", "github", "auth0", "azure")
	 * @param request The HTTP servlet request
	 * @return OAuth2Client instance or null if provider is not supported
	 */
	public static OAuth2Client createClient(final String provider, final HttpServletRequest request) throws IllegalArgumentException {
		if (provider == null) {
			return null;
		}

		// Look up provider configuration from registry
		final OAuth2ProviderRegistry.ProviderConfig config = OAuth2ProviderRegistry.get(provider);

		if (config == null) {
			throw new IllegalArgumentException("No OAuth2ProviderRegistry found for provider '" + provider + "'");
		}

		// Let the config create the appropriate client (standard or custom)
		return config.createClient(request, provider);
	}

	/**
	 * Checks if a provider is supported.
	 *
	 * @param provider The provider name
	 * @return true if the provider is supported
	 */
	public static boolean isProviderSupported(final String provider) {
		return OAuth2ProviderRegistry.isSupported(provider);
	}
}