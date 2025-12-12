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
package org.structr.web.auth.provider;

import com.github.scribejava.core.builder.api.DefaultApi20;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.web.auth.AbstractOAuth2Client;
import org.structr.web.auth.OAuth2ProviderRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Auth0 OAuth2 client implementation.
 *
 * Supports two configuration modes:
 * 1. Tenant-based (RECOMMENDED):
 *    oauth.auth0.tenant = your-tenant.auth0.com
 *    oauth.auth0.authorization_path = /authorize (optional, defaults shown)
 *    oauth.auth0.token_path = /oauth/token (optional, defaults shown)
 *    oauth.auth0.userinfo_path = /userinfo (optional, defaults shown)
 *
 * 2. Legacy explicit URLs (backwards compatible):
 *    oauth.auth0.authorization_location = https://...
 *    oauth.auth0.token_location = https://...
 *    oauth.auth0.user_details_resource_uri = https://...
 */
public class Auth0AuthClient extends AbstractOAuth2Client {

	private static final Logger logger = LoggerFactory.getLogger(Auth0AuthClient.class);
	private static final String AUTH_SERVER = "auth0";

	private final String audience;
	private final String tenant;
	private final String userInfoPath;

	public Auth0AuthClient(final HttpServletRequest request, OAuth2ProviderRegistry.ProviderConfig providerConfig) {


		final String tenant = Settings.getOrCreateStringSetting("oauth", AUTH_SERVER, "tenant").getValue(null);
		final String authPath = Settings.getOrCreateStringSetting("oauth", AUTH_SERVER, "authorization_path").getValue("/authorize");
		final String tokenPath = Settings.getOrCreateStringSetting("oauth", AUTH_SERVER, "token_path").getValue("/oauth/token");

		// Build URLs from tenant or use explicit configuration
		final String authUrl;
		final String tokenUrl;
		if (tenant != null && !tenant.isEmpty()) {
			final String baseUrl = tenant.startsWith("http") ? tenant : "https://" + tenant;
			authUrl = baseUrl + authPath;
			tokenUrl = baseUrl + tokenPath;
		} else {
			authUrl = Settings.getOrCreateStringSetting("oauth", AUTH_SERVER, "authorization_location").getValue("");
			tokenUrl = Settings.getOrCreateStringSetting("oauth", AUTH_SERVER, "token_location").getValue("");
		}

		this.tenant = tenant;
		this.audience = Settings.getOrCreateStringSetting("oauth", AUTH_SERVER, "audience").getValue("");
		this.userInfoPath = Settings.getOrCreateStringSetting("oauth", AUTH_SERVER, "userinfo_path").getValue("/userinfo");

		// Create API and pass to parent
		final DefaultApi20 api = new DefaultApi20() {
			@Override
			public String getAccessTokenEndpoint() {
				return tokenUrl;
			}

			@Override
			protected String getAuthorizationBaseUrl() {
				return authUrl;
			}
		};

		super(request, AUTH_SERVER, api, providerConfig);
	}

	@Override
	public String getAuthorizationURL(final String state) {
		final Map<String, String> additionalParams = new HashMap<>();
		if (audience != null && !audience.isEmpty()) {
			additionalParams.put("audience", audience);
		}
		return service.createAuthorizationUrlBuilder()
				.state(state)
				.additionalParams(additionalParams)
				.build();
	}

	@Override
	protected String getDefaultUserDetailsUri() {
		if (tenant != null && !tenant.isEmpty()) {
			final String baseUrl = tenant.startsWith("http") ? tenant : "https://" + tenant;
			return baseUrl + userInfoPath;
		}
		return "";
	}
}