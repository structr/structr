/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.auth;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.utils.JSONUtils;
import org.codehaus.jettison.json.JSONException;
import org.structr.core.Services;
import org.structr.core.entity.Person;
import org.structr.core.property.PropertyKey;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * Twitter supports only OAuth 1.0a, therefore we need a completely different
 * approach and library here.
 * 
 * @author Axel Morgner
 */
public class TwitterAuthServer extends OAuthServer {
	
	private static final Logger logger	= Logger.getLogger(TwitterAuthServer.class.getName());
	
	private static Twitter twitter;
	
	public TwitterAuthServer() {}

	@Override
	protected void init(final String authorizationLocation, final String tokenLocation, final String clientId, final String clientSecret, final String redirectUri, final Class tokenResponseClass) {

		super.init(authorizationLocation, tokenLocation, clientId, clientSecret, redirectUri, tokenResponseClass);

		twitter = new TwitterFactory().getInstance();
		twitter.setOAuthConsumer(clientId, clientSecret);
		
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
	@Override
	public String getEndUserAuthorizationRequestUri(final HttpServletRequest request) {
		
		RequestToken requestToken;
		
		try {
			// The following does not work, leads to the following error from Twitter:
			//String callbackUrl = getAbsoluteUrl(request, redirectUri);
			//logger.log(Level.INFO, "Callback URL: {0}", callbackUrl);
			//requestToken = twitter.getOAuthRequestToken(callbackUrl, "read");
			/*
			/* 401:Authentication credentials (https://dev.twitter.com/pages/auth) were missing or incorrect. Ensure that you have set valid consumer key/secret, access token/secret, and the system clock is in sync.
			/* <?xml version="1.0" encoding="UTF-8"?>
			/* <hash>
  			/*   <error>Desktop applications only support the oauth_callback value 'oob'</error>
  			/*   <request>/oauth/request_token</request>
			/* </hash>
			/* 
			/* Relevant discussions can be found on the Internet at:
			/*	http://www.google.co.jp/search?q=6c607809 or
			/*	http://www.google.co.jp/search?q=102175dd
			/* 			 
			 */

			requestToken = twitter.getOAuthRequestToken();
			request.getSession().setAttribute("requestToken", requestToken);
			
			logger.log(Level.INFO, "Authorization request location URI: {0}", requestToken.getAuthorizationURL());
			
			return requestToken.getAuthorizationURL();
			
		} catch (TwitterException ex) {
			
			logger.log(Level.SEVERE, null, ex);
			
		}

		return null;	
		
	}
		
	@Override
	public String getAccessToken(final HttpServletRequest request) {
		
		String verifier = request.getParameter("oauth_verifier");
		RequestToken requestToken = (RequestToken) request.getSession().getAttribute("requestToken");

		AccessToken accessToken;
		try {
			
			accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
		
			return accessToken.toString();
			
		} catch (TwitterException ex) {
			
			logger.log(Level.SEVERE, null, ex);
			
		}
		
		return null;
			
	}

	@Override
	public String getValue(final HttpServletRequest request, final String key) {
		
		try {
			
			return twitter.getScreenName();
			
		} catch (Exception ex) {
			
			logger.log(Level.SEVERE, null, ex);
			
		}
		
		return null;
		
	}
	
	
	@Override
	public ResponseFormat getResponseFormat() {
		
		return ResponseFormat.json;
		
	}

	@Override
	public String getReturnUri() {
		
		return Services.getConfigurationValue("oauth.twitter.return_uri", "/");
			
	}

	@Override
	public String getErrorUri() {
		
		return Services.getConfigurationValue("oauth.twitter.error_uri", "/");
			
	}

	@Override
	public PropertyKey getCredentialKey() {
		
		return Person.twitterName;
		
	}
	
	@Override
	public String getCredential(final HttpServletRequest request) {
		
		return getValue(request, "screen_name");
		
	}

	
}
