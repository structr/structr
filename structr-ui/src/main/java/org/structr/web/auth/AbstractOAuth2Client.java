/*
 * Copyright (C) 2010-2022 Structr GmbH
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

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.schema.action.EvaluationHints;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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

	protected OAuth20Service service;

	public AbstractOAuth2Client(final String provider) {
		this.provider = provider;
		authLocation = Settings.getOrCreateStringSetting("oauth", provider, "authorization_location").getValue("");
		tokenLocation = Settings.getOrCreateStringSetting("oauth", provider, "token_location").getValue("");
		clientId = Settings.getOrCreateStringSetting("oauth", provider, "client_id").getValue("");
		clientSecret = Settings.getOrCreateStringSetting("oauth", provider, "client_secret").getValue("");
		redirectUri = Settings.getOrCreateStringSetting("oauth", provider, "redirect_uri").getValue("");
		returnUri = Settings.getOrCreateStringSetting("oauth", provider, "return_uri").getValue("");
		errorUri = Settings.getOrCreateStringSetting("oauth", provider, "error_uri").getValue("");
		logoutUri = Settings.getOrCreateStringSetting("oauth", provider, "logout_uri").getValue("");
		userDetailsURI = Settings.getOrCreateStringSetting("oauth", provider, "user_details_resource_uri").getValue("");
	}

	@Override
	public String getAuthorizationURL(final String state) {

		return service.getAuthorizationUrl(state);
	}

	@Override
	public String getCredentialKey() {

		return "email";
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
	public OAuth2AccessToken getAccessToken(String authorizationReplyCode) {

		try {

			return service.getAccessToken(authorizationReplyCode);
		} catch (IOException | InterruptedException | ExecutionException e) {

			logger.error("Could not get accessToken", e);
		}

		return null;
	}

	@Override
	public String getClientCredentials(final OAuth2AccessToken accessToken) {
		final OAuthRequest request = new OAuthRequest(Verb.GET, userDetailsURI);

		service.signRequest(accessToken, request);

		try (Response response = service.execute(request)) {

			final String rawResponse = response.getBody();

			Gson gson = new Gson();
			Map<String, Object> params = gson.fromJson(rawResponse, Map.class);

			if (params.get(getCredentialKey()) != null) {

				return params.get(getCredentialKey()).toString();
			}

			return null;
		} catch (IOException | InterruptedException | ExecutionException e) {

			logger.error("Could not get perform client credential request", e);
		}

		return null;
	}

	@Override
	public void invokeOnLoginMethod(Principal user) throws FrameworkException {

		final Map<String, Object> methodParamerers = new LinkedHashMap<>();
		methodParamerers.put("provider", this.provider);

		user.invokeMethod(user.getSecurityContext(), "onOAuthLogin", methodParamerers, false, new EvaluationHints());
	}

	@Override
	public void initializeAutoCreatedUser(Principal user) {

	}
}
