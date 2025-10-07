/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.test.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseFeature;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.*;
import org.structr.api.schema.JsonSchema.Cascade;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.*;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.*;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.testng.AssertJUnit.*;

/**
 *
 *
 */
public class SchemaTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(SchemaTest.class.getName());

	@Test
	public void test00SimpleProperties() {

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			// a customer
			final JsonType customer = sourceSchema.addType("Customer");

			customer.addStringProperty("name", "public", "ui").setRequired(true).setUnique(true);
			customer.addStringProperty("street", "public", "ui");
			customer.addStringProperty("city", "public", "ui");
			customer.addDateProperty("birthday", "public", "ui");
			customer.addEnumProperty("status", "public", "ui").setFormat("active,retired,none").setDefaultValue("active");
			customer.addIntegerProperty("count", "public", "ui").setMinimum(1).setMaximum(10, true).setDefaultValue("5");
			customer.addDoubleProperty("number", "public", "ui").setMinimum(2.0, true).setMaximum(5.0, true).setDefaultValue("3.0");
			customer.addLongProperty("loong", "public", "ui").setMinimum(20, true).setMaximum(50);
			customer.addBooleanProperty("isCustomer", "public", "ui");
			customer.addFunctionProperty("displayName", "public", "ui").setReadFunction("concat(this.name, '.', this.id)");
			customer.addStringProperty("description", "public", "ui").setContentType("text/plain").setFormat("multi-line");
			customer.addStringArrayProperty("stringArray", "public", "ui");
			customer.addIntegerArrayProperty("intArray", "public", "ui").setMinimum(0, true).setMaximum(100, true);
			customer.addLongArrayProperty("longArray", "public", "ui").setMinimum(1, true).setMaximum(101, true);
			customer.addDoubleArrayProperty("doubleArray", "public", "ui").setMinimum(2.0, true).setMaximum(102.0, true);
			customer.addBooleanArrayProperty("booleanArray", "public", "ui");
			customer.addByteArrayProperty("byteArray", "public", "ui");

			final String schema = sourceSchema.toString();

			final Map<String, Object> map = new GsonBuilder().create().fromJson(schema, Map.class);

			mapPathValue(map, "definitions.Customer.type",                                          "object");
			mapPathValue(map, "definitions.Customer.required.0",                                    "name");
			mapPathValue(map, "definitions.Customer.properties.booleanArray.type",                   "array");
			mapPathValue(map, "definitions.Customer.properties.booleanArray.items.type",             "boolean");
			mapPathValue(map, "definitions.Customer.properties.city.unique",                         null);
			mapPathValue(map, "definitions.Customer.properties.count.type",                          "integer");
			mapPathValue(map, "definitions.Customer.properties.count.minimum",                       1.0);
			mapPathValue(map, "definitions.Customer.properties.count.maximum",                       10.0);
			mapPathValue(map, "definitions.Customer.properties.count.exclusiveMaximum",              true);
			mapPathValue(map, "definitions.Customer.properties.doubleArray.type",                   "array");
			mapPathValue(map, "definitions.Customer.properties.doubleArray.items.type",             "number");
			mapPathValue(map, "definitions.Customer.properties.doubleArray.items.exclusiveMaximum", true);
			mapPathValue(map, "definitions.Customer.properties.doubleArray.items.exclusiveMaximum", true);
			mapPathValue(map, "definitions.Customer.properties.doubleArray.items.maximum",          102.0);
			mapPathValue(map, "definitions.Customer.properties.doubleArray.items.minimum",          2.0);
			mapPathValue(map, "definitions.Customer.properties.number.type",                        "number");
			mapPathValue(map, "definitions.Customer.properties.number.minimum",                     2.0);
			mapPathValue(map, "definitions.Customer.properties.number.maximum",                     5.0);
			mapPathValue(map, "definitions.Customer.properties.number.exclusiveMinimum",            true);
			mapPathValue(map, "definitions.Customer.properties.number.exclusiveMaximum",            true);
			mapPathValue(map, "definitions.Customer.properties.longArray.type",                     "array");
			mapPathValue(map, "definitions.Customer.properties.longArray.items.type",               "long");
			mapPathValue(map, "definitions.Customer.properties.longArray.items.exclusiveMaximum",   true);
			mapPathValue(map, "definitions.Customer.properties.longArray.items.exclusiveMaximum",   true);
			mapPathValue(map, "definitions.Customer.properties.longArray.items.maximum",            101.0);
			mapPathValue(map, "definitions.Customer.properties.longArray.items.minimum",            1.0);
			mapPathValue(map, "definitions.Customer.properties.loong.type",                         "long");
			mapPathValue(map, "definitions.Customer.properties.loong.minimum",                      20.0);
			mapPathValue(map, "definitions.Customer.properties.loong.maximum",                      50.0);
			mapPathValue(map, "definitions.Customer.properties.loong.exclusiveMinimum",             true);
			mapPathValue(map, "definitions.Customer.properties.intArray.type",                      "array");
			mapPathValue(map, "definitions.Customer.properties.intArray.items.type",                "integer");
			mapPathValue(map, "definitions.Customer.properties.intArray.items.exclusiveMaximum",    true);
			mapPathValue(map, "definitions.Customer.properties.intArray.items.exclusiveMaximum",    true);
			mapPathValue(map, "definitions.Customer.properties.intArray.items.maximum",             100.0);
			mapPathValue(map, "definitions.Customer.properties.intArray.items.minimum",             0.0);
			mapPathValue(map, "definitions.Customer.properties.isCustomer.type",                    "boolean");
			mapPathValue(map, "definitions.Customer.properties.description.type",                   "string");
			mapPathValue(map, "definitions.Customer.properties.description.contentType",            "text/plain");
			mapPathValue(map, "definitions.Customer.properties.description.format",                 "multi-line");
			mapPathValue(map, "definitions.Customer.properties.displayName.type",                   "function");
			mapPathValue(map, "definitions.Customer.properties.displayName.readFunction",           "concat(this.name, '.', this.id)");
			mapPathValue(map, "definitions.Customer.properties.displayName.readFunctionWrapJS",     true);
			mapPathValue(map, "definitions.Customer.properties.displayName.writeFunctionWrapJS",    true);
			mapPathValue(map, "definitions.Customer.properties.name.type",                          "string");
			mapPathValue(map, "definitions.Customer.properties.name.unique",                        true);
			mapPathValue(map, "definitions.Customer.properties.street.type",                        "string");
			mapPathValue(map, "definitions.Customer.properties.status.type",                        "string");
			mapPathValue(map, "definitions.Customer.properties.status.enum.0",                      "active");
			mapPathValue(map, "definitions.Customer.properties.status.enum.1",                      "retired");
			mapPathValue(map, "definitions.Customer.properties.status.enum.2",                      "none");
			mapPathValue(map, "definitions.Customer.properties.stringArray.type",                   "array");
			mapPathValue(map, "definitions.Customer.properties.stringArray.items.type",             "string");
			mapPathValue(map, "definitions.Customer.views.public.0",                                "birthday");
			mapPathValue(map, "definitions.Customer.views.public.1",                                "booleanArray");
			mapPathValue(map, "definitions.Customer.views.public.2",                                "byteArray");
			mapPathValue(map, "definitions.Customer.views.public.3",                                "city");
			mapPathValue(map, "definitions.Customer.views.public.4",                                "count");
			mapPathValue(map, "definitions.Customer.views.public.5",                                "description");
			mapPathValue(map, "definitions.Customer.views.public.6",                                "displayName");
			mapPathValue(map, "definitions.Customer.views.public.7",                                "doubleArray");
			mapPathValue(map, "definitions.Customer.views.public.8",                                "intArray");
			mapPathValue(map, "definitions.Customer.views.public.9",                                "isCustomer");
			mapPathValue(map, "definitions.Customer.views.public.10",                                "longArray");
			mapPathValue(map, "definitions.Customer.views.public.11",                               "loong");
			mapPathValue(map, "definitions.Customer.views.public.12",                               "name");
			mapPathValue(map, "definitions.Customer.views.public.13",                               "number");
			mapPathValue(map, "definitions.Customer.views.public.14",                               "status");
			mapPathValue(map, "definitions.Customer.views.public.15",                               "street");
			mapPathValue(map, "definitions.Customer.views.ui.0",                                    "birthday");
			mapPathValue(map, "definitions.Customer.views.ui.1",                                    "booleanArray");
			mapPathValue(map, "definitions.Customer.views.ui.2",                                    "byteArray");
			mapPathValue(map, "definitions.Customer.views.ui.3",                                    "city");
			mapPathValue(map, "definitions.Customer.views.ui.4",                                    "count");
			mapPathValue(map, "definitions.Customer.views.ui.5",                                    "description");
			mapPathValue(map, "definitions.Customer.views.ui.6",                                    "displayName");
			mapPathValue(map, "definitions.Customer.views.ui.7",                                    "doubleArray");
			mapPathValue(map, "definitions.Customer.views.ui.8",                                    "intArray");
			mapPathValue(map, "definitions.Customer.views.ui.9",                                    "isCustomer");
			mapPathValue(map, "definitions.Customer.views.ui.10",                                    "longArray");
			mapPathValue(map, "definitions.Customer.views.ui.11",                                   "loong");
			mapPathValue(map, "definitions.Customer.views.ui.12",                                   "name");
			mapPathValue(map, "definitions.Customer.views.ui.13",                                   "number");
			mapPathValue(map, "definitions.Customer.views.ui.14",                                   "status");
			mapPathValue(map, "definitions.Customer.views.ui.15",                                   "street");

			// advanced: test schema roundtrip
			compareSchemaRoundtrip(sourceSchema);

		} catch (FrameworkException | InvalidSchemaException | URISyntaxException e) {

			e.printStackTrace();
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test01Inheritance() {

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			sourceSchema.addType("Contact").addTrait(StructrTraits.PRINCIPAL);
			sourceSchema.addType("Customer").addTrait("Contact");

			final String schema = sourceSchema.toString();

			final Map<String, Object> map = new GsonBuilder().create().fromJson(schema, Map.class);

			mapPathValue(map, "definitions.Contact.type",        "object");
			mapPathValue(map, "definitions.Contact.traits.0",  StructrTraits.PRINCIPAL);

			mapPathValue(map, "definitions.Customer.type",       "object");
			mapPathValue(map, "definitions.Customer.traits.0", "Contact");


			// advanced: test schema roundtrip
			compareSchemaRoundtrip(sourceSchema);

		} catch (Exception t) {
			logger.warn("", t);
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test02SimpleSymmetricReferences() {

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project = sourceSchema.addType("Project");
			final JsonObjectType task    = sourceSchema.addType("Task");

			// create relation
			final JsonReferenceType rel = project.relate(task, "has", Cardinality.OneToMany, "project", "tasks");
			rel.setName("ProjectTasks");

			final String schema = sourceSchema.toString();

			// test map paths
			final Map<String, Object> map = new GsonBuilder().create().fromJson(schema, Map.class);

			mapPathValue(map, "definitions.Project.type",                        "object");
			mapPathValue(map, "definitions.Project.properties.tasks.$link",      "#/definitions/ProjectTasks");
			mapPathValue(map, "definitions.Project.properties.tasks.items.$ref", "#/definitions/Task");
			mapPathValue(map, "definitions.Project.properties.tasks.type",       "array");

			mapPathValue(map, "definitions.ProjectTasks.$source",                "#/definitions/Project");
			mapPathValue(map, "definitions.ProjectTasks.$target",                "#/definitions/Task");
			mapPathValue(map, "definitions.ProjectTasks.cardinality",            "OneToMany");
			mapPathValue(map, "definitions.ProjectTasks.rel",                    "has");
			mapPathValue(map, "definitions.ProjectTasks.sourceName",             "project");
			mapPathValue(map, "definitions.ProjectTasks.targetName",             "tasks");
			mapPathValue(map, "definitions.ProjectTasks.type",                   "object");

			mapPathValue(map, "definitions.Task.type",                           "object");
			mapPathValue(map, "definitions.Task.properties.project.$link",       "#/definitions/ProjectTasks");
			mapPathValue(map, "definitions.Task.properties.project.$ref",        "#/definitions/Project");
			mapPathValue(map, "definitions.Task.properties.project.type",        "object");

			// test
			compareSchemaRoundtrip(sourceSchema);

		} catch (FrameworkException | InvalidSchemaException |URISyntaxException ex) {

			logger.warn("", ex);
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test03SchemaBuilder() {

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final String instanceId       = app.getInstanceId();

			final JsonObjectType task = sourceSchema.addType("Task");
			final JsonProperty title  = task.addStringProperty("title", "public", "ui").setRequired(true);
			final JsonProperty desc   = task.addStringProperty("description", "public", "ui").setRequired(true);
			task.addDateProperty("description", "public", "ui").setDatePattern("dd.MM.yyyy").setRequired(true);

			// test function property
			task.addFunctionProperty("displayName", "public", "ui").setReadFunction("this.name");
			task.addFunctionProperty("javascript", "public", "ui").setReadFunction("{ var x = 'test'; return x; }").setContentType("application/x-structr-javascript");


			// a project
			final JsonObjectType project = sourceSchema.addType("Project");
			project.addStringProperty("name", "public", "ui").setRequired(true);

			final JsonReferenceType projectTasks = project.relate(task, "HAS", Cardinality.OneToMany, "project", "tasks");
			projectTasks.setCascadingCreate(Cascade.targetToSource);

			project.getViewPropertyNames("public").add("tasks");
			task.getViewPropertyNames("public").add("project");

			// test enums
			project.addEnumProperty("status", "ui").setFormat("active,planned,finished");

			// a worker
			final JsonObjectType worker = sourceSchema.addType("Worker");
			final JsonReferenceType workerTasks = worker.relate(task, "HAS", Cardinality.OneToMany, "worker", "tasks");
			workerTasks.setCascadingDelete(Cascade.sourceToTarget);


			// reference Worker -> Task
			final JsonReferenceProperty workerProperty = workerTasks.getSourceProperty();
			final JsonReferenceProperty tasksProperty  = workerTasks.getTargetProperty();
			tasksProperty.setName("renamedTasks");


			worker.addReferenceProperty("taskNames",  tasksProperty, "public", "ui").setProperties("name");
			worker.addReferenceProperty("taskInfos",  tasksProperty, "public", "ui").setProperties("id", "name");
			worker.addReferenceProperty("taskErrors", tasksProperty, "public", "ui").setProperties("id");


			task.addReferenceProperty("workerName",   workerProperty, "public", "ui").setProperties("name");
			task.addReferenceProperty("workerNotion", workerProperty, "public", "ui").setProperties("id");


			// test date properties..
			project.addDateProperty("startDate", "public", "ui");

			// methods
			project.addMethod("onCreate", "set(this, 'name', 'wurst')").setIsPrivate(true);



			// test URIs
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/#", sourceSchema.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Task", task.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Task/properties/title", title.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Task/properties/description", desc.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Worker/properties/renamedTasks", tasksProperty.getId().toString());

			compareSchemaRoundtrip(sourceSchema);

		} catch (Exception ex) {

			ex.printStackTrace();
			logger.warn("", ex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test04ManualSchemaRelatedPropertyNameCreation() {

		try {

			try (final Tx tx = app.tx()) {

				final NodeInterface source = app.create(StructrTraits.SCHEMA_NODE, "Source");
				final NodeInterface target = app.create(StructrTraits.SCHEMA_NODE, "Target");

				app.create(StructrTraits.SCHEMA_RELATIONSHIP_NODE,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(SchemaRelationshipNodeTraitDefinition.RELATIONSHIP_TYPE_PROPERTY), "link"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(RelationshipInterfaceTraitDefinition.SOURCE_NODE_PROPERTY), source),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(RelationshipInterfaceTraitDefinition.TARGET_NODE_PROPERTY), target),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(SchemaRelationshipNodeTraitDefinition.SOURCE_MULTIPLICITY_PROPERTY), "1"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(SchemaRelationshipNodeTraitDefinition.TARGET_MULTIPLICITY_PROPERTY), "*")
				);

				tx.success();
			}

			checkSchemaString(StructrSchema.createFromDatabase(app).toString());

		} catch (FrameworkException t) {
			logger.warn("", t);
		}
	}

	@Test
	public void test05SchemaRelatedPropertyNameCreationWithPresets() {

		try {

			// create test case
			final JsonSchema schema     = StructrSchema.newInstance(URI.create(app.getInstanceId()));
			final JsonObjectType source = schema.addType("Source");
			final JsonObjectType target = schema.addType("Target");

			source.relate(target, "link", Cardinality.OneToMany, "sourceLink", "linkTargets");

			checkSchemaString(schema.toString());


		} catch (FrameworkException t) {
			logger.warn("", t);
		}

	}

	@Test
	public void test06SchemaRelatedPropertyNameCreationWithoutPresets() {

		try {

			// create test case
			final JsonSchema schema     = StructrSchema.newInstance(URI.create(app.getInstanceId()));
			final JsonObjectType source = schema.addType("Source");
			final JsonObjectType target = schema.addType("Target");

			source.relate(target, "link", Cardinality.OneToMany);

			checkSchemaString(schema.toString());

		} catch (FrameworkException t) {
			logger.warn("", t);
		}

	}

	@Test
	public void test00DeleteSchemaRelationshipInView() {

		NodeInterface rel = null;

		try (final Tx tx = app.tx()) {

			// create source and target node
			final NodeInterface fooNode = app.create(StructrTraits.SCHEMA_NODE, "Foo");
			final NodeInterface barNode = app.create(StructrTraits.SCHEMA_NODE, "Bar");

			// create relationship
			rel = app.create(StructrTraits.SCHEMA_RELATIONSHIP_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(RelationshipInterfaceTraitDefinition.SOURCE_NODE_PROPERTY), fooNode),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(RelationshipInterfaceTraitDefinition.TARGET_NODE_PROPERTY), barNode),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_RELATIONSHIP_NODE).key(SchemaRelationshipNodeTraitDefinition.RELATIONSHIP_TYPE_PROPERTY), "narf")
			);

			// create "public" view that contains the related property
			app.create(StructrTraits.SCHEMA_VIEW,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "public"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY), fooNode),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY), "type, id, narfBars")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			app.delete(rel);
			tx.success();

		} catch (Throwable t) {

			// deletion of relationship should not fail
			t.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testNonGraphPropertyInView() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema   = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType("Test");

			type.addViewProperty(PropertyView.Public, GraphObjectTraitDefinition.CREATED_BY_PROPERTY);

			// add new type
			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			// check that createdBy is registered in the public view of type Test

			final String test                  = "Test";
			final Set<PropertyKey> propertySet = Traits.of(test).getPropertyKeysForView(PropertyView.Public);

			assertTrue("Non-graph property not registered correctly", propertySet.contains(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.CREATED_BY_PROPERTY)));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testInheritedSchemaPropertyResolution() {

		// create "invalid" schema configuration
		try (final Tx tx = app.tx()) {

			final JsonSchema schema   = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType("Test");

			type.addTrait(StructrTraits.FILE);

			type.addViewProperty(PropertyView.Public, "children");

			// add new type
			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testModifiedPropertyValueAccessInScripting() {

		// schema setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema   = StructrSchema.createFromDatabase(app);
			final JsonObjectType type = schema.addType("Test");

			type.addStringProperty("desc");
			type.addStringProperty("nameBefore");
			type.addStringProperty("nameAfter");
			type.addStringProperty("descBefore");
			type.addStringProperty("descAfter");

			type.addMethod("onSave",
				"{"
					+ " var self = Structr.this;"
					+ " var mod = Structr.retrieve('modifications');"
					+ " self.nameBefore = mod.before.name;"
					+ " self.nameAfter  = mod.after.name;"
					+ " self.descBefore = mod.before.desc;"
					+ " self.descAfter  = mod.after.desc;"
				+ " }"
			);

			// add new type
			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		String uuid = null;

		final String type = "Test";

		// create test object
		try (final Tx tx = app.tx()) {

			assertNotNull(type);

			final GraphObject obj = app.create(type, "test");

			uuid = obj.getUuid();

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// test state before modification
		try (final Tx tx = app.tx()) {

			final GraphObject test = app.getNodeById(type, uuid);
			assertNotNull(test);

			assertNull("Invalid value before modification", test.getProperty(Traits.of(type).key("nameBefore")));
			assertNull("Invalid value before modification", test.getProperty(Traits.of(type).key("nameAfter")));
			assertNull("Invalid value before modification", test.getProperty(Traits.of(type).key("descBefore")));
			assertNull("Invalid value before modification", test.getProperty(Traits.of(type).key("descAfter")));

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// modify object
		try (final Tx tx = app.tx()) {

			final GraphObject test = app.getNodeById(type, uuid);
			assertNotNull(test);

			test.setProperty(Traits.of(type).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "new test");
			test.setProperty(Traits.of(type).key("desc"), "description");

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// test state after modification
		try (final Tx tx = app.tx()) {

			final GraphObject test = app.getNodeById(type, uuid);
			assertNotNull(test);

			assertEquals("Invalid value after modification", "test",        test.getProperty(Traits.of(type).key("nameBefore")));
			assertEquals("Invalid value after modification", "new test",    test.getProperty(Traits.of(type).key("nameAfter")));
			assertNull("Invalid value after modification",                          test.getProperty(Traits.of(type).key("descBefore")));
			assertEquals("Invalid value after modification", "description", test.getProperty(Traits.of(type).key("descAfter")));

			tx.success();

		} catch (Throwable fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testInitializationOfNonStructrNodesWithTenantIdentifier() {

		// don't run tests that depend on Cypher being available in the backend
		if (Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.QueryLanguage, "application/x-cypher-query")) {

			final String tenantId = Services.getInstance().getDatabaseService().getTenantIdentifier();

			try (final Tx tx = app.tx()) {

				final JsonSchema schema = StructrSchema.createFromDatabase(app);

				schema.addType("PERSON");

				StructrSchema.extendDatabaseSchema(app, schema);

				tx.success();

			} catch (Throwable t) {

				t.printStackTrace();
				fail("Unexpected exception.");
			}

			final String type = "PERSON";

			try (final Tx tx = app.tx()) {

				app.query("CREATE (p:PERSON:" + tenantId + " { name: \"p1\" } )", new HashMap<>());
				app.query("CREATE (p:PERSON:" + tenantId + " { name: \"p2\" } )", new HashMap<>());
				app.query("CREATE (p:PERSON:" + tenantId + " { name: \"p3\" } )", new HashMap<>());

				tx.success();

			} catch (Throwable t) {

				t.printStackTrace();
				fail("Unexpected exception.");
			}

			try (final Tx tx = app.tx()) {

				app.command(BulkCreateLabelsCommand.class).execute(Collections.emptyMap());
				app.command(BulkSetUuidCommand.class).execute(map("allNodes", true));
				app.command(BulkRebuildIndexCommand.class).execute(Collections.emptyMap());

				tx.success();

			} catch (Throwable t) {

				t.printStackTrace();
				fail("Unexpected exception.");
			}

			try (final Tx tx = app.tx()) {

				final List<NodeInterface> nodes = app.nodeQuery(type).getAsList();

				assertEquals("Non-Structr nodes not initialized correctly", 3, nodes.size());
				assertEquals("Non-Structr nodes not initialized correctly", "PERSON", nodes.get(0).getType());
				assertEquals("Non-Structr nodes not initialized correctly", "PERSON", nodes.get(1).getType());
				assertEquals("Non-Structr nodes not initialized correctly", "PERSON", nodes.get(2).getType());

				tx.success();

			} catch (Throwable t) {

				t.printStackTrace();
				fail("Unexpected exception.");
			}
		}
	}

	@Test
	public void testRelatedTypeOnNotionProperty() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project    = schema.addType("Project");
			final JsonObjectType task       = schema.addType("Task");
			final JsonReferenceType rel     = project.relate(task, "TASK", Cardinality.OneToMany, "project", "tasks");
			final JsonReferenceProperty ref = rel.getSourceProperty();

			project.addStringProperty("blah").setUnique(true);

			task.addReferenceProperty("projectBlah", ref).setProperties("blah", "true");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("NotionProperty setup failed.");
		}
	}

	@Test
	public void testSchemaRenameInheritedBaseType() {

		// setup 1: create types
		try (final Tx tx = app.tx()) {

			final JsonSchema schema    = StructrSchema.createFromDatabase(app);
			final JsonObjectType rel   = schema.addType("RelatedType");
			final JsonObjectType base  = schema.addType("BaseType");
			final JsonObjectType ext1  = schema.addType("Extended1");
			final JsonObjectType ext11 = schema.addType("Extended11");
			final JsonObjectType ext2  = schema.addType("Extended2");

			ext1.addTrait("BaseType");
			ext2.addTrait("BaseType");

			// two levels
			ext11.addTrait("Extended1");

			// relationship
			base.relate(rel);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// setup 2: delete base type
		try (final Tx tx = app.tx()) {

			logger.info("Renaming base type..");

			final NodeInterface base = app.nodeQuery(StructrTraits.SCHEMA_NODE).name("BaseType").getFirst();

			assertNotNull("Base type schema node not found", base);

			base.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "ModifiedBaseType");

			app.delete(base);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// test 1: add method to one of the types that doesn't have a base type any more
		try (final Tx tx = app.tx()) {

			logger.info("Adding method..");

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType ext1     = schema.getType("Extended");

			ext1.addMethod("doTest", "log('test')");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// test 2: create objects for each type
		try (final Tx tx = app.tx()) {

			logger.info("Creating node instances..");

			final String ext1  = "Extended1";
			final String ext11 = "Extended11";
			final String ext2  = "Extended2";

			app.create(ext1,  "ext1");
			app.create(ext11, "ext11");
			app.create(ext2,  "ext2");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

	}

	@Test
	public void testSchemaDeleteInheritedBaseType() {

		// setup 1: create types
		try (final Tx tx = app.tx()) {

			final JsonSchema schema    = StructrSchema.createFromDatabase(app);
			final JsonObjectType rel   = schema.addType("RelatedType");
			final JsonObjectType base  = schema.addType("BaseType");
			final JsonObjectType ext1  = schema.addType("Extended1");
			final JsonObjectType ext11 = schema.addType("Extended11");
			final JsonObjectType ext2  = schema.addType("Extended2");

			ext1.addTrait("BaseType");
			ext2.addTrait("BaseType");

			// two levels
			ext11.addTrait("Extended1");

			// relationship
			base.relate(rel);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// setup 2: delete base type
		try (final Tx tx = app.tx()) {

			System.out.println(StructrSchema.createFromDatabase(app).toString());

			logger.info("Deleting base type..");

			final NodeInterface base = app.nodeQuery(StructrTraits.SCHEMA_NODE).name("BaseType").getFirst();

			assertNotNull("Base type schema node not found", base);

			app.delete(base);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// test 1: add method to one of the types that doesn't have a base type any more
		try (final Tx tx = app.tx()) {

			logger.info("Adding method..");

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType ext1     = schema.getType("Extended");

			ext1.addMethod("doTest", "log('test')");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// test 2: create objects for each type
		try (final Tx tx = app.tx()) {

			logger.info("Creating node instances..");

			final String ext1  = "Extended1";
			final String ext11 = "Extended11";
			final String ext2  = "Extended2";

			app.create(ext1,  "ext1");
			app.create(ext11, "ext11");
			app.create(ext2,  "ext2");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testMethodInheritance() {

		// setup 1: create types
		try (final Tx tx = app.tx()) {

			final JsonSchema schema    = StructrSchema.createFromDatabase(app);
			final JsonObjectType base  = schema.addType("BaseType");
			final JsonObjectType ext1  = schema.addType("Extended1");
			final JsonObjectType ext11 = schema.addType("Extended11");
			final JsonObjectType ext2  = schema.addType("Extended2");

			ext1.addTrait("BaseType");
			ext2.addTrait("BaseType");

			// two levels
			ext11.addTrait("Extended1");

			// methods
			base.addMethod("doTest", "'BaseType'");
			ext1.addMethod("doTest", "'Extended1'");
			ext11.addMethod("doTest", "'Extended11'");
			ext2.addMethod("doTest", "'Extended2'");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// setup 2: create objects for each type
		try (final Tx tx = app.tx()) {

			logger.info("Creating node instances..");

			final String baseType  = "BaseType";
			final String ext1Type  = "Extended1";
			final String ext11Type = "Extended11";
			final String ext2Type  = "Extended2";

			final GraphObject base  = app.create(baseType,  "base");
			final GraphObject ext1  = app.create(ext1Type,  "ext1");
			final GraphObject ext11 = app.create(ext11Type, "ext11");
			final GraphObject ext2  = app.create(ext2Type,  "ext2");

			final ActionContext ctx = new ActionContext(securityContext);

			assertEquals("Invalid inheritance result, overriding method is not called", "BaseType",   (Scripting.evaluate(ctx, base,  "${{ $.this.doTest(); }}", "test1")));
			assertEquals("Invalid inheritance result, overriding method is not called", "Extended1",  (Scripting.evaluate(ctx, ext1,  "${{ $.this.doTest(); }}", "test2")));
			assertEquals("Invalid inheritance result, overriding method is not called", "Extended11", (Scripting.evaluate(ctx, ext11, "${{ $.this.doTest(); }}", "test3")));
			assertEquals("Invalid inheritance result, overriding method is not called", "Extended2",  (Scripting.evaluate(ctx, ext2,  "${{ $.this.doTest(); }}", "test4")));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testOverwrittenPropertyRemoval() {

		// setup 1: add type
		try (final Tx tx = app.tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final JsonType customer       = sourceSchema.addType("Customer");

			// apply schema changes
			StructrSchema.extendDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// test: check that no uniqueness is configured
		try (final Tx tx = app.tx()) {

			final String type = "Customer";

			app.create(type, "test");
			app.create(type, "test");

			tx.success();


		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Uniqueness validation should not be active any more");
		}

		// setup: remove all nodes
		try (final Tx tx = app.tx()) {

			final String type = "Customer";

			app.deleteAllNodesOfType(type);

			tx.success();


		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Uniqueness validation should not be active any more");
		}

		// setup 2: overwrite name property
		try (final Tx tx = app.tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final JsonType customer       = sourceSchema.addType("Customer");

			customer.addStringProperty("name").setIndexed(true).setRequired(true).setUnique(true);

			// apply schema changes
			StructrSchema.extendDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}


		// test 1: check that uniqueness is correctly configured
		try (final Tx tx = app.tx()) {

			final String type = "Customer";

			app.create(type, "test");

			// second attempt should fail
			app.create(type, "test");

			tx.success();

			fail("Uniqueness validation is not active");

		} catch (FrameworkException fex) {
		}

		// setup 2: remove overwritten property
		try (final Tx tx = app.tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final JsonType customer       = sourceSchema.getType("Customer");

			for (Iterator<JsonProperty> it = customer.getProperties().iterator(); it.hasNext();) {

				final JsonProperty prop = it.next();
				if ("name".equals(prop.getName())) {

					System.out.println("Removing name property");
					it.remove();
				}
			}

			// apply schema changes
			StructrSchema.replaceDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// test: check that no uniqueness is configured
		try (final Tx tx = app.tx()) {

			final String type = "Customer";

			app.create(type, "test");
			app.create(type, "test");

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Uniqueness validation should not be active any more");
		}
	}

	@Test
	public void testPropertyOnBuiltInType() {

		try (final Tx tx = app.tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final JsonType page           = sourceSchema.getType(StructrTraits.PAGE);

			page.addStringProperty("test");

			// apply schema changes
			StructrSchema.extendDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testMethodCacheInvalidation() {

		// create a method and a user-defined function and verify that the cache is invalidated correctly)
		try (final Tx tx = app.tx()) {

			final Traits methodTraits = Traits.of(StructrTraits.SCHEMA_METHOD);

			final NodeInterface testType = app.create(StructrTraits.SCHEMA_NODE, "Test");

			// create instance method
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(methodTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "test01"),
				new NodeAttribute<>(methodTraits.key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), testType),
				new NodeAttribute<>(methodTraits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ return 'first version'; }")
			);

			// create static method
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(methodTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "test02"),
				new NodeAttribute<>(methodTraits.key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), testType),
				new NodeAttribute<>(methodTraits.key(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY), true),
				new NodeAttribute<>(methodTraits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ return 'first version'; }")
			);

			// create user-defined function (schema method with no connection to a type)
			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(methodTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "test03"),
				new NodeAttribute<>(methodTraits.key(SchemaMethodTraitDefinition.IS_STATIC_PROPERTY), true),
				new NodeAttribute<>(methodTraits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ return 'first version'; }")
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// verify preconditions (all methods return "first version")
		try (final Tx tx = app.tx()) {

			final NodeInterface instance = app.create("Test", "MyTestInstance");
			final ActionContext actionContext = new ActionContext(securityContext);

			assertEquals("Invalid precondition", "first version", Scripting.evaluate(actionContext, instance, "{ $.this.test01(); }", "test01"));
			assertEquals("Invalid precondition", "first version", Scripting.evaluate(actionContext, instance, "{ $.Test.test02(); }", "test02"));
			assertEquals("Invalid precondition", "first version", Scripting.evaluate(actionContext, instance, "{ $.test03(); }", "test03"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// change methods
		try (final Tx tx = app.tx()) {

			final NodeInterface method1 = app.nodeQuery(StructrTraits.SCHEMA_METHOD).name("test01").getFirst();
			final NodeInterface method2 = app.nodeQuery(StructrTraits.SCHEMA_METHOD).name("test02").getFirst();
			final NodeInterface method3 = app.nodeQuery(StructrTraits.SCHEMA_METHOD).name("test03").getFirst();

			method1.as(SchemaMethod.class).setSource("{ return 'second version'; }");
			method2.as(SchemaMethod.class).setSource("{ return 'second version'; }");
			method3.as(SchemaMethod.class).setSource("{ return 'second version'; }");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// verify that the changes are applied (all methods return "second version")
		try (final Tx tx = app.tx()) {

			final NodeInterface instance = app.create("Test", "MyTestInstance");
			final ActionContext actionContext = new ActionContext(securityContext);

			assertEquals("Schema method cache for instance methods is not invalidated correctly.", "second version", Scripting.evaluate(actionContext, instance, "{ $.this.test01(); }", "test01"));
			assertEquals("Schema method cache for static methods is not invalidated correctly.", "second version", Scripting.evaluate(actionContext, instance, "{ $.Test.test02(); }", "test02"));
			assertEquals("Schema method cache for user-defined functions is not invalidated correctly.", "second version", Scripting.evaluate(actionContext, instance, "{ $.test03(); }", "test03"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testMultipleInheritanceForMethods1() {

		// setup: create the classical diamond-shaped inheritance
		//    A
		//   / \
		//  B   C
		//   \ /
		//    D
		//
		// Each type has its own method and, we expect TestD to inherit all four methods.

		try (final Tx tx = app.tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final JsonType testA       = sourceSchema.addType("TestA");
			final JsonType testB       = sourceSchema.addType("TestB");
			final JsonType testC       = sourceSchema.addType("TestC");
			final JsonType testD       = sourceSchema.addType("TestD");

			testB.addTrait("TestA");
			testC.addTrait("TestA");

			testD.addTrait("TestB");
			testD.addTrait("TestC");

			// add methods
			testA.addMethod("testAMethod", "{ return 'testA!'; }");
			testB.addMethod("testBMethod", "{ return 'testB!'; }");
			testC.addMethod("testCMethod", "{ return 'testC!'; }");
			testD.addMethod("testDMethod", "{ return 'testD!'; }");

			// apply schema changes
			StructrSchema.extendDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// test existence of all methods on TestD!
		try (final Tx tx = app.tx()) {

			final NodeInterface test = app.create("TestD", "TestObject");

			assertEquals("Method from TestA was not inherited correctly", "testA!", Actions.execute(securityContext, test, "{ $.this.testAMethod(); }", "testTestA"));
			assertEquals("Method from TestB was not inherited correctly", "testB!", Actions.execute(securityContext, test, "{ $.this.testBMethod(); }", "testTestB"));
			assertEquals("Method from TestC was not inherited correctly", "testC!", Actions.execute(securityContext, test, "{ $.this.testCMethod(); }", "testTestC"));
			assertEquals("Method from TestD was not inherited correctly", "testD!", Actions.execute(securityContext, test, "{ $.this.testDMethod(); }", "testTestD"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testMultipleInheritanceForMethods2() {

		// setup: create the classical diamond-shaped inheritance but with overlapping identical methods
		//    A
		//   / \
		//  B   C
		//   \ /
		//    D
		//
		// Each type except TestC has its own method, and we expect all methods to return the correct value

		try (final Tx tx = app.tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final JsonType testA       = sourceSchema.addType("TestA");
			final JsonType testB       = sourceSchema.addType("TestB");
			final JsonType testC       = sourceSchema.addType("TestC");
			final JsonType testD       = sourceSchema.addType("TestD");

			testB.addTrait("TestA");
			testC.addTrait("TestA");

			testD.addTrait("TestB");
			testD.addTrait("TestC");

			// add methods
			testA.addMethod("testAMethod", "{ return 'testA!'; }");
			testB.addMethod("testAMethod", "{ return 'testB!'; }");
			testD.addMethod("testAMethod", "{ return 'testD!'; }");

			// apply schema changes
			StructrSchema.extendDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// test existence of all methods on TestD!
		try (final Tx tx = app.tx()) {

			final NodeInterface testA = app.create("TestA", "TestObjectA");
			final NodeInterface testB = app.create("TestB", "TestObjectB");
			final NodeInterface testC = app.create("TestC", "TestObjectC");
			final NodeInterface testD = app.create("TestD", "TestObjectD");

			assertEquals("Method from TestA was not inherited correctly", "testA!", Actions.execute(securityContext, testA, "{ $.this.testAMethod(); }", "testTestA"));
			assertEquals("Method from TestB was not inherited correctly", "testB!", Actions.execute(securityContext, testB, "{ $.this.testAMethod(); }", "testTestB"));
			assertEquals("Method from TestA was not inherited correctly", "testA!", Actions.execute(securityContext, testC, "{ $.this.testAMethod(); }", "testTestC"));
			assertEquals("Method from TestD was not inherited correctly", "testD!", Actions.execute(securityContext, testD, "{ $.this.testAMethod(); }", "testTestD"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testMultipleInheritanceForDifferentProperties() {

		// setup: create the classical diamond-shaped inheritance
		//    A
		//   / \
		//  B   C
		//   \ /
		//    D

		try (final Tx tx = app.tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final JsonType testA          = sourceSchema.addType("TestA");
			final JsonType testB          = sourceSchema.addType("TestB");
			final JsonType testC          = sourceSchema.addType("TestC");
			final JsonType testD          = sourceSchema.addType("TestD");

			testB.addTrait("TestA");
			testC.addTrait("TestA");

			testD.addTrait("TestB");
			testD.addTrait("TestC");

			// add methods
			testA.addStringProperty("testAProperty");
			testB.addStringProperty("testBProperty");
			testC.addStringProperty("testCProperty");
			testD.addStringProperty("testDProperty");

			// apply schema changes
			StructrSchema.extendDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final Traits traits      = Traits.of("TestD");
			final NodeInterface test = app.create("TestD",
				new NodeAttribute<>(traits.key("testAProperty"), "testA!"),
				new NodeAttribute<>(traits.key("testBProperty"), "testB!"),
				new NodeAttribute<>(traits.key("testCProperty"), "testC!"),
				new NodeAttribute<>(traits.key("testDProperty"), "testD!")
			);

			assertEquals("Property from TestA was not inherited correctly", "testA!", Actions.execute(securityContext, test, "{ $.this.testAProperty; }", "testTestA"));
			assertEquals("Property from TestB was not inherited correctly", "testB!", Actions.execute(securityContext, test, "{ $.this.testBProperty; }", "testTestB"));
			assertEquals("Property from TestC was not inherited correctly", "testC!", Actions.execute(securityContext, test, "{ $.this.testCProperty; }", "testTestC"));
			assertEquals("Property from TestD was not inherited correctly", "testD!", Actions.execute(securityContext, test, "{ $.this.testDProperty; }", "testTestD"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testMultipleInheritanceForIdenticalProperties() {

		// setup: create the classical diamond-shaped inheritance and expect the schema creation to fail
		//    A
		//   / \
		//  B   C
		//   \ /
		//    D

		try (final Tx tx = app.tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final JsonType testA       = sourceSchema.addType("TestA");
			final JsonType testB       = sourceSchema.addType("TestB");
			final JsonType testC       = sourceSchema.addType("TestC");
			final JsonType testD       = sourceSchema.addType("TestD");

			testB.addTrait("TestA");
			testC.addTrait("TestA");

			// order of traits here determines the type of the inherited property in TestD!
			testD.addTrait("TestB");
			testD.addTrait("TestC");

			// add properties
			testB.addBooleanProperty("testAProperty");
			testC.addIntegerProperty("testAProperty");

			// apply schema changes
			StructrSchema.extendDatabaseSchema(app, sourceSchema);

			tx.success();

			fail("Defining clashing properties in inherited traits should throw an exception.");

		} catch (FrameworkException expected) {
		}
	}

	@Test
	public void testAbstractProperties() {

		try (final Tx tx = app.tx()) {

			// We create the following class structure:
			//    A
			//   / \
			//  B   C
			//
			// A has an abstract property that is overridden by B and C, and we
			// make sure that this only works of the property in A is abstract.

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final JsonType testA          = sourceSchema.addType("TestA");
			final JsonType testB          = sourceSchema.addType("TestB");
			final JsonType testC          = sourceSchema.addType("TestC");
			final JsonType testD          = sourceSchema.addType("TestD");

			testB.addTrait("TestA");
			testC.addTrait("TestB");
			testD.addTrait("TestC");

			// add properties
			testA.addStringProperty("testAProperty").setAbstract(true);
			testB.addBooleanProperty("testAProperty");
			testC.addIntegerProperty("testAProperty");

			// apply schema changes
			StructrSchema.extendDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();

			fail("Overwriting an abstract property should not throw an exception.");
		}
	}

	@Test
	public void testAbstractProperties2() {

		try (final Tx tx = app.tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final JsonType testA          = sourceSchema.addType("TestA");
			final JsonType testB          = sourceSchema.addType("TestB");

			testB.addTrait("TestA");

			// add properties
			testA.addStringProperty("testAProperty");
			testB.addFunctionProperty("testAProperty").setReadFunction("'test'").setTypeHint("string");

			// apply schema changes
			StructrSchema.extendDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();

			fail("Overwriting a property with a correctly typed function property should not throw an exception.");
		}
	}

	@Test
	public void testOverrideNameAttribute() {

		try (final Tx tx = app.tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final JsonType type           = sourceSchema.addType("User");

			// add properties
			type.addStringProperty("name").setIndexed(true, true);

			// apply schema changes
			StructrSchema.extendDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();

			fail("Unexpected exception.");
		}

		final PropertyKey key = Traits.of("User").key("name");

		assertEquals("Unable to overwrite name attribute in existing type.", "User", key.getDeclaringTrait().getLabel());

	}

	// ----- private methods -----
	private void checkSchemaString(final String source) {

		final Gson gson = new GsonBuilder().create();

		final Map<String, Object> map  = gson.fromJson(source, Map.class);
		assertNotNull("Invalid schema serialization", map);

		final Map<String, Object> defs = (Map)map.get("definitions");
		assertNotNull("Invalid schema serialization", defs);

		final Map<String, Object> src  = (Map)defs.get("Source");
		assertNotNull("Invalid schema serialization", src);

		final Map<String, Object> srcp = (Map)src.get("properties");
		assertNotNull("Invalid schema serialization", srcp);

		final Map<String, Object> tgt  = (Map)defs.get("Target");
		assertNotNull("Invalid schema serialization", tgt);

		final Map<String, Object> tgtp = (Map)tgt.get("properties");
		assertNotNull("Invalid schema serialization", tgtp);

		final Map<String, Object> lnk  = (Map)defs.get("SourcelinkTarget");
		assertNotNull("Invalid schema serialization", lnk);

		// check related property names
		assertTrue("Invalid schema serialization result", srcp.containsKey("linkTargets"));
		assertTrue("Invalid schema serialization result", tgtp.containsKey("sourceLink"));
		assertEquals("Invalid schema serialization result", "sourceLink", lnk.get("sourceName"));
		assertEquals("Invalid schema serialization result", "linkTargets", lnk.get("targetName"));
	}

	private void mapPathValue(final Map<String, Object> map, final String mapPath, final Object value) {

		final String[] parts = mapPath.split("[\\.]+");
		Object current       = map;

		for (int i=0; i<parts.length; i++) {

			final String part = parts[i];
			if (StringUtils.isNumeric(part)) {

				int index = Integer.valueOf(part);
				if (current instanceof List) {

					current = ((List)current).get(index);
				}

			} else {

				if (current instanceof Map) {

					current = ((Map)current).get(part);
				}
			}
		}

		assertEquals("Invalid map path result for " + mapPath, value, current);
	}

	private void compareSchemaRoundtrip(final JsonSchema sourceSchema) throws FrameworkException, InvalidSchemaException, URISyntaxException {

		final String source           = sourceSchema.toString();
		final JsonSchema targetSchema = StructrSchema.createFromSource(sourceSchema.toString());
		final String target           = targetSchema.toString();

		assertEquals("Invalid schema (de)serialization roundtrip result", source, target);

		StructrSchema.replaceDatabaseSchema(app, targetSchema);

		final JsonSchema replacedSchema = StructrSchema.createFromDatabase(app);
		final String replaced = replacedSchema.toString();

		assertEquals("Invalid schema replacement result", source, replaced);
	}

	private Map<String, Object> map(final String key, final Object value) {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put(key, value);

		return map;
	}
}
