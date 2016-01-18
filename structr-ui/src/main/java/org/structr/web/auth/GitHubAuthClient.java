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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.structr.core.app.StructrApp;

/**
 *
 *
 */
public class GitHubAuthClient extends StructrOAuthClient {

	private static final Logger logger = Logger.getLogger(GitHubAuthClient.class.getName());

	public GitHubAuthClient() {
	}

	;

	@Override
	public String getScope() {

		return "user:email";

	}

	@Override
	public ResponseFormat getResponseFormat() {

		return ResponseFormat.json;

	}

	@Override
	public String getUserResourceUri() {

		return StructrApp.getConfigurationValue("oauth.github.user_details_resource_uri", "");

	}

	@Override
	public String getReturnUri() {

		return StructrApp.getConfigurationValue("oauth.github.return_uri", "/");

	}

	@Override
	public String getErrorUri() {

		return StructrApp.getConfigurationValue("oauth.github.error_uri", "/");

	}

	@Override
	public String getCredential(final HttpServletRequest request) {

		OAuthResourceResponse userResponse = getUserResponse(request);

		if (userResponse == null) {

			return null;

		}

		String body = userResponse.getBody();
		logger.log(Level.FINE, "User response body: {0}", body);

		final JsonParser parser = new JsonParser();
		final JsonElement result = parser.parse(body);

		if (result instanceof JsonArray) {

			final JsonArray arr = (JsonArray) result;

			if (arr.iterator().hasNext()) {

				final JsonElement el = arr.iterator().next();

				final String address = el.getAsJsonObject().get("email").getAsString();
				logger.log(Level.INFO, "Got 'email' credential from GitHub: {0}", address);
				return address;
			}

		}

		return null;

	}

}
