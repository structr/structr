/**
 * Copyright (C) 2010-2020 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.auth;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.GitHubTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
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
import org.structr.web.entity.User;

/**
 * Central class for OAuth client implementations.
 *
 *
 */
public abstract class StructrOAuthClient {

	private static final Logger logger = LoggerFactory.getLogger(StructrOAuthClient.class.getName());

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
		+ "\nclientSecret: " + clientSecret
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
	public String getEndUserAuthorizationRequestUri(final HttpServletRequest request) {

		OAuthClientRequest oauthClientRequest;

		try {

			oauthClientRequest = OAuthClientRequest
				.authorizationLocation(authorizationLocation)
				.setClientId(clientId)
				.setRedirectURI(getAbsoluteUrl(request, redirectUri))
				.setScope(getScope())
				.setResponseType(getResponseType())
				.setState(getState())
				.buildQueryMessage();

			logger.info("Authorization request location URI: {}", oauthClientRequest.getLocationUri());

			return oauthClientRequest.getLocationUri();

		} catch (OAuthSystemException ex) {
			logger.error("", ex);
		}

		return null;

	}

	/**
	 * Build an OAuth2 server from the configured values for the given name.
	 *
	 * @param name
	 * @return server
	 */
	public static StructrOAuthClient getServer(final String name) {

		String configuredOauthServers  = Settings.OAuthServers.getValue();
		String[] authServers = configuredOauthServers.split(" ");

		for (String authServer : authServers) {

			if (authServer.equals(name)) {

				String authLocation  = Settings.getOrCreateStringSetting("oauth", authServer, "authorization_location").getValue("");
				String tokenLocation = Settings.getOrCreateStringSetting("oauth", authServer, "token_location").getValue("");
				String clientId      = Settings.getOrCreateStringSetting("oauth", authServer, "client_id").getValue("");
				String clientSecret  = Settings.getOrCreateStringSetting("oauth", authServer, "client_secret").getValue("");
				String redirectUri   = Settings.getOrCreateStringSetting("oauth", authServer, "redirect_uri").getValue("");

				// Minumum required fields
				if (clientId != null && clientSecret != null && redirectUri != null) {

					Class serverClass		= getServerClassForName(name);
					Class tokenResponseClass	= getTokenResponseClassForName(name);

					if (serverClass != null) {

						StructrOAuthClient oauthServer;
						try {

							oauthServer = (StructrOAuthClient) serverClass.newInstance();
							oauthServer.init(authLocation, tokenLocation, clientId, clientSecret, redirectUri, tokenResponseClass);

							logger.info("Using OAuth server {}", oauthServer);

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
				return Auth0AuthClient.class;
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

		OAuthAuthzResponse oar;

		try {

			logger.info("Trying to get authorization code from request {}", request);

			oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);

			String code = oar.getCode();

			logger.info("Got code {} from authorization request", code);

			return oar.getCode();

		} catch (OAuthProblemException e) {

			logger.error("Could not read authorization request: {}, {}", new Object[] { e.getError(), e.getDescription() });

		}

		return null;
	}
	
	public void initializeUser(final Principal user) throws FrameworkException {
		// override me
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

			logger.info("User response body: {}", body);

			// make full user info available to implementing classes
			this.userInfo = JSONUtils.parseJSON(body);

			// return desired value
			return (String)this.userInfo.get(key);

		} catch (Exception ex) {

			logger.warn("Could not extract {} from JSON response", ex);
		}

		return null;

	}

	public String getAccessToken(final HttpServletRequest request) {

		OAuthAccessTokenResponse resp = getAccessTokenResponse(request);

		if (resp == null) {
			return null;
		}

		return resp.getAccessToken();

	}

	public Long getExpiresIn(final HttpServletRequest request) {

		OAuthAccessTokenResponse resp = getAccessTokenResponse(request);

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

			logger.info("Request body: {}", clientReq.getBody());

			OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

			tokenResponse = oAuthClient.accessToken(clientReq, tokenResponseClass);

			logger.info("Access token response: {}", tokenResponse.getBody());

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

			String accessToken = getAccessToken(request);

			if (accessToken != null) {

				final String accessTokenParameterKey = this.getAccessTokenParameterKey();

				OAuthClientRequest clientReq = new OAuthBearerClientRequest(getUserResourceUri()) {

					@Override
					public OAuthBearerClientRequest setAccessToken(String accessToken) {
					    this.parameters.put(accessTokenParameterKey, accessToken);
					    return this;
					}
				}
					.setAccessToken(accessToken)
					.buildQueryMessage();

				logger.info("User info request: {}", clientReq.getLocationUri());

				OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

				userResponse = oAuthClient.resource(clientReq, "GET", OAuthResourceResponse.class);
				logger.info("User info response: {}", userResponse);

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
}
