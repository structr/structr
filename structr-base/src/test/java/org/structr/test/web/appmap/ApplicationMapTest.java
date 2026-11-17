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
package org.structr.test.web.appmap;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.structr.api.config.Settings;
import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractSchemaNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.RelationshipInterfaceTraitDefinition;
import org.structr.core.traits.definitions.ResourceAccessTraitDefinition;
import org.structr.core.traits.definitions.SchemaGrantTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.core.traits.definitions.SchemaNodeTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.core.traits.definitions.SchemaRelationshipNodeTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;
import org.structr.web.traits.definitions.PagePathParameterTraitDefinition;
import org.structr.web.traits.definitions.ParameterMappingTraitDefinition;
import org.structr.web.traits.definitions.PagePathTraitDefinition;
import org.structr.web.traits.definitions.SiteTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.structr.web.traits.definitions.dom.PageTraitDefinition;
import org.structr.websocket.command.CreateComponentCommand;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.*;

/**
 * Tests the application map endpoint: the flat graph of types, methods, pages, repeaters and
 * action mappings, how the types of repeaters and action mappings are resolved, and the CRUD
 * analysis behind the business edges.
 */
public class ApplicationMapTest extends StructrUiTest {

	private final Map<String, String> ids = new HashMap<>();

	@Test
	public void testMapHasFourPartsAndEdgesAreTuples() {

		createApplication();

		final JsonPath map = fetchMap();

		assertNotNull("The map must have nodes",   map.getList("result.nodes"));
		assertNotNull("The map must have edges",   map.getList("result.edges"));
		assertNotNull("The map must have groups",  map.getList("result.groups"));
		assertNotNull("The map must have helpers", map.getList("result.helpers"));

		for (final Map<String, Object> edge : map.<Map<String, Object>>getList("result.edges")) {
			assertTrue("Every edge must have source, target, weight, kind and evidence, and may have details", edge.keySet().containsAll(List.of("source", "target", "weight", "kind", "evidence")) && (!edge.containsKey("details") || edge.get("details") instanceof Map));
		}

		assertEquals("A schema type must be a node of kind type", "type", node(map, ids.get("Item"), "kind"));
		assertEquals("A type method must be named after its type", "Item.archive", node(map, ids.get("archive"), "name"));
		assertEquals("A service class must be marked", Boolean.TRUE, map.get("result.nodes.find { it.id == '" + ids.get("ItemService") + "' }.service"));
		assertNull("A type that is no service class must not be marked", map.get("result.nodes.find { it.id == '" + ids.get("Item") + "' }.service"));
		assertEquals("A page outline must start with the html element", "html", map.getString("result.nodes.find { it.id == '" + ids.get("page") + "' }.outline[0].tag"));
	}

	@Test
	public void testGroupingsAndAreas() {

		createApplication();

		final JsonPath map = fetchMap();

		assertEquals("A type must be a grouping of its own",                            ids.get("Item"),        node(map, ids.get("Item"), "grouping"));
		assertEquals("A type method must belong to the grouping of its type",           ids.get("Item"),        node(map, ids.get("archive"), "grouping"));
		assertEquals("An action must belong to the grouping of its page",               ids.get("page"),        node(map, ids.get("archiveAction"), "grouping"));
		assertEquals("A repeater must belong to the grouping of its page",              ids.get("page"),        node(map, ids.get("itemRepeater"), "grouping"));
		assertEquals("A user-defined function that writes nothing must stand alone",    ids.get("formatItem"),  node(map, ids.get("formatItem"), "grouping"));
		assertEquals("The application's own type must be part of the data model",       "data",                 node(map, ids.get("Item"), "area"));
		assertEquals("A member must be in the area of its grouping",                    "data",                 node(map, ids.get("archive"), "area"));
		assertEquals("A service class must be part of the logic",                       "logic",                node(map, ids.get("ItemService"), "area"));
		assertEquals("A standalone user-defined function must be part of the logic",    "logic",                node(map, ids.get("formatItem"), "area"));
		assertEquals("A page must be part of the user interface",                       "ui",                   node(map, ids.get("page"), "area"));
		assertEquals("A group must be part of the security", "security", map.getString("result.groups.find { it.id == '" + ids.get("group") + "' }.area"));
	}

	@Test
	public void testDataModelEdges() {

		createApplication();

		final JsonPath map = fetchMap();

		assertTrue("A type method must be a member of its type",          hasEdge(map, ids.get("archive"), ids.get("Item"), "member"));
		assertTrue("A subtype must inherit from its parent",               hasEdge(map, ids.get("DerivedItem"), ids.get("Item"), "inherits"));
		assertTrue("A schema relationship must relate source and target", hasEdge(map, ids.get("Category"), ids.get("Item"), "relates"));
	}

	@Test
	public void testRepeaterTypes() {

		createApplication();

		final JsonPath map = fetchMap();

		assertEquals("A repeater must show the type its query finds",                  "Item",         node(map, ids.get("itemRepeater"), "type"));
		assertEquals("A repeater type from the query must be marked as such",           "query",        node(map, ids.get("itemRepeater"), "how"));
		assertEquals("A nested key must follow the relationship of its parent key",    "Category",     node(map, ids.get("categoryRepeater"), "type"));
		assertEquals("A type from a relationship path must be marked as such",         "relationship", node(map, ids.get("categoryRepeater"), "how"));
		assertEquals("A query that names no type must leave it to the properties read below", "Category", node(map, ids.get("rowRepeater"), "type"));
		assertEquals("A type from the properties read must be marked as such",         "properties",   node(map, ids.get("rowRepeater"), "how"));
		assertTrue("A page must contain its repeaters",                                hasEdge(map, ids.get("page"), ids.get("itemRepeater"), "contains"));
		assertTrue("A repeater must read the type it shows",                           hasEdge(map, ids.get("itemRepeater"), ids.get("Item"), "reads"));
	}

	@Test
	public void testOutlineNamesRepeatersAndActionsAtTheirElements() {

		createApplication();

		final JsonPath map        = fetchMap();
		final List<Object> outline = map.getList("result.nodes.find { it.id == '" + ids.get("page") + "' }.outline");
		final Set<String> found    = new HashSet<>();

		collectOutlineIds(outline, found);

		assertTrue("The outline must name a repeater at its element", found.contains(ids.get("itemRepeater")));
		assertTrue("The outline must give the id of an element, so that its settings can be changed", collectOutlineElements(outline, new HashSet<>()).contains(ids.get("itemRepeater")));
		assertTrue("The outline must mark the elements that hold a text of their own", itemsOf(outline).stream().anyMatch(item -> Boolean.TRUE.equals(item.get("text"))));
		assertTrue("The outline must name the data sources of a repeater that are set, so that a client can point out one that cannot work", collectOutlineSources(outline, new HashSet<>()).contains("functionQuery"));

		for (final String action : List.of("archiveAction", "updateAction", "createAction", "deleteAction", "callAction")) {
			assertTrue("The outline must name every action mapping of the page at its trigger element, missing " + action, found.contains(ids.get(action)));
		}
	}

