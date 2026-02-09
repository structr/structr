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
package org.structr.web.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.api.NamedArguments;
import org.structr.core.entity.Principal;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.schema.action.Actions;
import org.structr.schema.action.EvaluationHints;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Abstract base class for OAuth2 clients.
 * Provides common functionality for all OAuth2 providers.
 */
public abstract class AbstractOAuth2Client implements OAuth2Client {

	private static final Logger logger = LoggerFactory.getLogger(AbstractOAuth2Client.class);

	protected final String provider;
	protected final String authLocation;
	protected final String tokenLocation;
	protected final String clientId;
	protected final String clientSecret;
	protected final String redirectUri;
	protected final String returnUri;
	protected final String errorUri;
	protected final String logoutUri;
	protected final String userDetailsURI;
	protected final String scope;
	protected final OAuth2ProviderRegistry.ProviderConfig providerConfig;

	protected Map<String, Object> userInfo;
	protected OAuth20Service service;

	/**
	 * Constructor that loads configuration and builds the OAuth service.
	 *
	 * @param request The HTTP servlet request
	 * @param provider The provider name
	 * @param api The DefaultApi20 for this provider
	 */
	protected AbstractOAuth2Client(final HttpServletRequest request, final String provider, final DefaultApi20 api, final OAuth2ProviderRegistry.ProviderConfig providerConfig) {
		this.provider = provider;
		this.providerConfig = providerConfig;

		// Load OAuth configuration from settings with defaults
		authLocation   = getSetting("authorization_location", "");
		tokenLocation  = getSetting("token_location", "");
		clientId       = getSetting("client_id", "");
		clientSecret   = getSetting("client_secret", "");
		redirectUri    = getAbsoluteUrl(request, getDefaultRedirectUri());
		returnUri      = getSetting("return_uri", "/");
		errorUri       = getSetting("error_uri", "/error");
		logoutUri      = getSetting("logout_uri", "/logout");
		userDetailsURI = getSetting("user_details_resource_uri", getDefaultUserDetailsUri());
		scope          = getSetting("scope", getDefaultScope());

		buildOAuthService(api);
	}

	/**
	 * Gets a setting value with fallback to default.
	 * Allows subclasses to provide provider-specific defaults.
	 */
	protected String getSetting(final String key, final String defaultValue) {
		return Settings.getOrCreateStringSetting("oauth", provider, key).getValue(defaultValue);
	}

	/**
	 * Gets the default redirect URI for this provider.
	 * Can be overridden by subclasses.
	 */
	protected String getDefaultRedirectUri() {
		return "/oauth/" + provider + "/auth";
	}

	/**
	 * Gets the default user details URI for this provider.
	 * Must be overridden by subclasses that use defaults.
	 */
	protected String getDefaultUserDetailsUri() {
		return providerConfig.getDefaultUserInfoEndpoint();
	}

	/**
	 * Gets the default scope for this provider.
	 * Can be overridden by subclasses.
	 */
	protected String getDefaultScope() {
		return providerConfig.getDefaultScope();
	}

	/**
	 * Builds the OAuth service using the provided API instance.
	 * Can be overridden by subclasses that need custom service configuration.
	 *
	 * @param api The ScribeJava API instance
	 */
	protected void buildOAuthService(final DefaultApi20 api) {
		this.service = new ServiceBuilder(clientId)
				.apiSecret(clientSecret)
				.callback(redirectUri)
				.defaultScope(scope)
				.build(api);
	}

	/**
	 * Converts relative URLs to absolute URLs based on request.
	 */
	protected String getAbsoluteUrl(final HttpServletRequest request, final String uri) {
		if (uri.startsWith("http")) {
			return uri;
		}
		return "http" + (request.isSecure() ? "s" : "") + "://" +
				request.getServerName() + ":" + request.getServerPort() + uri;
	}

	@Override
	public String getAuthorizationURL(final String state) {
		return service.getAuthorizationUrl(state);
	}

