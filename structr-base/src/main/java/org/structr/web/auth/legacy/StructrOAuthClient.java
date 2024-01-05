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
package org.structr.web.auth.legacy;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.*;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.EvaluationHints;
import org.structr.web.entity.User;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central class for OAuth client implementations.
 *
 *
 */
public abstract class StructrOAuthClient {

	protected static final Logger logger = LoggerFactory.getLogger(StructrOAuthClient.class.getName());

	protected enum ResponseFormat {
		json, urlEncoded
	}

	protected enum ResponseType {
		code
	}

	// protected attributes
	protected Map<String, Object> userInfo = null;
	protected String authorizationLocation = null;
	protected String tokenLocation         = null;
	protected String clientId              = null;
	protected String clientSecret          = null;
	protected String redirectUri           = null;
	protected String state                 = null;
	protected Class tokenResponseClass     = null;

	// private properties
	private OAuthAccessTokenResponse tokenResponse;
	private OAuthResourceResponse userResponse;

	public abstract String getUserResourceUri();
	public abstract String getReturnUri();
	public abstract String getErrorUri();
	protected abstract String getScope();
	protected abstract String getAccessTokenLocationKey();
	protected abstract String getAccessTokenLocation();

	public StructrOAuthClient() {};

	public StructrOAuthClient(final String authorizationLocation, final String tokenLocation, final String clientId, final String clientSecret, final String redirectUri) {

		this.init(authorizationLocation, tokenLocation, clientId, clientSecret, redirectUri, OAuthJSONAccessTokenResponse.class);
	}

	protected void init(final String authorizationLocation, final String tokenLocation, final String clientId, final String clientSecret, final String redirectUri, final Class tokenResponseClass) {

		this.authorizationLocation = authorizationLocation;
		this.tokenLocation         = tokenLocation;
		this.clientId              = clientId;
		this.clientSecret          = clientSecret;
		this.redirectUri           = redirectUri;

		this.tokenResponseClass    = tokenResponseClass;
	}

	@Override
	public String toString() {

		return this.getClass().getName()
		+ "\nauthorizationLocation: " + authorizationLocation
		+ "\ntokenLocation: " + tokenLocation
		+ "\nclientId: " + clientId
		+ "\nclientSecret: ****** HIDDEN ******"
		+ "\nredirectUri: " + redirectUri
		+ "\nstate: " + state;
	}

	/**
	 * Create an end-user authorization request
	 *
	 * Use with {@literal response.setRedirect(request.getLocationUri());}
	 *
	 * @param request
	 * @return request URI
	 */
	public String getEndUserAuthorizationRequestUri(final HttpServletRequest request, String state) {

		try {
			if (state == null) {
				state = getState();
			}

			final OAuthClientRequest oauthClientRequest = OAuthClientRequest
					.authorizationLocation(authorizationLocation)
					.setClientId(clientId)
					.setRedirectURI(getAbsoluteUrl(request, redirectUri))
					.setScope(getScope())
					.setResponseType(getResponseType())
					.setState(state)
					.buildQueryMessage();

			if (isVerboseLoggingEnabled()) {
				logger.info("Authorization request location URI: {}", oauthClientRequest.getLocationUri());
			}

			return oauthClientRequest.getLocationUri();

		} catch (OAuthSystemException ex) {
			logger.error("", ex);
		}

		return null;
	}

	public String getEndUserLogoutRequestUri() throws OAuthSystemException {
		final OAuthClientRequest oauthClientRequest = OAuthClientRequest
				.authorizationLocation(this.getLogoutUri())
				.setClientId(this.clientId)
				.setParameter(this.getLogouReturnUriParameterKey(), this.getLogoutReturnUri())
				.buildQueryMessage();

		return oauthClientRequest.getLocationUri();
	}

