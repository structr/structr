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
package org.structr.core.auth;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Tokens that work once and for a short time, handed to a browser as a request parameter. An admin creates one for a
 * purpose, here to open a session as another user for testing; the authenticator redeems it on whatever URL carries
 * it. They are kept in memory only, so none survives a restart.
 */
public class OneTimeTokens {

	public static final String PARAMETER            = "_structrOneTimeToken";
	public static final String PURPOSE_TEST_SESSION = "test-session";
	public static final String REDEEMED_ATTRIBUTE   = "org.structr.oneTimeToken.redeemed";
	public static final String SESSION_ATTRIBUTE    = "structrTestSessionOpenedBy";
	public static final int TEST_SESSION_TIMEOUT_SECONDS = 3600;

	private static final Map<String, Token> tokens = new ConcurrentHashMap<>();
	private static final SecureRandom random       = new SecureRandom();
	private static volatile long validityMillis    = 120_000L;

	public record Token(String purpose, String userId, String createdById, String createdByName, long created) {

		boolean isExpired() {
			return System.currentTimeMillis() - created > validityMillis;
		}
	}

	public static String create(final String purpose, final String userId, final String createdById, final String createdByName) {

		tokens.values().removeIf(Token::isExpired);

		final byte[] bytes = new byte[32];

		random.nextBytes(bytes);

		final String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

		tokens.put(value, new Token(purpose, userId, createdById, createdByName, System.currentTimeMillis()));

		return value;
	}

	/**
	 * Returns what the token stands for and forgets it, or null for a token that is unknown, used or too old.
	 */
	public static Token redeem(final String value) {

		if (value == null) {
			return null;
		}

		final Token token = tokens.remove(value);

		return (token == null || token.isExpired()) ? null : token;
	}

	/**
	 * The address of a request without the parameter of the token, for the redirect after it was redeemed.
	 */
	public static String urlWithoutToken(final String uri, final String queryString) {

		if (queryString == null || queryString.isEmpty()) {
			return uri;
		}

		final String rest = Arrays.stream(queryString.split("&")).filter(pair -> !pair.equals(PARAMETER) && !pair.startsWith(PARAMETER + "=")).collect(Collectors.joining("&"));

		return rest.isEmpty() ? uri : uri + "?" + rest;
	}

	public static long getValidityMillis() {
		return validityMillis;
	}

	// for tests of the expiry
	public static void setValidityMillis(final long millis) {
		validityMillis = millis;
	}
}
