/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.web.auth.provider;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.web.auth.AbstractOAuth2Client;

import java.util.HashMap;

public class Auth0AuthClient extends AbstractOAuth2Client {

	private static final Logger logger = LoggerFactory.getLogger(Auth0AuthClient.class);

	private final static String authServer = "auth0";
	protected String audience = null;

	public Auth0AuthClient(final HttpServletRequest request) {

		super(request, authServer);

		this.audience = Settings.getOrCreateStringSetting("oauth", provider, "audience").getValue("");

		service = new ServiceBuilder(clientId)
			.apiSecret(clientSecret)
			.callback(redirectUri)
			.defaultScope(scope)
			.build(new DefaultApi20() {

				@Override
				public String getAccessTokenEndpoint() {
					return tokenLocation;
				}

				@Override
				protected String getAuthorizationBaseUrl() {
					return authLocation;
				}
			});
	}

	@Override
	public String getAuthorizationURL(final String state) {
		HashMap parameters = new HashMap<String, String>();
		parameters.put("audience", this.audience);

		return service.createAuthorizationUrlBuilder().state(state).additionalParams(parameters).build();
	}
}
