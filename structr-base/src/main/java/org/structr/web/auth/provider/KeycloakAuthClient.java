/*
 * Copyright (C) 2010-2026 Structr GmbH
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

/**
 * Keycloak OAuth2 client implementation.
 *
 * Configuration:
 *    oauth.keycloak.server_url = https://keycloak.example.com
 *    oauth.keycloak.realm = your-realm
 *    oauth.keycloak.client_id = your-client-id
 *    oauth.keycloak.client_secret = your-client-secret
 *
 * The implementation automatically builds all endpoint URLs:
 * - Authorization: {server_url}/realms/{realm}/protocol/openid-connect/auth
 * - Token: {server_url}/realms/{realm}/protocol/openid-connect/token
 * - User info: {server_url}/realms/{realm}/protocol/openid-connect/userinfo
 */
public class KeycloakAuthClient extends AbstractOAuth2Client {

	private static final Logger logger = LoggerFactory.getLogger(KeycloakAuthClient.class);
	private static final String AUTH_SERVER = "keycloak";

	private final String serverUrl;
	private final String realm;

	public KeycloakAuthClient(final HttpServletRequest request, OAuth2ProviderRegistry.ProviderConfig providerConfig) {

		final String serverUrl = Settings.getOrCreateStringSetting("oauth", AUTH_SERVER, "server_url").getValue(null);
		final String realm = Settings.getOrCreateStringSetting("oauth", AUTH_SERVER, "realm").getValue("master");

		// Build URLs from server URL and realm
		final String baseUrl = serverUrl != null && !serverUrl.isEmpty()
				? (serverUrl.startsWith("http") ? serverUrl : "https://" + serverUrl)
				: "";
		final String realmPath = "/realms/" + realm + "/protocol/openid-connect";
		final String authUrl = baseUrl + realmPath + "/auth";
		final String tokenUrl = baseUrl + realmPath + "/token";

		this.serverUrl = serverUrl;
		this.realm = realm;

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
	protected String getDefaultUserDetailsUri() {
		if (serverUrl != null && !serverUrl.isEmpty()) {
			final String baseUrl = serverUrl.startsWith("http") ? serverUrl : "https://" + serverUrl;
			return baseUrl + "/realms/" + realm + "/protocol/openid-connect/userinfo";
		}
		return "";
	}
}