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

import org.structr.websocket.StructrWebSocket;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

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

	/** With httpservice.cookies.httponly = true the page cannot read the cookie: a LOGIN without a session id binds the session of the handshake cookie, and does not hand its id to the page. */
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

	/** A page that logged in elsewhere (another tab, Structr Desktop) is authenticated by the handshake cookie alone. */
	@Test
	public void testPingWithoutSessionIdIsAuthenticatedByTheHandshakeSession() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");

		final MockedWebsocketSetup first = getMockedWebsocketSetup();

		login(first.getWebSocket(), "admin", "admin", "SHAREDSESSION");
		assertResponse(first, "LOGIN", 200.0, true);

		final MockedWebsocketSetup second = getMockedWebsocketSetup();

		second.getWebSocket().setHandshakeSessionId("SHAREDSESSION");
		second.getWebSocket().onWebSocketText(toJson(Map.of("command", "PING")));

		final Map<String, Object> response = second.getLastWebsocketResponse();

		assertEquals(100.0, response.get("code"));
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
}
