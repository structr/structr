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

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.web.auth.AbstractOAuth2Client;
import org.structr.web.auth.OAuth2ProviderRegistry;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GithubOAuthClient extends AbstractOAuth2Client {

	private static final Logger logger = LoggerFactory.getLogger(GithubOAuthClient.class);
	private static final String AUTH_SERVER = "github";

	public GithubOAuthClient(final HttpServletRequest request, OAuth2ProviderRegistry.ProviderConfig providerConfig) {

		super(request, AUTH_SERVER, providerConfig.getApi(), providerConfig);
	}

	@Override
	public String getClientCredentials(final OAuth2AccessToken accessToken) {

		try {

			final OAuthRequest userDetailsRequest = new OAuthRequest(Verb.GET, userDetailsURI);
			service.signRequest(accessToken, userDetailsRequest);

			final OAuthRequest userEmailsRequest = new OAuthRequest(Verb.GET, userDetailsURI + "/emails");
			service.signRequest(accessToken, userEmailsRequest);

			String userResponseBody;
			String emailResponseBody;

			try (Response response = service.execute(userDetailsRequest)) {

				if (!response.isSuccessful() && Settings.OAuthVerboseLogging.getValue(false)) {

					logger.error("User details request to {} failed: {} - {}", provider, response.getCode(), response.getMessage());
				}

				userResponseBody = response.getBody();
			}

			try (Response response = service.execute(userEmailsRequest)) {

				if (!response.isSuccessful() && Settings.OAuthVerboseLogging.getValue(false)) {

					logger.error("User emails request to {} failed: {} - {}", provider, response.getCode(), response.getMessage());
				}

				emailResponseBody = response.getBody();
			}

			final Set<String> verifiedAddresses = getVerifiedEmailAddresses(emailResponseBody);
			final String defaultEmailAddress    = getDefaultUserEmailAddress(emailResponseBody);
			final String credential             = parseUserCredentials(userResponseBody, accessToken, defaultEmailAddress);

			/* Ticket 1593, the GitHub shape of it: the base class refuses an address the provider reports
			   as unverified, but it looks for the OIDC claim email_verified, and GitHub does not send one -
			   it reports verification per address on /user/emails, which this client fetches anyway. Both
			   ways an address gets here are covered: the public profile address from /user, and the primary
			   address picked out of that same list. An address GitHub does not vouch for is an address
			   anybody with a GitHub account can put in their profile. */
			if (!isVerifiedAddress(credential, verifiedAddresses)) {

				logger.warn("Refusing OAuth login for provider {}: the {} it returned is not among the addresses GitHub lists as verified for that account.", provider, getCredentialKey());

				return null;
			}

			return credential;

		} catch (Exception e) {

			if (Settings.OAuthVerboseLogging.getValue(false)) {

				logger.error("Failed to get client credentials from {}: {}", provider, e.getMessage());
			}

			logger.debug("Client credentials error details", e);
		}

		return null;
	}

	/**
	 * Parses the user details response and extracts credentials.
	 *
	 * @param responseBody The response body from the user details endpoint
	 * @param accessToken The OAuth access token
	 * @param defaultEmailAddress The default email address from the /emails endpoint
	 * @return The credential value (typically email)
	 */
	protected String parseUserCredentials(final String responseBody, final OAuth2AccessToken accessToken, final String defaultEmailAddress) {

		try {

			final Gson gson = new Gson();
			final Map<String, Object> params = gson.fromJson(responseBody, Map.class);

			// Add decoded access token claims to user info
			final Map<String, Object> accessTokenClaims = decodeAccessTokenClaims(accessToken);
			if (accessTokenClaims != null) {

				params.put("accessTokenClaims", accessTokenClaims);
			}

			// Store full user info for later use
			this.userInfo = params;

			final Object credentialValue = params.get(getCredentialKey());

			// email might be null, if user has set it to private
			if (credentialValue == null) {

				this.userInfo.put(getCredentialKey(), defaultEmailAddress);

				return defaultEmailAddress;
			}

			return credentialValue.toString();

		} catch (Exception e) {

			if (Settings.OAuthVerboseLogging.getValue(false)) {

				logger.error("Failed to parse user credentials from {}: {}", provider, e.getMessage());
			}

			logger.debug("Credential parsing error details", e);
		}

		return null;
	}

	/**
	 * Whether the address that came out of the two responses is one GitHub vouches for.
	 *
	 * <p>True for a credential of null - there is nothing to accept or refuse then, and the caller
	 * answers null either way - and true when the verified set is null, which is the "no information"
	 * case described at {@link #getVerifiedEmailAddresses}.
	 */
	protected boolean isVerifiedAddress(final String credential, final Set<String> verifiedAddresses) {

		if (credential == null || verifiedAddresses == null) {

			return true;
		}

		return verifiedAddresses.contains(credential.toLowerCase());
	}

	/**
	 * The addresses GitHub lists as verified for this account, lower-cased, or null when the
	 * {@code /user/emails} response could not be read as a list at all.
	 *
	 * <p>Null rather than an empty set, and the difference matters: a token whose scope does not include
	 * {@code user:email} gets an error object from that endpoint, and that is the absence of information
	 * rather than a statement that nothing is verified. Refusing there would lock out an installation
	 * that has been running on a narrower scope. Same rule as
	 * {@link org.structr.web.auth.AbstractOAuth2Client#isCredentialVerified}: only an explicit no is
	 * refused. An empty set, on the other hand, is an answer - the account has no verified address.
	 */
	protected Set<String> getVerifiedEmailAddresses(final String emailResponse) {

		try {

			final List<Map<String, Object>> entries = new Gson().fromJson(emailResponse, List.class);
			if (entries == null) {

				return null;
			}

			final Set<String> verified = new LinkedHashSet<>();

			for (final Map<String, Object> entry : entries) {

				final Object address = entry.get(getCredentialKey());
				if (address != null && Boolean.TRUE.equals(entry.get("verified"))) {

					verified.add(address.toString().toLowerCase());
				}
			}

			return verified;

		} catch (Exception e) {

			if (Settings.OAuthVerboseLogging.getValue(false)) {

				logger.error("Failed to read verified addresses from {}: {}", provider, e.getMessage());
			}

			logger.debug("Verified address parsing error details", e);
		}

		return null;
	}

	protected String getDefaultUserEmailAddress(final String emailResponse) {

		try {

			final Gson gson = new Gson();
			final List<Map<String, Object>> params = gson.fromJson(emailResponse, List.class);

			for (Map<String, Object> param : params) {

				if (param.get(getCredentialKey()) != null && (boolean) param.get("primary")) {

					return param.get(getCredentialKey()).toString();
				}
			}

		} catch (Exception e) {

			if (Settings.OAuthVerboseLogging.getValue(false)) {

				logger.error("Failed to parse user credentials from {}: {}", provider, e.getMessage());
			}

			logger.debug("Credential parsing error details", e);
		}

		return null;
	}
}