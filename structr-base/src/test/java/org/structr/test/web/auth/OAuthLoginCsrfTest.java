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
package org.structr.test.web.auth;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.rest.auth.AuthHelper;
import org.structr.web.auth.OAuth2Flow;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1593: the OAuth2 callback logs somebody in no matter which browser it arrives in.
 *
 * <p>UiAuthenticator generated the state parameter as a random UUID and put it in a static cache, which
 * is not a binding to anything - the cache answers the same for every browser on the instance. A
 * callback whose state was unknown produced {@code originalRequestParameters == null} and then carried
 * on regardless, all the way to {@code AuthHelper.doLogin(request, user)}.
 *
 * <p>The attack that follows from it: the attacker starts the flow themselves, authenticates at the
 * provider with their own account, and stops before handing the resulting
 * {@code /oauth/&lt;provider&gt;/auth?code=...} URL to Structr. They send that URL to the victim instead.
 * The victim's browser opens it, Structr redeems the code, finds the attacker's account behind it and
 * logs the victim's browser into that account. Nothing on screen says so, and everything the victim
 * enters from that point on - documents, credentials typed into a form, anything - is stored in an
 * account the attacker can log into.
 *
 * <p>The fix is that a callback has to be the answer to a flow the same browser started: {@link
 * OAuth2Flow} keeps state, PKCE verifier and nonce together and ties them to one browser with a
 * short-lived cookie carrying the state, and a callback that matches no such flow is refused before the
 * code is redeemed.
 */
public class OAuthLoginCsrfTest extends KeycloakOAuth2TestBase {

	/**
	 * The attack, end to end. The authorization is real and the code is valid - it just belongs to
	 * somebody else's browser, which has to be enough to refuse it.
	 */
	@Test
	public void test01ACallbackDeliveredToAnotherBrowserLogsNobodyIn() {

		// the attacker runs the flow in their own browser and keeps the callback URL to themselves
		final StartedFlow attackerFlow = startLogin(null);
		final String callbackUrl       = authorizeAtKeycloak(attackerFlow, TEST_USERNAME, TEST_PASSWORD);
		final List<String> sessionsBefore = sessionIdsOf(TEST_EMAIL);

		// the victim opens the link: their browser never started a flow, so it has no state cookie
		final Response victimResponse = deliverCallback(callbackUrl, null);

		assertEquals("the callback was not refused", 302, victimResponse.getStatusCode());

		assertTrue("the victim's browser was sent on to the return URI, which is where a successful login ends: " + victimResponse.getHeader("Location"),
			victimResponse.getHeader("Location").endsWith("/error"));

		assertEquals("the callback logged a browser into the account behind the code", sessionsBefore, sessionIdsOf(TEST_EMAIL));
	}

	/**
	 * The property the refusal rests on: the login request has to leave something behind in the browser
	 * that only that browser has. Without it there is nothing to compare the callback against.
	 */
	@Test
	public void test02TheLoginRequestBindsTheFlowToTheBrowser() {

		final StartedFlow flow = startLogin(null);

		assertNotNull("the login request did not bind the flow to this browser, so any browser can answer it", flow.stateCookie);

		assertTrue("the authorization URL carries no state", flow.authorizationUrl.contains("state="));
		assertTrue("the state sent to the provider is not the one bound to the browser", flow.authorizationUrl.contains("state=" + flow.stateCookie));

		assertTrue("the authorization request asks for no PKCE challenge, so an intercepted code can be redeemed by whoever holds it", flow.authorizationUrl.contains("code_challenge="));
		assertTrue("the PKCE challenge is not hashed", flow.authorizationUrl.contains("code_challenge_method=S256"));
		assertTrue("the authorization request carries no nonce, so an id_token from an earlier exchange can be replayed into it", flow.authorizationUrl.contains("nonce="));
	}

	/**
	 * A callback URL is not a secret - it sits in the browser history and in every proxy and access log
	 * on the way. So the state has to be good for exactly one callback, even in the browser that started
	 * the flow.
	 */
	@Test
	public void test03AReplayedCallbackIsRefused() {

		final StartedFlow flow   = startLogin(null);
		final String callbackUrl = authorizeAtKeycloak(flow, TEST_USERNAME, TEST_PASSWORD);

		deliverCallback(callbackUrl, flow.stateCookie);

		final Response replay = deliverCallback(callbackUrl, flow.stateCookie);

		assertTrue("the same callback was accepted a second time: " + replay.getHeader("Location"), replay.getHeader("Location").endsWith("/error"));
	}

	/**
	 * Ticket 1593, the token handout: with createTokens=true the callback answered with a 302 whose query
	 * string held a live access token and refresh token. A query string is written to the browser
	 * history, logged verbatim by every proxy in between, and sent on as the Referer of the landing
	 * page's own same-origin requests.
	 */
	@Test
	public void test04TokensAreNotHandedOutInTheQueryString() {

		final StartedFlow flow   = startLogin("createTokens=true");
		final String callbackUrl = authorizeAtKeycloak(flow, TEST_USERNAME, TEST_PASSWORD);
		final Response response = deliverCallback(callbackUrl, flow.stateCookie);
		final String location   = response.getHeader("Location");

		assertNotNull("no redirect after a token login", location);

		final int fragmentStart = location.indexOf('#');
		final String beforeHash = fragmentStart < 0 ? location : location.substring(0, fragmentStart);

		assertFalse("the access token is in the query string of the redirect: " + beforeHash, beforeHash.contains("access_token="));
		assertFalse("the refresh token is in the query string of the redirect: " + beforeHash, beforeHash.contains("refresh_token="));

		assertTrue("the tokens were not handed out at all, so the token login is broken rather than fixed: " + location,
			fragmentStart >= 0 && location.substring(fragmentStart).contains("access_token="));
	}