	/**
	 * Build an OAuth2 server from the configured values for the given name.
	 *
	 * @param name
	 * @return server
	 */
	public static StructrOAuthClient getServer(final String name) {

		final String configuredOauthServers = Settings.OAuthServers.getValue();
		final String[] authServers          = configuredOauthServers.split(" ");

		for (String authServer : authServers) {

			if (authServer.equals(name)) {

				final String authLocation  = Settings.getOrCreateStringSetting("oauth", authServer, "authorization_location").getValue("");
				final String tokenLocation = Settings.getOrCreateStringSetting("oauth", authServer, "token_location").getValue("");
				final String clientId      = Settings.getOrCreateStringSetting("oauth", authServer, "client_id").getValue("");
				final String clientSecret  = Settings.getOrCreateStringSetting("oauth", authServer, "client_secret").getValue("");
				final String redirectUri   = Settings.getOrCreateStringSetting("oauth", authServer, "redirect_uri").getValue("");

				// Minimum required fields
				if (clientId != null && clientSecret != null && redirectUri != null) {

					final Class serverClass		   = getServerClassForName(name);
					final Class tokenResponseClass = getTokenResponseClassForName(name);

					if (serverClass != null) {

						try {

							final StructrOAuthClient oauthServer = (StructrOAuthClient) serverClass.newInstance();
							oauthServer.init(authLocation, tokenLocation, clientId, clientSecret, redirectUri, tokenResponseClass);

							if (isVerboseLoggingEnabled()) {
								logger.info("Using OAuth server {}", oauthServer);
							}

							return oauthServer;

						} catch (Throwable t) {

							logger.error("Could not instantiate auth server", t);
						}

					} else {

						logger.warn("No OAuth provider found for name {}, ignoring.", name);
					}
				}
			}
		}

		return null;
	}

	private static Class getTokenResponseClassForName(final String name) {

		// The following comments are taken from https://cwiki.apache.org/confluence/display/OLTU/OAuth+2.0+Client+Quickstart

		// "Facebook is not fully compatible with OAuth 2.0 draft 10, access token response is
		// application/x-www-form-urlencoded, not json encoded so we use dedicated response class for that
		// Custom response classes are an easy way to deal with oauth providers that introduce modifications to
		// OAuth 2.0 specification"

		switch (name) {

			case "github" :
				return GitHubTokenResponse.class;
			case "facebook" :
				return GitHubTokenResponse.class;
			default :
				return OAuthJSONAccessTokenResponse.class;
		}

	}

	public String getProviderName () {
		return null;
	}

	public String getLogoutUri () {
		return null;
	}

	public String getLogoutReturnUri () {
		return null;
	}

	public String getLogouReturnUriParameterKey () {
		return null;
	}

	private static Class getServerClassForName(final String name) {

		switch (name) {

			case "github":
				return GitHubAuthClient.class;

			case "twitter":
				return TwitterAuthClient.class;

			case "facebook":
				return FacebookAuthClient.class;

			case "linkedin":
				return LinkedInAuthClient.class;

			case "google":
				return GoogleAuthClient.class;

			case "auth0":
				return Auth0LegacyAuthClient.class;
		}

		// no provider found, ignore?
		return null;
	}

	protected GrantType getGrantType() {
		return GrantType.AUTHORIZATION_CODE;
	}

	protected String getAccessTokenParameterKey() {
		return OAuth.OAUTH_BEARER_TOKEN;
	}

	private static String getCode(final HttpServletRequest request) {

		try {

			if (isVerboseLoggingEnabled()) {
				logger.info("Trying to get authorization code from request {}", request);
			}

			// TODO: FIX OAuth for Jetty11
			//final OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
			final OAuthAuthzResponse oar = OAuthAuthzResponse.oauthCodeAuthzResponse(null);
			final String code            = oar.getCode();

			if (isVerboseLoggingEnabled()) {
				logger.info("Got code {} from authorization request", code);
			}

			return code;

		} catch (OAuthProblemException e) {

			logger.error("Could not read authorization request: {}, {}", new Object[] { e.getError(), e.getDescription() });
		}

		return null;
	}

	public void initializeUser(final Principal user) throws FrameworkException {
		// overridden in specific implementations
	}

	public PropertyKey getCredentialKey() {
		return StructrApp.key(User.class, "eMail");
	}

	public String getCredential(final HttpServletRequest request) {
		return getValue(request, "email");
	}

	protected String getResponseType() {
		return ResponseType.code.name();
	}

	protected String getState() {
		return "";
	}

	public String getValue(final HttpServletRequest request, final String key) {

		try {

			OAuthResourceResponse userResponse = getUserResponse(request);

			if (userResponse == null) {

				return null;
			}

			final String body = userResponse.getBody();

			if (isVerboseLoggingEnabled()) {
				logger.info("User response body: {}", body);
			}

			// make full user info available to implementing classes
			this.userInfo = JSONUtils.parseJSON(body);

			// return desired value
			return (String) this.userInfo.get(key);

		} catch (Exception ex) {

			logger.warn("Could not extract {} from JSON response", ex);
		}		return null;
	}

