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
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;

import java.util.Iterator;

/**
 *
 *
 */
public class GitHubAuthClient extends StructrOAuthClient {

	private static final Logger logger = LoggerFactory.getLogger(GitHubAuthClient.class.getName());

	public GitHubAuthClient() {
	}

	@Override
	public String getProviderName () {
		return "github";
	}

	@Override
	public String getScope() {
		return Settings.OAuthGithubScope.getValue();
	}

	@Override
	public String getUserResourceUri() {
		return Settings.OAuthGithubUserDetailsUri.getValue();
	}

	@Override
	public String getReturnUri() {
		return Settings.OAuthGithubReturnUri.getValue();
	}

	@Override
	public String getErrorUri() {
		return Settings.OAuthGithubErrorUri.getValue();
	}

	@Override
	public String getCredential(final HttpServletRequest request) {

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

		if (result instanceof JsonArray) {

			final JsonArray arr = (JsonArray) result;
			final Iterator<JsonElement> iterator = arr.iterator();

			if (iterator.hasNext()) {

				final JsonElement el = iterator.next();
				final String address = el.getAsJsonObject().get("email").getAsString();

				if (isVerboseLoggingEnabled()) {
					logger.info("Got 'email' credential from GitHub: {}", address);
				}

				return address;
			}
		}

		return null;
	}

	@Override
	protected String getAccessTokenLocationKey() {
		return Settings.OAuthGithubAccessTokenLocation.getKey();
	}

	@Override
	protected String getAccessTokenLocation() {
		return Settings.OAuthGithubAccessTokenLocation.getValue("query");
	}
}
