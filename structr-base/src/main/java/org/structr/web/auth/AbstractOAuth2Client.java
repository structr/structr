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
import com.github.scribejava.core.oauth.AccessTokenRequestParams;
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
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;
import java.net.URI;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
		redirectUri    = getAbsoluteUrl(request, getSetting("redirect_uri", getDefaultRedirectUri()));
		returnUri      = getAbsoluteUrl(request, getSetting("return_uri", "/"));
		errorUri       = getAbsoluteUrl(request, getSetting("error_uri", "/error"));
		logoutUri      = getAbsoluteUrl(request, getSetting("logout_uri", "/logout"));
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
	 * Converts relative URLs to absolute URLs based on request or configured baseURL.
	 */
	protected String getAbsoluteUrl(final HttpServletRequest request, final String uri) {

		if (uri.startsWith("http")) {

			return uri;
		}

		final String baseUrlOverride = Settings.BaseUrlOverride.getValue();
		if (StringUtils.isNotBlank(baseUrlOverride)) {

			String base = baseUrlOverride.endsWith("/") ? baseUrlOverride : baseUrlOverride + "/";
			String relative = uri.startsWith("/") ? uri.substring(1) : uri;

			return URI.create(base).resolve(relative).toString();
		}

		final int port = request.getServerPort();
		final boolean isSecure = request.isSecure();
		final String portPart = (port == 80 || port == 443) ? "" : ":" + port;
		final String baseUrl = "http" + (isSecure ? "s" : "") + "://"
				+ request.getServerName()
				+ portPart;

		return URI.create(baseUrl).resolve(uri).toString();
	}

	@Override
	public String getAuthorizationURL(final OAuth2Flow flow) {

		/* PKCE (RFC 7636) and the OIDC nonce ride along with every authorization request, for every
		   provider, with nothing to configure: RFC 6749 3.1 has the authorization server ignore request
		   parameters it does not recognise, so a provider that implements neither behaves as it did
		   before. What they buy is the two things state alone does not cover - an authorization code
		   intercepted on its way back is worthless without the verifier that only this Structr instance
		   holds, and an id_token from an earlier exchange cannot be replayed into this one. */
		final Map<String, String> additionalParams = new HashMap<>(getAdditionalAuthorizationParameters());

		additionalParams.put("nonce", flow.getNonce());

		return service.createAuthorizationUrlBuilder()
			.state(flow.getState())
			.pkce(flow.getPKCE())
			.additionalParams(additionalParams)
			.build();
	}

	/**
	 * Provider-specific parameters to add to the authorization request. Overriding this rather than
	 * {@link #getAuthorizationURL(OAuth2Flow)} is what keeps a provider from silently dropping the state,
	 * nonce and PKCE binding the base implementation puts there.
	 */
	protected Map<String, String> getAdditionalAuthorizationParameters() {

		return Map.of();
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
	public OAuth2AccessToken getAccessToken(final String authorizationReplyCode, final OAuth2Flow flow) {

		try {

			final OAuth2AccessToken accessToken = service.getAccessToken(AccessTokenRequestParams.create(authorizationReplyCode).pkceCodeVerifier(flow.getCodeVerifier()));
			if (accessToken != null && !hasMatchingNonce(accessToken, flow.getNonce())) {

				return null;
			}

			return accessToken;

		} catch (Exception e) {

			if (Settings.OAuthVerboseLogging.getValue(false)) {

				logger.error("Failed to get access token from {}: {}", provider, e.getMessage());
			}

			logger.debug("Access token error details", e);
		}

		return null;
	}

	/**
	 * Checks the nonce of the id_token the provider returned alongside the access token, if it returned
	 * one at all.
	 *
	 * <p>A provider that speaks plain OAuth2 rather than OIDC answers without an id_token, so the absence
	 * of one is not treated as a failure - which does mean the nonce buys nothing against such a
	 * provider, and that is the honest position rather than refusing a login over a protocol the provider
	 * does not speak. What must not happen is accepting an id_token whose nonce is missing or belongs to
	 * a different request.
	 *
	 * <p>The signature is not re-verified: the id_token comes back on this instance's own request to the
	 * token endpoint rather than through the browser, and OIDC Core 3.1.3.7 lets TLS server validation
	 * stand in for checking the token signature when it arrives that way. That rests on the token
	 * endpoint being https, which is the provider's URL and not something checked here. Only the nonce
	 * binding is at stake.
	 */
	protected boolean hasMatchingNonce(final OAuth2AccessToken accessToken, final String expectedNonce) {

		final String idToken = extractIdToken(accessToken);
		if (idToken == null) {

			return true;
		}

		try {

			final Claim nonce = JWT.decode(idToken).getClaim("nonce");
			if (nonce == null || nonce.isNull()) {

				logger.warn("Refusing OAuth login for provider {}: the id_token carries no nonce, so it cannot be tied to this login attempt.", provider);

				return false;
			}

			if (!StringUtils.equals(expectedNonce, nonce.asString())) {

				logger.warn("Refusing OAuth login for provider {}: the nonce in the id_token belongs to a different login attempt.", provider);

				return false;
			}

			return true;

		} catch (Exception e) {

			logger.warn("Refusing OAuth login for provider {}: the id_token could not be decoded: {}", provider, e.getMessage());

			return false;
		}
	}

	/**
	 * Returns the raw id_token from the token endpoint response, or null if there is none.
	 *
	 * <p>Read out of the raw response because that is the one place it is available whichever provider
	 * answered. ScribeJava models the id_token as OpenIdOAuth2AccessToken, but only for an API whose
	 * token extractor produces one, and of the APIs reachable here that is GoogleApi20 alone.
	 *
	 * <p>Nor is the raw response always JSON: GitHubApi extracts with OAuth2AccessTokenExtractor, which
	 * scans for {@code access_token=([^&]+)} in a form-encoded body. That is why the shape is checked
	 * before parsing rather than left to an exception.
	 */
	protected String extractIdToken(final OAuth2AccessToken accessToken) {

		final String rawResponse = accessToken.getRawResponse();
		if (StringUtils.isBlank(rawResponse) || !rawResponse.trim().startsWith("{")) {

			return null;
		}

		try {

			final Map<String, Object> response = new Gson().fromJson(rawResponse, Map.class);
			final Object idToken               = response != null ? response.get("id_token") : null;

			return idToken != null ? idToken.toString() : null;

		} catch (Exception e) {

			logger.debug("Could not read id_token from token response of {}: {}", provider, e.getMessage());
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

					logger.error("User details request to {} failed: {} - {}", provider, response.getCode(), response.getMessage());

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
			if (credentialValue == null) {

				if (Settings.OAuthVerboseLogging.getValue(false)) {

					logger.error("User details from location {} for provider {} does not contain credentials key {}", userDetailsURI , provider, getCredentialKey());
				}

				return null;
			}

			if (!isCredentialVerified(params)) {

				return null;
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

	/**
	 * Refuses a credential the provider itself says it has not verified.
	 *
	 * <p>The credential is the e-mail address for every provider registered here, and Structr looks the
	 * local account up by it - so an address the provider has not verified is an address anybody with an
	 * account at that provider can claim. With an open registration at the IdP, typing someone else's
	 * address into the profile and running the login is enough to be handed their Structr account. OIDC
	 * providers report this as {@code email_verified} on the userinfo response; both spellings are
	 * checked because the endpoint is configurable ({@code user_details_resource_uri}) and Google's
	 * older v2 userinfo answers {@code verified_email}.
	 *
	 * <p>Only an explicit "no" is refused: a provider that does not report verification at all leaves the
	 * behaviour as it was, because there is nothing to act on and an installation that has been running
	 * on such a provider must not be locked out by an upgrade.
	 */
	protected boolean isCredentialVerified(final Map<String, Object> userDetails) {

		for (final String key : Set.of("email_verified", "verified_email")) {

			final Object verified = userDetails.get(key);

			// an explicit "no" only: anything else, including a value shaped in some way this code does not
			// know, is treated as "the provider did not say"
			if (verified != null && "false".equalsIgnoreCase(verified.toString().trim())) {

				logger.warn("Refusing OAuth login for provider {}: the provider reports {}=false for the {} it returned, so it cannot be used to identify an account here.",
					provider, key, getCredentialKey());

				return false;
			}
		}

		return true;
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

		final AbstractMethod method = Methods.resolveMethod(Traits.of(StructrTraits.USER), Actions.NOTIFICATION_ON_OAUTH_LOGIN);
		if (method != null) {

			final NamedArguments arguments = new NamedArguments();
			arguments.add("provider", this.provider);
			arguments.add("userinfo", this.getUserInfo());

			method.execute(new ActionContext(user.getSecurityContext()), user, arguments);
		}
	}

	@Override
	public void initializeAutoCreatedUser(final Principal user) {

		// Default implementation does nothing
		// Subclasses can override to customize user initialization
	}
}