	@Test
	public void testActionMappings() {

		createApplication();

		final JsonPath map = fetchMap();

		assertEquals("An action without data type must act on the type of the key in its id expression", "Item",       node(map, ids.get("archiveAction"), "type"));
		assertEquals("A type from the scope must name the key",                                            "scope:item", node(map, ids.get("archiveAction"), "how"));
		assertEquals("An action must name the method it calls, resolved on its type",                      ids.get("archive"), node(map, ids.get("archiveAction"), "method"));
		assertEquals("An action mapping must name its action",                                             "method",     node(map, ids.get("archiveAction"), "action"));
		assertEquals("An action mapping must name the event that triggers it",                             "click",      node(map, ids.get("archiveAction"), "event"));
		assertTrue("An action must invoke its method",                                                    hasEdge(map, ids.get("archiveAction"), ids.get("archive"), "invokes"));
		assertTrue("An action must update what its method updates",                                       crud(map, ids.get("archiveAction")).contains("U:Item"));
		assertEquals("A create action must create its data type and nothing else",                        List.of("C:Category"), crud(map, ids.get("createAction")));
		assertTrue("A create action must be an edge that creates the type",                               hasEdge(map, ids.get("createAction"), ids.get("Category"), "creates"));
		assertEquals("An empty data type, as the editor leaves it, must not hide the type of the id expression", "Item", node(map, ids.get("updateAction"), "type"));
		assertTrue("An update action on the key of a repeater must update its type",                     crud(map, ids.get("updateAction")).contains("U:Item"));
		assertEquals("A key outside every repeater must name the type it is named after",                 "Category",              node(map, ids.get("deleteAction"), "type"));
		assertEquals("A type from the key name must say so",                                               "key name:category",     node(map, ids.get("deleteAction"), "how"));
		assertTrue("A delete action on a key named after a type must delete that type",                   hasEdge(map, ids.get("deleteAction"), ids.get("Category"), "deletes"));
		assertEquals("A method called without a type must be the only method of that name",              ids.get("archive"),      node(map, ids.get("callAction"), "method"));
		assertTrue("An action must invoke the only method of its name",                                   hasEdge(map, ids.get("callAction"), ids.get("archive"), "invokes"));
	}

	@Test
	public void testRestAccessOpensTypesAndMethods() {

		createApplication();

		final Traits traits = Traits.of(StructrTraits.RESOURCE_ACCESS);

		try (final Tx tx = app.tx()) {

			for (final Object[] grant : new Object[][] { { "Item", 17L }, { "Item/_Ui", 1L }, { "ItemService/createItem", 4L }, { "Item/Archive", 4L }, { "listItems", 8192L } }) {

				app.create(StructrTraits.RESOURCE_ACCESS,
					new NodeAttribute<>(traits.key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY), (String) grant[0]),
					new NodeAttribute<>(traits.key(ResourceAccessTraitDefinition.FLAGS_PROPERTY),     (Long) grant[1])
				);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final JsonPath map  = fetchMap();
		final String item   = "result.nodes.find { it.id == '" + ids.get("Item") + "' }.rest";

		assertEquals("Every grant on a type or one of its views must count as a rule of the type", 2, map.getInt(item + ".rules"));
		assertTrue("A grant for anonymous users must mark the type as public",                           map.getBoolean(item + ".public"));
		assertTrue("A grant for authenticated users must mark the type as such",                         map.getBoolean(item + ".authenticated"));
		assertFalse("A grant that only reads must not mark the type as writable",                        map.getBoolean(item + ".publicWrite") || map.getBoolean(item + ".authenticatedWrite"));
		assertTrue("A POST grant on a method must open the endpoint for writing to authenticated users", map.getBoolean("result.nodes.find { it.id == 'api:ItemService/createItem' }.rest.authenticatedWrite"));
		assertTrue("A PATCH grant for public users must open the endpoint to them",                      map.getBoolean("result.nodes.find { it.id == 'api:listItems' }.rest.public"));
		assertTrue("A PATCH grant for public users must let them write",                                 map.getBoolean("result.nodes.find { it.id == 'api:listItems' }.rest.publicWrite"));
		assertNull("A type without grants must not carry REST access", map.get("result.nodes.find { it.id == '" + ids.get("Category") + "' }.rest"));
		assertTrue("The grants on a type must make an endpoint for the type", hasEdge(map, "api:Item", ids.get("Item"), "endpoint"));
		assertEquals("Every grant on a type or one of its views must count as a rule of its endpoint", 2, map.getInt("result.nodes.find { it.id == 'api:Item' }.rest.rules"));
		assertEquals("The endpoint of a type must be part of the REST API", "api", node(map, "api:Item", "area"));
		assertEquals("The endpoint of a method must be part of the REST API", "api", node(map, "api:ItemService/createItem", "area"));
		assertNull("A type without grants must not become an endpoint", map.get("result.nodes.find { it.id == 'api:Category' }"));
		assertTrue("A grant on a method must make the method an endpoint", hasEdge(map, "api:ItemService/createItem", ids.get("createItem"), "endpoint"));
		assertTrue("A grant that spells the method with a capital letter, as older versions wrote it, must find the method", hasEdge(map, "api:Item/Archive", ids.get("archive"), "endpoint"));
	}

	@Test
	public void testUserEndpointsStandForTheUsers() {

		createApplication();

		final Traits nodeTraits = Traits.of(StructrTraits.SCHEMA_NODE);
		final Traits traits     = Traits.of(StructrTraits.RESOURCE_ACCESS);

		try (final Tx tx = app.tx()) {

			final NodeInterface member = app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(nodeTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),            "Member"),
				new NodeAttribute<>(nodeTraits.key(SchemaNodeTraitDefinition.INHERITED_TRAITS_PROPERTY), new String[] { StructrTraits.USER })
			);

			ids.put("Member", member.getUuid());
			ids.put("updateProfile", createMethod("updateProfile", member, "{ $.me.name = 'member'; }").getUuid());

			for (final Object[] grant : new Object[][] { { "_login", 64L }, { "_logout", 4L }, { "_registration", 64L }, { "_resetPassword", 64L }, { "_token", 64L }, { "me", 1L }, { "me/_Ui", 1L }, { "me/updateProfile", 4L }, { "_schema", 1L } }) {

				app.create(StructrTraits.RESOURCE_ACCESS,
					new NodeAttribute<>(traits.key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY), (String) grant[0]),
					new NodeAttribute<>(traits.key(ResourceAccessTraitDefinition.FLAGS_PROPERTY),     (Long) grant[1])
				);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final JsonPath map = fetchMap();
		final String user  = map.getString("result.nodes.find { it.kind == 'type' && it.name == 'User' }.id");

		for (final String endpoint : new String[] { "_login", "_logout", "_registration", "_resetPassword", "_token", "me" }) {

			assertTrue("The endpoint " + endpoint + " must stand for the users", hasEdge(map, "api:" + endpoint, user, "endpoint"));
			assertEquals("The endpoint " + endpoint + " must be part of the REST API", "api", node(map, "api:" + endpoint, "area"));
		}

		assertTrue("A grant for anonymous users on the registration must open it to them",          map.getBoolean("result.nodes.find { it.id == 'api:_registration' }.rest.public"));
		assertEquals("The grants on the current user and its views must make one endpoint",      2, map.getInt("result.nodes.find { it.id == 'api:me' }.rest.rules"));
		assertNull("The endpoints of the users must not count as REST access to the users as data", map.get("result.nodes.find { it.id == '" + user + "' }.rest"));
		assertTrue("A grant on a method of the current user must make the method of a user type an endpoint", hasEdge(map, "api:me/updateProfile", ids.get("updateProfile"), "endpoint"));
		assertNull("A grant on the schema must not become an endpoint", map.get("result.nodes.find { it.id == 'api:_schema' }"));
		assertEquals("Users the application does not extend must be on the map as the built-in type", "type:User", user);
		assertEquals("The built-in users must lie among the system types", "builtin", node(map, user, "area"));
		assertTrue("A type that extends the built-in users must inherit from them", hasEdge(map, ids.get("Member"), user, "inherits"));
	}

