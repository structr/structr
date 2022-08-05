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

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class Auth0OAuth2Client implements OAuth2Client{

    private static final Logger logger = LoggerFactory.getLogger(Auth0OAuth2Client.class);

    private final static String authServer = "auth0";

    private final String authLocation      = Settings.getOrCreateStringSetting("oauth", authServer, "authorization_location").getValue("");
    private final String tokenLocation     = Settings.getOrCreateStringSetting("oauth", authServer, "token_location").getValue("");
    private final String clientId          = Settings.getOrCreateStringSetting("oauth", authServer, "client_id").getValue("");
    private final String clientSecret      = Settings.getOrCreateStringSetting("oauth", authServer, "client_secret").getValue("");
    private final String redirectUri       = Settings.getOrCreateStringSetting("oauth", authServer, "redirect_uri").getValue("");
    private final String returnUri         = Settings.getOrCreateStringSetting("oauth", authServer, "return_uri").getValue("");
    private final String errorUri          = Settings.getOrCreateStringSetting("oauth", authServer, "error_uri").getValue("");
    private final String userDetailsURI    = Settings.getOrCreateStringSetting("oauth", authServer, "user_details_resource_uri").getValue("");
    private final OAuth20Service service;

    public Auth0OAuth2Client() {
        service = new ServiceBuilder(clientId)
                .apiSecret(clientSecret)
                .callback("http://localhost:8082" + redirectUri)
                .defaultScope("openid profile email")
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
        return service.getAuthorizationUrl(state);
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
    public String getReturnURI() {
        return returnUri;
    }

    @Override
    public String getErrorURI() {
        return errorUri;
    }




}