	/**
	 * Ticket 1593, the credential itself: the value Structr looks accounts up by is an e-mail address, so an
	 * address the provider itself has not verified is an address anybody with an account there can claim.
	 */
	@Test
	public void test05AnUnverifiedEmailAddressIsRefused() {

		final StartedFlow flow   = startLogin(null);
		final String callbackUrl = authorizeAtKeycloak(flow, "unverifieduser", "unverifiedpass");
		final Response response = deliverCallback(callbackUrl, flow.stateCookie);

		assertTrue("an unverified e-mail address was accepted as an identity: " + response.getHeader("Location"), response.getHeader("Location").endsWith("/error"));

		assertNull("an account was created for an e-mail address the provider has not verified", principalFor("unverified@example.com"));
	}

	/**
	 * The refusal must not cost the browser the login it does have. An unmatched callback belongs to no
	 * flow of this browser's, so clearing the state cookie on the way past would take down whatever
	 * login is in flight - a second tab, or the one a stray link arrived during. Someone who wants that
	 * only has to send the victim a link with any state at all.
	 */
	@Test
	public void test06AStrayCallbackDoesNotCancelALoginInFlight() {

		final StartedFlow flow   = startLogin(null);
		final String callbackUrl = authorizeAtKeycloak(flow, TEST_USERNAME, TEST_PASSWORD);

		// the same URL with somebody else's state, delivered to this browser while its own login waits
		final String strayUrl = callbackUrl.replace("state=" + flow.stateCookie, "state=" + NodeServiceCommand.getNextUuid());
		final Response stray  = deliverCallback(strayUrl, flow.stateCookie);

		assertTrue("the stray callback was not refused: " + stray.getHeader("Location"), stray.getHeader("Location").endsWith("/error"));

		assertNull("the stray callback cleared the state cookie, which is the cookie of the login still in flight", stray.getCookie(OAuth2Flow.STATE_COOKIE_NAME));

		final Response real = deliverCallback(callbackUrl, flow.stateCookie);

		assertFalse("the login that was in flight could not be completed afterwards: " + real.getHeader("Location"), real.getHeader("Location").endsWith("/error"));
	}

	/**
	 * The attributes are the mechanism, not decoration. SameSite=Lax is what makes the browser send the
	 * cookie on the top-level redirect back from the provider while keeping it away from cross-site
	 * requests that are not that redirect - Strict would break the login outright. HttpOnly is there
	 * because nothing in the browser has any use for the value, so there is no reason to let a script
	 * read it; on its own it stops less than it looks, since a callback still needs an authorization code
	 * and the PKCE verifier for this flow never leaves the server.
	 */
	@Test
	public void test07TheStateCookieCarriesTheAttributesItRestsOn() {

		final String setCookie = stateSetCookieHeader();

		assertNotNull("the login request set no state cookie at all", setCookie);

		assertTrue("HttpOnly is missing, so a script can read the state: " + setCookie, setCookie.contains("HttpOnly"));
		assertTrue("SameSite=Lax is missing: " + setCookie, setCookie.contains("SameSite=Lax"));
		assertTrue("Path=/ is missing, so the callback path may not see the cookie: " + setCookie, setCookie.contains("Path=/"));
		assertTrue("the cookie does not expire on its own: " + setCookie, setCookie.contains("Max-Age=600"));

		assertFalse("Secure although these tests turn httpservice.cookies.secure off: " + setCookie, setCookie.contains("Secure"));

		/* The other half of the same rule, and the reason it is worth a test: the flag follows the setting
		   that governs the session cookie, so an installation serving HTTPS does not have to decide it a
		   second time for this one. */
		final Boolean previous = Settings.CookieSecure.getValue();

		try {

			Settings.CookieSecure.setValue(true);

			assertTrue("the state cookie ignores httpservice.cookies.secure", stateSetCookieHeader().contains("Secure"));

		} finally {

			Settings.CookieSecure.setValue(previous);
		}
	}

	// ----- private methods -----

	/**
	 * The Set-Cookie header the login request answers with for the state cookie, raw, because the
	 * attributes are what is being asserted and a parsed cookie object drops the ones it does not model.
	 */
	private String stateSetCookieHeader() {

		RestAssured.basePath = "/";

		final Response response = RestAssured
			.given()
				.redirects().follow(false)
			.when()
				.get("/oauth/keycloak/login")
			.then()
				.statusCode(302)
			.extract().response();

		return response.getHeaders().getValues("Set-Cookie").stream()
			.filter(header -> header.startsWith(OAuth2Flow.STATE_COOKIE_NAME + "="))
			.findFirst()
			.orElse(null);
	}

	private Principal principalFor(final String eMail) {

		final PropertyKey credentialKey = Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY);

		try (final Tx tx = app.tx()) {

			final Principal principal = AuthHelper.getPrincipalForCredential(credentialKey, eMail);

			tx.success();

			return principal;

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());

			return null;
		}
	}

	/**
	 * The sessions of the account behind the given e-mail address, or an empty list if no such account
	 * exists yet - which is the state this test class starts in.
	 */
	private List<String> sessionIdsOf(final String eMail) {

		try (final Tx tx = app.tx()) {

			final PropertyKey credentialKey = Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY);
			final Principal principal       = AuthHelper.getPrincipalForCredential(credentialKey, eMail);

			if (principal == null) {

				tx.success();

				return List.of();
			}

			final String[] sessionIds = principal.getSessionIds();

			tx.success();

			return sessionIds != null ? Arrays.asList(sessionIds) : List.of();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());

			return List.of();
		}
	}
}
