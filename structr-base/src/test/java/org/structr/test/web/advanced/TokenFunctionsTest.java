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
package org.structr.test.web.advanced;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.GenericProperty;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.rest.auth.JWTHelper;
import org.structr.schema.action.ActionContext;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.*;

/**
 * Tests for the scripting functions create_access_token and create_access_and_refresh_token: they accept any node
 * with the User trait, reject other nodes, and accept any number (or ignore a non-number) as timeout.
 */
public class TokenFunctionsTest extends StructrUiTest {

	private static final String USER_NAME  = "tester";
	private static final String GROUP_NAME = "testgroup";
	private static final String SECRET     = "a test secret that is long enough for signing tokens";

	private static final String FIND_USER_JS   = "$.find('User', { name: '" + USER_NAME + "' })[0]";
	private static final String FIND_USER_SS   = "first(find('User', 'name', '" + USER_NAME + "'))";
	private static final String FIND_GROUP_JS  = "$.find('Group', { name: '" + GROUP_NAME + "' })[0]";
	private static final String FIND_GROUP_SS  = "first(find('Group', 'name', '" + GROUP_NAME + "'))";

	private String previousSecret     = null;
	private String previousSecretType = null;

	@BeforeMethod
	public void setUpSecret() {

		previousSecret     = Settings.JWTSecret.getValue();
		previousSecretType = Settings.JWTSecretType.getValue();

		Settings.JWTSecret.setValue(SECRET);
		Settings.JWTSecretType.setValue("secret");
	}

	@AfterMethod
	public void restoreSecret() {

		Settings.JWTSecret.setValue(previousSecret);
		Settings.JWTSecretType.setValue(previousSecretType);
	}

	// ----- create_access_token -----

	@Test
	public void testCreateAccessTokenWithDefaultTimeout() {

		createTestData();

		for (final String script : new String[] {
			"${{ $.createAccessToken(" + FIND_USER_JS + "); }}",
			"${create_access_token(" + FIND_USER_SS + ")}"
		}) {

			final long before   = System.currentTimeMillis();
			final Object result = evaluate(script);
			final long after    = System.currentTimeMillis();

			assertAccessToken(script, result, before, after, Settings.JWTExpirationTimeout.getValue());
		}

		assertEquals("create_access_token must not create refresh tokens", 0, countRefreshTokens());
	}

	@Test
	public void testCreateAccessTokenWithNumericTimeout() {

		createTestData();

		// JavaScript integer, JavaScript floating point, and StructrScript numbers arrive as different Number types
		assertAccessTokenTimeout("${{ $.createAccessToken(" + FIND_USER_JS + ", 30); }}",   30);
		assertAccessTokenTimeout("${{ $.createAccessToken(" + FIND_USER_JS + ", 30.0); }}", 30);
		assertAccessTokenTimeout("${{ $.createAccessToken(" + FIND_USER_JS + ", 45.7); }}", 45);
		assertAccessTokenTimeout("${create_access_token(" + FIND_USER_SS + ", 30)}",                30);
		assertAccessTokenTimeout("${create_access_token(" + FIND_USER_SS + ", 45.7)}",              45);
	}

	@Test
	public void testCreateAccessTokenWithNonNumericTimeoutUsesDefault() {

		createTestData();

		final int defaultTimeout = Settings.JWTExpirationTimeout.getValue();

		assertAccessTokenTimeout("${{ $.createAccessToken(" + FIND_USER_JS + ", 'abc'); }}", defaultTimeout);
		assertAccessTokenTimeout("${{ $.createAccessToken(" + FIND_USER_JS + ", '30'); }}",  defaultTimeout);
		assertAccessTokenTimeout("${{ $.createAccessToken(" + FIND_USER_JS + ", true); }}",  defaultTimeout);
		assertAccessTokenTimeout("${create_access_token(" + FIND_USER_SS + ", 'abc')}",              defaultTimeout);
	}

