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
import org.structr.api.config.Settings;
import org.structr.common.RequestHeaders;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.rest.auth.SessionHelper;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1594: logging in keeps whatever session id the browser arrived with. AuthHelper.doLogin()
 * takes request.getSession().getId() as it finds it and adds that id to the user's sessionIds;
 * request.changeSessionId() is never called. SessionHelper.newSession() does contain the call, but
 * behind {@code if (request.getSession(true) == null)}, and getSession(true) does not return null, so
 * that branch never runs.
 *
 * <p>That is session fixation: an attacker who can get a session id of their choosing into the
 * victim's browser - a cookie from a sibling subdomain, a {@code ;jsessionid=} link, plain HTTP - waits
 * for the victim to log in, and the id they already hold is now an authenticated session of the
 * victim's account. Nothing about the login tells the victim anything happened.
 *
 * <p>ConfigServlet.authenticateSession() already does it the other way round, so the shape of the fix
 * is in the code base: call request.changeSessionId() and bind the new id.
 */
public class SessionFixationTest extends StructrUiTest {

	private static final String PASSWORD = "correct-horse-battery-staple";

	/**
	 * The attack, end to end. The id is planted before the login and has to be worthless after it.
	 */
	@Test
	public void testAnIdPresentedBeforeLoginIsNotValidAfterwards() {

		createEntityAsSuperUser("/User", "{ 'name': 'victim', 'password': '" + PASSWORD + "' }");

		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);

		final String plantedSessionId = anonymousSessionId();

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.sessionId(plantedSessionId)
				.body("{ 'name': 'victim', 'password': '" + PASSWORD + "' }")
			.expect()
				.statusCode(200)
			.when()
				.post("/login");

		assertFalse("the session id the browser carried into the login became a session of the account: " + plantedSessionId,
			sessionIdsOf("victim").contains(SessionHelper.getShortSessionId(plantedSessionId)));
	}

	/**
	 * The same property seen from the outside: the browser must be handed a different id than it sent,
	 * because that is the only thing that makes the old one worthless.
	 */
	@Test
	public void testLoginAnswersWithARotatedSessionId() {

		createEntityAsSuperUser("/User", "{ 'name': 'rotator', 'password': '" + PASSWORD + "' }");

		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);

		final String plantedSessionId = anonymousSessionId();

		final String afterLogin = RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.sessionId(plantedSessionId)
				.body("{ 'name': 'rotator', 'password': '" + PASSWORD + "' }")
			.expect()
				.statusCode(200)
			.when()
				.post("/login")
			.getSessionId();

		assertNotNull("login did not answer with a session at all", afterLogin);

		assertFalse("login kept the session id the browser sent, so an id known beforehand stays valid",
			SessionHelper.getShortSessionId(plantedSessionId).equals(SessionHelper.getShortSessionId(afterLogin)));
	}

	/**
	 * Ticket 1594, second half: Jetty is left with its default session tracking modes, COOKIE and URL,
	 * so {@code /User;jsessionid=<id>} authenticates. That is the delivery vector for the attack above -
	 * a link is enough to put an id of the attacker's choosing into the victim's browser, no cookie from
	 * a sibling subdomain and no plain-text HTTP needed. Nothing in Structr uses it.
	 */
	@Test
	public void testSessionIdInTheUrlIsNotAccepted() {

		createEntityAsSuperUser("/User", "{ 'name': 'urluser', 'password': '" + PASSWORD + "' }");

		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);
		grant(StructrTraits.USER, UiAuthenticator.AUTH_USER_GET, false);

		final String sessionId = RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ 'name': 'urluser', 'password': '" + PASSWORD + "' }")
			.expect()
				.statusCode(200)
			.when()
				.post("/login")
			.getSessionId();

		assertNotNull("login did not answer with a session at all", sessionId);

		// no cookie, only the URL
		RestAssured
			.given()
				.urlEncodingEnabled(false)
			.expect()
				.statusCode(401)
			.when()
				.get("/User;jsessionid=" + sessionId);
	}

	/**
	 * Ticket 1594: the session cookie is the credential, and a cookie without the Secure flag is sent
	 * over plain HTTP, where anyone on the path can read it - and an id read that way is an id that can
	 * be planted, which is the attack at the top of this file. An installation that genuinely serves
	 * plain HTTP has to say so by turning it off.
	 */
	@Test
	public void testSessionCookieIsSecureByDefault() {

		assertTrue("the session cookie must carry the Secure flag unless an installation opts out",
			Settings.CookieSecure.getDefaultValue());
	}

	/**
	 * Ticket 1594: X-Structr-Session-Token resolves the user straight out of the sessionIds property
	 * (UiAuthenticator.getUser -> AuthHelper.getPrincipalForSessionId) and never reaches
	 * SessionHelper.checkSessionAuthentication, which is where a session is checked for being timed out
	 * or gone. Jetty is configured never to expire sessions itself (HttpService sets maxInactiveInterval
	 * to -1, "we handle timeout"), so for this header nobody handles it: an id stays valid until the
	 * user logs in or out again, whatever application.session.timeout says.
	 *
	 * <p>The timeout is the property the ticket names, so the test uses it: nothing else expresses
	 * "this session is past its time" without depending on how sessions are stored. Invalidating one
	 * does not do it - the session cache loads it straight back out of its SessionDataNode.</p>
	 */
	@Test
	public void testSessionTokenHeaderRespectsTheSessionTimeout() {

		createEntityAsSuperUser("/User", "{ 'name': 'tokenuser', 'password': '" + PASSWORD + "' }");

		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);
		grant(StructrTraits.USER, UiAuthenticator.AUTH_USER_GET, false);

		final String sessionId = RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ 'name': 'tokenuser', 'password': '" + PASSWORD + "' }")
			.expect()
				.statusCode(200)
			.when()
				.post("/login")
			.getSessionId();

		assertNotNull("login did not answer with a session at all", sessionId);

		// the header works while the session is alive - otherwise the assertion below proves nothing
		RestAssured
			.given()
				.header(RequestHeaders.XStructrSessionToken.getName(), sessionId)
			.expect()
				.statusCode(200)
			.when()
				.get("/User");

		final Integer previousTimeout = Settings.SessionTimeout.getValue();

		try {

			Settings.SessionTimeout.setValue(1);

			// the request below carries no cookie, so nothing touches the session's last-accessed time
			Thread.sleep(1500);

			RestAssured
				.given()
					.header(RequestHeaders.XStructrSessionToken.getName(), sessionId)
				.expect()
					.statusCode(401)
				.when()
					.get("/User");

		} catch (final InterruptedException iex) {

			fail("Interrupted while waiting for the session to time out");

		} finally {

			Settings.SessionTimeout.setValue(previousTimeout);
		}
	}

	// ----- private methods -----
	/**
	 * A session id the server will accept, obtained the way an attacker obtains one: by asking for
	 * anything at all. The request itself is refused, the session behind it is not.
	 */
	private String anonymousSessionId() {

		final String sessionId = RestAssured
			.given()
			.when()
				.get("/User")
			.getSessionId();

		assertNotNull("could not obtain an anonymous session id to plant", sessionId);

		return sessionId;
	}

	private List<String> sessionIdsOf(final String name) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.nodeQuery(StructrTraits.USER).name(name).getFirst();

			if (node == null) {

				fail("user " + name + " was not created");
			}

			final Principal user      = node.as(Principal.class);
			final String[] sessionIds = user.getSessionIds();

			tx.success();

			return sessionIds != null ? Arrays.asList(sessionIds) : List.of();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());

			return List.of();
		}
	}
}
