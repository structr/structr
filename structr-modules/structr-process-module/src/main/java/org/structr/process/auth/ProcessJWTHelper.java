/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.process.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Map;

/**
 * JWT helper for process engine access tokens.
 *
 * Creates and validates self-contained, signed JWTs for process-scoped access.
 * Used by the "low" security level to grant sessionless, one-time access to
 * specific process steps via notification links.
 *
 * The JWT carries process context (processInstanceId, taskId, action) as claims,
 * is cryptographically signed (tamper-proof), and has a configurable expiry.
 *
 * Reuses Structr's existing JWT signing infrastructure (JWTSecret setting).
 */
public class ProcessJWTHelper {

	private static final Logger logger = LoggerFactory.getLogger(ProcessJWTHelper.class);

	// Claim keys
	public static final String CLAIM_PROCESS_INSTANCE_ID = "processInstanceId";
	public static final String CLAIM_TASK_ID             = "taskId";
	public static final String CLAIM_ACTION              = "action";
	public static final String CLAIM_SCOPE               = "scope";
	public static final String SCOPE_VALUE               = "process";

	// Default expiry: 48 hours
	public static final int DEFAULT_EXPIRY_MINUTES = 48 * 60;

	/**
	 * Creates a signed JWT for process-scoped access.
	 *
	 * @param processInstanceId  UUID of the ProcessInstance
	 * @param taskId             UUID of the TaskInstance (may be null for view-only access)
	 * @param action             the allowed action (e.g. "review", "view")
	 * @param expiryMinutes      token lifetime in minutes (0 or negative = use default)
	 * @return the signed JWT string
	 * @throws FrameworkException if the JWT cannot be created
	 */
	public static String createProcessToken(final String processInstanceId, final String taskId, final String action, final int expiryMinutes) throws FrameworkException {

		final Algorithm algorithm = getAlgorithm();
		final Calendar expiration = Calendar.getInstance();

		expiration.add(Calendar.MINUTE, expiryMinutes > 0 ? expiryMinutes : DEFAULT_EXPIRY_MINUTES);

		final String issuer = Settings.JWTIssuer.getValue();

		try {

			return JWT.create()
				.withIssuer(issuer)
				.withExpiresAt(expiration.getTime())
				.withClaim(CLAIM_SCOPE, SCOPE_VALUE)
				.withClaim(CLAIM_PROCESS_INSTANCE_ID, processInstanceId)
				.withClaim(CLAIM_TASK_ID, taskId)
				.withClaim(CLAIM_ACTION, action)
				.sign(algorithm);

		} catch (JWTCreationException ex) {

			throw new FrameworkException(500, "Failed to create process access token: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Creates a signed JWT with default expiry (48 hours).
	 */
	public static String createProcessToken(final String processInstanceId, final String taskId, final String action) throws FrameworkException {

		return createProcessToken(processInstanceId, taskId, action, DEFAULT_EXPIRY_MINUTES);
	}

	/**
	 * Validates a process access token and returns its claims.
	 *
	 * @param token  the JWT string to validate
	 * @return the claims map if valid, null if invalid or expired
	 */
	public static Map<String, Claim> validateProcessToken(final String token) {

		try {

			final Algorithm algorithm = getAlgorithm();
			final String issuer = Settings.JWTIssuer.getValue();
			final JWTVerifier verifier = JWT.require(algorithm)
				.withIssuer(issuer)
				.withClaim(CLAIM_SCOPE, SCOPE_VALUE)
				.build();

			final DecodedJWT decoded = verifier.verify(token);

			return decoded.getClaims();

		} catch (JWTVerificationException ex) {

			logger.debug("Invalid process access token: {}", ex.getMessage());

			return null;

		} catch (FrameworkException ex) {

			logger.warn("Error validating process access token: {}", ex.getMessage());

			return null;
		}
	}

	/**
	 * Extracts the processInstanceId claim from a validated claims map.
	 */
	public static String getProcessInstanceId(final Map<String, Claim> claims) {

		final Claim claim = claims.get(CLAIM_PROCESS_INSTANCE_ID);

		return claim != null && !claim.isNull() ? claim.asString() : null;
	}

	/**
	 * Extracts the taskId claim from a validated claims map.
	 */
	public static String getTaskId(final Map<String, Claim> claims) {

		final Claim claim = claims.get(CLAIM_TASK_ID);

		return claim != null && !claim.isNull() ? claim.asString() : null;
	}

	/**
	 * Extracts the action claim from a validated claims map.
	 */
	public static String getAction(final Map<String, Claim> claims) {

		final Claim claim = claims.get(CLAIM_ACTION);

		return claim != null && !claim.isNull() ? claim.asString() : null;
	}

	// --- private helpers ---

	private static Algorithm getAlgorithm() throws FrameworkException {

		final String jwtSecretType = Settings.JWTSecretType.getValue();

		switch (jwtSecretType) {

			default:
			case "secret":
				// generate and persist a strong secret on first use if none is configured
				// (a present-but-weak secret is left untouched so the check below still rejects it)
				final String secret = Settings.getOrGenerateJWTSecret();
				if (secret == null || secret.length() < 32) {

					throw new FrameworkException(500, "JWT secret is not configured or too weak (must be at least 32 characters). Configure " + Settings.JWTSecret.getKey());
				}

				return Algorithm.HMAC256(secret.getBytes(StandardCharsets.UTF_8));

			case "keypair":
				// For keypair mode, we would need to load the RSA keys.
				// For now, fall through to an error since process tokens
				// are primarily designed for HMAC256 shared secret mode.
				throw new FrameworkException(500, "Process access tokens currently require JWT secret type 'secret'. Keypair mode is not yet supported for process tokens.");
		}
	}
}