	@Test
	public void testCreateAccessTokenAuthenticatesUser() {

		createTestData();

		final String token = (String) evaluate("${{ $.createAccessToken(" + FIND_USER_JS + "); }}");

		try (final Tx tx = app.tx()) {

			final Principal principal = JWTHelper.getPrincipalForAccessToken(token, Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY));

			assertNotNull("The access token does not authenticate anyone", principal);
			assertEquals("The access token authenticates the wrong principal", USER_NAME, principal.getName());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testCreateAccessTokenRejectsNonUser() {

		createTestData();

		// StructrScript does not support exception handling, the function logs a warning and returns null
		assertNull("create_access_token must not accept a Group", evaluate("${create_access_token(" + FIND_GROUP_SS + ")}"));
		assertNull("create_access_token must not accept a string", evaluate("${create_access_token('" + USER_NAME + "')}"));

		// JavaScript supports exception handling, the function throws
		assertThrows("${{ $.createAccessToken(" + FIND_GROUP_JS + "); }}");
		assertThrows("${{ $.createAccessToken('" + USER_NAME + "'); }}");
		assertThrows("${{ $.createAccessToken({ name: '" + USER_NAME + "' }); }}");
	}

	// ----- create_access_and_refresh_token -----

	@Test
	public void testCreateAccessAndRefreshTokenWithDefaultTimeouts() {

		createTestData();

		int expectedRefreshTokens = 0;

		for (final String script : new String[] {
			"${{ $.createAccessAndRefreshToken(" + FIND_USER_JS + "); }}",
			"${create_access_and_refresh_token(" + FIND_USER_SS + ")}"
		}) {

			final long before   = System.currentTimeMillis();
			final Object result = evaluate(script);
			final long after    = System.currentTimeMillis();

			assertTokens(script, result, before, after, Settings.JWTExpirationTimeout.getValue(), Settings.JWTRefreshTokenExpirationTimeout.getValue());

			assertEquals("create_access_and_refresh_token must store exactly one refresh token at the user", ++expectedRefreshTokens, countRefreshTokens());
		}
	}

	@Test
	public void testCreateAccessAndRefreshTokenWithNumericTimeouts() {

		createTestData();

		assertTokensTimeouts("${{ $.createAccessAndRefreshToken(" + FIND_USER_JS + ", 30, 120); }}",     30, 120);
		assertTokensTimeouts("${{ $.createAccessAndRefreshToken(" + FIND_USER_JS + ", 30.0, 120.0); }}", 30, 120);
		assertTokensTimeouts("${{ $.createAccessAndRefreshToken(" + FIND_USER_JS + ", 45.7, 90.2); }}",  45, 90);
		assertTokensTimeouts("${create_access_and_refresh_token(" + FIND_USER_SS + ", 30, 120)}",               30, 120);
		assertTokensTimeouts("${create_access_and_refresh_token(" + FIND_USER_SS + ", 45.7, 90.2)}",            45, 90);

		// only the access token timeout is given
		assertTokensTimeouts("${{ $.createAccessAndRefreshToken(" + FIND_USER_JS + ", 30); }}", 30, Settings.JWTRefreshTokenExpirationTimeout.getValue());
		assertTokensTimeouts("${create_access_and_refresh_token(" + FIND_USER_SS + ", 30)}",           30, Settings.JWTRefreshTokenExpirationTimeout.getValue());
	}

	@Test
	public void testCreateAccessAndRefreshTokenWithNonNumericTimeoutsUsesDefaults() {

		createTestData();

		final int defaultAccess  = Settings.JWTExpirationTimeout.getValue();
		final int defaultRefresh = Settings.JWTRefreshTokenExpirationTimeout.getValue();

		// each invalid parameter falls back to its own default and does not affect the other one
		assertTokensTimeouts("${{ $.createAccessAndRefreshToken(" + FIND_USER_JS + ", 'abc', 'def'); }}", defaultAccess, defaultRefresh);
		assertTokensTimeouts("${{ $.createAccessAndRefreshToken(" + FIND_USER_JS + ", 'abc', 120); }}",   defaultAccess, 120);
		assertTokensTimeouts("${{ $.createAccessAndRefreshToken(" + FIND_USER_JS + ", 30, 'def'); }}",    30,            defaultRefresh);
		assertTokensTimeouts("${create_access_and_refresh_token(" + FIND_USER_SS + ", 'abc', 'def')}",           defaultAccess, defaultRefresh);
		assertTokensTimeouts("${create_access_and_refresh_token(" + FIND_USER_SS + ", 'abc', 120)}",             defaultAccess, 120);
		assertTokensTimeouts("${create_access_and_refresh_token(" + FIND_USER_SS + ", 30, 'def')}",              30,            defaultRefresh);
	}

	@Test
	public void testCreateAccessAndRefreshTokenAuthenticatesUser() {

		createTestData();

		final GraphObjectMap tokens = (GraphObjectMap) evaluate("${{ $.createAccessAndRefreshToken(" + FIND_USER_JS + "); }}");
		final String accessToken    = get(tokens, JWTHelper.ACCESS_TOKEN_KEY);
		final String refreshToken   = get(tokens, JWTHelper.REFRESH_TOKEN_KEY);

		try (final Tx tx = app.tx()) {

			final Principal fromAccessToken = JWTHelper.getPrincipalForAccessToken(accessToken, Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY));

			assertNotNull("The access token does not authenticate anyone", fromAccessToken);
			assertEquals("The access token authenticates the wrong principal", USER_NAME, fromAccessToken.getName());

			final Principal fromRefreshToken = JWTHelper.getPrincipalForRefreshToken(refreshToken);

			assertNotNull("The refresh token does not authenticate anyone", fromRefreshToken);
			assertEquals("The refresh token authenticates the wrong principal", USER_NAME, fromRefreshToken.getName());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		assertEquals("Using the refresh token must consume it", 0, countRefreshTokens());
	}

	@Test
	public void testCreateAccessAndRefreshTokenRejectsNonUser() {

		createTestData();

		assertNull("create_access_and_refresh_token must not accept a Group", evaluate("${create_access_and_refresh_token(" + FIND_GROUP_SS + ")}"));
		assertNull("create_access_and_refresh_token must not accept a string", evaluate("${create_access_and_refresh_token('" + USER_NAME + "')}"));

		assertThrows("${{ $.createAccessAndRefreshToken(" + FIND_GROUP_JS + "); }}");
		assertThrows("${{ $.createAccessAndRefreshToken('" + USER_NAME + "'); }}");
		assertThrows("${{ $.createAccessAndRefreshToken({ name: '" + USER_NAME + "' }); }}");

		assertEquals("A rejected call must not store refresh tokens", 0, countRefreshTokens());
	}

	// ----- private methods -----
	private void createTestData() {

		try (final Tx tx = app.tx()) {

			createTestNode(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), USER_NAME),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY), USER_NAME + "@example.com")
			);

			// a group is a principal but not a user
			createTestNode(StructrTraits.GROUP,
				new NodeAttribute<>(Traits.of(StructrTraits.GROUP).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), GROUP_NAME)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	private Object evaluate(final String script) {

		try (final Tx tx = app.tx()) {

			final Object result = Scripting.evaluate(new ActionContext(securityContext), null, script, "test");

			tx.success();

			return result;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception evaluating " + script);
		}

		return null;
	}

	private void assertThrows(final String script) {

		try (final Tx tx = app.tx()) {

			Scripting.evaluate(new ActionContext(securityContext), null, script, "test");

			fail("Expected an exception evaluating " + script);

		} catch (FrameworkException expected) {

			assertTrue("Unexpected error message for " + script + ": " + expected.getMessage(), String.valueOf(expected.getMessage()).contains("Node of type User required as first argument"));
		}
	}

	private void assertAccessTokenTimeout(final String script, final int expectedTimeout) {

		final long before   = System.currentTimeMillis();
		final Object result = evaluate(script);
		final long after    = System.currentTimeMillis();

		assertAccessToken(script, result, before, after, expectedTimeout);
	}

	private void assertTokensTimeouts(final String script, final int expectedAccessTimeout, final int expectedRefreshTimeout) {

		final long before   = System.currentTimeMillis();
		final Object result = evaluate(script);
		final long after    = System.currentTimeMillis();

		assertTokens(script, result, before, after, expectedAccessTimeout, expectedRefreshTimeout);
	}

	private void assertAccessToken(final String script, final Object result, final long before, final long after, final int expectedTimeout) {

		assertTrue("Expected a string from " + script + ", got " + result, result instanceof String);

		final DecodedJWT decoded = JWT.decode((String) result);

		assertEquals("Wrong subject in token from " + script, USER_NAME, decoded.getSubject());
		assertEquals("Wrong token type in token from " + script, JWTHelper.ACCESS_TOKEN_KEY, decoded.getClaim("tokenType").asString());
		assertTrue("Access token from " + script + " must not reference a refresh token", decoded.getClaim("tokenId").isNull() || decoded.getClaim("tokenId").isMissing());
		assertExpiration(script, decoded.getExpiresAt().getTime(), before, after, expectedTimeout);
	}

	private void assertTokens(final String script, final Object result, final long before, final long after, final int expectedAccessTimeout, final int expectedRefreshTimeout) {

		assertTrue("Expected a map from " + script + ", got " + result, result instanceof GraphObjectMap);

		final GraphObjectMap tokens     = (GraphObjectMap) result;
		final String accessToken        = get(tokens, JWTHelper.ACCESS_TOKEN_KEY);
		final String refreshToken       = get(tokens, JWTHelper.REFRESH_TOKEN_KEY);
		final String expirationDate     = get(tokens, JWTHelper.EXPIRATION_DATE_KEY);

		assertNotNull("No access token from " + script, accessToken);
		assertNotNull("No refresh token from " + script, refreshToken);
		assertNotNull("No expiration date from " + script, expirationDate);

		final DecodedJWT access  = JWT.decode(accessToken);
		final DecodedJWT refresh = JWT.decode(refreshToken);

		assertEquals("Wrong subject in access token from " + script, USER_NAME, access.getSubject());
		assertEquals("Wrong subject in refresh token from " + script, USER_NAME, refresh.getSubject());
		assertEquals("Wrong token type in access token from " + script, JWTHelper.ACCESS_TOKEN_KEY, access.getClaim("tokenType").asString());
		assertEquals("Wrong token type in refresh token from " + script, JWTHelper.REFRESH_TOKEN_KEY, refresh.getClaim("tokenType").asString());
		assertEquals("Access token and refresh token from " + script + " must share the token id", refresh.getClaim("tokenId").asString(), access.getClaim("tokenId").asString());

		assertExpiration(script, access.getExpiresAt().getTime(), before, after, expectedAccessTimeout);
		assertExpiration(script, refresh.getExpiresAt().getTime(), before, after, expectedRefreshTimeout);

		// the expiration date has millisecond precision, the token only seconds
		assertEquals("Expiration date from " + script + " does not match the access token", access.getExpiresAt().getTime() / 1000, Long.parseLong(expirationDate) / 1000);
	}

	private void assertExpiration(final String script, final long expiresAt, final long before, final long after, final int expectedTimeoutMinutes) {

		final long timeout = TimeUnit.MINUTES.toMillis(expectedTimeoutMinutes);

		// JWT expiration has second precision, the fraction is cut off
		final long earliest = ((before + timeout) / 1000) * 1000;
		final long latest   = after + timeout;

		assertTrue("Expected expiration in " + expectedTimeoutMinutes + " minutes for " + script + ", was " + (expiresAt - before) / 60_000.0 + " minutes", expiresAt >= earliest && expiresAt <= latest);
	}

	private String get(final GraphObjectMap map, final String key) {

		final Object value = map.getProperty(new GenericProperty<>(key));

		return value != null ? value.toString() : null;
	}

	private int countRefreshTokens() {

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.nodeQuery(StructrTraits.USER).name(USER_NAME).getFirst();

			assertNotNull("The test user does not exist", user);

			final String[] tokens = user.as(Principal.class).getRefreshTokens();

			tx.success();

			return tokens != null ? tokens.length : 0;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		return -1;
	}
}