	@Test
	public void testUserEndpointsStandForTheExtendedUsers() {

		createApplication();

		final Traits nodeTraits = Traits.of(StructrTraits.SCHEMA_NODE);
		final Traits traits     = Traits.of(StructrTraits.RESOURCE_ACCESS);

		try (final Tx tx = app.tx()) {

			ids.put("User", app.create(StructrTraits.SCHEMA_NODE, new NodeAttribute<>(nodeTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), StructrTraits.USER)).getUuid());

			app.create(StructrTraits.RESOURCE_ACCESS,
				new NodeAttribute<>(traits.key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY), "_login"),
				new NodeAttribute<>(traits.key(ResourceAccessTraitDefinition.FLAGS_PROPERTY),     64L)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final JsonPath map = fetchMap();

		assertTrue("The login must stand for the type User the application extends", hasEdge(map, "api:_login", ids.get("User"), "endpoint"));
		assertNull("Users the application extends must not be on the map a second time", map.get("result.nodes.find { it.id == 'type:User' }"));
	}

	@Test
	public void testPublicPagesAreMarked() {

		createApplication();

		try (final Tx tx = app.tx()) {

			app.getNodeById(StructrTraits.PAGE, ids.get("page")).setVisibleToPublicUsers(true);
			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final JsonPath map = fetchMap();

		assertEquals("A page visible to public users must be marked public",        "true", node(map, ids.get("page"), "public"));
		assertEquals("The access of a page must carry its visibility to public users", "true", node(map, ids.get("page"), "access.public"));
		assertEquals("The access of a page must carry its visibility to authenticated users", "false", node(map, ids.get("page"), "access.authenticated"));
	}

	@Test
	public void testSizes() {

		createApplication();

		final JsonPath map = fetchMap();

		assertEquals("A method must measure the lines of its code", "1", node(map, ids.get("archive"), "size"));
		assertTrue("A page must measure its HTML elements", Integer.parseInt(node(map, ids.get("page"), "size")) > 5);
		assertTrue("A type must measure its properties", Integer.parseInt(node(map, ids.get("Item"), "size")) > 0);
	}

	@Test
	public void testScheduledMethodsCarryTheirCronExpression() {

		createApplication();

		Settings.CronTasks.setValue("listItems ItemService.createItem");
		Settings.createSettingForValue(Settings.cronGroup, "listItems.cronExpression",              "0 0 2 * * *");
		Settings.createSettingForValue(Settings.cronGroup, "ItemService.createItem.cronExpression", "0 */5 * * * *");

		try {

			final JsonPath map = fetchMap();

			assertEquals("A user-defined function listed in CronService.tasks must carry its schedule", "0 0 2 * * *",   node(map, ids.get("listItems"), "cron"));
			assertEquals("A static method listed as Type.method must carry its schedule",               "0 */5 * * * *", node(map, ids.get("createItem"), "cron"));
			assertNull("A method that is not scheduled must not carry a schedule",                      node(map, ids.get("formatItem"), "cron"));
			assertEquals("A scheduled user-defined function must be a grouping of its own",            ids.get("listItems"), node(map, ids.get("listItems"), "grouping"));
			assertEquals("A scheduled user-defined function must be a scheduled task",                 "scheduled",          node(map, ids.get("listItems"), "area"));

		} finally {

			Settings.CronTasks.setValue("");
		}
	}

	@Test
	public void testMethodFootprintAndHelpers() {

		createApplication();

		final JsonPath map = fetchMap();

		assertTrue("$.create must create the type",                          crud(map, ids.get("createItem")).contains("C:Item"));
		assertTrue("$.find must read the type",                              crud(map, ids.get("createItem")).contains("R:Category"));
		assertTrue("Writing a property of a created object must update it",  crud(map, ids.get("createItem")).contains("U:Item"));
		assertTrue("Writing a property of $.this must update the owner",     crud(map, ids.get("archive")).contains("U:Item"));
		assertTrue("A method called from four places that writes nothing must be a helper", map.getList("result.helpers").contains(ids.get("formatItem")));
		assertTrue("A call of a helper method must be a helper edge",        hasEdge(map, ids.get("listItems"), ids.get("formatItem"), "helper"));
	}

	@Test
	public void testExternalServicesAreEdges() {

		createApplication();

		final JsonPath map = fetchMap();

		assertEquals("A host a script sends requests to must be an external service", "external", node(map, "ext:api.example.com", "kind"));
		assertTrue("A request with $.GET must be an edge to the host",                               hasEdge(map, ids.get("syncItems"), "ext:api.example.com", "requests"));
		assertTrue("A request with the older Structr.POST must be an edge to the host",              hasEdge(map, ids.get("syncItems"), "ext:legacy.example.com", "requests"));
		assertTrue("Sending mail must be an edge to the mail service, whatever the spelling of the function", hasEdge(map, ids.get("syncItems"), "ext:mail", "mails"));
		assertEquals("The mail service must say that it sends e-mail, and through which server", "Send e-mail via " + Settings.SmtpHost.getValue(), node(map, "ext:mail", "name"));
		assertEquals("An external service must be part of the integrations", "integration", node(map, "ext:api.example.com", "area"));
		assertEquals("The use of an external service must name the function that calls it", "GET", map.getString("result.edges.find { it.source == '" + ids.get("syncItems") + "' && it.target == 'ext:api.example.com' }.details.function"));
		assertTrue("The use of an external service must show the line of the call", map.getString("result.edges.find { it.source == '" + ids.get("syncItems") + "' && it.target == 'ext:api.example.com' }.details.code").contains("GET("));
	}

	@Test
	public void testRequestsCountForTheirEntryPoints() {

		createApplication();

		RestAssured.basePath = "/";

		RestAssured.given().header(X_USER_HEADER, ADMIN_USERNAME).header(X_PASSWORD_HEADER, ADMIN_PASSWORD).expect().statusCode(200).when().get("/items");

		RestAssured.basePath = "/structr/rest";

		RestAssured.given().header(X_USER_HEADER, ADMIN_USERNAME).header(X_PASSWORD_HEADER, ADMIN_PASSWORD).expect().statusCode(200).when().get("/Item");

		// a page is rendered on a thread of its own, which records the statistics after the response has gone out
		JsonPath map = fetchMap();

		for (int i = 0; i < 20 && node(map, ids.get("page"), "requests") == null; i++) {

			try {

				Thread.sleep(100);

			} catch (InterruptedException ignore) {
			}

			map = fetchMap();
		}

		assertNotNull("A rendered page must count its requests",                     node(map, ids.get("page"), "requests"));
		assertTrue("A REST request to a type without an endpoint must count for the type", Integer.parseInt(node(map, ids.get("Item"), "requests")) >= 1);
		assertNull("An element nobody requested must carry no requests",              node(map, ids.get("formatItem"), "requests"));

		final JsonPath activity = RestAssured
			.given()
				.header(X_USER_HEADER,     ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
			.when()
				.get("/_appmap/activity")
			.jsonPath();

		assertEquals("The activity endpoint must report the requests of a type as the map does", node(map, ids.get("Item"), "requests"), activity.getString("result.nodes.'" + ids.get("Item") + "'.requests"));
		assertNull("The activity endpoint must leave out elements nobody requested",               activity.get("result.nodes.'" + ids.get("formatItem") + "'"));

		final String requests = node(map, ids.get("Item"), "requests");

		try (final Tx tx = app.tx()) {

			final Traits traits = Traits.of(StructrTraits.RESOURCE_ACCESS);

			app.create(StructrTraits.RESOURCE_ACCESS,
				new NodeAttribute<>(traits.key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY), "Item"),
				new NodeAttribute<>(traits.key(ResourceAccessTraitDefinition.FLAGS_PROPERTY),     1L)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		map = fetchMap();

		assertEquals("A REST request to a type with grants must count for the endpoint of the type", requests, node(map, "api:Item", "requests"));
		assertNull("A REST request to a type with grants must not count for the type as well",      node(map, ids.get("Item"), "requests"));
	}

	@Test
	public void testUsageCountsEveryMethodAndTheRunsOfTasks() {

		createApplication();

		try (final Tx tx = app.tx()) {

			final Traits traits = Traits.of(StructrTraits.RESOURCE_ACCESS);

			app.create(StructrTraits.RESOURCE_ACCESS,
				new NodeAttribute<>(traits.key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY), "ItemService/createItem"),
				new NodeAttribute<>(traits.key(ResourceAccessTraitDefinition.FLAGS_PROPERTY),     4L)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final long before = RuntimeEventLog.getRestCounts().getOrDefault("ItemService/createItem", 0L);

		RestAssured.given().header(X_USER_HEADER, ADMIN_USERNAME).header(X_PASSWORD_HEADER, ADMIN_PASSWORD).body("{}").expect().statusCode(200).when().post("/ItemService/createItem");
		RestAssured.given().header(X_USER_HEADER, ADMIN_USERNAME).header(X_PASSWORD_HEADER, ADMIN_PASSWORD).body("{}").expect().statusCode(200).when().post("/ItemService/createItem");

		// the counts run since the start, so earlier tests may have added to them
		assertEquals("Every POST to a method must be counted under its resource signature", before + 2, (long) RuntimeEventLog.getRestCounts().get("ItemService/createItem"));

		RuntimeEventLog.cron("listItems");

		final JsonPath map = fetchMap();

		assertEquals("The POST requests to a method must count for its endpoint", String.valueOf(before + 2), node(map, "api:ItemService/createItem", "requests"));
		assertTrue("The runs of a scheduled function must count for the function", Integer.parseInt(node(map, ids.get("listItems"), "requests")) >= 1);
	}

	@Test
	public void testIntegrationsThroughBuiltInFunctions() {

		createApplication();

		final JsonPath map    = fetchMap();
		final String   method = ids.get("integrateItems");

		assertTrue("A request with FETCH must be an edge to the host",                                  hasEdge(map, method, "ext:fetch.example.com", "requests"));
		assertTrue("A request to a computed URL must be an edge to the dynamic HTTP service",           hasEdge(map, method, "ext:http", "requests"));
		assertEquals("The dynamic HTTP service must say that its target is computed", "Call computed URLs", node(map, "ext:http", "name"));
		assertEquals("A web host must be named after the calls to it", "Call fetch.example.com", node(map, "ext:fetch.example.com", "name"));

		assertTrue("A jdbc query must be an edge to the database host",                                 hasEdge(map, method, "ext:jdbc:db.example.com", "queries"));
		assertEquals("A database must be named by its host and the kind of access", "Query db.example.com (JDBC)", node(map, "ext:jdbc:db.example.com", "name"));
		assertTrue("A mongodb query must be an edge to the host, without the credentials of its URL",   hasEdge(map, method, "ext:mongodb:mongo.example.com", "queries"));
		assertTrue("A remoteCypher query with a template string must be an edge to the host",           hasEdge(map, method, "ext:cypher:graph.example.com", "queries"));

		assertTrue("Geocoding must be an edge to the geocoding service",                                hasEdge(map, method, "ext:geocoding", "requests"));
		assertEquals("The geocoding service must name the configured provider", "Geocode via Google",  node(map, "ext:geocoding", "name"));
		assertTrue("A translation with a provider must be an edge to that provider",                    hasEdge(map, method, "ext:translation:deepl", "requests"));
		assertEquals("A translation provider must have its proper name", "Translate via DeepL",        node(map, "ext:translation:deepl", "name"));
		assertTrue("A translation without a provider must be an edge to Google",                        hasEdge(map, method, "ext:translation:google", "requests"));

		assertTrue("exec must be an edge to the configured script",                                     hasEdge(map, method, "ext:exec:backup", "executes"));
		assertTrue("execBinary must be an edge to the configured script in its second argument",        hasEdge(map, method, "ext:exec:render", "executes"));
		assertNull("exec() of a regular expression must not be an integration",                         node(map, "ext:exec", "kind"));
	}

	@Test
	public void testGroupsWithGrants() {

		createApplication();

		try (final Tx tx = app.tx()) {

			final NodeInterface member = app.create(StructrTraits.USER, "member");
			final NodeInterface staff  = app.create(StructrTraits.GROUP, "Staff");
			final Group editors        = app.getNodeById(StructrTraits.GROUP, ids.get("group")).as(Group.class);

			editors.addMember(securityContext, member.as(Principal.class));
			staff.as(Group.class).addMember(securityContext, editors);

			// a grant with access control alone, which the card of the group shows as well
			final Traits grantTraits = Traits.of(StructrTraits.SCHEMA_GRANT);

			app.create(StructrTraits.SCHEMA_GRANT,
				new NodeAttribute<>(grantTraits.key(SchemaGrantTraitDefinition.PRINCIPAL_PROPERTY),            staff),
				new NodeAttribute<>(grantTraits.key(SchemaGrantTraitDefinition.SCHEMA_NODE_PROPERTY),          app.getNodeById(StructrTraits.SCHEMA_NODE, ids.get("Item"))),
				new NodeAttribute<>(grantTraits.key(SchemaGrantTraitDefinition.ALLOW_ACCESS_CONTROL_PROPERTY), true)
			);

			ids.put("staff", staff.getUuid());
			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final JsonPath map = fetchMap();

		assertTrue("A group must list the types it has grants on", map.getList("result.groups.find { it.id == '" + ids.get("group") + "' }.types").contains(ids.get("Item")));
		assertEquals("A group must count its members", 1, map.getInt("result.groups.find { it.id == '" + ids.get("group") + "' }.members"));
		assertNull("A group must not be a node", map.get("result.nodes.find { it.id == '" + ids.get("group") + "' }"));
		assertEquals("A group must name the rights of its grants", "r", map.getString("result.groups.find { it.id == '" + ids.get("group") + "' }.rights['" + ids.get("Item") + "']"));
		assertTrue("A group must list the groups it is a member of", map.getList("result.groups.find { it.id == '" + ids.get("group") + "' }.memberOf").contains(ids.get("staff")));
		assertEquals("A group must name access control among the rights of its grants", "a", map.getString("result.groups.find { it.id == '" + ids.get("staff") + "' }.rights['" + ids.get("Item") + "']"));
	}

	@Test
	public void testSchemaRelationshipsSayWhatTheyDo() {

		createApplication();

		try (final Tx tx = app.tx()) {

			final Traits relTraits       = Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE);
			final Traits nodeTraits      = Traits.of(StructrTraits.SCHEMA_NODE);
			final NodeInterface relation = app.nodeQuery(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(relTraits.key(SchemaRelationshipNodeTraitDefinition.RELATIONSHIP_TYPE_PROPERTY), "HAS").getFirst();

			relation.setProperty(relTraits.key(SchemaRelationshipNodeTraitDefinition.CASCADING_DELETE_FLAG_PROPERTY), 1L);
			relation.setProperty(relTraits.key(SchemaRelationshipNodeTraitDefinition.PERMISSION_PROPAGATION_PROPERTY), PropagationDirection.Out.name());
			relation.setProperty(relTraits.key(SchemaRelationshipNodeTraitDefinition.READ_PROPAGATION_PROPERTY), PropagationMode.Add.name());

			app.getNodeById(StructrTraits.SCHEMA_NODE, ids.get("Item")).setProperty(nodeTraits.key(SchemaNodeTraitDefinition.DEFAULT_VISIBLE_TO_AUTH_PROPERTY), true);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final JsonPath map    = fetchMap();
		final String relation = "result.edges.find { it.source == '" + ids.get("Category") + "' && it.target == '" + ids.get("Item") + "' && it.kind == 'relates' }.details";

		assertEquals("A schema relationship must name its multiplicity",                     "1:*", map.getString(relation + ".multiplicity"));
		assertEquals("A schema relationship must say whether it deletes along",              1,     map.getInt(relation + ".cascadingDelete"));
		assertEquals("A schema relationship must say in which direction it passes on rights", "Out", map.getString(relation + ".propagation"));
		assertEquals("A schema relationship must say what it does with the read right",      "Add", map.getString(relation + ".read"));
		assertTrue("A type must say whom all its objects are visible to",                           map.getBoolean("result.nodes.find { it.id == '" + ids.get("Item") + "' }.defaultVisibility.authenticated"));
	}

	@Test
	public void testPagesSayHowTheyAreReached() {

		createApplication();

		try (final Tx tx = app.tx()) {

			final Traits pageTraits      = Traits.of(StructrTraits.PAGE);
			final Traits pathTraits      = Traits.of(StructrTraits.PAGE_PATH);
			final Traits parameterTraits = Traits.of(StructrTraits.PAGE_PATH_PARAMETER);
			final Traits siteTraits      = Traits.of(StructrTraits.SITE);
			final Page page              = app.getNodeById(StructrTraits.PAGE, ids.get("page")).as(Page.class);
			final NodeInterface path     = app.create(StructrTraits.PAGE_PATH,
				new NodeAttribute<>(pathTraits.key(PagePathTraitDefinition.PAGE_PROPERTY),          page),
				new NodeAttribute<>(pathTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/item/{item}")
			);

			app.create(StructrTraits.PAGE_PATH_PARAMETER,
				new NodeAttribute<>(parameterTraits.key(PagePathParameterTraitDefinition.PATH_PROPERTY),       path),
				new NodeAttribute<>(parameterTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),           "item"),
				new NodeAttribute<>(parameterTraits.key(PagePathParameterTraitDefinition.POSITION_PROPERTY),   0),
				new NodeAttribute<>(parameterTraits.key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY), "Node"),
				new NodeAttribute<>(parameterTraits.key(PagePathParameterTraitDefinition.FORMAT_PROPERTY),     "Item")
			);

			page.setProperty(pageTraits.key(PageTraitDefinition.POSITION_PROPERTY),            10);
			page.setProperty(pageTraits.key(PageTraitDefinition.SHOW_ON_ERROR_CODES_PROPERTY), "404");

			app.create(StructrTraits.SITE,
				new NodeAttribute<>(siteTraits.key(SiteTraitDefinition.HOSTNAME_PROPERTY), "items.example.com"),
				new NodeAttribute<>(siteTraits.key(SiteTraitDefinition.PAGES_PROPERTY),    List.of(page))
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final JsonPath map = fetchMap();
		final String page  = "result.nodes.find { it.id == '" + ids.get("page") + "' }";

		assertEquals("A page must name its route",                              "/item/{item}",      map.getString(page + ".routes[0].path"));
		assertEquals("A route must name the type of its current object",        "Item",              map.getString(page + ".routes[0].currentType"));
		assertTrue("A page must lead to the type of the current object of its route", hasEdge(map, ids.get("page"), ids.get("Item"), "current"));
		assertEquals("A page must say its position",                            10,                  map.getInt(page + ".position"));
		assertEquals("A page must say the errors it is shown for",              "404",               map.getString(page + ".errorCodes"));
		assertEquals("A site must name its host",                               "items.example.com", map.getString("result.sites[0].name"));
		assertTrue("A site must list its pages", map.getList("result.sites[0].pages").contains(ids.get("page")));
	}

	@Test
	public void testMapIsRestrictedToAdmins() {

		final Traits userTraits   = Traits.of(StructrTraits.USER);
		final Traits accessTraits = Traits.of(StructrTraits.RESOURCE_ACCESS);

		// a user who passes the resource access grant still is no admin
		try (final Tx tx = app.tx()) {

			createAdminUser();

			app.create(StructrTraits.USER,
				new NodeAttribute<>(userTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),      "tester"),
				new NodeAttribute<>(userTraits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY),     "test")
			);

			app.create(StructrTraits.RESOURCE_ACCESS,
				new NodeAttribute<>(accessTraits.key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY), "_appmap"),
				new NodeAttribute<>(accessTraits.key(ResourceAccessTraitDefinition.FLAGS_PROPERTY),     1L)
			).setVisibility(false, true);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/structr/rest";

		RestAssured
			.given()
				.header(X_USER_HEADER,     "tester")
				.header(X_PASSWORD_HEADER, "test")
			.expect()
				.statusCode(403)
			.when()
				.get("/_appmap");
	}

	@Test
	public void testActionMappingWithoutActionDoesNotBreakTheMap() {

		createApplication();

		try (final Tx tx = app.tx()) {

			final Page page         = app.getNodeById(StructrTraits.PAGE, ids.get("page")).as(Page.class);
			final DOMElement button = page.createElement("button");

			page.getElementsByTagName("div").get(0).appendChild(button);

			ids.put("emptyAction", createActionMapping(button, null, null, null, null).getUuid());
			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		assertNotNull("An action mapping whose action was never chosen must still be on the map", node(fetchMap(), ids.get("emptyAction"), "kind"));
	}

	@Test
	public void testScriptBlocksStandAsElementsOfTheirOwn() {

		createApplication();

		try (final Tx tx = app.tx()) {

			final Page page      = app.getNodeById(StructrTraits.PAGE, ids.get("page")).as(Page.class);
			final DOMElement div = page.createElement("div");
			final DOMNode script = page.createTextNode("${{ $.store('items', $.find('Item')); }}");

			page.getElementsByTagName("body").get(0).appendChild(div);
			div.appendChild(script);

			ids.put("scriptHolder", div.getUuid());
			ids.put("scriptBlock",  script.getUuid());
			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final List<Object> outline = fetchMap().getList("result.nodes.find { it.id == '" + ids.get("page") + "' }.outline");
		final Map<?, ?> holder     = findOutlineItem(outline, ids.get("scriptHolder"));
		final Map<?, ?> block      = findOutlineItem(outline, ids.get("scriptBlock"));

		assertNotNull("A text node with a script block must stand in the outline, so that a client can show its code", block);
		assertEquals("A script block must have the tag content, as the Pages area calls it", "content", block.get("tag"));
		assertEquals("A script block must have a text", Boolean.TRUE, block.get("text"));
		assertTrue("A script block must stand below its element", childrenOf(outline, holder).contains(block));
		assertNull("An element whose only text is a script block must have no text of its own", holder.get("text"));
	}

	@Test
	public void testRepeatersAndActionsSayHowTheyUseTheirType() {

		createApplication();

		try (final Tx tx = app.tx()) {

			final Traits elementTraits   = Traits.of(StructrTraits.DOM_ELEMENT);
			final Traits parameterTraits = Traits.of(StructrTraits.PARAMETER_MAPPING);
			final Page page              = app.getNodeById(StructrTraits.PAGE, ids.get("page")).as(Page.class);
			final DOMElement select      = page.createElement("select");
			final DOMElement option      = page.createElement("option");
			final DOMElement saveButton  = page.createElement("button");

			option.setProperty(elementTraits.key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY),       "choice");
			option.setProperty(elementTraits.key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY), "find('Category')");

			page.getElementsByTagName("div").get(0).appendChild(select);
			page.getElementsByTagName("div").get(0).appendChild(saveButton);
			select.appendChild(option);

			final NodeInterface save = createActionMapping(saveButton, "update", null, "Item", "${item.id}");

			app.create(StructrTraits.PARAMETER_MAPPING,
				new NodeAttribute<>(parameterTraits.key(ParameterMappingTraitDefinition.ACTION_MAPPING_PROPERTY), save),
				new NodeAttribute<>(parameterTraits.key(ParameterMappingTraitDefinition.PARAMETER_TYPE_PROPERTY), "user-input"),
				new NodeAttribute<>(parameterTraits.key(ParameterMappingTraitDefinition.PARAMETER_NAME_PROPERTY), "category"),
				new NodeAttribute<>(parameterTraits.key(ParameterMappingTraitDefinition.INPUT_ELEMENT_PROPERTY),  select)
			);

			ids.put("optionRepeater", option.getUuid());
			ids.put("saveAction",     save.getUuid());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final JsonPath map = fetchMap();

		assertEquals("A repeater must name its data expression",                               "find('Item')",     node(map, ids.get("itemRepeater"), "use.query"));
		assertEquals("A repeater of other elements than options and rows must be items",       "item",             node(map, ids.get("itemRepeater"), "use.role"));
		assertEquals("The options of a select must be a choice",                               "choice",           node(map, ids.get("optionRepeater"), "use.role"));
		assertEquals("A choice must name the property the action mapping it is the input of sets", "Item.category", node(map, ids.get("optionRepeater"), "use.field"));
		assertEquals("An action mapping must name the user input it sends",                    List.of("category"), map.getList("result.nodes.find { it.id == '" + ids.get("saveAction") + "' }.use.inputs"));
		assertTrue("The outline of a page must name the data key of a repeater",               map.getList("result.nodes.find { it.id == '" + ids.get("page") + "' }.outline").toString().contains("choice"));
		assertTrue("The evidence of a method must name the line of its code",                  evidence(map, ids.get("createItem"), ids.get("Item"), "creates").startsWith("ItemService.createItem, line 1: { const item = $.create('Item');"));
	}

	@Test
	public void testFunctionPropertiesKnowTheirReaders() {

		createApplication();

		try (final Tx tx = app.tx()) {

			final Traits propertyTraits = Traits.of(StructrTraits.SCHEMA_PROPERTY);
			final NodeInterface item    = app.getNodeById(StructrTraits.SCHEMA_NODE, ids.get("Item"));
			final Page page             = app.getNodeById(StructrTraits.PAGE, ids.get("page")).as(Page.class);
			final DOMElement repeater   = app.getNodeById(StructrTraits.DOM_ELEMENT, ids.get("itemRepeater")).as(DOMElement.class);
			final DOMElement span       = page.createElement("span");

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(propertyTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),          "total"),
				new NodeAttribute<>(propertyTraits.key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   item),
				new NodeAttribute<>(propertyTraits.key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "Function"),
				new NodeAttribute<>(propertyTraits.key(SchemaPropertyTraitDefinition.READ_FUNCTION_PROPERTY), "{ return 1; }")
			);

			ids.put("reportItems",   createMethod("reportItems", app.getNodeById(StructrTraits.SCHEMA_NODE, ids.get("ItemService")), "{ for (const item of $.find('Item')) { $.log(item.total); } }").getUuid());
			ids.put("describeTotal", createMethod("describeTotal", app.getNodeById(StructrTraits.SCHEMA_NODE, ids.get("DerivedItem")), "{ return $.this.total; }").getUuid());
			ids.put("readsName",     createMethod("readsName", null, "{ const total = 1; return total.name; }").getUuid());

			repeater.appendChild(span);
			span.appendChild(page.createTextNode("${item.total}"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final JsonPath map                 = fetchMap();
		final List<Map<String, String>> readers = map.getList("result.nodes.find { it.id == '" + ids.get("Item") + "' }.functionReads.total");
		final Map<String, String> byCaller = new HashMap<>();

		assertNotNull("A type must name who reads its function property", readers);

		readers.forEach(reader -> byCaller.put(reader.get("caller"), reader.get("evidence")));

		assertTrue("A method must read a function property on an object it found",      byCaller.containsKey(ids.get("reportItems")));
		assertTrue("A method of a subtype must read a function property of its parent",  byCaller.containsKey(ids.get("describeTotal")));
		assertTrue("A page must read a function property on the key of a repeater",      byCaller.getOrDefault(ids.get("page"), "").endsWith("shows item.total"));
		assertFalse("A variable that is no object of the type must not read it",         byCaller.containsKey(ids.get("readsName")));
		assertNull("A property that computes nothing must have no readers",              map.get("result.nodes.find { it.id == '" + ids.get("Category") + "' }.functionReads"));
	}

	@Test
	public void testPagesFormSections() {

		createApplication();

		try (final Tx tx = app.tx()) {

			final Traits pageTraits = Traits.of(StructrTraits.PAGE);
			final Page categories   = Page.createSimplePage(securityContext, "categories");
			final Page category     = Page.createSimplePage(securityContext, "category");
			final Page about        = Page.createSimplePage(securityContext, "about");
			final Page imprint      = Page.createSimplePage(securityContext, "imprint");

			categories.getElementsByTagName("div").get(0).appendChild(repeater(categories, "category", "find('Category')"));
			category.getElementsByTagName("div").get(0).appendChild(repeater(category, "category", "find('Category')"));

			// two pages that only lead to each other, in a category of their own
			about.setProperty(pageTraits.key(PageTraitDefinition.CATEGORY_PROPERTY),   "Info");
			imprint.setProperty(pageTraits.key(PageTraitDefinition.CATEGORY_PROPERTY), "Info");

			about.getElementsByTagName("div").get(0).appendChild(link(about, "/imprint"));
			imprint.getElementsByTagName("div").get(0).appendChild(link(imprint, "/about"));

			ids.put("categories", categories.getUuid());
			ids.put("category",   category.getUuid());
			ids.put("about",      about.getUuid());
			ids.put("imprint",    imprint.getUuid());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final JsonPath map    = fetchMap();
		final String section  = node(map, ids.get("categories"), "grouping");

		assertEquals("Pages that work with the same type must share a section",   section,                 node(map, ids.get("category"), "grouping"));
		assertEquals("A section of a type must be named after it",                 "Category pages",        map.getString("result.sections.find { it.id == '" + section + "' }.name"));
		assertEquals("A section of a type must say so",                            "type",                  map.getString("result.sections.find { it.id == '" + section + "' }.how"));
		assertEquals("Pages of the same category must share its section",          "section:category:Info", node(map, ids.get("about"), "grouping"));
		assertEquals("Pages of the same category must share its section",          "section:category:Info", node(map, ids.get("imprint"), "grouping"));
		assertEquals("A section must be part of the user interface",               "ui",                    node(map, ids.get("about"), "area"));
		assertEquals("A repeater must stay in the grouping of its page",           ids.get("page"),         node(map, ids.get("itemRepeater"), "grouping"));
	}

	@Test
	public void testLinksWithOnlyAQueryLeadToNoPage() {

		createApplication();

		try (final Tx tx = app.tx()) {

			final Page list  = Page.createSimplePage(securityContext, "list");
			final Page about = Page.createSimplePage(securityContext, "about");
			final DOMNode div = list.getElementsByTagName("div").get(0);

			div.appendChild(link(list, "?"));
			div.appendChild(link(list, "/?"));
			div.appendChild(link(list, "?page=2"));
			div.appendChild(link(list, "/about?tab=1"));

			ids.put("about", about.getUuid());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final JsonPath map = fetchMap();

		assertNotNull("A link with a query must lead to the page its path names", map.get("result.edges.find { it.target == '" + ids.get("about") + "' && it.kind == 'navigates' }"));
	}

	@Test
	public void testSharedComponentsFormAnAreaOfTheirOwn() {

		createApplication();

		try (final Tx tx = app.tx()) {

			final ShadowDocument components = CreateComponentCommand.getOrCreateHiddenDocument();
			final DOMElement card           = components.createElement("div");
			final Page sheet                = Page.createNewPage(securityContext, "sheet");

			card.setProperty(card.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "card");
			card.appendChild(components.createTextNode("Name: ${current.name} ${render(children)}"));

			// the map leaves out what nothing uses, so the page includes the shared component
			sheet.appendChild(sheet.createTemplate("${{ $.print('sheet'); $.include('card'); }}"));

			ids.put("card",          card.getUuid());
			ids.put("sheet",         sheet.getUuid());
			ids.put("sheetTemplate", sheet.getFirstChild().getUuid());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final JsonPath map = fetchMap();
		final Map<?, ?> card = (Map<?, ?>) map.getList("result.nodes.find { it.id == '" + ids.get("card") + "' }.outline").get(0);

		assertEquals("A shared component must be an area of its own",                      "components",             node(map, ids.get("card"), "area"));
		assertEquals("A shared component must belong to no section",                       ids.get("card"),          node(map, ids.get("card"), "grouping"));
		assertEquals("An element must show the expressions of its text, without render()", "${current.name}",        card.get("expressions"));
		assertEquals("A page of a single template must name it",                           ids.get("sheetTemplate"), node(map, ids.get("sheet"), "template"));
		assertNull("A page with elements must name no template",                                                     node(map, ids.get("page"), "template"));
	}

	@Test
	public void testLocateLeadsToTheElementsOfTheMap() {

		createApplication();

		RestAssured.basePath = "/structr/rest";

		final JsonPath json = RestAssured
			.given()
				.header(X_USER_HEADER,     ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.body(Map.of("ids", List.of(ids.get("color"), ids.get("rowText"), ids.get("archiveButton"), ids.get("archiveAction"), "00000000000000000000000000000000")))
			.expect()
				.statusCode(200)
			.when()
				.post("/_appmap/locate")
			.jsonPath();

		// a single result may come wrapped in a list
		final String root                     = json.get("result") instanceof List ? "result[0]" : "result";
		final Map<String, List<String>> where = json.getMap(root);
		final List<String> text               = where.get(ids.get("rowText"));
		final List<String> button             = where.get(ids.get("archiveButton"));
		final List<String> action             = where.get(ids.get("archiveAction"));

		assertEquals("A schema property must lie in its type",                  List.of(ids.get("color"), ids.get("Category")), where.get(ids.get("color")));
		assertEquals("An HTML element must lie first in itself",                ids.get("rowText"),       text.get(0));
		assertEquals("An HTML element must lie next in the repeater above it",  ids.get("rowRepeater"),   text.get(1));
		assertEquals("An HTML element must lie last in its page",               ids.get("page"),          text.get(text.size() - 1));
		assertTrue("An element inside a repeater must lie in the repeater before its page", button.indexOf(ids.get("itemRepeater")) > 0 && button.indexOf(ids.get("itemRepeater")) < button.indexOf(ids.get("page")));
		assertEquals("An action mapping must lie next at its trigger element",  ids.get("archiveButton"), action.get(1));
		assertFalse("An unknown id must be left out", where.containsKey("00000000000000000000000000000000"));
	}

	// ----- private methods -----
	private void createApplication() {

		final Traits nodeTraits     = Traits.of(StructrTraits.SCHEMA_NODE);
		final Traits relTraits      = Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE);
		final Traits propertyTraits = Traits.of(StructrTraits.SCHEMA_PROPERTY);
		final Traits grantTraits    = Traits.of(StructrTraits.SCHEMA_GRANT);

		try (final Tx tx = app.tx()) {

			final NodeInterface item     = app.create(StructrTraits.SCHEMA_NODE, "Item");
			final NodeInterface category = app.create(StructrTraits.SCHEMA_NODE, "Category");
			final NodeInterface derived  = app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(nodeTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),            "DerivedItem"),
				new NodeAttribute<>(nodeTraits.key(SchemaNodeTraitDefinition.INHERITED_TRAITS_PROPERTY), new String[] { "Item" })
			);
			final NodeInterface service  = app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(nodeTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),                     "ItemService"),
				new NodeAttribute<>(nodeTraits.key(AbstractSchemaNodeTraitDefinition.IS_SERVICE_CLASS_PROPERTY), true)
			);

			final NodeInterface color = app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(propertyTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),          "color"),
				new NodeAttribute<>(propertyTraits.key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY),   category),
				new NodeAttribute<>(propertyTraits.key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String")
			);

			app.create(StructrTraits.SCHEMA_RELATIONSHIP_NODE,
				new NodeAttribute<>(relTraits.key(RelationshipInterfaceTraitDefinition.SOURCE_NODE_PROPERTY),          category),
				new NodeAttribute<>(relTraits.key(RelationshipInterfaceTraitDefinition.TARGET_NODE_PROPERTY),          item),
				new NodeAttribute<>(relTraits.key(SchemaRelationshipNodeTraitDefinition.RELATIONSHIP_TYPE_PROPERTY),   "HAS"),
				new NodeAttribute<>(relTraits.key(SchemaRelationshipNodeTraitDefinition.SOURCE_MULTIPLICITY_PROPERTY), "1"),
				new NodeAttribute<>(relTraits.key(SchemaRelationshipNodeTraitDefinition.TARGET_MULTIPLICITY_PROPERTY), "*"),
				new NodeAttribute<>(relTraits.key(SchemaRelationshipNodeTraitDefinition.SOURCE_JSON_NAME_PROPERTY),    "category"),
				new NodeAttribute<>(relTraits.key(SchemaRelationshipNodeTraitDefinition.TARGET_JSON_NAME_PROPERTY),    "items")
			);

			ids.put("Item",        item.getUuid());
			ids.put("color",       color.getUuid());
			ids.put("Category",    category.getUuid());
			ids.put("DerivedItem", derived.getUuid());
			ids.put("ItemService", service.getUuid());

			ids.put("archive",    createMethod("archive",    item,    "{ $.this.archived = true; }").getUuid());
			ids.put("describe",   createMethod("describe",   derived, "{ return $.this.name; }").getUuid());
			ids.put("createItem", createMethod("createItem", service, "{ const item = $.create('Item'); item.category = $.find('Category')[0]; }").getUuid());
			ids.put("formatItem", createMethod("formatItem", null,    "{ return 'item'; }").getUuid());

			ids.put("syncItems", createMethod("syncItems", null, "{ $.GET('https://api.example.com/items'); Structr.POST('https://legacy.example.com/items', '{}'); $.sendPlaintextMail('a@example.com', 'A', 'b@example.com', 'B', 'Items', 'synced'); }").getUuid());

			ids.put("integrateItems", createMethod("integrateItems", null, "{ const url = 'https://' + $.me.name; $.FETCH('https://fetch.example.com/items', 'GET'); $.GET(url); "
				+ "$.jdbc('jdbc:mysql://db.example.com:3306/shop', 'SELECT 1'); $.mongodb('mongodb://user:secret@mongo.example.com:27017', 'shop', 'items'); $.remoteCypher(`bolt://graph.example.com`, 'user', 'secret', 'MATCH (n) RETURN n'); "
				+ "$.geocode('Main Street', 'Frankfurt', 'Germany'); $.translate('Hallo', 'de', 'en', 'deepl'); $.translate(['Hallo', 'Welt'].join(' '), 'de', 'en'); "
				+ "$.exec('backup', ['a', 'b']); $.execBinary($.response, 'render'); /a/.exec('a'); }").getUuid());

			// four callers make formatItem a helper
			ids.put("listItems", createMethod("listItems", null, "{ $.formatItem(); return $.find('Item'); }").getUuid());

			for (final String name : List.of("countItems", "exportItems", "printItems")) {
				createMethod(name, null, "{ $.formatItem(); return $.find('Item'); }");
			}

			final NodeInterface group = app.create(StructrTraits.GROUP, "Editors");

			app.create(StructrTraits.SCHEMA_GRANT,
				new NodeAttribute<>(grantTraits.key(SchemaGrantTraitDefinition.PRINCIPAL_PROPERTY),   group),
				new NodeAttribute<>(grantTraits.key(SchemaGrantTraitDefinition.SCHEMA_NODE_PROPERTY), item),
				new NodeAttribute<>(grantTraits.key(SchemaGrantTraitDefinition.ALLOW_READ_PROPERTY),  true)
			);

			ids.put("group", group.getUuid());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page                = Page.createSimplePage(securityContext, "items");
			final DOMNode div              = page.getElementsByTagName("div").get(0);
			final DOMElement itemRepeater  = repeater(page, "item", "find('Item')");
			final DOMElement categoryList  = repeater(page, "cat", "item.category");
			final DOMElement rowRepeater   = repeater(page, "row", "unknownFunction()");
			final DOMNode rowText          = page.createTextNode("${row.color}");
			final DOMElement archiveButton = page.createElement("button");
			final DOMElement updateButton  = page.createElement("button");
			final DOMElement createButton  = page.createElement("button");
			final DOMElement deleteButton  = page.createElement("button");
			final DOMElement callButton    = page.createElement("button");

			div.appendChild(itemRepeater);
			div.appendChild(rowRepeater);
			div.appendChild(createButton);
			div.appendChild(deleteButton);
			div.appendChild(callButton);

			itemRepeater.appendChild(categoryList);
			itemRepeater.appendChild(archiveButton);
			itemRepeater.appendChild(updateButton);
			rowRepeater.appendChild(rowText);

			ids.put("page",             page.getUuid());
			ids.put("itemRepeater",     itemRepeater.getUuid());
			ids.put("categoryRepeater", categoryList.getUuid());
			ids.put("rowRepeater",      rowRepeater.getUuid());
			ids.put("rowText",          rowText.getUuid());
			ids.put("archiveButton",    archiveButton.getUuid());
			ids.put("archiveAction",    createActionMapping(archiveButton, "method", "archive", null, "${item.id}").getUuid());
			ids.put("updateAction",     createActionMapping(updateButton, "update", null, "", "${item.id}").getUuid());
			ids.put("createAction",     createActionMapping(createButton, "create", null, "Category", null).getUuid());
			ids.put("deleteAction",     createActionMapping(deleteButton, "delete", null, "", "${category.id}").getUuid());
			ids.put("callAction",       createActionMapping(callButton, "method", "archive", null, null).getUuid());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	private NodeInterface createMethod(final String name, final NodeInterface owner, final String source) throws FrameworkException {

		final Traits traits                     = Traits.of(StructrTraits.SCHEMA_METHOD);
		final List<NodeAttribute<?>> attributes = new LinkedList<>();

		attributes.add(new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),  name));
		attributes.add(new NodeAttribute<>(traits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), source));

		// a method without owner is a global method
		if (owner != null) {
			attributes.add(new NodeAttribute<>(traits.key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), owner));
		}

		return app.create(StructrTraits.SCHEMA_METHOD, attributes.toArray(new NodeAttribute[0]));
	}

	private DOMElement repeater(final Page page, final String dataKey, final String functionQuery) throws FrameworkException {

		final Traits traits      = Traits.of(StructrTraits.DOM_ELEMENT);
		final DOMElement element = page.createElement("div");

		element.setProperty(traits.key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY),       dataKey);
		element.setProperty(traits.key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY), functionQuery);

		return element;
	}

