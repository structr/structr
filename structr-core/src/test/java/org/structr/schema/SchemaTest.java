/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractUser;
import org.structr.core.entity.Relation;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.entity.SchemaView;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.json.InvalidSchemaException;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonReferenceProperty;
import org.structr.schema.json.JsonReferenceType;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonSchema.Cascade;
import org.structr.schema.json.JsonType;

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
			customer.addEnumProperty("status", "public", "ui").setEnums("active", "retired", "none");
			customer.addIntegerProperty("count", "public", "ui").setMinimum(1).setMaximum(10, true);
			customer.addNumberProperty("number", "public", "ui").setMinimum(2.0, true).setMaximum(5.0, true);
			customer.addLongProperty("loong", "public", "ui").setMinimum(20, true).setMaximum(50);
			customer.addBooleanProperty("isCustomer", "public", "ui");
			customer.addFunctionProperty("displayName", "public", "ui").setReadFunction("concat(this.name, '.', this.id)");
			customer.addStringProperty("description", "public", "ui").setContentType("text/plain").setFormat("multi-line");
			customer.addStringArrayProperty("stringArray", "public", "ui");

			final String schema = sourceSchema.toString();

			final Map<String, Object> map = new GsonBuilder().create().fromJson(schema, Map.class);

			mapPathValue(map, "definitions.Customer.type",                                   "object");
			mapPathValue(map, "definitions.Customer.required.0",                             "name");
			mapPathValue(map, "definitions.Customer.properties.city.unique",                 null);
			mapPathValue(map, "definitions.Customer.properties.count.type",                  "integer");
			mapPathValue(map, "definitions.Customer.properties.count.minimum",               1.0);
			mapPathValue(map, "definitions.Customer.properties.count.maximum",               10.0);
			mapPathValue(map, "definitions.Customer.properties.count.exclusiveMaximum",      true);
			mapPathValue(map, "definitions.Customer.properties.number.type",                 "number");
			mapPathValue(map, "definitions.Customer.properties.number.minimum",              2.0);
			mapPathValue(map, "definitions.Customer.properties.number.maximum",              5.0);
			mapPathValue(map, "definitions.Customer.properties.number.exclusiveMinimum",     true);
			mapPathValue(map, "definitions.Customer.properties.number.exclusiveMaximum",     true);
			mapPathValue(map, "definitions.Customer.properties.loong.type",                  "long");
			mapPathValue(map, "definitions.Customer.properties.loong.minimum",               20.0);
			mapPathValue(map, "definitions.Customer.properties.loong.maximum",               50.0);
			mapPathValue(map, "definitions.Customer.properties.loong.exclusiveMinimum",      true);
			mapPathValue(map, "definitions.Customer.properties.isCustomer.type",             "boolean");
			mapPathValue(map, "definitions.Customer.properties.description.type",            "string");
			mapPathValue(map, "definitions.Customer.properties.description.contentType",     "text/plain");
			mapPathValue(map, "definitions.Customer.properties.description.format",          "multi-line");
			mapPathValue(map, "definitions.Customer.properties.displayName.type",            "function");
			mapPathValue(map, "definitions.Customer.properties.displayName.readFunction",    "concat(this.name, '.', this.id)");
			mapPathValue(map, "definitions.Customer.properties.name.type",                   "string");
			mapPathValue(map, "definitions.Customer.properties.name.unique",                 true);
			mapPathValue(map, "definitions.Customer.properties.street.type",                 "string");
			mapPathValue(map, "definitions.Customer.properties.status.type",                 "string");
			mapPathValue(map, "definitions.Customer.properties.status.enum.0",               "active");
			mapPathValue(map, "definitions.Customer.properties.status.enum.1",               "none");
			mapPathValue(map, "definitions.Customer.properties.status.enum.2",               "retired");
			mapPathValue(map, "definitions.Customer.properties.stringArray.type",            "array");
			mapPathValue(map, "definitions.Customer.properties.stringArray.items.type",      "string");
			mapPathValue(map, "definitions.Customer.views.public.0",                         "birthday");
			mapPathValue(map, "definitions.Customer.views.public.1",                         "city");
			mapPathValue(map, "definitions.Customer.views.public.2",                         "count");
			mapPathValue(map, "definitions.Customer.views.public.3",                         "description");
			mapPathValue(map, "definitions.Customer.views.public.4",                         "displayName");
			mapPathValue(map, "definitions.Customer.views.public.5",                         "isCustomer");
			mapPathValue(map, "definitions.Customer.views.public.6",                         "loong");
			mapPathValue(map, "definitions.Customer.views.public.7",                         "name");
			mapPathValue(map, "definitions.Customer.views.public.8",                         "number");
			mapPathValue(map, "definitions.Customer.views.public.9",                         "status");
			mapPathValue(map, "definitions.Customer.views.public.10",                        "street");
			mapPathValue(map, "definitions.Customer.views.ui.0",                             "birthday");
			mapPathValue(map, "definitions.Customer.views.ui.1",                             "city");
			mapPathValue(map, "definitions.Customer.views.ui.2",                             "count");
			mapPathValue(map, "definitions.Customer.views.ui.3",                             "description");
			mapPathValue(map, "definitions.Customer.views.ui.4",                             "displayName");
			mapPathValue(map, "definitions.Customer.views.ui.5",                             "isCustomer");
			mapPathValue(map, "definitions.Customer.views.ui.6",                             "loong");
			mapPathValue(map, "definitions.Customer.views.ui.7",                             "name");
			mapPathValue(map, "definitions.Customer.views.ui.8",                             "number");
			mapPathValue(map, "definitions.Customer.views.ui.9",                             "status");
			mapPathValue(map, "definitions.Customer.views.ui.10",                            "street");

			// advanced: test schema roundtrip
			compareSchemaRoundtrip(sourceSchema);

		} catch (Exception t) {

			logger.warn("", t);
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test01Inheritance() {

		// we need to wait for the schema service to be initialized here.. :(
		try { Thread.sleep(1000); } catch (Throwable t) {}

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			final JsonType contact  = sourceSchema.addType("Contact").setExtends(StructrApp.getSchemaId(AbstractUser.class));
			final JsonType customer = sourceSchema.addType("Customer").setExtends(contact);

			final String schema = sourceSchema.toString();

			final Map<String, Object> map = new GsonBuilder().create().fromJson(schema, Map.class);

			mapPathValue(map, "definitions.Contact.type",      "object");
			mapPathValue(map, "definitions.Contact.$extends",  "https://structr.org/v1.1/definitions/AbstractUser");

			mapPathValue(map, "definitions.Customer.type",      "object");
			mapPathValue(map, "definitions.Customer.$extends",  "#/definitions/Contact");


			// advanced: test schema roundtrip
			compareSchemaRoundtrip(sourceSchema);

		} catch (Exception t) {
			logger.warn("", t);
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test02SimpleSymmetricReferences() {

		// we need to wait for the schema service to be initialized here.. :(
		try { Thread.sleep(1000); } catch (Throwable t) {}

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			final JsonObjectType project = sourceSchema.addType("Project");
			final JsonObjectType task    = sourceSchema.addType("Task");

			// create relation
			final JsonReferenceType rel = project.relate(task, "has", Cardinality.OneToMany, "project", "tasks");
			rel.setName("ProjectTasks");

			final String schema = sourceSchema.toString();

			System.out.println(schema);

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

		// we need to wait for the schema service to be initialized here.. :(
		try { Thread.sleep(1000); } catch (Throwable t) {}

		try {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);
			final String instanceId       = app.getInstanceId();

			final JsonObjectType task = sourceSchema.addType("Task");
			final JsonProperty title  = task.addStringProperty("title", "public", "ui").setRequired(true);
			final JsonProperty desc   = task.addStringProperty("description", "public", "ui").setRequired(true);
			task.addDateProperty("description", "public", "ui").setDatePattern("dd.MM.yyyy").setRequired(true);

			// test function property
			task.addFunctionProperty("displayName", "public", "ui").setReadFunction("this.name");
			task.addFunctionProperty("javascript", "public", "ui").setReadFunction("{ var x = 'test'; return x; }").setContentType("text/javascript");


			// a project
			final JsonObjectType project = sourceSchema.addType("Project");
			project.addStringProperty("name", "public", "ui").setRequired(true);

			final JsonReferenceType projectTasks = project.relate(task, "HAS", Cardinality.OneToMany, "project", "tasks");
			projectTasks.setCascadingCreate(Cascade.targetToSource);

			project.getViewPropertyNames("public").add("tasks");
			task.getViewPropertyNames("public").add("project");


			// test enums
			project.addEnumProperty("status", "ui").setEnums("active", "planned", "finished");


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
			worker.addReferenceProperty("taskErrors", tasksProperty, "public", "ui");


			task.addReferenceProperty("workerName",   workerProperty, "public", "ui").setProperties("name");
			task.addReferenceProperty("workerNotion", workerProperty, "public", "ui");




			// test date properties..
			project.addDateProperty("startDate", "public", "ui");

			// methods
			project.addMethod("onCreate", "set(this, 'name', 'wurst')", "comment for wurst");



			// test URIs
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/#", sourceSchema.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Task", task.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Task/properties/title", title.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Task/properties/description", desc.getId().toString());
			assertEquals("Invalid schema URI", "https://structr.org/schema/" + instanceId + "/definitions/Worker/properties/renamedTasks", tasksProperty.getId().toString());





			compareSchemaRoundtrip(sourceSchema);

		} catch (Exception ex) {

			logger.warn("", ex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test04ManualSchemaRelatedPropertyNameCreation() {

		try {

			try (final Tx tx = app.tx()) {

				final SchemaNode source = app.create(SchemaNode.class, "Source");
				final SchemaNode target = app.create(SchemaNode.class, "Target");

				app.create(SchemaRelationshipNode.class,
					new NodeAttribute(SchemaRelationshipNode.relationshipType, "link"),
					new NodeAttribute(SchemaRelationshipNode.sourceNode, source),
					new NodeAttribute(SchemaRelationshipNode.targetNode, target),
					new NodeAttribute(SchemaRelationshipNode.sourceMultiplicity, "1"),
					new NodeAttribute(SchemaRelationshipNode.targetMultiplicity, "*")
				);

				tx.success();
			}

			checkSchemaString(StructrSchema.createFromDatabase(app).toString());

		} catch (FrameworkException | URISyntaxException t) {
			t.printStackTrace();
		}
	}

	@Test
	public void test05SchemaRelatedPropertyNameCreationWithPresets() {

		try {

			// create test case
			final JsonSchema schema     = StructrSchema.newInstance(URI.create(app.getInstanceId()));
			final JsonObjectType source = schema.addType("Source");
			final JsonObjectType target = schema.addType("Target");

			source.relate(target, "link", Relation.Cardinality.OneToMany, "sourceLink", "linkTargets");

			checkSchemaString(schema.toString());


		} catch (FrameworkException | URISyntaxException t) {
			t.printStackTrace();
		}

	}

	@Test
	public void test06SchemaRelatedPropertyNameCreationWithoutPresets() {

		try {

			// create test case
			final JsonSchema schema     = StructrSchema.newInstance(URI.create(app.getInstanceId()));
			final JsonObjectType source = schema.addType("Source");
			final JsonObjectType target = schema.addType("Target");

			source.relate(target, "link", Relation.Cardinality.OneToMany);

			checkSchemaString(schema.toString());

		} catch (FrameworkException | URISyntaxException t) {
			t.printStackTrace();
		}

	}

	@Test
	public void test00DeleteSchemaRelationshipInView() {

		SchemaRelationshipNode rel = null;

		try (final Tx tx = app.tx()) {

			// create source and target node
			final SchemaNode fooNode = app.create(SchemaNode.class, "Foo");
			final SchemaNode barNode = app.create(SchemaNode.class, "Bar");

			// create relationship
			rel = app.create(SchemaRelationshipNode.class,
				new NodeAttribute<>(SchemaRelationshipNode.sourceNode, fooNode),
				new NodeAttribute<>(SchemaRelationshipNode.targetNode, barNode),
				new NodeAttribute<>(SchemaRelationshipNode.relationshipType, "narf")
			);

			// create "public" view that contains the related property
			app.create(SchemaView.class,
				new NodeAttribute<>(SchemaView.name, "public"),
				new NodeAttribute<>(SchemaView.schemaNode, fooNode),
				new NodeAttribute<>(SchemaView.nonGraphProperties, "type, id, narfBars")
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

	// ----- private methods -----
	private void checkSchemaString(final String source) {

		System.out.println("########################################## checking");
		System.out.println(source);

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

		final String source = sourceSchema.toString();

		System.out.println("##################### source");
		System.out.println(source);

		final JsonSchema targetSchema = StructrSchema.createFromSource(sourceSchema.toString());
		final String target = targetSchema.toString();

		System.out.println("##################### target");
		System.out.println(target);

		assertEquals("Invalid schema (de)serialization roundtrip result", source, target);

		StructrSchema.replaceDatabaseSchema(app, targetSchema);

		final JsonSchema replacedSchema = StructrSchema.createFromDatabase(app);
		final String replaced = replacedSchema.toString();

		System.out.println("##################### replaced");
		System.out.println(replaced);

		assertEquals("Invalid schema replacement result", source, replaced);
	}
}