	@Override
	public String getCredentialKey() {
		return providerConfig.getCredentialKey();
	}

	@Override
	public String getReturnURI() {
		return returnUri;
	}

	@Override
	public String getErrorURI() {
		return errorUri;
	}

	@Override
	public String getLogoutURI() {
		return logoutUri;
	}

	@Override
	public OAuth2AccessToken getAccessToken(final String authorizationReplyCode) {
		try {

			return service.getAccessToken(authorizationReplyCode);

		} catch (Exception e) {
			if (Settings.OAuthVerboseLogging.getValue(false)) {
				logger.error("Failed to get access token from {}: {}", provider, e.getMessage());
			}

			logger.debug("Access token error details", e);
		}
		return null;
	}

	@Override
	public String getClientCredentials(final OAuth2AccessToken accessToken) {
		try {
			final OAuthRequest request = new OAuthRequest(Verb.GET, userDetailsURI);
			service.signRequest(accessToken, request);

			try (Response response = service.execute(request)) {
				if (!response.isSuccessful()) {
					logger.error("User details request to {} failed: {} - {}",
							provider, response.getCode(), response.getMessage());
					return null;
				}

				return parseUserCredentials(response.getBody(), accessToken);
			}
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
	 * @return The credential value (typically email)
	 */
	protected String parseUserCredentials(final String responseBody, final OAuth2AccessToken accessToken) {
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

			// Extract and return the credential value
			final Object credentialValue = params.get(getCredentialKey());
			return credentialValue != null ? credentialValue.toString() : null;

		} catch (Exception e) {
			if (Settings.OAuthVerboseLogging.getValue(false)) {
				logger.error("Failed to parse user credentials from {}: {}", provider, e.getMessage());
			}
			logger.debug("Credential parsing error details", e);
		}
		return null;
	}

	/**
	 * Decodes JWT access token and extracts claims as a map.
	 *
	 * @param accessToken The OAuth access token
	 * @return Map of claims or null if decoding fails
	 */
	protected Map<String, Object> decodeAccessTokenClaims(final OAuth2AccessToken accessToken) {
		try {
			final DecodedJWT jwt = JWT.decode(accessToken.getAccessToken());
			final Map<String, Object> claims = new HashMap<>();

			for (Map.Entry<String, Claim> entry : jwt.getClaims().entrySet()) {
				final Claim claim = entry.getValue();
				final String key = entry.getKey();

				// Extract claim value based on type
				if (claim.asDouble() != null || claim.asLong() != null || claim.asInt() != null) {
					claims.put(key, claim.as(Number.class));
				} else if (claim.asBoolean() != null) {
					claims.put(key, claim.asBoolean());
				} else if (claim.asList(Object.class) != null) {
					claims.put(key, claim.asList(Object.class));
				} else if (claim.asMap() != null) {
					claims.put(key, claim.asMap());
				} else if (claim.asString() != null) {
					claims.put(key, claim.asString());
				}
			}

			return claims;

		} catch (Exception e) {
			logger.debug("Could not decode access token claims for {}: {}", provider, e.getMessage());
		}
		return null;
	}

	@Override
	public Map<String, Object> getUserInfo() {
		return this.userInfo;
	}

	@Override
	public void invokeOnLoginMethod(final Principal user) throws FrameworkException {

		final AbstractMethod method = Methods.resolveMethod(Traits.of(StructrTraits.USER), Actions.NOTIFICATION_OAUTH_LOGIN);

		if (method != null) {
			final NamedArguments arguments = new NamedArguments();
			arguments.add("provider", this.provider);
			arguments.add("userinfo", this.getUserInfo());

			method.execute(user.getSecurityContext(), user, arguments, new EvaluationHints());
		}
	}

	@Override
	public void initializeAutoCreatedUser(final Principal user) {
		// Default implementation does nothing
		// Subclasses can override to customize user initialization
	}
}