	private DOMElement link(final Page page, final String href) throws FrameworkException {

		final DOMElement link = page.createElement("a");

		link.setProperty(link.getTraits().key("_html_href"), href);

		return link;
	}

	private NodeInterface createActionMapping(final DOMElement trigger, final String action, final String method, final String dataType, final String idExpression) throws FrameworkException {

		final Traits traits = Traits.of(StructrTraits.ACTION_MAPPING);

		return app.create(StructrTraits.ACTION_MAPPING,
			new NodeAttribute<>(traits.key(ActionMappingTraitDefinition.TRIGGER_ELEMENTS_PROPERTY), List.of(trigger)),
			new NodeAttribute<>(traits.key(ActionMappingTraitDefinition.EVENT_PROPERTY),            "click"),
			new NodeAttribute<>(traits.key(ActionMappingTraitDefinition.ACTION_PROPERTY),           action),
			new NodeAttribute<>(traits.key(ActionMappingTraitDefinition.METHOD_PROPERTY),           method),
			new NodeAttribute<>(traits.key(ActionMappingTraitDefinition.DATA_TYPE_PROPERTY),        dataType),
			new NodeAttribute<>(traits.key(ActionMappingTraitDefinition.ID_EXPRESSION_PROPERTY),    idExpression)
		);
	}

