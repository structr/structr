/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.test.websocket.mock;

import org.structr.common.SecurityContext;
import org.structr.core.entity.Principal;
import org.structr.websocket.StructrWebSocket;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertNull;
import static org.testng.AssertJUnit.assertEquals;

public class BasicWebsocketTest extends StructrWebsocketBaseTest {

	@Test
	public void testWebsocketLoginSuccess() {

		createEntityAsSuperUser("/User","{ name: admin, password: admin, isAdmin: true }");

		final MockedWebsocketSetup mock  = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();
		final String sessionId           = "TESTSESSION";

		login(websocket, "admin", "admin", sessionId);

		final SecurityContext securityContext = websocket.getSecurityContext();
		final Principal userInWebsocket       = websocket.getCurrentUser();
		final Principal userInSession         = securityContext.getUser(false);

		assertNotNull(userInWebsocket);
		assertNotNull(userInSession);

		final Map<String, Object> response = assertResponse(mock, "LOGIN", 200, true);
		final Map<String, Object> data     = (Map)response.get("data");

		assertEquals("Invalid session ID in websocket ping response", sessionId, response.get("sessionId"));
		assertEquals("Invalid username in node data after websocket login", "admin", data.get("username"));
	}

	@Test
	public void testWebsocketLoginFailure() {

		createEntityAsSuperUser("/User","{ name: admin, password: admin, isAdmin: true }");

		final MockedWebsocketSetup mock  = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();
		final String sessionId           = "TESTSESSION";

		login(websocket, "admin", "wrong", sessionId);

		final SecurityContext securityContext = websocket.getSecurityContext();

		assertNull(securityContext);

		assertResponse(mock, "STATUS", 403, false);
	}

	@Test
	public void testWebsocketPingWithoutSession() {

		final MockedWebsocketSetup mock = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();
		final String sessionId = "TESTSESSION";

		websocket.onWebSocketText(toJson(Map.of(
			"command", "PING",
			"sessionId", sessionId
		)));

		assertResponse(mock, "STATUS", 401, false);
	}

	@Test
	public void testWebsocketPingWithSession() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");
		createEntityAsSuperUser("/SessionDataNode", "{ vhost: '0.0.0.0', sessionId: 'TESTSESSION' }");

		final MockedWebsocketSetup mock = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();
		final String sessionId = "TESTSESSION";

		login(websocket, "admin", "admin", sessionId);

		try { Thread.sleep(1000); } catch (Throwable t) {}

		websocket.onWebSocketText(toJson(Map.of(
			"command", "PING",
			"sessionId", sessionId
		)));

		final SecurityContext securityContext = websocket.getSecurityContext();
		final Principal userInWebsocket       = websocket.getCurrentUser();
		final Principal userInSession         = securityContext.getUser(false);

		assertNotNull(userInWebsocket);
		assertNotNull(userInSession);

		final Map<String, Object> response = assertResponse(mock, "STATUS", 100, true);
		final Map<String, Object> data     = (Map)response.get("data");

		assertEquals("Invalid username in node data in websocket ping response", "admin", data.get("username"));
		assertEquals("Invalid isAdmin flag in node data in websocket ping response", true, data.get("isAdmin"));
	}

	@Test
	public void testWebsocketGetWithDefaultView() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");
		createEntityAsSuperUser("/SessionDataNode", "{ vhost: '0.0.0.0', sessionId: 'TESTSESSION' }");

		// create test object to GET
		final String id = createEntityAsSuperUser("/Group", "{ name: Testgroup }");

		final MockedWebsocketSetup mock = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();
		final String sessionId = "TESTSESSION";

		login(websocket, "admin", "admin", sessionId);

		try { Thread.sleep(1000); } catch (Throwable t) {}

		websocket.onWebSocketText(toJson(Map.of(
			"command", "GET",
			"sessionId", sessionId,
			"id", id
		)));

		final Map<String, Object> response     = assertResponse(mock, "GET", 200, true);
		final List<Map<String, Object>> result = (List)response.get("result");
		final Map<String, Object> first        = result.get(0);

		assertEquals("Group",                            first.get("type"));
		assertEquals("Testgroup",                        first.get("name"));
		assertEquals("00000000000000000000000000000000", first.get("createdBy"));
		assertEquals(null,                               first.get("jwksReferenceId"));
		assertEquals(null,                               first.get("owner"));
		assertEquals(false,                              first.get("visibleToPublicUsers"));
		assertEquals(false,                              first.get("visibleToAuthenticatedUsers"));
		assertEquals(false,                              first.get("hidden"));
		assertEquals(false,                              first.get("blocked"));
		assertEquals(true,                               first.get("isGroup"));
		assertEquals(0,                                  ((List)first.get("members")).size());
		assertEquals(id,                                         first.get("id"));

	}

	@Test
	public void testWebsocketGetWithCustomView() {

		final String sessionId = "TESTSESSION";

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");
		createEntityAsSuperUser("/SessionDataNode", "{ vhost: '0.0.0.0', sessionId: '" + sessionId + "' }");

		// create test object to GET
		final String id = createEntityAsSuperUser("/Group", "{ name: Testgroup }");

		final MockedWebsocketSetup mock = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();

		login(websocket, "admin", "admin", sessionId);

		try { Thread.sleep(1000); } catch (Throwable t) {}

		websocket.onWebSocketText(toJson(Map.of(
			"command", "GET",
			"sessionId", sessionId,
			"id", id,
			"data", Map.of(
				"properties", "id,type,name,visibleToPublicUsers"
			)
		)));

		final Map<String, Object> response     = assertResponse(mock, "GET", 200, true);
		final List<Map<String, Object>> result = (List)response.get("result");
		final Map<String, Object> first        = result.get(0);

		// check properties given above
		assertEquals("Group",     first.get("type"));
		assertEquals("Testgroup", first.get("name"));
		assertEquals(false,       first.get("visibleToPublicUsers"));
		assertEquals(id,                   first.get("id"));

		// everything else must be null
		assertEquals(null,        first.get("createdBy"));
		assertEquals(null,        first.get("jwksReferenceId"));
		assertEquals(null,        first.get("owner"));
		assertEquals(null,        first.get("visibleToAuthenticatedUsers"));
		assertEquals(null,        first.get("hidden"));
		assertEquals(null,        first.get("blocked"));
		assertEquals(null,        first.get("isGroup"));
		assertEquals(null,        first.get("members"));

	}
}
