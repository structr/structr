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

import com.github.scribejava.apis.*;
import com.github.scribejava.core.builder.api.DefaultApi20;
import jakarta.servlet.http.HttpServletRequest;
import org.structr.web.auth.provider.Auth0AuthClient;
import org.structr.web.auth.provider.AzureAuthClient;
import org.structr.web.auth.provider.GithubOAuthClient;
import org.structr.web.auth.provider.KeycloakAuthClient;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class OAuth2ProviderRegistry {

	private static final Map<String, ProviderConfig> PROVIDERS = new HashMap<>();

	static {

		PROVIDERS.put("google", new ProviderConfig(
				GoogleApi20.instance(),
				"email",
				"https://www.googleapis.com/oauth2/v3/userinfo",
				"email"
		));

		// Github needs special handling with userInfoEndpoint parsing, since email can be null in default response
		PROVIDERS.put("github", new ProviderConfig(
				GitHubApi.instance(),
				"email",
				"https://api.github.com/user",
				"user:email",
				GithubOAuthClient::new
		));

		PROVIDERS.put("facebook", new ProviderConfig(
				FacebookApi.instance(),
				"email",
				"https://graph.facebook.com/me",
				"email"
		));

		PROVIDERS.put("linkedin", new ProviderConfig(
				LinkedInApi20.instance(),
				"email",
				"https://api.linkedin.com/v2/userinfo",
				"openid profile email"
		));

		// Auth0: Requires tenant configuration
		PROVIDERS.put("auth0", new ProviderConfig(
				null, // API created dynamically based on tenant
				"email",
				"", // User info endpoint built from tenant
				"openid profile email",
				Auth0AuthClient::new
		));

		// Azure: Requires tenant_id configuration
		PROVIDERS.put("azure", new ProviderConfig(
				null, // API created dynamically based on tenant_id
				"mail",
				"https://graph.microsoft.com/v1.0/me",
				"openid profile email",
				AzureAuthClient::new
		));

		// Keycloak: Requires realm and url configuration
		PROVIDERS.put("keycloak", new ProviderConfig(
				null, // API created dynamically based on server_url and realm
				"email",
				"", // User info endpoint built from server_url and realm
				"openid profile email",
				KeycloakAuthClient::new
		));
	}

	/**
	 * Get configuration for a provider.
	 *
	 * @param provider Provider name (e.g., "google", "github", "auth0", "azure")
	 * @return Configuration or null if not registered
	 */
	public static ProviderConfig get(final String provider) {
		return PROVIDERS.get(provider != null ? provider.toLowerCase() : null);
	}

	/**
	 * Check if a provider is supported.
	 *
	 * @param provider Provider name
	 * @return true if provider is registered
	 */
	public static boolean isSupported(final String provider) {
		return PROVIDERS.containsKey(provider);
	}

	/**
	 * Configuration holder for a single OAuth2 provider.
	 *
	 * Contains all information needed to configure OAuth2 for a provider:
	 * - ScribeJava API (provides auth + token endpoints)
	 * - Default user info endpoint
	 * - Default scope
	 * - Credential key
	 * - Optional custom client factory
	 */
	public static class ProviderConfig {

		private final DefaultApi20 api;
		private final String credentialKey;
		private final String defaultUserInfoEndpoint;
		private final String defaultScope;
		private final BiFunction<HttpServletRequest, ProviderConfig, OAuth2Client> customClientFactory;

		/**
		 * Constructor for standard providers (use StandardOAuth2Client).
		 */
		public ProviderConfig(final DefaultApi20 api, final String credentialKey, final String defaultUserInfoEndpoint, final String defaultScope) {
			this(api, credentialKey, defaultUserInfoEndpoint, defaultScope, null);
		}

		/**
		 * Constructor with custom client factory.
		 */
		public ProviderConfig(final DefaultApi20 api, final String credentialKey, final String defaultUserInfoEndpoint, final String defaultScope, final BiFunction<HttpServletRequest, ProviderConfig, OAuth2Client> customClientFactory) {
			this.api = api;
			this.credentialKey = credentialKey;
			this.defaultUserInfoEndpoint = defaultUserInfoEndpoint;
			this.defaultScope = defaultScope;
			this.customClientFactory = customClientFactory;
		}

		public DefaultApi20 getApi() {
			return api;
		}

		public String getCredentialKey() {
			return credentialKey;
		}

		public String getDefaultUserInfoEndpoint() {
			return defaultUserInfoEndpoint;
		}

		public String getDefaultScope() {
			return defaultScope;
		}

		/**
		 * Create client instance for this provider.
		 *
		 * @param request HTTP servlet request
		 * @param provider Provider name
		 * @return OAuth2Client instance
		 */
		public OAuth2Client createClient(final HttpServletRequest request, final String provider) {
			if (customClientFactory != null) {
				return customClientFactory.apply(request, this);
			} else {
				return new StandardOAuth2Client(request, provider, this);
			}
		}
	}
}