	/**
	 * The ids of an outline: every item is [tag, flags, children, ids], the last two optional.
	 */
	private void collectOutlineIds(final List<?> items, final Set<String> found) {

		for (final Map<?, ?> item : itemsOf(items)) {

			for (final Object id : listOf(item, "ids")) {
				found.add(id.toString());
			}
		}
	}

	private Set<String> collectOutlineElements(final List<?> items, final Set<String> found) {

		for (final Map<?, ?> item : itemsOf(items)) {
			found.add(item.get("id").toString());
		}

		return found;
	}

	private Set<String> collectOutlineSources(final List<?> items, final Set<String> found) {

		for (final Map<?, ?> item : itemsOf(items)) {

			for (final Object source : listOf(item, "sources")) {
				found.add(source.toString());
			}
		}

		return found;
	}

	private Map<?, ?> findOutlineItem(final List<?> items, final String id) {

		return itemsOf(items).stream().filter(item -> id.equals(item.get("id"))).findFirst().orElse(null);
	}

	// the items of an outline, a flat list in which each item stands before the items below it
	private List<Map<?, ?>> itemsOf(final List<?> outline) {

		return outline.stream().<Map<?, ?>>map(entry -> (Map<?, ?>) entry).toList();
	}

	// the items directly below an item: those after it one level deeper, up to the next item on its level or above
	private List<Map<?, ?>> childrenOf(final List<?> outline, final Map<?, ?> parent) {

		final List<Map<?, ?>> items    = itemsOf(outline);
		final List<Map<?, ?>> children = new LinkedList<>();
		final int depth                = depthOf(parent);

		for (final Map<?, ?> item : items.subList(items.indexOf(parent) + 1, items.size())) {

			if (depthOf(item) <= depth) {
				break;
			}

			if (depthOf(item) == depth + 1) {
				children.add(item);
			}
		}

		return children;
	}

