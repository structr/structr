/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Person;
import org.structr.core.property.PropertyKey;

/**
 * Central class for OAuth client implementations.
 * 
 *
 */
public class StructrOAuthClient {
	
	private static final Logger logger = Logger.getLogger(StructrOAuthClient.class.getName());
	
	protected enum ResponseFormat {
		json, urlEncoded
	}
	
	private static final String CONFIGURED_OAUTH_SERVERS = "oauth.servers";
	
	protected String authorizationLocation;
	protected String tokenLocation;
	protected String clientId;
	protected String clientSecret;
	protected String redirectUri;
	protected String state;
	
	protected Class tokenResponseClass;
	
	private OAuthAccessTokenResponse tokenResponse;
	private OAuthResourceResponse userResponse;
	
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

			logger.log(Level.INFO, "Authorization request location URI: {0}", oauthClientRequest.getLocationUri());
		
			return oauthClientRequest.getLocationUri();
			
		} catch (OAuthSystemException ex) {
			logger.log(Level.SEVERE, null, ex);
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

		String configuredOauthServers  = StructrApp.getConfigurationValue(CONFIGURED_OAUTH_SERVERS, "twitter facebook google github stackoverflow");
		String[] authServers = configuredOauthServers.split(" ");

		for (String authServer : authServers) {
			
			if (authServer.equals(name)) {
			
				String authLocation  = StructrApp.getConfigurationValue("oauth." + authServer + ".authorization_location", null);
				String tokenLocation = StructrApp.getConfigurationValue("oauth." + authServer + ".token_location", null);
				String clientId      = StructrApp.getConfigurationValue("oauth." + authServer + ".client_id", null);
				String clientSecret  = StructrApp.getConfigurationValue("oauth." + authServer + ".client_secret", null);
				String redirectUri   = StructrApp.getConfigurationValue("oauth." + authServer + ".redirect_uri", null);

				// Minumum required fields
				if (clientId != null && clientSecret != null && redirectUri != null) {

					Class serverClass		= getServerClassForName(name);
					Class tokenResponseClass	= getTokenResponseClassForName(name);
					
					StructrOAuthClient oauthServer;
					try {
						
						oauthServer = (StructrOAuthClient) serverClass.newInstance();
						oauthServer.init(authLocation, tokenLocation, clientId, clientSecret, redirectUri, tokenResponseClass);
						
						logger.log(Level.INFO, "Using OAuth server {0}", oauthServer);
						
						return oauthServer;
	
					} catch (Throwable t) {
						
						logger.log(Level.SEVERE, "Could not instantiate auth server", t);
						
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
			
			case "github" :
				return GitHubAuthClient.class;
			case "twitter" : 
				return TwitterAuthClient.class;
			case "facebook" : 
				return FacebookAuthClient.class;
			case "linkedin" :
				return LinkedInAuthClient.class;
			case "google" :
				return GoogleAuthClient.class;
			default :
				return StructrOAuthClient.class;
		}
		
	}

	protected GrantType getGrantType() {
		return GrantType.AUTHORIZATION_CODE;
	}
	
	protected String getScope() {
		return "";
	}
	
	protected String getResponseType() {
		return "code";
	}

	protected String getState() {
		return "";
	}

	protected String getAccessTokenParameterKey() {
		return OAuth.OAUTH_BEARER_TOKEN;
	}
	
	private static String getCode(final HttpServletRequest request) {
		
		OAuthAuthzResponse oar;

		try {

			logger.log(Level.INFO, "Trying to get authorization code from request {0}", request);
			
			oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
			
			String code = oar.getCode();
			
			logger.log(Level.INFO, "Got code {0} from authorization request", code);
			
			return oar.getCode();

		} catch (OAuthProblemException e) {

			logger.log(Level.SEVERE, "Could not read authorization request: {0}, {1}", new Object[] { e.getError(), e.getDescription() });

		}
		
		return null;
		
		
	}

	public PropertyKey getCredentialKey() {
		
		return Person.eMail;
		
	}
	
	public String getCredential(final HttpServletRequest request) {
		
		return getValue(request, "email");
		
	}
	
	public String getValue(final HttpServletRequest request, final String key) {
		
		try {
			
			OAuthResourceResponse userResponse = getUserResponse(request);

			if (userResponse == null) {

				return null;

			}

			String body = userResponse.getBody();
	
			logger.log(Level.INFO, "User response body: {0}", body);
			return (String) JSONUtils.parseJSON(body).get(key);
			
		} catch (Exception ex) {
			
			logger.log(Level.WARNING, "Could not extract {0} from JSON response", ex);
			
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
				
				logger.log(Level.SEVERE, "Could not get code from request, cancelling authorization process");
				return null;
				
			}
			
			OAuthClientRequest clientReq = OAuthClientRequest
				.tokenLocation(tokenLocation)
				.setGrantType(getGrantType())
				.setClientId(clientId)
				.setClientSecret(clientSecret)
				.setRedirectURI(getAbsoluteUrl(request, redirectUri))
				.setCode(getCode(request))
			.buildBodyMessage();

			logger.log(Level.INFO, "Request body: {0}", clientReq.getBody());

			OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

			tokenResponse = oAuthClient.accessToken(clientReq, tokenResponseClass);
			
			logger.log(Level.INFO, "Access token response: {0}", tokenResponse.getBody());
			
			return tokenResponse;
			
		} catch (Throwable t) {
			
			logger.log(Level.SEVERE, "Could not get access token response", t);
			
		}
		
		return null;
		
	}

	/**
	 * Where to fetch user information from the auth server
	 * 
	 * @return resource URI
	 */
	public String getUserResourceUri() {
		
		return "";
		
	}
	
	/**
	 * Path to redirect to on success
	 * @return return URI
	 */
	public String getReturnUri() {
		
		return "/";
		
	}
	
	/**
	 * Path to redirect to on error
	 * 
	 * @return error URI
	 */
	public String getErrorUri() {
		
		return "/";
		
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
				
				// needed for LinkedIn
				clientReq.setHeader("x-li-format", "json");
				
				logger.log(Level.INFO, "User info request: {0}", clientReq.getLocationUri());
				
				OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

				userResponse = oAuthClient.resource(clientReq, "GET", OAuthResourceResponse.class);
				logger.log(Level.INFO, "User info response: {0}", userResponse);

				return userResponse;
				
			}
			
		} catch (Throwable t) {
			
			logger.log(Level.SEVERE, "Could not get user response", t);
			
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
