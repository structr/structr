/*
 *  Copyright (C) 2013 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
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
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.utils.JSONUtils;
import org.codehaus.jettison.json.JSONException;
import org.structr.core.Services;

/**
 *
 * @author Axel Morgner
 */
public class OAuth2Server {
	
	private static final Logger logger = Logger.getLogger(OAuth2Server.class.getName());
	
	protected enum ResponseType {
		json, urlEncoded
	}
	
	private static final String CONFIGURED_OAUTH_SERVERS = "oauth.servers";
	
	protected String authorizationLocation;
	protected String tokenLocation;
	protected String clientId;
	protected String clientSecret;
	protected String redirectUri;
	
	private OAuthAccessTokenResponse tokenResponse;
	private OAuthResourceResponse userResponse;
	
	public OAuth2Server() {};
	
	public OAuth2Server(final String authorizationLocation, final String tokenLocation, final String clientId, final String clientSecret, final String redirectUri) {
		
		this.init(authorizationLocation, tokenLocation, clientId, clientSecret, redirectUri);
	}
	
	protected final void init(final String authorizationLocation, final String tokenLocation, final String clientId, final String clientSecret, final String redirectUri) {

		this.authorizationLocation = authorizationLocation;
		this.tokenLocation         = tokenLocation;
		this.clientId              = clientId;
		this.clientSecret          = clientSecret;
		this.redirectUri           = redirectUri;
		
	}
	
	/**
	 * Create an end-user authorization request
	 * 
	 * Use with {@code response.setRedirect(request.getLocationUri());}
	 * 
	 * @param request
	 * @return
	 * @throws OAuthSystemException 
	 */
	public OAuthClientRequest getEndUserAuthorizationRequest(final HttpServletRequest request) throws OAuthSystemException {
		
		OAuthClientRequest oauthClientRequest = OAuthClientRequest
			.authorizationLocation(authorizationLocation)
			.setClientId(clientId)
			.setRedirectURI(getAbsoluteRedirectUri(request, redirectUri))
			.setScope(getScope())
		.buildQueryMessage();
		
		return oauthClientRequest;
		
	}
	
	/**
	 * Build an OAuth2 server from the configured values for the given name.
	 * 
	 * @param name
	 * @return 
	 */
	public static OAuth2Server getServer(final String name) {

		String configuredOauthServers  = Services.getConfigurationValue(CONFIGURED_OAUTH_SERVERS, "twitter facebook google github stackoverflow");
		String[] authServers = configuredOauthServers.split(" ");

		for (String authServer : authServers) {
			
			if (authServer.equals(name)) {
			
				String authLocation  = Services.getConfigurationValue("oauth." + authServer + ".authorization_location", null);
				String tokenLocation = Services.getConfigurationValue("oauth." + authServer + ".token_location", null);
				String clientId      = Services.getConfigurationValue("oauth." + authServer + ".client_id", null);
				String clientSecret  = Services.getConfigurationValue("oauth." + authServer + ".client_secret", null);
				String redirectUri   = Services.getConfigurationValue("oauth." + authServer + ".redirect_uri", null);

				if (authLocation != null && clientId != null && clientSecret != null && redirectUri != null) {

					Class serverClass = getServerClassForName(name);
					
					OAuth2Server oauthServer;
					try {
						
						oauthServer = (OAuth2Server) serverClass.newInstance();
						oauthServer.init(authLocation, tokenLocation, clientId, clientSecret, redirectUri);
						return oauthServer;
	
					} catch (Exception ex) {
						
						logger.log(Level.SEVERE, "Could not instantiate auth server", ex);
						
					}


				}
				
			}

		}
				
		return null;
	}
	
	private static Class getServerClassForName(final String name) {
		
		if ("github".equals(name)) {
			
			return GitHubAuthServer.class;
			
		} else if ("facebook".equals(name)) {
			
			return FacebookAuthServer.class;
			
		} else {
			
			return OpenIdAuthServer.class;
			
		}
		
	}
	
	protected String getScope() {
		return "";
	}
	
	private static String getCode(final HttpServletRequest request) {
		
		OAuthAuthzResponse oar;

		try {

			oar = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
			return oar.getCode();

		} catch (Throwable t) {

			logger.log(Level.SEVERE, "Could not read authorization response", t);

		}
		
		return null;
		
		
	}

	public String getEmail(final HttpServletRequest request) {
		return get(request, "email");
	}
	
	public String get(final HttpServletRequest request, final String key) {
		
		try {
			
			String body = getUserResponse(request).getBody();
			logger.log(Level.INFO, "User response body: {0}", body);
			return (String) JSONUtils.parseJSON(body).get(key);
			
		} catch (JSONException ex) {
			
			logger.log(Level.WARNING, "Could not extract {0} from JSON response", ex);
			
		}
		
		return null;
		
	}

	public String getAccessToken(final HttpServletRequest request) {
		
		return getAccessTokenResponse(request).getAccessToken();
			
	}
	
	public Long getExpiresIn(final HttpServletRequest request) {
		
		return getAccessTokenResponse(request).getExpiresIn();
		
	}
	
	private OAuthAccessTokenResponse getAccessTokenResponse(final HttpServletRequest request) {
		
		if (tokenResponse != null) {
			
			return tokenResponse;
			
		}
		
		try {
		
			OAuthClientRequest clientReq = OAuthClientRequest
				.tokenLocation(tokenLocation)
				.setGrantType(GrantType.AUTHORIZATION_CODE)
				.setClientId(clientId)
				.setClientSecret(clientSecret)
				.setRedirectURI(getAbsoluteRedirectUri(request, redirectUri))
				.setCode(getCode(request))
			.buildBodyMessage();

			OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

			if (ResponseType.urlEncoded.equals(getResponseType())) {
				
				tokenResponse = oAuthClient.accessToken(clientReq, GitHubTokenResponse.class);
				
			} else {
				
				tokenResponse = oAuthClient.accessToken(clientReq);
			}
			
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
	 * @return 
	 */
	public String getUserResourceUri() {
		
		return "";
		
	}
	
	/**
	 * Path to redirect to on success
	 * @return 
	 */
	public String getReturnUri() {
		
		return "/";
		
	}
	
	/**
	 * Path to redirect to on error
	 * 
	 * @return 
	 */
	public String getErrorUri() {
		
		return "/";
		
	}

	protected OAuthResourceResponse getUserResponse(final HttpServletRequest request) {
		
		if (userResponse != null) {
			
			return userResponse;
			
		}
		
		try {
		
			OAuthClientRequest clientReq = new OAuthBearerClientRequest(getUserResourceUri())
				.setAccessToken(getAccessToken(request))
				.buildQueryMessage();

			OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
			
			userResponse = oAuthClient.resource(clientReq, "GET", OAuthResourceResponse.class);
			logger.log(Level.INFO, "User response: {0}", userResponse.getBody());

			return userResponse;
			
		} catch (Throwable t) {
			
			logger.log(Level.SEVERE, "Could not get user response", t);
			
		}
		
		return null;
		
	}
		
	protected String getAbsoluteRedirectUri(final HttpServletRequest request, final String redirectUri) {
		
		return !(redirectUri.startsWith("http")) ? "http" + (request.isSecure() ? "s" : "") + "://" + Services.getApplicationHost() + ":" + request.getServerPort() + redirectUri : redirectUri;
		
	}
	
	public ResponseType getResponseType() {
		
		return ResponseType.json;
		
	}
	
}
