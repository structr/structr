/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import io.restassured.RestAssured;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.GroupTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.web.entity.File;
import org.structr.websocket.StructrWebSocket;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Base64;
import java.util.LinkedList;
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

				AssertJUnit.assertNotNull(type + " " + name + " was not created correctly", app.nodeQuery(type).name(name).getFirst());

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

	@Test
	public void testCreateWithNestedObject() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonObjectType project  = schema.addType("Project");
			final JsonObjectType task     = schema.addType("Task");

			project.relate(task, "HAS", Cardinality.OneToMany, "project", "tasks");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			AssertJUnit.fail(fex.getMessage());
		}

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

		// create folder and store uuid
		final String taskId = createEntityAsSuperUser("/Task", "{ name: 'Test Task' }");

		websocket.onWebSocketText(toJson(Map.of(
			"command", "CREATE",
			"sessionId", sessionId,
			"data", Map.of(
				"name", "Test Project",
				"type", "Project",
				"tasks", List.of(Map.of(
					"id", taskId
				))
			)
		)));

		// successful create does not send a response, so no response check here,
		// but we can check that the object exists
		try (final Tx tx = app.tx()) {

			final NodeInterface project = app.nodeQuery("Project").getFirst();

			AssertJUnit.assertNotNull("Entity was not created correctly", project);


			final PropertyKey<Iterable<NodeInterface>> tasksKey = project.getTraits().key("tasks");

			final List tasks = Iterables.toList(project.getProperty(tasksKey));

			AssertJUnit.assertEquals("Relationship was not created correctly", 1, tasks.size());

			tx.success();

		} catch (FrameworkException t) {
			fail("Unexpected exception: " + t.getMessage());
		}
	}

	@Test
	public void testRawResultCountWithCypher() {

		// setup: create 100 Localizations
		final List<String> parts = new LinkedList<>();

		for (int i=0; i<100; i++) {
			parts.add("{ name: test" + i + ", locale: de, domain: test, localizedName: localizedTest" + i + " }");
		}

		RestAssured
			.given()
			.contentType("application/json; charset=UTF-8")
			.body(" [ " + StringUtils.join(parts, ", ") + " ] ")
			.when().post("/Localization").getBody().as(Map.class);

		try { Thread.sleep(100); } catch (Throwable t) {}


		final String query     = "MATCH (n:Localization) RETURN DISTINCT { name: n.name, domain: n.domain } as res ORDER BY res.name asc";
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

		websocket.onWebSocketText(toJson(Map.of(
			"command", "SEARCH",
			"sessionId", sessionId,
			"data", Map.of(
				"cypherQuery", query
			),
			"pageSize", 25
		)));

		final Map<String, Object> data = mock.getLastWebsocketResponse();

		assertEquals("Invalid status code in websocket response to SEARCH query with Cypher", 200.0, data.get("code"));
		assertEquals("Invalid raw result count in websocket response to SEARCH query with Cypher", 100.0, data.get("rawResultCount"));
		assertEquals("Invalid page size in websocket response to SEARCH query with Cypher", 25.0, data.get("pageSize"));
		assertEquals("Invalid result size in websocket response to SEARCH query with Cypher", 25, ((List) data.get("result")).size());
	}

}