	private int depthOf(final Map<?, ?> item) {

		return ((Number) item.get("depth")).intValue();
	}

	// an outline item leaves out a list that is empty
	private List<?> listOf(final Map<?, ?> item, final String key) {

		return item.get(key) instanceof List<?> list ? list : List.of();
	}

	private JsonPath fetchMap() {

		RestAssured.basePath = "/structr/rest";

		return RestAssured
			.given()
				.header(X_USER_HEADER,     ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
			.when()
				.get("/_appmap")
			.jsonPath();
	}

	private String node(final JsonPath map, final String id, final String field) {

		return map.getString("result.nodes.find { it.id == '" + id + "' }." + field);
	}

	private List<String> crud(final JsonPath map, final String id) {

		final List<String> crud = map.getList("result.nodes.find { it.id == '" + id + "' }.crud");

		return crud != null ? crud : List.of();
	}

	private String evidence(final JsonPath map, final String source, final String target, final String kind) {

		return map.getString("result.edges.find { it.source == '" + source + "' && it.target == '" + target + "' && it.kind == '" + kind + "' }.evidence");
	}

	private boolean hasEdge(final JsonPath map, final String source, final String target, final String kind) {

		return map.get("result.edges.find { it.source == '" + source + "' && it.target == '" + target + "' && it.kind == '" + kind + "' }") != null;
	}
}
