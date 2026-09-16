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
}
