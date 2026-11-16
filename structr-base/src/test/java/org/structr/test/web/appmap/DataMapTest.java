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
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.RelationshipInterfaceTraitDefinition;
import org.structr.core.traits.definitions.ResourceAccessTraitDefinition;
import org.structr.core.traits.definitions.SchemaRelationshipNodeTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * Tests the data map endpoint: the types in families, the objects in clusters and the relationships between the
 * clusters (see DataMap).
 */
public class DataMapTest extends StructrUiTest {

	@Test
	public void testTheDataIsMappedInFamiliesAndClusters() {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final NodeInterface order = app.create(StructrTraits.SCHEMA_NODE, "ShopOrder");
			final NodeInterface item  = app.create(StructrTraits.SCHEMA_NODE, "ShopItem");

			app.create(StructrTraits.SCHEMA_NODE, "Note");

			relate(order, item, "HAS_ITEM", "order", "items");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final Traits itemTraits  = Traits.of("ShopItem");
			final NodeInterface first = app.create("ShopOrder", "First");

			app.create("ShopOrder", "Second");

			app.create("ShopItem", new NodeAttribute<>(itemTraits.key("name"), "Apple"), new NodeAttribute<>(itemTraits.key("order"), first));
			app.create("ShopItem", new NodeAttribute<>(itemTraits.key("name"), "Pear"),  new NodeAttribute<>(itemTraits.key("order"), first));
			app.create("Note", "Reminder");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/structr/rest";

		final JsonPath map = RestAssured
			.given()
				.header(X_USER_HEADER,     ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
			.when()
				.get("/_appmap/data?fresh=true")
			.jsonPath();

		// the data is read with Cypher, so a database without it has an empty data map
		if (!Services.getInstance().getDatabaseService().supportsCypher()) {

			assertEquals("A database without Cypher must have an empty data map", 0, map.getInt("result.objects"));
			return;
		}

		final List<Integer> sizes      = map.getList("result.nodes.s");
		final List<String> clusterTypes = map.getList("result.nodes.t");
		final List<String> areas        = map.getList("result.areas.key");
		final String order              = map.getString("result.nodes.find { it.t == 'ShopOrder' }.id");
		final String item               = map.getString("result.nodes.find { it.t == 'ShopItem' }.id");
		final List<List<Object>> edges  = map.getList("result.edges");
		final List<List<Object>> between = edges.stream().filter(e -> order.equals(e.get(0)) && item.equals(e.get(1))).toList();

		assertEquals("The data map must hold every object of the application",          5, map.getInt("result.objects"));
		assertEquals("The clusters of the data map must hold every object once",         5, sizes.stream().mapToInt(Integer::intValue).sum());
		assertTrue("The data map must leave out the types Structr brings along",         !clusterTypes.contains(StructrTraits.USER));
		assertEquals("Types of one family must share a sector, the others one more",     List.of("family-shop", "other"), areas);
		assertEquals("A type must lie in the sector of its family",                      "family-shop", map.getString("result.nodes.find { it.t == 'ShopItem' }.area"));
		assertEquals("A type of no family must lie in the sector of the others",         "other",       map.getString("result.nodes.find { it.t == 'Note' }.area"));
		assertEquals("The relationships between two clusters must be one relation",      1, between.size());
		assertEquals("A relation between clusters must count their relationships",       2, between.get(0).get(2));
	}

	@Test
	public void testOnlyAdminsMayMapTheData() {

		final Traits userTraits   = Traits.of(StructrTraits.USER);
		final Traits accessTraits = Traits.of(StructrTraits.RESOURCE_ACCESS);

		try (final Tx tx = app.tx()) {

			createAdminUser();

			app.create(StructrTraits.USER,
				new NodeAttribute<>(userTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),  "tester"),
				new NodeAttribute<>(userTraits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "test")
			);

			app.create(StructrTraits.RESOURCE_ACCESS,
				new NodeAttribute<>(accessTraits.key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY), "_appmap/data"),
				new NodeAttribute<>(accessTraits.key(ResourceAccessTraitDefinition.FLAGS_PROPERTY),     1L)
			).setVisibility(false, true);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/structr/rest";

		RestAssured
			.given()
				.header(X_USER_HEADER,     "tester")
				.header(X_PASSWORD_HEADER, "test")
			.expect()
				.statusCode(403)
			.when()
				.get("/_appmap/data");
	}

	// ----- private methods -----
	private void relate(final NodeInterface source, final NodeInterface target, final String type, final String sourceName, final String targetName) throws FrameworkException {

		final Traits traits = Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE);

		app.create(StructrTraits.SCHEMA_RELATIONSHIP_NODE,
			new NodeAttribute<>(traits.key(RelationshipInterfaceTraitDefinition.SOURCE_NODE_PROPERTY),          source),
			new NodeAttribute<>(traits.key(RelationshipInterfaceTraitDefinition.TARGET_NODE_PROPERTY),          target),
			new NodeAttribute<>(traits.key(SchemaRelationshipNodeTraitDefinition.RELATIONSHIP_TYPE_PROPERTY),   type),
			new NodeAttribute<>(traits.key(SchemaRelationshipNodeTraitDefinition.SOURCE_MULTIPLICITY_PROPERTY), "1"),
			new NodeAttribute<>(traits.key(SchemaRelationshipNodeTraitDefinition.TARGET_MULTIPLICITY_PROPERTY), "*"),
			new NodeAttribute<>(traits.key(SchemaRelationshipNodeTraitDefinition.SOURCE_JSON_NAME_PROPERTY),    sourceName),
			new NodeAttribute<>(traits.key(SchemaRelationshipNodeTraitDefinition.TARGET_JSON_NAME_PROPERTY),    targetName)
		);
	}
}
