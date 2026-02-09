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

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.web.auth.AbstractOAuth2Client;
import org.structr.web.auth.OAuth2ProviderRegistry;

import java.util.List;
import java.util.Map;

public class GithubOAuthClient extends AbstractOAuth2Client {

	private static final Logger logger = LoggerFactory.getLogger(GithubOAuthClient.class);
	private static final String AUTH_SERVER = "github";

	public GithubOAuthClient(final HttpServletRequest request, OAuth2ProviderRegistry.ProviderConfig providerConfig) {
		super(request, AUTH_SERVER, providerConfig.getApi(), providerConfig);
	}

	@Override
	public String getClientCredentials(final OAuth2AccessToken accessToken) {
		try {
			final OAuthRequest userDetailsRequest = new OAuthRequest(Verb.GET, userDetailsURI);
			service.signRequest(accessToken, userDetailsRequest);

			final OAuthRequest userEmailsRequest = new OAuthRequest(Verb.GET, userDetailsURI + "/emails");
			service.signRequest(accessToken, userEmailsRequest);

			String userResponseBody;
			String emailResponseBody;

			try (Response response = service.execute(userDetailsRequest)) {
				if (!response.isSuccessful() && Settings.OAuthVerboseLogging.getValue(false)) {
					logger.error("User details request to {} failed: {} - {}", provider, response.getCode(), response.getMessage());
				}
				userResponseBody = response.getBody();
			}

			try (Response response = service.execute(userEmailsRequest)) {
				if (!response.isSuccessful() && Settings.OAuthVerboseLogging.getValue(false)) {
					logger.error("User emails request to {} failed: {} - {}", provider, response.getCode(), response.getMessage());
				}
				emailResponseBody = response.getBody();
			}

			final String defaultEmailAddress = getDefaultUserEmailAddress(emailResponseBody);
			return parseUserCredentials(userResponseBody, accessToken, defaultEmailAddress);
		} catch (Exception e) {
			if (Settings.OAuthVerboseLogging.getValue(false)) {
				logger.error("Failed to get client credentials from {}: {}", provider, e.getMessage());
			}
			logger.debug("Client credentials error details", e);
		}
		return null;
	}

	/**
	 * Parses the user details response and extracts credentials.
	 *
	 * @param responseBody The response body from the user details endpoint
	 * @param accessToken The OAuth access token
	 * @param defaultEmailAddress The default email address from the /emails endpoint
	 * @return The credential value (typically email)
	 */
	protected String parseUserCredentials(final String responseBody, final OAuth2AccessToken accessToken, final String defaultEmailAddress) {
		try {
			final Gson gson = new Gson();
			final Map<String, Object> params = gson.fromJson(responseBody, Map.class);

			// Add decoded access token claims to user info
			final Map<String, Object> accessTokenClaims = decodeAccessTokenClaims(accessToken);
			if (accessTokenClaims != null) {
				params.put("accessTokenClaims", accessTokenClaims);
			}

			// Store full user info for later use
			this.userInfo = params;

			final Object credentialValue = params.get(getCredentialKey());

			// email might be null, if user has set it to private
			if (credentialValue == null) {
				this.userInfo.put(getCredentialKey(), defaultEmailAddress);
				return defaultEmailAddress;
			}

			return credentialValue.toString();

		} catch (Exception e) {
			if (Settings.OAuthVerboseLogging.getValue(false)) {
				logger.error("Failed to parse user credentials from {}: {}", provider, e.getMessage());
			}
			logger.debug("Credential parsing error details", e);
		}
		return null;
	}

	protected String getDefaultUserEmailAddress(final String emailResponse) {
		try {
			final Gson gson = new Gson();
			final List<Map<String, Object>> params = gson.fromJson(emailResponse, List.class);

			for (Map<String, Object> param : params) {
				if (param.get(getCredentialKey()) != null && (boolean) param.get("primary")) {
					return param.get(getCredentialKey()).toString();
				}
			}

		} catch (Exception e) {
			if (Settings.OAuthVerboseLogging.getValue(false)) {
				logger.error("Failed to parse user credentials from {}: {}", provider, e.getMessage());
			}
			logger.debug("Credential parsing error details", e);
		}
		return null;
	}
}