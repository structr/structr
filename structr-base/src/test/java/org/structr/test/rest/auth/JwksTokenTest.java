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
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.sun.net.httpserver.HttpServer;
import io.restassured.RestAssured;
import org.structr.api.config.Settings;
import org.structr.common.RequestHeaders;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1593, the JWKS half: in {@code security.jwt.secrettype=jwks} the access token is issued by an
 * external identity provider, and Structr checked two things about it - the signature, and the issuer.
 *
 * <p>Neither says the token was meant for this installation. Every token that provider ever issued
 * carries the same issuer and verifies against the same keys, including the tokens it issues to other
 * applications in the same tenant and the tokens it issues to anybody who signs up there. The claim that
 * says "this one was meant for you" is {@code aud}, and it was not looked at.
 *
 * <p>What made that worth an exploit rather than a hardening note is the shortcut in
 * getPrincipalForTokenClaims: a token whose {@code instance} claim matches this installation's name is
 * resolved to the user named by its {@code uuid} claim, with no e-mail address and nothing else
 * involved. Neither value protects anything: application.instance.name is empty unless an administrator
 * sets it, and is shown in the UI once they do, and the uuid of an admin user is no secret either. So
 * one token from the external provider - with two claims an attacker controls - was a login as that
 * admin.
 *
 * <p>The test runs a JWKS provider of its own so the signature is genuinely valid throughout: every
 * refusal below is a refusal on the merits of the claims, not a failed signature.
 */
public class JwksTokenTest extends StructrUiTest {

	private static final String ISSUER   = "https://idp.example.com/";
	private static final String AUDIENCE = "structr-test-audience";
	private static final String KEY_ID   = "test-key-1";

	private HttpServer jwksServer;
	private RSAPublicKey publicKey;
	private RSAPrivateKey privateKey;
	private String adminUuid;

	@AfterClass(alwaysRun = true)
	@Override
	public void teardown() throws Exception {

		super.teardown();

		if (jwksServer != null) {

			jwksServer.stop(0);
		}
	}

	/**
	 * The exploit: a token with a perfectly correct audience - one the provider would hand to any of its
	 * users for this very application - carrying the two claims that used to be taken as an identity
	 * statement. The audience check of the next test is not what stops this one; only refusing the
	 * instance/uuid shortcut for an externally issued token is.
	 */
	@Test
	public void test01AnExternalTokenCannotClaimAUuid() {

		configureJWKS(AUDIENCE);

		final String token = token(builder -> builder
			.withAudience(AUDIENCE)
			.withClaim("instance", Settings.InstanceName.getValue())
			.withClaim("uuid", adminUuid));

		expectRefused(token, "a token from the external provider was accepted as the admin user it named in its uuid claim");
	}

	/**
	 * The check that was missing: without an audience the token says nothing about who it was issued for.
	 */
	@Test
	public void test02ATokenForSomebodyElseIsRefused() {

		configureJWKS(AUDIENCE);

		final String tokenWithoutAudience = token(builder -> builder
			.withClaim("instance", Settings.InstanceName.getValue())
			.withClaim("uuid", adminUuid));

		expectRefused(tokenWithoutAudience, "a token with no audience at all was accepted");

		final String tokenForAnotherApplication = token(builder -> builder
			.withAudience("some-other-application")
			.withClaim("instance", Settings.InstanceName.getValue())
			.withClaim("uuid", adminUuid));

		expectRefused(tokenForAnotherApplication, "a token issued for a different application was accepted");
	}

	/**
	 * And with no audience configured there is nothing to check tokens against, so there is no honest way
	 * to accept one.
	 */
	@Test
	public void test03NoTokenIsAcceptedWhileNoAudienceIsConfigured() {

		configureJWKS("");

		final String token = token(builder -> builder
			.withAudience(AUDIENCE)
			.withClaim("instance", Settings.InstanceName.getValue())
			.withClaim("uuid", adminUuid));

		expectRefused(token, "tokens from the external provider are accepted although no audience is configured to check them against");
	}

	// ----- private methods -----

	private interface ClaimWriter {

		JWTCreator.Builder write(final JWTCreator.Builder builder);
	}

	/**
	 * A refusal, whichever shape it takes: a token whose claims resolve to nobody leaves the request
	 * anonymous, while one that fails verification is reported as an error by the servlet. The two arrive
	 * with different status codes and neither is pinned down here - what is asserted is the thing that
	 * matters, that the request is not served as the admin user the token named.
	 */
	private void expectRefused(final String token, final String message) {

		RestAssured.basePath = "/structr/rest";

		final int statusCode = RestAssured
			.given()
				.header(RequestHeaders.Authorization.getName(), "Bearer " + token)
			.when()
				.get("/User")
			.getStatusCode();

		assertTrue(message, statusCode != 200);
	}

	private String token(final ClaimWriter claims) {

		final Calendar expiry = Calendar.getInstance();

		expiry.add(Calendar.MINUTE, 10);

		final JWTCreator.Builder builder = JWT.create()
			.withIssuer(ISSUER)
			.withKeyId(KEY_ID)
			.withIssuedAt(new Date())
			.withExpiresAt(expiry.getTime());

		return claims.write(builder).sign(Algorithm.RSA256(publicKey, privateKey));
	}

	private void configureJWKS(final String audience) {

		if (jwksServer == null) {

			startJwksProvider();

			adminUuid = createEntityAsSuperUser("/User", "{ 'name': 'jwks-admin', 'isAdmin': true }");
		}

		Settings.JWTSecretType.setValue("jwks");
		Settings.JWTIssuer.setValue(ISSUER);
		Settings.JWKSProvider.setValue("http://localhost:" + jwksServer.getAddress().getPort() + "/jwks.json");
		Settings.JWKSAudience.setValue(audience);
	}

	private void startJwksProvider() {

		try {

			final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

			generator.initialize(2048);

			final KeyPair keyPair = generator.generateKeyPair();

			publicKey  = (RSAPublicKey) keyPair.getPublic();
			privateKey = (RSAPrivateKey) keyPair.getPrivate();

			final String jwks = "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\",\"kid\":\"" + KEY_ID + "\","
				+ "\"n\":\"" + base64url(publicKey.getModulus()) + "\",\"e\":\"" + base64url(publicKey.getPublicExponent()) + "\"}]}";

			jwksServer = HttpServer.create(new InetSocketAddress(0), 0);

			jwksServer.createContext("/jwks.json", exchange -> {

				final byte[] body = jwks.getBytes(StandardCharsets.UTF_8);

				exchange.getResponseHeaders().add("Content-Type", "application/json");
				exchange.sendResponseHeaders(200, body.length);

				try (final OutputStream out = exchange.getResponseBody()) {

					out.write(body);
				}
			});

			jwksServer.start();

		} catch (final Exception ex) {

			fail("Could not start JWKS provider: " + ex.getMessage());
		}
	}

	/**
	 * A JWK carries the modulus and exponent as base64url of the unsigned big-endian bytes, which is
	 * BigInteger.toByteArray() minus the sign byte Java puts in front of a positive number whose top bit
	 * is set.
	 */
	private static String base64url(final BigInteger value) {

		byte[] bytes = value.toByteArray();
		if (bytes.length > 1 && bytes[0] == 0) {

			final byte[] unsigned = new byte[bytes.length - 1];

			System.arraycopy(bytes, 1, unsigned, 0, unsigned.length);

			bytes = unsigned;
		}

		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
