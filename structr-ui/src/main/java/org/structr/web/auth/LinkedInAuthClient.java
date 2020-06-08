/*
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Iterator;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;

/**
 *
 *
 */
public class LinkedInAuthClient extends StructrOAuthClient {

	private static final Logger logger = LoggerFactory.getLogger(LinkedInAuthClient.class.getName());

	public LinkedInAuthClient() {}

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

		OAuthResourceResponse userResponse = getUserResponse(request);

		if (userResponse == null) {

			return null;

		}

		String body = userResponse.getBody();
		logger.debug("User response body: {}", body);
		
		final JsonParser parser = new JsonParser();
		final JsonElement result = parser.parse(body);

		final JsonArray elementsArray = result.getAsJsonObject().getAsJsonArray("elements");
		
		if (elementsArray != null) {

			final Iterator<JsonElement> iterator = elementsArray.iterator();

			if (iterator.hasNext()) {

				final JsonElement el = iterator.next();
				
				if (el.getAsJsonObject().get("handle~") != null) {
				
					final String address = el.getAsJsonObject().get("handle~").getAsJsonObject().get("emailAddress").getAsString();
					logger.info("Got 'email' credential from GitHub: {}", address);

					return address;
				}
			}
		}

		return null;
	}
}
