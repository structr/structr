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

import org.apache.pulsar.shade.org.apache.commons.io.IOUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.GroupTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.web.entity.File;
import org.structr.websocket.StructrWebSocket;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertNull;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertEquals;

public class BasicWebsocketTest extends StructrWebsocketBaseTest {

	@Test
	public void testLoginSuccess() {

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
	public void testLoginFailure() {

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
	public void testPingWithoutSession() {

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
	public void testPingWithSession() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");
		createEntityAsSuperUser("/SessionDataNode", "{ vhost: '0.0.0.0', sessionId: 'TESTSESSION' }");

		final MockedWebsocketSetup mock = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();
		final String sessionId = "TESTSESSION";

		login(websocket, "admin", "admin", sessionId);

		try { Thread.sleep(200); } catch (Throwable t) {}

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
	public void testGetWithDefaultView() {

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");
		createEntityAsSuperUser("/SessionDataNode", "{ vhost: '0.0.0.0', sessionId: 'TESTSESSION' }");

		// create test object to GET
		final String id = createEntityAsSuperUser("/Group", "{ name: Testgroup }");

		final MockedWebsocketSetup mock = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();
		final String sessionId = "TESTSESSION";

		login(websocket, "admin", "admin", sessionId);

		try { Thread.sleep(200); } catch (Throwable t) {}

		websocket.onWebSocketText(toJson(Map.of(
			"command", "GET",
			"sessionId", sessionId,
			"id", id
		)));

		final Map<String, Object> response     = assertResponse(mock, "GET", 200, true);
		final List<Map<String, Object>> result = (List)response.get("result");
		final Map<String, Object> first        = result.get(0);

		assertEquals("Group",                            first.get(GraphObjectTraitDefinition.TYPE_PROPERTY));
		assertEquals("Testgroup",                        first.get(NodeInterfaceTraitDefinition.NAME_PROPERTY));
		assertEquals("00000000000000000000000000000000", first.get(GraphObjectTraitDefinition.CREATED_BY_PROPERTY));
		assertEquals(null,                               first.get(GroupTraitDefinition.JWKS_REFERENCE_ID_PROPERTY));
		assertEquals(null,                               first.get(NodeInterfaceTraitDefinition.OWNER_PROPERTY));
		assertEquals(false,                              first.get(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY));
		assertEquals(false,                              first.get(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY));
		assertEquals(false,                              first.get(NodeInterfaceTraitDefinition.HIDDEN_PROPERTY));
		assertEquals(false,                              first.get(PrincipalTraitDefinition.BLOCKED_PROPERTY));
		assertEquals(true,                               first.get(GroupTraitDefinition.IS_GROUP_PROPERTY));
		assertEquals(0,                                  ((List)first.get(GroupTraitDefinition.MEMBERS_PROPERTY)).size());
		assertEquals(id,                                          first.get(GraphObjectTraitDefinition.ID_PROPERTY));

	}

	@Test
	public void testGetWithCustomView() {

		final String sessionId = "TESTSESSION";

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");
		createEntityAsSuperUser("/SessionDataNode", "{ vhost: '0.0.0.0', sessionId: '" + sessionId + "' }");

		// create test object to GET
		final String id = createEntityAsSuperUser("/Group", "{ name: Testgroup }");

		final MockedWebsocketSetup mock = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();

		login(websocket, "admin", "admin", sessionId);

		try { Thread.sleep(200); } catch (Throwable t) {}

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
		assertEquals("Group",     first.get(GraphObjectTraitDefinition.TYPE_PROPERTY));
		assertEquals("Testgroup", first.get(NodeInterfaceTraitDefinition.NAME_PROPERTY));
		assertEquals(false,       first.get(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY));
		assertEquals(id,                   first.get(GraphObjectTraitDefinition.ID_PROPERTY));

		// everything else must be null
		assertEquals(null,        first.get(GraphObjectTraitDefinition.CREATED_BY_PROPERTY));
		assertEquals(null,        first.get(GroupTraitDefinition.JWKS_REFERENCE_ID_PROPERTY));
		assertEquals(null,        first.get(NodeInterfaceTraitDefinition.OWNER_PROPERTY));
		assertEquals(null,        first.get(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY));
		assertEquals(null,        first.get(NodeInterfaceTraitDefinition.HIDDEN_PROPERTY));
		assertEquals(null,        first.get(PrincipalTraitDefinition.BLOCKED_PROPERTY));
		assertEquals(null,        first.get(GroupTraitDefinition.IS_GROUP_PROPERTY));
		assertEquals(null,        first.get(GroupTraitDefinition.MEMBERS_PROPERTY));
	}

	@Test
	public void testCreate() {

		final Map<String, String> data = Map.of(
			"File", "test_create.txt",
			"Folder", "TestFolder",
			"User", "TestUser",
			"Group", "TestGroup",
			"Page", "TestPage"
		);

		final String sessionId = "TESTSESSION";

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");
		createEntityAsSuperUser("/SessionDataNode", "{ vhost: '0.0.0.0', sessionId: '" + sessionId + "' }");

		final MockedWebsocketSetup mock = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();

		login(websocket, "admin", "admin", sessionId);

		try {
			Thread.sleep(200);
		} catch (Throwable t) {
		}

		// iterate over the above data map which contains type -> name mappings
		// for all the different objects we want to create

		for (final String type : data.keySet()) {

			final String name = data.get(type);

			websocket.onWebSocketText(toJson(Map.of(
				"command", "CREATE",
				"sessionId", sessionId,
				"data", Map.of(
					"name", name
				)
			)));

			final Map<String, Object> createResponse = assertResponse(mock, "STATUS", 422, true);
			assertEquals("Empty type (null). Please supply a valid class name in the type property.", createResponse.get("message"));

			websocket.onWebSocketText(toJson(Map.of(
				"command", "CREATE",
				"sessionId", sessionId,
				"data", Map.of(
					"name", name,
					"type", type
				)
			)));

			// successful create does not send a response, so no response check here,
			// but we can check that the object exists
			try (final Tx tx = app.tx()) {

				AssertJUnit.assertNotNull(type + " " + name + " was not created correctly", app.nodeQuery(type).andName(name).getFirst());

				tx.success();

			} catch (FrameworkException t) {
				fail("Unexpected exception: " + t.getMessage());
			}
		}
	}

	@Test
	public void testModificationAndDeletionOfExistingFile() {

		final String sessionId      = "TESTSESSION";
		final String testContent    = "This is a test string!";
		final double expectedLength = testContent.length();
		final String base64Data     = Base64.getEncoder().encodeToString(testContent.getBytes());

		createEntityAsSuperUser("/User", "{ name: admin, password: admin, isAdmin: true }");
		createEntityAsSuperUser("/SessionDataNode", "{ vhost: '0.0.0.0', sessionId: '" + sessionId + "' }");

		// create file to work with
		final String fileId = createEntityAsUser("admin", "admin", "/File", "{ name: test.txt }");

		final MockedWebsocketSetup mock = getMockedWebsocketSetup();
		final StructrWebSocket websocket = mock.getWebSocket();

		login(websocket, "admin", "admin", sessionId);

		try { Thread.sleep(200); } catch (Throwable t) {}

		// #####################################################################################################
		// change file contents using the CHUNK command

		websocket.onWebSocketText(toJson(Map.of(
			"command", "CHUNK",
			"sessionId", sessionId,
			"id", fileId,
			"data", Map.of(
				"chunkId", 0,
				"chunkSize", 65536,
				"chunk", base64Data,
				"chunks", 1
			)
		)));

		final Map<String, Object> editResponse = assertResponse(mock, "STATUS", 200, true);
		final String message                   = (String) editResponse.get("message");
		final Map<String, Object> data         = fromJson(message);

		// check properties given above
		assertEquals(fileId, data.get("id"));
		assertEquals("test.txt", data.get("name"));
		assertEquals(expectedLength, data.get("size"));

		// check that file has given content
		try (final Tx tx = app.tx()) {

			final File file          = app.getNodeById(fileId).as(File.class);
			final List<String> lines = IOUtils.readLines(file.getInputStream(), "utf-8");

			assertEquals(1, lines.size());
			assertEquals(testContent, lines.get(0));

			tx.success();

		} catch (FrameworkException | IOException t) {
			fail("Unexpected exception: " + t.getMessage());
		}

		// #####################################################################################################
		// delete the file

		websocket.onWebSocketText(toJson(Map.of(
			"command", "DELETE",
			"sessionId", sessionId,
			"id", fileId
		)));

		// delete command does not send a response, so no check here..

		// verify that the file is gone
		try (final Tx tx = app.tx()) {

			assertNull(app.getNodeById(fileId));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception: " + fex.getMessage());
		}
	}
}