	public Map<String, Object> getUserInfo() {
		return userInfo;
	}

	public String getAccessToken(final HttpServletRequest request) {

		final OAuthAccessTokenResponse resp = getAccessTokenResponse(request);

		if (resp == null) {
			return null;
		}

		return resp.getAccessToken();
	}

	public Long getExpiresIn(final HttpServletRequest request) {

		final OAuthAccessTokenResponse resp = getAccessTokenResponse(request);

		if (resp == null) {
			return null;
		}

		return resp.getExpiresIn();
	}

	private OAuthAccessTokenResponse getAccessTokenResponse(final HttpServletRequest request) {

		if (tokenResponse != null) {

			return tokenResponse;
		}

		try {

			String code = getCode(request);

			if (code == null) {

				logger.error("Could not get code from request, cancelling authorization process");
				return null;

			}

			OAuthClientRequest clientReq = OAuthClientRequest
				.tokenLocation(tokenLocation)
				.setGrantType(getGrantType())
				.setClientId(clientId)
				.setClientSecret(clientSecret)
				.setRedirectURI(getAbsoluteUrl(request, redirectUri))
				.setScope(getScope())
				.setCode(getCode(request))
			.buildBodyMessage();

			if (isVerboseLoggingEnabled()) {
				logger.info("Request body: {}", clientReq.getBody());
			}

			final OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

			tokenResponse = oAuthClient.accessToken(clientReq, tokenResponseClass);

			if (isVerboseLoggingEnabled()) {
				logger.info("Access token response: {}", tokenResponse.getBody());
			}

			return tokenResponse;

		} catch (Throwable t) {

			logger.error("Could not get access token response", t);
		}

		return null;
	}

	protected OAuthResourceResponse getUserResponse(final HttpServletRequest request) {

		if (userResponse != null) {

			return userResponse;
		}

		try {

			final String accessToken = getAccessToken(request);
			if (accessToken != null) {

				final String accessTokenParameterKey        = this.getAccessTokenParameterKey();
				final OAuthBearerClientRequest oauthRequest = new OAuthBearerClientRequest(getUserResourceUri()) {

					@Override
					public OAuthBearerClientRequest setAccessToken(String accessToken) {
					    this.parameters.put(accessTokenParameterKey, accessToken);
					    return this;
					}
				};

				oauthRequest.setAccessToken(accessToken);

				final String accessTokenLocation = getAccessTokenLocation();
				OAuthClientRequest clientReq     = null;

				switch (accessTokenLocation) {

					case "body":
						clientReq = oauthRequest.buildBodyMessage();
						break;

					case "header":
						clientReq = oauthRequest.buildHeaderMessage();
						break;

					case "query":
						clientReq = oauthRequest.buildQueryMessage();
						break;
				}

				if (clientReq != null) {

					if (isVerboseLoggingEnabled()) {
						logger.info("User info request: {}", clientReq.getLocationUri());
					}

					final OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

					userResponse = oAuthClient.resource(clientReq, "GET", OAuthResourceResponse.class);

					if (isVerboseLoggingEnabled()) {
						logger.info("User info response: {}", userResponse);
					}

				} else {

					logger.warn("Unable to access userinfo endpoint, invalid access token location in configuration. Please set {} to one of [body, header, query]", getAccessTokenLocationKey());
				}

				return userResponse;
			}

		} catch (Throwable t) {

			logger.error("Could not get user response", t);
		}

		return null;
	}

	protected String getAbsoluteUrl(final HttpServletRequest request, final String redirectUri) {
		return !(redirectUri.startsWith("http")) ? "http" + (request.isSecure() ? "s" : "") + "://" + request.getServerName() + ":" + request.getServerPort() + redirectUri : redirectUri;
	}

	public ResponseFormat getResponseFormat() {
		return ResponseFormat.json;
	}

	protected static boolean isVerboseLoggingEnabled() {
		return (Boolean.TRUE.equals(Settings.OAuthVerboseLogging.getValue()));
	}

	public void invokeOnLoginMethod(Principal user) throws FrameworkException {

		final Map<String, Object> methodParamerers = new LinkedHashMap<>();
		methodParamerers.put("provider", this.getProviderName());
		methodParamerers.put("userinfo", this.getUserInfo());

		user.invokeMethod(user.getSecurityContext(), "onOAuthLogin", methodParamerers, false, new EvaluationHints());
	}
}
