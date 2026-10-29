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
import org.structr.common.error.FrameworkException;
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
import org.structr.web.traits.definitions.ActionMappingTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
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

		for (final List<Object> edge : map.<List<Object>>getList("result.edges")) {
			assertEquals("Every edge must be source, target, weight, kind, inferred and evidence", 6, edge.size());
		}

		assertEquals("A schema type must be a node of kind type", "type", node(map, ids.get("Item"), "k"));
		assertEquals("A type method must be named after its type", "Item.archive", node(map, ids.get("archive"), "n"));
		assertEquals("A service class must be marked", Boolean.TRUE, map.get("result.nodes.find { it.id == '" + ids.get("ItemService") + "' }.svc"));
		assertNull("A type that is no service class must not be marked", map.get("result.nodes.find { it.id == '" + ids.get("Item") + "' }.svc"));
		assertEquals("A page outline must start with the html element", "html", map.getString("result.nodes.find { it.id == '" + ids.get("page") + "' }.d[0][0]"));
	}

	@Test
	public void testGroupingsAndAreas() {

		createApplication();

		final JsonPath map = fetchMap();

		assertEquals("A type must be a grouping of its own",                            ids.get("Item"),        node(map, ids.get("Item"), "g"));
		assertEquals("A type method must belong to the grouping of its type",           ids.get("Item"),        node(map, ids.get("archive"), "g"));
		assertEquals("An action must belong to the grouping of its page",               ids.get("page"),        node(map, ids.get("archiveAction"), "g"));
		assertEquals("A repeater must belong to the grouping of its page",              ids.get("page"),        node(map, ids.get("itemRepeater"), "g"));
		assertEquals("A user-defined function that writes nothing must stand alone",    ids.get("formatItem"),  node(map, ids.get("formatItem"), "g"));
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

		assertEquals("A repeater must show the type its query finds",                  "Item",         node(map, ids.get("itemRepeater"), "t"));
		assertEquals("A repeater type from the query must be marked as such",           "query",        node(map, ids.get("itemRepeater"), "how"));
		assertEquals("A nested key must follow the relationship of its parent key",    "Category",     node(map, ids.get("categoryRepeater"), "t"));
		assertEquals("A type from a relationship path must be marked as such",         "relationship", node(map, ids.get("categoryRepeater"), "how"));
		assertEquals("A query that names no type must leave it to the properties read below", "Category", node(map, ids.get("rowRepeater"), "t"));
		assertEquals("A type from the properties read must be marked as such",         "properties",   node(map, ids.get("rowRepeater"), "how"));
		assertTrue("A page must contain its repeaters",                                hasEdge(map, ids.get("page"), ids.get("itemRepeater"), "contains"));
		assertTrue("A repeater must read the type it shows",                           hasEdge(map, ids.get("itemRepeater"), ids.get("Item"), "reads"));
	}

	@Test
	public void testOutlineNamesRepeatersAndActionsAtTheirElements() {

		createApplication();

		final JsonPath map        = fetchMap();
		final List<Object> outline = map.getList("result.nodes.find { it.id == '" + ids.get("page") + "' }.d");
		final Set<String> found    = new HashSet<>();

		collectOutlineIds(outline, found);

		assertTrue("The outline must name a repeater at its element", found.contains(ids.get("itemRepeater")));

		for (final String action : List.of("archiveAction", "updateAction", "createAction", "deleteAction", "callAction")) {
			assertTrue("The outline must name every action mapping of the page at its trigger element, missing " + action, found.contains(ids.get(action)));
		}
	}

	@Test
	public void testActionMappings() {

		createApplication();

		final JsonPath map = fetchMap();

		assertEquals("An action without data type must act on the type of the key in its id expression", "Item",       node(map, ids.get("archiveAction"), "t"));
		assertEquals("A type from the scope must name the key",                                            "scope:item", node(map, ids.get("archiveAction"), "how"));
		assertEquals("An action must name the method it calls, resolved on its type",                      ids.get("archive"), node(map, ids.get("archiveAction"), "m"));
		assertEquals("An action mapping must name its action",                                             "method",     node(map, ids.get("archiveAction"), "a"));
		assertEquals("An action mapping must name the event that triggers it",                             "click",      node(map, ids.get("archiveAction"), "ev"));
		assertTrue("An action must invoke its method",                                                    hasEdge(map, ids.get("archiveAction"), ids.get("archive"), "invokes"));
		assertTrue("An action must update what its method updates",                                       crud(map, ids.get("archiveAction")).contains("U:Item"));
		assertEquals("A create action must create its data type and nothing else",                        List.of("C:Category"), crud(map, ids.get("createAction")));
		assertTrue("A create action must be an edge that creates the type",                               hasEdge(map, ids.get("createAction"), ids.get("Category"), "creates"));
		assertEquals("An empty data type, as the editor leaves it, must not hide the type of the id expression", "Item", node(map, ids.get("updateAction"), "t"));
		assertTrue("An update action on the key of a repeater must update its type",                     crud(map, ids.get("updateAction")).contains("U:Item"));
		assertEquals("A key outside every repeater must name the type it is named after",                 "Category",              node(map, ids.get("deleteAction"), "t"));
		assertEquals("A type from the key name must say so",                                               "key name:category",     node(map, ids.get("deleteAction"), "how"));
		assertTrue("A delete action on a key named after a type must delete that type",                   hasEdge(map, ids.get("deleteAction"), ids.get("Category"), "deletes"));
		assertEquals("A method called without a type must be the only method of that name",              ids.get("archive"),      node(map, ids.get("callAction"), "m"));
		assertTrue("An action must invoke the only method of its name",                                   hasEdge(map, ids.get("callAction"), ids.get("archive"), "invokes"));
	}

	@Test
	public void testRestAccessIsAPropertyOfTheType() {

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

		assertEquals("Every grant on a type or one of its views must count as a rule of the type", 2, map.getInt(item + ".n"));
		assertEquals("A grant for anonymous users must mark the type as public",                  1, map.getInt(item + ".pub"));
		assertEquals("A grant for authenticated users must mark the type as such",                1, map.getInt(item + ".auth"));
		assertEquals("A grant that only reads must not mark the type as writable",                0, map.getInt(item + ".pubw") + map.getInt(item + ".authw"));
		assertEquals("A POST grant on a method must open the endpoint for writing to authenticated users", 1, map.getInt("result.nodes.find { it.id == 'api:ItemService/createItem' }.rest.authw"));
		assertEquals("A PATCH grant for public users must open the endpoint to them",                     1, map.getInt("result.nodes.find { it.id == 'api:listItems' }.rest.pub"));
		assertEquals("A PATCH grant for public users must let them write",                                1, map.getInt("result.nodes.find { it.id == 'api:listItems' }.rest.pubw"));
		assertNull("A type without grants must not carry REST access", map.get("result.nodes.find { it.id == '" + ids.get("Category") + "' }.rest"));
		assertNull("A grant on a type must not become an endpoint",     map.get("result.nodes.find { it.k == 'api' && it.n == '/Item' }"));
		assertTrue("A grant on a method must make the method an endpoint", hasEdge(map, "api:ItemService/createItem", ids.get("createItem"), "endpoint"));
		assertTrue("A grant that spells the method with a capital letter, as older versions wrote it, must find the method", hasEdge(map, "api:Item/Archive", ids.get("archive"), "endpoint"));
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

		assertEquals("A page visible to public users must be marked public",        "1", node(map, ids.get("page"), "p"));
		assertEquals("The access of a page must carry its visibility to public users", "1", node(map, ids.get("page"), "acc.pub"));
		assertEquals("The access of a page must carry its visibility to authenticated users", "0", node(map, ids.get("page"), "acc.auth"));
	}

	@Test
	public void testSizes() {

		createApplication();

		final JsonPath map = fetchMap();

		assertEquals("A method must measure the lines of its code", "1", node(map, ids.get("archive"), "s"));
		assertTrue("A page must measure its HTML elements", Integer.parseInt(node(map, ids.get("page"), "s")) > 5);
		assertTrue("A type must measure its properties", Integer.parseInt(node(map, ids.get("Item"), "s")) > 0);
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
			assertEquals("A scheduled user-defined function must be a grouping of its own",            ids.get("listItems"), node(map, ids.get("listItems"), "g"));
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

		assertEquals("A host a script sends requests to must be an external service", "external", node(map, "ext:api.example.com", "k"));
		assertTrue("A request with $.GET must be an edge to the host",                               hasEdge(map, ids.get("syncItems"), "ext:api.example.com", "requests"));
		assertTrue("A request with the older Structr.POST must be an edge to the host",              hasEdge(map, ids.get("syncItems"), "ext:legacy.example.com", "requests"));
		assertTrue("Sending mail must be an edge to the mail service, whatever the spelling of the function", hasEdge(map, ids.get("syncItems"), "ext:mail", "mails"));
		assertEquals("An external service must be part of the integrations", "integration", node(map, "ext:api.example.com", "area"));
	}

	@Test
	public void testGroupsWithGrants() {

		createApplication();

		try (final Tx tx = app.tx()) {

			final NodeInterface member = app.create(StructrTraits.USER, "member");

			app.getNodeById(StructrTraits.GROUP, ids.get("group")).as(Group.class).addMember(securityContext, member.as(Principal.class));
			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final JsonPath map = fetchMap();

		assertTrue("A group must list the types it has grants on", map.getList("result.groups.find { it.id == '" + ids.get("group") + "' }.t").contains(ids.get("Item")));
		assertEquals("A group must count its members", 1, map.getInt("result.groups.find { it.id == '" + ids.get("group") + "' }.m"));
		assertNull("A group must not be a node", map.get("result.nodes.find { it.id == '" + ids.get("group") + "' }"));
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

		assertNotNull("An action mapping whose action was never chosen must still be on the map", node(fetchMap(), ids.get("emptyAction"), "k"));
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

		for (final Object entry : items) {

			final List<?> item = (List<?>) entry;

			if (item.size() > 2) {
				collectOutlineIds((List<?>) item.get(2), found);
			}

			if (item.size() > 3) {

				for (final Object id : (List<?>) item.get(3)) {
					found.add(id.toString());
				}
			}
		}
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

	private boolean hasEdge(final JsonPath map, final String source, final String target, final String kind) {

		return map.get("result.edges.find { it[0] == '" + source + "' && it[1] == '" + target + "' && it[3] == '" + kind + "' }") != null;
	}
}
