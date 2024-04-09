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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.web.entity.User;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Twitter supports only OAuth 1.0a, therefore we need a completely different
 * approach and library here.
 *
 *
 */
public class TwitterAuthClient extends StructrOAuthClient {

	private static final Logger logger	= LoggerFactory.getLogger(TwitterAuthClient.class.getName());

	private static Twitter twitter;

	public TwitterAuthClient() {}

	@Override
	public String getProviderName () {
		return "twitter";
	}

	@Override
	protected void init(final String authorizationLocation, final String tokenLocation, final String clientId, final String clientSecret, final String redirectUri, final Class tokenResponseClass) {

		super.init(authorizationLocation, tokenLocation, clientId, clientSecret, redirectUri, tokenResponseClass);

		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setOAuthAuthorizationURL(authorizationLocation);
		cb.setOAuthAccessTokenURL(tokenLocation);

		Configuration conf = cb.build();

		TwitterFactory tf = new TwitterFactory(conf);
		twitter = tf.getInstance();
		twitter.setOAuthConsumer(clientId, clientSecret);

	}

	/**
	 * Create an end-user authorization request
	 *
	 * Use with {@literal response.setRedirect(request.getLocationUri());}
	 *
	 * @param request HTTP request
	 * @return auth request URI
	 */
	@Override
	public String getEndUserAuthorizationRequestUri(final HttpServletRequest request, String state) {

		RequestToken requestToken;

		try {
			// The following does not work, leads to the following error from Twitter:
			//String callbackUrl = getAbsoluteUrl(request, redirectUri);
			//if (isVerboseLogging()) {
			//	logger.info("Callback URL: {}", callbackUrl);
			//}
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

			// Workaround for requestToken.getAuthorizationURL() ignoring configuration built with ConfigurationBuilder
			final String authorizationUrl = twitter.getConfiguration().getOAuthAuthorizationURL().concat("?oauth_token=").concat(requestToken.getToken());

			if (isVerboseLoggingEnabled()) {
				logger.info("Authorization request location URI: {}", authorizationUrl);
			}

			return authorizationUrl;

		} catch (TwitterException ex) {

			logger.error("", ex);
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

			logger.error("", ex);
		}

		return null;
	}

	@Override
	public String getValue(final HttpServletRequest request, final String key) {

		try {

			return twitter.getScreenName();

		} catch (TwitterException | IllegalStateException ex) {

			logger.error("", ex);
		}

		return null;
	}

	@Override
	public String getUserResourceUri() {
		return "";
	}

	@Override
	protected String getScope() {
		return Settings.OAuthTwitterScope.getValue();
	}

	@Override
	public String getReturnUri() {
		return Settings.OAuthTwitterReturnUri.getValue();
	}

	@Override
	public String getErrorUri() {
		return Settings.OAuthTwitterErrorUri.getValue();
	}

	@Override
	public PropertyKey<?> getCredentialKey() {
		return StructrApp.key(User.class, "twitterName");
	}

	@Override
	public String getCredential(final HttpServletRequest request) {
		return getValue(request, "screen_name");
	}

	@Override
	protected String getAccessTokenLocationKey() {
		return Settings.OAuthTwitterAccessTokenLocation.getKey();
	}

	@Override
	protected String getAccessTokenLocation() {
		return Settings.OAuthTwitterAccessTokenLocation.getValue("query");
	}
}
