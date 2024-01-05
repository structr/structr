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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Person;
import org.structr.core.entity.Principal;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 *
 *
 */
public class LinkedInAuthClient extends StructrOAuthClient {

	private static final Logger logger = LoggerFactory.getLogger(LinkedInAuthClient.class.getName());

	public LinkedInAuthClient() {}

	private Map<String, Object> userProfileInfo = null;

	@Override
	public String getProviderName () {
		return "linkedin";
	}

	@Override
	protected String getAccessTokenParameterKey() {
		return "oauth2_access_token";
	}

	@Override
	public String getScope() {
		return Settings.OAuthLinkedInScope.getValue();
	}

	@Override
	public String getUserResourceUri() {
		return Settings.OAuthLinkedInUserDetailsUri.getValue();
	}

	@Override
	public String getReturnUri() {
		return Settings.OAuthLinkedInReturnUri.getValue();
	}

	@Override
	public String getErrorUri() {
		return Settings.OAuthLinkedInErrorUri.getValue();
	}

	@Override
	protected String getState() {
		return UUID.randomUUID().toString();
	}

	@Override
	public String getCredential(final HttpServletRequest request) {

		OAuthResourceResponse userProfileResponse = getUserProfileResponse(request);

		if (userProfileResponse != null) {

			userProfileInfo = JSONUtils.parseJSON(userProfileResponse.getBody());
		}

		final OAuthResourceResponse userResponse = getUserResponse(request);

		if (userResponse == null) {

			return null;
		}

		final String body = userResponse.getBody();

		if (isVerboseLoggingEnabled()) {
			logger.info("User response body: {}", body);
		}

		final JsonParser parser = new JsonParser();
		final JsonElement result = parser.parse(body);

		final JsonArray elementsArray = result.getAsJsonObject().getAsJsonArray("elements");

		if (elementsArray != null) {

			final Iterator<JsonElement> iterator = elementsArray.iterator();

			if (iterator.hasNext()) {

				final JsonElement el = iterator.next();

				if (el.getAsJsonObject().get("handle~") != null) {

					final String address = el.getAsJsonObject().get("handle~").getAsJsonObject().get("emailAddress").getAsString();

					if (isVerboseLoggingEnabled()) {
						logger.info("Got 'email' credential from GitHub: {}", address);
					}

					return address;
				}
			}
		}

		return null;
	}

	@Override
	public void initializeUser(final Principal user) throws FrameworkException {

		// initialize user from user profile response
		if (userProfileInfo != null) {

			final String firstName = (String) userProfileInfo.get("localizedFirstName");
			final String lastName  = (String) userProfileInfo.get("localizedLastName");
			final String name      = "" + firstName + " " + lastName;

			user.setProperty(Principal.name, name);
			user.setProperty(StructrApp.key(Person.class, "firstName"), firstName);
			user.setProperty(StructrApp.key(Person.class, "lastName"), firstName);
		}
	}

	private OAuthResourceResponse getUserProfileResponse(final HttpServletRequest request) {

		try {

			String accessToken = getAccessToken(request);

			if (accessToken != null) {

				final String accessTokenParameterKey = this.getAccessTokenParameterKey();

				OAuthClientRequest clientReq = new OAuthBearerClientRequest(Settings.OAuthLinkedInUserProfileUri.getValue()) {

					@Override
					public OAuthBearerClientRequest setAccessToken(String accessToken) {
						this.parameters.put(accessTokenParameterKey, accessToken);
						return this;
					}
				}
					.setAccessToken(accessToken)
					.buildQueryMessage();

				if (isVerboseLoggingEnabled()) {
					logger.info("User profile request: {}", clientReq.getLocationUri());
				}

				OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

				OAuthResourceResponse userProfileResponse = oAuthClient.resource(clientReq, "GET", OAuthResourceResponse.class);

				if (isVerboseLoggingEnabled()) {
					logger.info("User profile response: {}", userProfileResponse);
				}

				return userProfileResponse;
			}

		} catch (Throwable t) {

			logger.error("Could not get user profile response", t);
		}

		return null;
	}

	@Override
	protected String getAccessTokenLocationKey() {
		return Settings.OAuthLinkedInAccessTokenLocation.getKey();
	}

	@Override
	protected String getAccessTokenLocation() {
		return Settings.OAuthLinkedInAccessTokenLocation.getValue("query");
	}
}
