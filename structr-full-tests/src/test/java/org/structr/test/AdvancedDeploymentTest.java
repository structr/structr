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
package org.structr.test;

import org.structr.api.schema.JsonSchema;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PersonTraitDefinition;
import org.structr.core.traits.definitions.SchemaNodeTraitDefinition;
import org.structr.schema.SchemaService;
import org.structr.schema.export.StructrSchema;
import org.testng.annotations.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.*;

public class AdvancedDeploymentTest extends FullStructrTest {

	@Test
	public void testSchemaImportFromVersion40() {

		try (final Reader reader = new InputStreamReader(AdvancedDeploymentTest.class.getResourceAsStream("/test/schema4.0.json"))) {

			final JsonSchema schema = StructrSchema.createFromSource(reader);
			StructrSchema.replaceDatabaseSchema(app, schema);

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}

		// check existing schema nodes after import
		try (final Tx tx = app.tx()) {

			final Set<String> nodeTypes  = new LinkedHashSet<>();
			final Set<String> relTypes   = new LinkedHashSet<>();
			final Set<String> properties = new LinkedHashSet<>();
			final Set<String> methods    = new LinkedHashSet<>();

			app.nodeQuery(StructrTraits.SCHEMA_RELATIONSHIP_NODE).getResultStream().forEach(n -> relTypes.add(n.getName()));
			app.nodeQuery(StructrTraits.SCHEMA_PROPERTY).getResultStream().forEach(n -> properties.add(n.getName()));
			app.nodeQuery(StructrTraits.SCHEMA_METHOD).getResultStream().forEach(n -> methods.add(n.getName()));
			app.nodeQuery(StructrTraits.SCHEMA_NODE).getResultStream().forEach(n -> nodeTypes.add(n.getName()));

			assertEquals("Invalid number of remaining types after schema import", 2, nodeTypes.size());
			assertTrue(nodeTypes.contains("Project"));
			assertTrue(nodeTypes.contains("Page"));

			assertEquals("Invalid number of remaining relationship types after schema import", 0, relTypes.size());

			assertEquals("Invalid number of remaining methods after schema import", 1, methods.size());
			assertTrue(methods.contains("onCreate"));

			assertEquals("Invalid number of remaining properties after schema import", 3, properties.size());
			assertTrue(properties.contains("title"));
			assertTrue(properties.contains("description"));
			assertTrue(properties.contains("index"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testSchemaImportFromVersion52() {

		try (final Reader reader = new InputStreamReader(AdvancedDeploymentTest.class.getResourceAsStream("/test/schema5.2.json"))) {

			final JsonSchema schema = StructrSchema.createFromSource(reader);
			StructrSchema.replaceDatabaseSchema(app, schema);

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}

		// check existing schema nodes after import
		try (final Tx tx = app.tx()) {

			final Set<String> nodeTypes  = new LinkedHashSet<>();
			final Set<String> relTypes   = new LinkedHashSet<>();
			final Set<String> properties = new LinkedHashSet<>();
			final Set<String> methods    = new LinkedHashSet<>();

			app.nodeQuery(StructrTraits.SCHEMA_RELATIONSHIP_NODE).getResultStream().forEach(n -> relTypes.add(n.getName()));
			app.nodeQuery(StructrTraits.SCHEMA_PROPERTY).getResultStream().forEach(n -> properties.add(n.getName()));
			app.nodeQuery(StructrTraits.SCHEMA_METHOD).getResultStream().forEach(n -> methods.add(n.getName()));
			app.nodeQuery(StructrTraits.SCHEMA_NODE).getResultStream().forEach(n -> nodeTypes.add(n.getName()));

			assertEquals("Invalid number of remaining types after schema import",              110, nodeTypes.size());
			assertEquals("Invalid number of remaining relationship types after schema import", 126, relTypes.size());
			assertEquals("Invalid number of remaining methods after schema import",            125, methods.size());
			assertEquals("Invalid number of remaining properties after schema import",         397, properties.size());

			// check some node types
			assertTrue("Imported schema is missing type Contact",       nodeTypes.contains("Contact"));
			assertTrue("Imported schema is missing type CreditMemo",    nodeTypes.contains("CreditMemo"));
			assertTrue("Imported schema is missing type CreditNote",    nodeTypes.contains("CreditNote"));
			assertTrue("Imported schema is missing type Lead",          nodeTypes.contains("Lead"));
			assertTrue("Imported schema is missing type MarketingTask", nodeTypes.contains("MarketingTask"));

			// check inheritance of imported schema
			assertTrue("Imported type CreditMemo does not inherit from Invoice", getSchemaNodeTraits("CreditMemo").contains("Invoice"));
			assertTrue("Imported type CreditNote does not inherit from File",    getSchemaNodeTraits("CreditNote").contains("File"));
			assertTrue("Imported type Contact does not inherit from Person",     getSchemaNodeTraits("Contact").contains("Person"));
			assertTrue("Imported type Lead does not inherit from MarketingTask", getSchemaNodeTraits("Lead").contains("MarketingTask"));

			// check key from inherited trait (Contact inherits from Person)
			assertTrue("Imported type Contact does not have eMail property", Traits.of("Contact").hasKey(PersonTraitDefinition.EMAIL_PROPERTY));
			assertTrue("Imported type Person does not have eMail property", Traits.of(StructrTraits.PERSON).hasKey(PersonTraitDefinition.EMAIL_PROPERTY));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testSchemaImportFromVersion60() {

		try (final Reader reader = new InputStreamReader(AdvancedDeploymentTest.class.getResourceAsStream("/test/schema6.0.json"))) {

			final JsonSchema schema = StructrSchema.createFromSource(reader);
			StructrSchema.replaceDatabaseSchema(app, schema);

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception");
		}

		// check existing schema nodes after import
		try (final Tx tx = app.tx()) {

			final Set<String> nodeTypes  = new LinkedHashSet<>();
			final Set<String> relTypes   = new LinkedHashSet<>();
			final Set<String> properties = new LinkedHashSet<>();
			final Set<String> methods    = new LinkedHashSet<>();

			app.nodeQuery(StructrTraits.SCHEMA_RELATIONSHIP_NODE).getResultStream().forEach(n -> relTypes.add(n.getName()));
			app.nodeQuery(StructrTraits.SCHEMA_PROPERTY).getResultStream().forEach(n -> properties.add(n.getName()));
			app.nodeQuery(StructrTraits.SCHEMA_METHOD).getResultStream().forEach(n -> methods.add(n.getName()));
			app.nodeQuery(StructrTraits.SCHEMA_NODE).getResultStream().forEach(n -> nodeTypes.add(n.getName()));

			assertEquals("Invalid number of remaining node types after schema import", 3, nodeTypes.size());
			assertTrue(nodeTypes.contains("Project"));
			assertTrue(nodeTypes.contains("Task"));
			assertTrue(nodeTypes.contains("User"));

			assertEquals("Invalid number of remaining relationship types after schema import", 3, relTypes.size());
			assertTrue(relTypes.contains("GroupHASFiles"));
			assertTrue(relTypes.contains("ProjectTASKTask"));
			assertTrue(relTypes.contains("TaskHASFiles"));

			assertEquals("Invalid number of remaining methods after schema import", 7, methods.size());
			assertTrue(methods.contains("afterCreate"));
			assertTrue(methods.contains("afterDelete"));
			assertTrue(methods.contains("afterSave"));
			assertTrue(methods.contains("customMethod"));
			assertTrue(methods.contains("onCreate"));
			assertTrue(methods.contains("onDelete"));
			assertTrue(methods.contains("onSave"));

			assertEquals("Invalid number of remaining properties after schema import", 2, properties.size());
			assertTrue(properties.contains("customDateProperty"));
			assertTrue(properties.contains("customStringProperty"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

	}

	@Test
	public void testDatabaseContentMigration() {

		try (final Tx tx = app.tx()) {

			// create a "schema" using Cypher calls to simulate starting with a previous Structr version..
			createNodeWithCypher(Set.of(StructrTraits.ABSTRACT_SCHEMA_NODE, StructrTraits.SCHEMA_NODE), Map.of(
				GraphObjectTraitDefinition.TYPE_PROPERTY, StructrTraits.SCHEMA_NODE,
				NodeInterfaceTraitDefinition.NAME_PROPERTY, "Contact1",
				SchemaNodeTraitDefinition.INHERITED_TRAITS_PROPERTY, List.of("Person")
			));

			createNodeWithCypher(Set.of(StructrTraits.ABSTRACT_SCHEMA_NODE, StructrTraits.SCHEMA_NODE), Map.of(
				GraphObjectTraitDefinition.TYPE_PROPERTY, StructrTraits.SCHEMA_NODE,
				NodeInterfaceTraitDefinition.NAME_PROPERTY, "Contact2",
				"extendsClassInternal", "org.structr.core.entity.Person"
			));

			final String id1 = createNodeWithCypher(Set.of(StructrTraits.ABSTRACT_SCHEMA_NODE, StructrTraits.SCHEMA_NODE), Map.of(
				GraphObjectTraitDefinition.TYPE_PROPERTY, StructrTraits.SCHEMA_NODE,
				NodeInterfaceTraitDefinition.NAME_PROPERTY, "Contact3"
			));

			final String id2 = createNodeWithCypher(Set.of(StructrTraits.ABSTRACT_SCHEMA_NODE, StructrTraits.SCHEMA_NODE), Map.of(
				GraphObjectTraitDefinition.TYPE_PROPERTY, StructrTraits.SCHEMA_NODE,
				NodeInterfaceTraitDefinition.NAME_PROPERTY, "ExtendedPerson",
				"extendsClassInternal", "org.structr.core.entity.Person"
			));

			createRelationshipWithCypher(id1, id2, "SchemaNodeExtendsSchemaNode", "EXTENDS", Map.of());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final ErrorBuffer errorBuffer = new ErrorBuffer();

			SchemaService.reloadSchema(errorBuffer, null, true, false);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final Set<String> types = Set.of("Contact1", "Contact2", "Contact3");

		for (final String type : types) {

			// check some node types
			assertTrue("Migrated schema is missing type " + type, Traits.exists(type));
			assertTrue("Migrated type " + type + " does not inherit from Person", Traits.of(type).getAllTraits().contains(StructrTraits.PERSON));
			assertTrue("Migrated type " + type + " does not have eMail property", Traits.of(type).hasKey(PersonTraitDefinition.EMAIL_PROPERTY));
		}

		assertTrue("Static type Person does not have eMail property", Traits.of(StructrTraits.PERSON).hasKey(PersonTraitDefinition.EMAIL_PROPERTY));
	}
}


























