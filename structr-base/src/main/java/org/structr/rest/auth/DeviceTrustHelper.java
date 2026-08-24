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
package org.structr.rest.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DeviceTrustHelper {

	private static final Logger logger = LoggerFactory.getLogger(DeviceTrustHelper.class.getName());

	private static final String FINGERPRINT_HASH = "fingerprintHash";
	private static final String SECRET_HASH      = "secretHash";

	public static final String DEVICE_TRUST_REQUESTED_STRING = "trustDevice";

	private static final UserAgentAnalyzer UAA = UserAgentAnalyzer.newBuilder()
														 .withField(UserAgent.AGENT_NAME)
														 .withField(UserAgent.AGENT_VERSION_MAJOR)
														 .withField(UserAgent.OPERATING_SYSTEM_NAME)
														 .withField(UserAgent.OPERATING_SYSTEM_VERSION_MAJOR)
														 .withField(UserAgent.DEVICE_CLASS)
														 .withCache(1000)
														 .build();

	public static String generateDeviceTrustToken(final String userAgentString, final String userDeviceTrustSecret) {

		final String signingSecret = Settings.getOrGenerateDeviceTrustSecret();
		final Algorithm algorithm  = Algorithm.HMAC256(signingSecret);

		final String fingerprintHash = DeviceTrustHelper.generateFingerprintHash(userAgentString);
		final String secretHash      = sha256Hex(userDeviceTrustSecret);

		final int durationInDays = Settings.TwoFactorDeviceTrustDuration.getValue();
		final Instant expiresAt  = Instant.now().plus(durationInDays, ChronoUnit.DAYS);

		return JWT.create()
					   .withClaim("fingerprintHash", fingerprintHash)
					   .withClaim("secretHash", secretHash)
					   .withExpiresAt(Date.from(expiresAt))
					   .withIssuedAt(Date.from(Instant.now()))
					   .sign(algorithm);
	}

	public static boolean isValidDeviceTrustToken(final String token, final String userAgentString, final String userDeviceTrustSecret) {

		if (token == null || token.isBlank()) {
			return false;
		}

		try {

			final String signingSecret = Settings.getOrGenerateDeviceTrustSecret();
			final Algorithm algorithm  = Algorithm.HMAC256(signingSecret);
			final JWTVerifier verifier = JWT.require(algorithm).build();

			final DecodedJWT jwt = verifier.verify(token);

			final String fingerprintHashClaim = jwt.getClaim(FINGERPRINT_HASH).asString();
			final String secretHashClaim = jwt.getClaim(SECRET_HASH).asString();

			if (fingerprintHashClaim == null || secretHashClaim == null) {
				return false;
			}

			boolean fingerprintMatches = constantTimeEquals(fingerprintHashClaim, generateFingerprintHash(userAgentString));
			boolean secretMatches      = constantTimeEquals(secretHashClaim, sha256Hex(userDeviceTrustSecret));

			return fingerprintMatches && secretMatches;

		} catch (JWTVerificationException e) {

			// Covers: bad signature, expired token, malformed token
			return false;
		}
	}

	/**
	 * Generates a hash representing a normalized, coarse browser/OS fingerprint,
	 * derived from the User-Agent header (and client hints, if present).
	 *
	 * @param userAgentHeader raw "User-Agent" request header
	 * @return                a hex-encoded SHA-256 hash of the normalized fingerprint
	 */
	public static String generateFingerprintHash(final String userAgentHeader) {

		final String normalized = normalizeUserAgent(userAgentHeader);
		return sha256Hex(normalized);
	}



	// ----- private methods -----

	private static String normalizeUserAgent(final String userAgentHeader) {

		if (userAgentHeader == null || userAgentHeader.isBlank()) {
			return "unknown";
		}

		final UserAgent agent = UAA.parse(userAgentHeader);

		final String browserName    = agent.getValue(UserAgent.AGENT_NAME);
		final String browserVersion = agent.getValue(UserAgent.AGENT_VERSION_MAJOR);
		final String osName         = agent.getValue(UserAgent.OPERATING_SYSTEM_NAME);
		final String osVersion      = agent.getValue(UserAgent.OPERATING_SYSTEM_VERSION_MAJOR);
		final String deviceClass    = agent.getValue(UserAgent.DEVICE_CLASS);

		// Pipe-delimited, coarse fields only - no full version strings
		return String.join("|",
				safe(browserName),
				safe(browserVersion),
				safe(osName),
				safe(osVersion),
				safe(deviceClass)
		);
	}

	private static String safe(final String value) {
		return (value == null) ? "?" : value;
	}

	private static String sha256Hex(final String input) {

		try {

			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			final byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

			final StringBuilder hex = new StringBuilder();
			for (byte b : hash) {
				hex.append(String.format("%02x", b));
			}

			return hex.toString();

		} catch (NoSuchAlgorithmException e) {
			// SHA-256 is guaranteed available on every JVM - this should never happen
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}

	private static boolean constantTimeEquals(final String a, final String b) {

		if (a == null || b == null) {
			return false;
		}

		return java.security.MessageDigest.isEqual(
				a.getBytes(StandardCharsets.UTF_8),
				b.getBytes(StandardCharsets.UTF_8)
		);
	}
}
