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
package org.structr.test.rest.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.structr.api.config.Settings;
import org.structr.rest.auth.JWTHelper;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Covers emission and verification of the JWT {@code aud} claim driven by
 * the {@code security.jwt.audience} setting. Exercises the shared
 * verifier / emitter helpers rather than the full login flow, which
 * would require a running Structr instance.
 */
public class JWTAudienceTest {

	private static final String SECRET = "this-is-a-32-char-test-secret-0123456789";
	private static final Algorithm ALG = Algorithm.HMAC256(SECRET.getBytes(StandardCharsets.UTF_8));

	private String previousAudience;
	private String previousIssuer;

	@BeforeMethod
	public void setUp() {

		previousAudience = Settings.JWTAudience.getValue();
		previousIssuer   = Settings.JWTIssuer.getValue();

		// Fix the issuer so we're only varying audience behaviour.
		Settings.JWTIssuer.setValue("structr-test");
	}

	@AfterMethod
	public void tearDown() {

		Settings.JWTAudience.setValue(previousAudience);
		Settings.JWTIssuer.setValue(previousIssuer);
	}

	// ---- emit side ---------------------------------------------------------

	@Test
	public void applyAudience_omitsClaimWhenSettingIsEmpty() {

		Settings.JWTAudience.setValue("");

		final String token = JWTHelper.applyAudience(JWT.create().withIssuer("structr-test")).sign(ALG);
		final DecodedJWT decoded = JWT.decode(token);

		// auth0-jwt returns an empty list (not null) when the claim is absent;
		// both signal "no audience was set".
		assertTrue("Unexpected aud: " + decoded.getAudience(),
			decoded.getAudience() == null || decoded.getAudience().isEmpty());
	}

	@Test
	public void applyAudience_emitsSingleValue() {

		Settings.JWTAudience.setValue("https://structr.example.com");

		final String token = JWTHelper.applyAudience(JWT.create().withIssuer("structr-test")).sign(ALG);
		final DecodedJWT decoded = JWT.decode(token);

		assertEquals(List.of("https://structr.example.com"), decoded.getAudience());
	}

	@Test
	public void applyAudience_emitsMultiValueAndTrimsWhitespace() {

		Settings.JWTAudience.setValue("  a , b ,, c ");

		final String token = JWTHelper.applyAudience(JWT.create().withIssuer("structr-test")).sign(ALG);
		final DecodedJWT decoded = JWT.decode(token);

		assertEquals(List.of("a", "b", "c"), decoded.getAudience());
	}

	// ---- verify side -------------------------------------------------------

	@Test
	public void verifier_acceptsTokenWithoutAudWhenAudienceDisabled() {

		Settings.JWTAudience.setValue("");

		final String token = JWT.create().withIssuer("structr-test").withExpiresAt(futureDate()).sign(ALG);
		final JWTVerifier verifier = JWTHelper.buildVerifier(ALG);

		// Should not throw.
		verifier.verify(token);
	}

	@Test
	public void verifier_rejectsTokenWithoutAudWhenAudienceRequired() {

		Settings.JWTAudience.setValue("a");

		final String token = JWT.create().withIssuer("structr-test").withExpiresAt(futureDate()).sign(ALG);
		final JWTVerifier verifier = JWTHelper.buildVerifier(ALG);

		try {
			verifier.verify(token);
			fail("Expected verification to fail because the token has no aud claim");
		} catch (final JWTVerificationException expected) {
			// ok
		}
	}

	@Test
	public void verifier_acceptsMatchingAud() {

		Settings.JWTAudience.setValue("structr-prod");

		final String token = JWT.create()
			.withIssuer("structr-test")
			.withAudience("structr-prod")
			.withExpiresAt(futureDate())
			.sign(ALG);

		JWTHelper.buildVerifier(ALG).verify(token);
	}

	@Test
	public void verifier_rejectsMismatchingAud() {

		Settings.JWTAudience.setValue("structr-prod");

		final String token = JWT.create()
			.withIssuer("structr-test")
			.withAudience("structr-staging")
			.withExpiresAt(futureDate())
			.sign(ALG);

		try {
			JWTHelper.buildVerifier(ALG).verify(token);
			fail("Expected verification to fail for mismatched aud");
		} catch (final JWTVerificationException expected) {
			// ok
		}
	}

	@Test
	public void verifier_acceptsWhenTokenAudIntersectsConfiguredList() {

		// Multi-value configured list — auth0-jwt accepts the token if at
		// least one configured audience appears in the token's aud claim.
		Settings.JWTAudience.setValue("structr-prod, structr-staging");

		final String token = JWT.create()
			.withIssuer("structr-test")
			.withAudience("structr-staging")
			.withExpiresAt(futureDate())
			.sign(ALG);

		JWTHelper.buildVerifier(ALG).verify(token);
	}

	private static Date futureDate() {
		return new Date(System.currentTimeMillis() + 60_000);
	}
}
