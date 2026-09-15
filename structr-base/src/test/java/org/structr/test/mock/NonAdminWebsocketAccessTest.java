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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.websocket.StructrWebSocket;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.fail;

/**
 * The websocket back end is for administrators, and that is a server-side rule, not a property of the
 * administration interface being the only client that opens one: StructrWebSocket.isAuthenticated()
 * runs every caller through isPrivilegedUser(), which is user.isAdmin(), and the dispatch in
 * onWebSocketText() lets nothing but LOGIN past it.
 *
 * <p>The rule is load-bearing for ticket 1584. That ticket restricted the internal "ui" and "all"
 * views to administrators in UiAuthenticator.checkResourceAccess(), which is the REST path only - the
 * websocket never calls it, and WebSocketDataGSONAdapter renders in PropertyView.Ui whenever a command
 * sets no view of its own, which GetCommand does not. So a non-admin websocket session would be a
 * second way to the session ids and two-factor tokens the ticket is about, and one that needs no
 * "User/_Ui" grant at all. Nothing says so where isPrivilegedUser() is written, hence these tests.
 *
 * <p>Not covered: the upgrade itself. The mocked session starts at onWebSocketOpen(), past
 * StructrWebSocketCreator, which checks the Origin header and the sub-protocol and nothing else.
 */
public class NonAdminWebsocketAccessTest extends StructrWebsocketBaseTest {

	private static final String SESSION_ID      = "TESTSESSION";
	private static final String ADMIN_SESSION   = "ADMINSESSION-0123456789";
	private static final String TWO_FACTOR      = "some-two-factor-token";

	@Test
	public void testNonAdminCanHoldAnAuthenticatedWebsocketSession() {

		createEntityAsSuperUser("/User", "{ name: tester, password: tester }");

		final MockedWebsocketSetup mock  = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();

		login(websocket, "tester", "tester", SESSION_ID);

		assertResponse(mock, "LOGIN", 403.0, false);

		/* The control: the same login as an administrator, so a future change that breaks logging in
		   altogether cannot make the assertion above pass for the wrong reason. */
		createEntityAsSuperUser("/User", "{ name: boss, password: boss, isAdmin: true }");

		final MockedWebsocketSetup adminMock = getMockedWebsocketSetup();

		login(adminMock.getWebSocket(), "boss", "boss", "ADMINWEBSOCKETSESSION");

		assertResponse(adminMock, "LOGIN", 200.0, true);
	}

	/**
	 * The consequence, spelled out: even with the ticket's own precondition met - the caller may read
	 * the victim's User node - a non-admin gets nothing out of the websocket, because the session the
	 * GET would need was never established.
	 */
	@Test
	public void testNonAdminCannotReadInternalViewOfAnotherUserOverWebsocket() {

		final String adminId = createAdminWithSessionAndToken();

		createEntityAsSuperUser("/User", "{ name: tester, password: tester }");

		final MockedWebsocketSetup mock  = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();

		login(websocket, "tester", "tester", SESSION_ID);

		websocket.onWebSocketText(toJson(Map.of(
			"command",   "GET",
			"sessionId", SESSION_ID,
			"id",        adminId,
			"data",      Map.of("properties", "id,name,sessionIds,twoFactorToken")
		)));

		final String response = mock.getLastWebsocketResponse().toString();

		assertFalse("the websocket handed out the administrator's session ids: " + response,
			response.contains(ADMIN_SESSION));

		assertFalse("the websocket handed out the administrator's two-factor token: " + response,
			response.contains(TWO_FACTOR));
	}

	// ----- private methods -----
	/**
	 * An administrator with a live session and a two-factor token, readable by authenticated users -
	 * a member list, a group grant, anything that makes other users visible.
	 */
	private String createAdminWithSessionAndToken() {

		try (final Tx tx = app.tx()) {

			final Traits traits       = Traits.of(StructrTraits.USER);
			final NodeInterface admin = app.create(StructrTraits.USER, "admin");

			admin.setProperty(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY),         "admin");
			admin.setProperty(traits.key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY),         true);
			admin.setProperty(traits.key(PrincipalTraitDefinition.SESSION_IDS_PROPERTY),      new String[] { ADMIN_SESSION });
			admin.setProperty(traits.key(PrincipalTraitDefinition.TWO_FACTOR_TOKEN_PROPERTY), TWO_FACTOR);

			admin.setVisibleToAuthenticatedUsers(true);

			tx.success();

			return admin.getUuid();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}

		return null;
	}
}
