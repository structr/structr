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
package org.structr.test.mock;

import io.restassured.response.Response;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.rest.auth.SessionHelper;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.servlet.WebSocketLoginServlet;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1594: the websocket LOGIN command takes the session id out of the message
 * ({@code webSocketData.getSessionId()}) and calls user.addSessionId() with it, without ever asking
 * whether a session by that id exists. The id is the client's to choose, and afterwards it is an authenticated session of
 * that account - reachable over HTTP as well, because both sides read the same sessionIds property.
 *
 * <p>StructrWebSocket.authenticate() completes the picture: it treats {@code session == null} as a
 * valid session ({@code session == null || !isSessionTimedOut(...)}), so an id that exists nowhere but
 * in the user's sessionIds is never subject to the idle timeout either. It stays valid until that user
 * logs in or out again.
 *
 * <p>Binding the id of the handshake cookie instead was still session fixation: an id planted in the
 * browser beforehand became an authenticated session the moment the user logged into the Admin UI. A
 * websocket cannot set a cookie, so LOGIN binds nothing and answers with a ticket, which the page
 * redeems over HTTP at {@link WebSocketLoginServlet#PATH}, where the session id is rotated.</p>
 */
public class WebsocketSessionBindingTest extends StructrWebsocketBaseTest {

	@Test
	public void testLoginDoesNotAcceptASessionIdTheClientMadeUp() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		final MockedWebsocketSetup mock  = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();

		// no SessionDataNode, so there is no session behind this id - the client simply named it
		loginWithUnknownSession(websocket, "admin", "admin", "ID-THE-CLIENT-INVENTED");

		// a refused login answers as STATUS, the way LoginCommand reports every other refusal
		assertResponse(mock, "STATUS", 403.0, false);
	}

	/**
	 * The counterpart, so the assertion above cannot pass just because websocket login is broken: the
	 * same login against an id that does have a session behind it has to succeed.
	 */
	@Test
	public void testLoginStillWorksForAnEstablishedSession() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");
		createEntityAsSuperUser("/SessionDataNode", "{ vhost: '0.0.0.0', sessionId: 'ESTABLISHEDSESSION' }");

		final MockedWebsocketSetup mock  = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();

		login(websocket, "admin", "admin", "ESTABLISHEDSESSION");

		assertResponse(mock, "LOGIN", 200.0, true);
	}

	/** With httpservice.cookies.httponly = true the page cannot read the cookie: a LOGIN without a session id is made with the session of the handshake cookie, and does not hand its id to the page. */
	@Test
	public void testLoginWithoutSessionIdUsesTheHandshakeSession() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");
		establishSession("HANDSHAKESESSION");

		final MockedWebsocketSetup mock  = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();

		websocket.setHandshakeSessionId("HANDSHAKESESSION");
		websocket.onWebSocketText(toJson(Map.of("command", "LOGIN", "data", Map.of("username", "admin", "password", "admin"))));

		final Map<String, Object> response = assertResponse(mock, "LOGIN", 200.0, true);

		assertFalse("the session id from the HttpOnly cookie must not be sent to the page", response.containsKey("sessionId"));
	}

	/** A page that logged in elsewhere (another tab, Structr Desktop) is authenticated by the handshake cookie alone, once that carries the session id the login was completed with. */
	@Test
	public void testPingWithoutSessionIdIsAuthenticatedByTheHandshakeSession() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		final String sessionId = anonymousSessionId();
		final String ticket    = loginForTicket(getMockedWebsocketSetup(), sessionId);
		final Response redeem  = redeem(ticket, sessionId);

		assertEquals(204, redeem.getStatusCode());

		assertEquals(100.0, ping(redeem.getSessionId()).get("code"));
	}

	/**
	 * The attack, end to end: an id planted in the browser before the Admin UI login must be worthless
	 * afterwards - neither bound to the account nor accepted by a websocket - and the browser must hold
	 * a different one that is.
	 */
	@Test
	public void testLoginDoesNotBindTheSessionTheBrowserArrivedWith() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		final String plantedSessionId = anonymousSessionId();
		final String ticket           = loginForTicket(getMockedWebsocketSetup(), plantedSessionId);

		assertFalse("the websocket login bound the session id the browser carried into it", sessionIdsOfAdmin().contains(SessionHelper.getShortSessionId(plantedSessionId)));

		final Response redeem = redeem(ticket, plantedSessionId);

		assertEquals(204, redeem.getStatusCode());

		final String newSessionId = redeem.getSessionId();

		assertNotNull("redeeming the ticket must set a new session cookie", newSessionId);
		assertFalse("the session id was not rotated", SessionHelper.getShortSessionId(plantedSessionId).equals(SessionHelper.getShortSessionId(newSessionId)));

		final List<String> sessionIds = sessionIdsOfAdmin();

		assertTrue("the rotated session id was not bound to the account", sessionIds.contains(SessionHelper.getShortSessionId(newSessionId)));
		assertFalse("the planted session id was bound to the account", sessionIds.contains(SessionHelper.getShortSessionId(plantedSessionId)));

		assertEquals(401.0, ping(plantedSessionId).get("code"));
	}

	@Test
	public void testTicketCanBeRedeemedOnlyOnce() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		final String sessionId = anonymousSessionId();
		final String ticket    = loginForTicket(getMockedWebsocketSetup(), sessionId);
		final Response first   = redeem(ticket, sessionId);

		assertEquals(204, first.getStatusCode());
		assertEquals(401, redeem(ticket, first.getSessionId()).getStatusCode());
	}

	/** A ticket that leaked is useless to a browser that does not also hold the session the login was made with. */
	@Test
	public void testTicketCanOnlyBeRedeemedWithTheSessionOfTheLogin() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		final String sessionId = anonymousSessionId();
		final String ticket    = loginForTicket(getMockedWebsocketSetup(), sessionId);

		assertEquals(401, redeem(ticket, anonymousSessionId()).getStatusCode());
		assertEquals(401, redeem(ticket, null).getStatusCode());

		assertTrue("a refused redemption bound a session", sessionIdsOfAdmin().isEmpty());
	}

	@Test
	public void testTicketRedemptionRequiresSameOrigin() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		final String sessionId = anonymousSessionId();
		final String ticket    = loginForTicket(getMockedWebsocketSetup(), sessionId);

		assertEquals(403, redemption(ticket, sessionId).header("Origin", "http://foreign.example").when().post(WebSocketLoginServlet.PATH).getStatusCode());
	}

	/** The websocket refuses a user without backend access, and so is the ticket that would log them in over HTTP. */
	@Test
	public void testNonAdminLoginIssuesNoTicket() {

		createEntityAsSuperUser("/User", "{ name: tester, password: tester }");

		final MockedWebsocketSetup mock = getMockedWebsocketSetup();

		mock.getWebSocket().setHandshakeSessionId(anonymousSessionId());
		mock.getWebSocket().onWebSocketText(toJson(Map.of("command", "LOGIN", "data", Map.of("username", "tester", "password", "tester"))));

		final Map<String, Object> response = assertResponse(mock, "LOGIN", 403.0, false);

		assertNull("a user without backend access was given a login ticket", ((Map<String, Object>) response.get("data")).get("loginTicket"));
	}

	/** Without a session id in the message and without a session cookie in the handshake, LOGIN is refused with a reason the Admin UI explains. */
	@Test
	public void testLoginWithoutAnySessionIsRefused() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		final MockedWebsocketSetup mock  = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();

		websocket.onWebSocketText(toJson(Map.of("command", "LOGIN", "data", Map.of("username", "admin", "password", "admin"))));

		final Map<String, Object> response = assertResponse(mock, "STATUS", 403.0, false);

		assertEquals("noSessionCookie", ((Map<String, Object>) response.get("data")).get("reason"));
	}

	// ----- private methods -----
	/** Logs in as admin over a websocket whose handshake carried the given session id, and answers the ticket. */
	private String loginForTicket(final MockedWebsocketSetup mock, final String sessionId) {

		mock.getWebSocket().setHandshakeSessionId(sessionId);
		mock.getWebSocket().onWebSocketText(toJson(Map.of("command", "LOGIN", "data", Map.of("username", "admin", "password", "admin"))));

		return loginTicket(mock);
	}

	private Map<String, Object> ping(final String handshakeSessionId) {

		final MockedWebsocketSetup mock = getMockedWebsocketSetup();

		mock.getWebSocket().setHandshakeSessionId(handshakeSessionId);
		mock.getWebSocket().onWebSocketText(toJson(Map.of("command", "PING")));

		return mock.getLastWebsocketResponse();
	}

	private List<String> sessionIdsOfAdmin() {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.nodeQuery(StructrTraits.USER).name("admin").getFirst();
			final String[] ids       = node.as(Principal.class).getSessionIds();

			tx.success();

			return ids != null ? Arrays.asList(ids) : List.of();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());

			return List.of();
		}
	}
}
