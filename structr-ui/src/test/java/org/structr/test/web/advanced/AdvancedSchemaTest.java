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
package org.structr.test.web.advanced;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.function.TypeInfoFunction;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractSchemaNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.core.traits.definitions.SchemaNodeTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.core.traits.definitions.SchemaViewTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.basic.FrontendTest;
import org.structr.test.web.basic.ResourceAccessTest;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.entity.File;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.structr.test.web.basic.ResourceAccessTest.createResourceAccess;
import static org.testng.AssertJUnit.*;

public class AdvancedSchemaTest extends FrontendTest {

	private static final Logger logger = LoggerFactory.getLogger(AdvancedSchemaTest.class.getName());

	private final int count1 = 4;
	private final int count2 = 4;

	@Test
	public void test01InheritanceOfFileAttributesToImage() {

		try (final Tx tx = app.tx()) {

			createAdminUser();
			ResourceAccessTest.createResourceAccess("_schema", UiAuthenticator.AUTH_USER_GET);
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			NodeInterface testFileProperty = app.create(StructrTraits.SCHEMA_PROPERTY);

			final PropertyMap testFileProperties = new PropertyMap();
			testFileProperties.put(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testFile");
			testFileProperties.put(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String");
			testFileProperties.put(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.STATIC_SCHEMA_NODE_NAME_PROPERTY), StructrTraits.FILE);
			testFileProperty.setProperties(testFileProperty.getSecurityContext(), testFileProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
					.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)

				.expect()
					.statusCode(200)

					.body("result", Matchers.hasSize(count1))
					.body("result", Matchers.hasItem(Matchers.allOf(hasEntry("jsonName", "testFile"), hasEntry("declaringClass", StructrTraits.FILE))))

				.when()
					.get("/_schema/File/custom");

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
					.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)

				.expect()
					.statusCode(200)

					.body("result", Matchers.hasSize(count2))
					.body("result", Matchers.hasItem(Matchers.allOf(hasEntry("jsonName", "testFile"), hasEntry("declaringClass", StructrTraits.FILE))))

				.when()
					.get("/_schema/Image/custom");

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}

	@Test
	public void test02InheritanceOfFileAttributesToSubclass() {

		try (final Tx tx = app.tx()) {

			createAdminUser();
			createResourceAccess("_schema", UiAuthenticator.AUTH_USER_GET);
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			NodeInterface fileNodeDef = app.nodeQuery(StructrTraits.SCHEMA_NODE).andName(StructrTraits.FILE).getFirst();

			NodeInterface testFileProperty = app.create(StructrTraits.SCHEMA_PROPERTY);

			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testFile");
			changedProperties.put(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String");
			changedProperties.put(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), fileNodeDef);
			testFileProperty.setProperties(testFileProperty.getSecurityContext(), changedProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			// Create new schema node for dynamic class SubFile which extends File
			NodeInterface subFile = app.create(StructrTraits.SCHEMA_NODE);

			final PropertyMap subFileProperties = new PropertyMap();
			subFileProperties.put(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "SubFile");
			subFileProperties.put(Traits.of(StructrTraits.SCHEMA_NODE).key(SchemaNodeTraitDefinition.INHERITED_TRAITS_PROPERTY), new String[] { StructrTraits.FILE });
			subFile.setProperties(subFile.getSecurityContext(), subFileProperties);


			// Add String property "testSubFile" to new dynamic class
			NodeInterface testFileProperty = app.create(StructrTraits.SCHEMA_PROPERTY);

			final PropertyMap testFileProperties = new PropertyMap();
			testFileProperties.put(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testSubFile");
			testFileProperties.put(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String");
			testFileProperties.put(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), subFile);
			testFileProperty.setProperties(testFileProperty.getSecurityContext(), testFileProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
					.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)

				.expect()
					.statusCode(200)

					.body("result",	hasSize(count1 + 1))
					.body("result", Matchers.hasItem(Matchers.allOf(hasEntry("jsonName", "testFile"),    hasEntry("declaringClass", StructrTraits.FILE))))
					.body("result", Matchers.hasItem(Matchers.allOf(hasEntry("jsonName", "testSubFile"), hasEntry("declaringClass", "SubFile"))))

				.when()
					.get("/_schema/SubFile/custom");

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

	}

	@Test
	public void test03InheritanceOfFileAttributesToSubclassOfImage() {

		try (final Tx tx = app.tx()) {

			createAdminUser();
			createResourceAccess("_schema", UiAuthenticator.AUTH_USER_GET);
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			NodeInterface fileNodeDef = app.nodeQuery(StructrTraits.SCHEMA_NODE).andName(StructrTraits.FILE).getFirst();

			NodeInterface testFileProperty = app.create(StructrTraits.SCHEMA_PROPERTY);

			final PropertyMap testFileProperties = new PropertyMap();
			testFileProperties.put(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testFile");
			testFileProperties.put(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String");
			testFileProperties.put(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), fileNodeDef);
			testFileProperty.setProperties(testFileProperty.getSecurityContext(), testFileProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			// Create new schema node for dynamic class SubFile which extends File
			NodeInterface subFile = app.create(StructrTraits.SCHEMA_NODE);

			final PropertyMap subFileProperties = new PropertyMap();
			subFileProperties.put(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "SubFile");
			subFileProperties.put(Traits.of(StructrTraits.SCHEMA_NODE).key(SchemaNodeTraitDefinition.INHERITED_TRAITS_PROPERTY), new String[] { StructrTraits.IMAGE });
			subFile.setProperties(subFile.getSecurityContext(), subFileProperties);


			// Add String property "testSubFile" to new dynamic class
			NodeInterface testFileProperty = app.create(StructrTraits.SCHEMA_PROPERTY);

			final PropertyMap testFileProperties = new PropertyMap();
			testFileProperties.put(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testSubFile");
			testFileProperties.put(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String");
			testFileProperties.put(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), subFile);
			testFileProperty.setProperties(testFileProperty.getSecurityContext(), testFileProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}


		try (final Tx tx = app.tx()) {

			RestAssured

				.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
					.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)

				.expect()
					.statusCode(200)

					.body("result",	hasSize(count2 + 1))
					.body("result", Matchers.hasItem(Matchers.allOf(hasEntry("jsonName", "testFile"),    hasEntry("declaringClass", StructrTraits.FILE))))
					.body("result", Matchers.hasItem(Matchers.allOf(hasEntry("jsonName", "testSubFile"), hasEntry("declaringClass", "SubFile"))))

				.when()
					.get("/_schema/SubFile/custom");

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}

	@Test
	public void test04SchemaPropertyOrderInBuiltInViews() {

		try (final Tx tx = app.tx()) {

			createAdminUser();
			createResourceAccess("_schema", UiAuthenticator.AUTH_USER_GET);
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		NodeInterface test = null;
		String id          = null;

		try (final Tx tx = app.tx()) {

			// create test type
			test = app.create(StructrTraits.SCHEMA_NODE, "Test");

			app.create(StructrTraits.SCHEMA_VIEW,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "myView"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY), test)
			);

			app.create(StructrTraits.SCHEMA_VIEW,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testView"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY), test)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		// This test seems to assume that every dynamic entity automatically gets a "public" view..

		try (final Tx tx = app.tx()) {

			// create view with sort order
			final List<NodeInterface> list = Iterables.toList((Iterable<NodeInterface>)test.getProperty(Traits.of(StructrTraits.SCHEMA_NODE).key(AbstractSchemaNodeTraitDefinition.SCHEMA_VIEWS_PROPERTY)));

			// create properties
			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), test),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_VIEWS_PROPERTY), list),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "one")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), test),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_VIEWS_PROPERTY), list),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "two")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), test),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_VIEWS_PROPERTY), list),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "three")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), test),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_VIEWS_PROPERTY), list),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "four")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final String type            = "Test";
		final List<PropertyKey> list = new LinkedList<>(Traits.of(type).getPropertyKeysForView("public"));

		assertEquals("Invalid number of properties in sorted view", 7, list.size());
		assertEquals("id",    list.get(0).dbName());
		assertEquals("type",  list.get(1).dbName());
		assertEquals("name",  list.get(2).dbName());
		assertEquals("one",   list.get(3).dbName());
		assertEquals("two",   list.get(4).dbName());
		assertEquals("three", list.get(5).dbName());
		assertEquals("four",  list.get(6).dbName());

		try (final Tx tx = app.tx()) {

			for (final NodeInterface testView : (Iterable<NodeInterface>)test.getProperty(Traits.of(StructrTraits.SCHEMA_NODE).key(AbstractSchemaNodeTraitDefinition.SCHEMA_VIEWS_PROPERTY))) {

				// modify sort order
				testView.setProperty(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SORT_ORDER_PROPERTY), "type, one, id, two, three, four, name");
			}

			// create test entity
			final NodeInterface node = app.create("Test");

			// save UUID for later
			id = node.getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<PropertyKey> list2 = new LinkedList<>(Traits.of(type).getPropertyKeysForView("public"));

		assertEquals("Invalid number of properties in sorted view", 7, list2.size());
		assertEquals("id",    list2.get(0).dbName());
		assertEquals("type",  list2.get(1).dbName());
		assertEquals("name",  list2.get(2).dbName());
		assertEquals("one",   list2.get(3).dbName());
		assertEquals("two",   list2.get(4).dbName());
		assertEquals("three", list2.get(5).dbName());
		assertEquals("four",  list2.get(6).dbName());

		// test schema resource
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)

			.expect()
				.statusCode(200)

				.body("result",	                  hasSize(7))
				.body("result[0].jsonName",       equalTo("id"))
				.body("result[1].jsonName",       equalTo("type"))
				.body("result[2].jsonName",       equalTo("name"))
				.body("result[3].jsonName",       equalTo("one"))
				.body("result[4].jsonName",       equalTo("two"))
				.body("result[5].jsonName",       equalTo("three"))
				.body("result[6].jsonName",       equalTo("four"))

			.when()
				.get("/_schema/Test/public");

		// test actual REST resource (not easy to extract and verify
		// JSON property order, that's why we're using replaceAll and
		// string comparison..
		final String[] actual = RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)

			.expect()
				.statusCode(200)

			.when()
				.get("/Test")
			.body()
			.asString()
			.replaceAll("[\\s]+", "")
			.split("[\\W]+");

		// we can only test the actual ORDER of the JSON result object by splitting it on whitespace and validating the resulting array
		assertEquals("Invalid JSON result for sorted property view", "result",             actual[1]);
		assertEquals("Invalid JSON result for sorted property view", "id",                 actual[2]);
		assertEquals("Invalid JSON result for sorted property view", id,                   actual[3]);
		assertEquals("Invalid JSON result for sorted property view", "type",               actual[4]);
		assertEquals("Invalid JSON result for sorted property view", "Test",               actual[5]);
		assertEquals("Invalid JSON result for sorted property view", "name",               actual[6]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[7]);
		assertEquals("Invalid JSON result for sorted property view", "one",                actual[8]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[9]);
		assertEquals("Invalid JSON result for sorted property view", "two",                actual[10]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[11]);
		assertEquals("Invalid JSON result for sorted property view", "three",              actual[12]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[13]);
		assertEquals("Invalid JSON result for sorted property view", "four",               actual[14]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[15]);
		assertEquals("Invalid JSON result for sorted property view", "query_time",         actual[16]);
		assertEquals("Invalid JSON result for sorted property view", "0",                  actual[17]);
		assertEquals("Invalid JSON result for sorted property view", "result_count",       actual[19]);
		assertEquals("Invalid JSON result for sorted property view", "1",                  actual[20]);
		assertEquals("Invalid JSON result for sorted property view", "page_count",         actual[21]);
		assertEquals("Invalid JSON result for sorted property view", "1",                  actual[22]);
		assertEquals("Invalid JSON result for sorted property view", "result_count_time",  actual[23]);
		assertEquals("Invalid JSON result for sorted property view", "0",                  actual[24]);
		assertEquals("Invalid JSON result for sorted property view", "serialization_time", actual[26]);

		// try built-in function
		try {

			final List<GraphObjectMap> list3 = (List)new TypeInfoFunction().apply(new ActionContext(securityContext), null, new Object[] { "Test", "public" });

			assertEquals("Invalid number of properties in sorted view", 7, list3.size());
			assertEquals("id",    list3.get(0).toMap().get("jsonName"));
			assertEquals("type",  list3.get(1).toMap().get("jsonName"));
			assertEquals("name",  list3.get(2).toMap().get("jsonName"));
			assertEquals("one",   list3.get(3).toMap().get("jsonName"));
			assertEquals("two",   list3.get(4).toMap().get("jsonName"));
			assertEquals("three", list3.get(5).toMap().get("jsonName"));
			assertEquals("four",  list3.get(6).toMap().get("jsonName"));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		// try scripting call
		try {

			final List<GraphObjectMap> list4 = (List)Scripting.evaluate(new ActionContext(securityContext), null, "${type_info('Test', 'public')}", "test");

			assertEquals("Invalid number of properties in sorted view", 7, list4.size());
			assertEquals("id",    list4.get(0).toMap().get("jsonName"));
			assertEquals("type",  list4.get(1).toMap().get("jsonName"));
			assertEquals("name",  list4.get(2).toMap().get("jsonName"));
			assertEquals("one",   list4.get(3).toMap().get("jsonName"));
			assertEquals("two",   list4.get(4).toMap().get("jsonName"));
			assertEquals("three", list4.get(5).toMap().get("jsonName"));
			assertEquals("four",  list4.get(6).toMap().get("jsonName"));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	@Test
	public void test04SchemaPropertyOrderInCustomViews() {

		try (final Tx tx = app.tx()) {

			createAdminUser();
			createResourceAccess("_schema", UiAuthenticator.AUTH_USER_GET);
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		final GenericProperty jsonName = new GenericProperty("jsonName");
		NodeInterface testView1        = null;
		NodeInterface testView2        = null;
		String id                      = null;

		try (final Tx tx = app.tx()) {

			// create test type
			final NodeInterface test = app.create(StructrTraits.SCHEMA_NODE, "Test");

			// create view with sort order
			testView1 = app.create(StructrTraits.SCHEMA_VIEW,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "test"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY), test),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SORT_ORDER_PROPERTY), "one, two, three, four, id, type, name"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY), "id, type, name")
			);

			// create view with sort order
			testView2 = app.create(StructrTraits.SCHEMA_VIEW,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "other"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY), test),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SORT_ORDER_PROPERTY), "four, id, type, name, one, three, two"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY), "id, type, name")
			);

			final List<NodeInterface> list = new LinkedList<>();
			list.add(testView1);
			list.add(testView2);

			// create properties
			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), test),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_VIEWS_PROPERTY), list),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "one")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), test),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_VIEWS_PROPERTY), list),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "two")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), test),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_VIEWS_PROPERTY), list),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "three")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), test),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_VIEWS_PROPERTY), list),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "four")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final String type            = "Test";
		final List<PropertyKey> list = new LinkedList<>(Traits.of(type).getPropertyKeysForView("test"));

		assertEquals("Invalid number of properties in sorted view", 7, list.size());
		assertEquals("Invalid view order", "one",   list.get(0).dbName());
		assertEquals("Invalid view order", "two",   list.get(1).dbName());
		assertEquals("Invalid view order", "three", list.get(2).dbName());
		assertEquals("Invalid view order", "four",  list.get(3).dbName());
		assertEquals("Invalid view order", "id",    list.get(4).dbName());
		assertEquals("Invalid view order", "type",  list.get(5).dbName());
		assertEquals("Invalid view order", "name",  list.get(6).dbName());

		try (final Tx tx = app.tx()) {

			// modify sort order
			testView1.setProperty(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SORT_ORDER_PROPERTY), "type, one, id, two, three, four, name");

			// create test entity
			final NodeInterface node = app.create("Test");
			id = node.getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<PropertyKey> list2 = new LinkedList<>(Traits.of(type).getPropertyKeysForView("test"));

		assertEquals("Invalid number of properties in sorted view", 7, list2.size());
		assertEquals("Invalid view order", "type",  list2.get(0).dbName());
		assertEquals("Invalid view order", "one",   list2.get(1).dbName());
		assertEquals("Invalid view order", "id",    list2.get(2).dbName());
		assertEquals("Invalid view order", "two",   list2.get(3).dbName());
		assertEquals("Invalid view order", "three", list2.get(4).dbName());
		assertEquals("Invalid view order", "four",  list2.get(5).dbName());
		assertEquals("Invalid view order", "name",  list2.get(6).dbName());

		// test schema resource
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)

			.expect()
				.statusCode(200)

				.body("result",	                  hasSize(7))
				.body("result[0].jsonName",       equalTo("type"))
				.body("result[1].jsonName",       equalTo("one"))
				.body("result[2].jsonName",       equalTo("id"))
				.body("result[3].jsonName",       equalTo("two"))
				.body("result[4].jsonName",       equalTo("three"))
				.body("result[5].jsonName",       equalTo("four"))
				.body("result[6].jsonName",       equalTo("name"))

			.when()
				.get("/_schema/Test/test");

		// test actual REST resource (not easy to extract and verify
		// JSON property order, that's why we're using replaceAll and
		// string comparison..
		final String[] actual = RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)

			.expect()
				.statusCode(200)

			.when()
				.get("/Test/test")
			.body()
			.asString()
			.replaceAll("[\\s]+", "")
			.split("[\\W]+");

		System.out.println(Arrays.asList(actual));

		// we can only test the actual ORDER of the JSON result object by splitting it on whitespace and validating the resulting array
		assertEquals("Invalid JSON result for sorted property view", "result",             actual[1]);
		assertEquals("Invalid JSON result for sorted property view", "type",               actual[2]);
		assertEquals("Invalid JSON result for sorted property view", "Test",               actual[3]);
		assertEquals("Invalid JSON result for sorted property view", "one",                actual[4]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[5]);
		assertEquals("Invalid JSON result for sorted property view", "id",                 actual[6]);
		assertEquals("Invalid JSON result for sorted property view", id,                   actual[7]);
		assertEquals("Invalid JSON result for sorted property view", "two",                actual[8]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[9]);
		assertEquals("Invalid JSON result for sorted property view", "three",              actual[10]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[11]);
		assertEquals("Invalid JSON result for sorted property view", "four",               actual[12]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[13]);
		assertEquals("Invalid JSON result for sorted property view", "name",               actual[14]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[15]);
		assertEquals("Invalid JSON result for sorted property view", "query_time",         actual[16]);
		assertEquals("Invalid JSON result for sorted property view", "0",                  actual[17]);
		assertEquals("Invalid JSON result for sorted property view", "result_count",       actual[19]);
		assertEquals("Invalid JSON result for sorted property view", "1",                  actual[20]);
		assertEquals("Invalid JSON result for sorted property view", "page_count",         actual[21]);
		assertEquals("Invalid JSON result for sorted property view", "1",                  actual[22]);
		assertEquals("Invalid JSON result for sorted property view", "result_count_time",  actual[23]);
		assertEquals("Invalid JSON result for sorted property view", "0",                  actual[24]);
		assertEquals("Invalid JSON result for sorted property view", "serialization_time", actual[26]);


		// try built-in function
		try {

			final List<GraphObjectMap> list3 = (List)new TypeInfoFunction().apply(new ActionContext(securityContext), null, new Object[] { "Test", "test" });

			assertEquals("Invalid number of properties in sorted view", 7, list2.size());
			assertEquals("Invalid view order", "type",  list3.get(0).get(jsonName));
			assertEquals("Invalid view order", "one",   list3.get(1).get(jsonName));
			assertEquals("Invalid view order", "id",    list3.get(2).get(jsonName));
			assertEquals("Invalid view order", "two",   list3.get(3).get(jsonName));
			assertEquals("Invalid view order", "three", list3.get(4).get(jsonName));
			assertEquals("Invalid view order", "four",  list3.get(5).get(jsonName));
			assertEquals("Invalid view order", "name",  list3.get(6).get(jsonName));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		// try scripting call
		try {

			final List<GraphObjectMap> list4 = (List)Scripting.evaluate(new ActionContext(securityContext), null, "${type_info('Test', 'test')}", "test");

			assertEquals("Invalid number of properties in sorted view", 7, list2.size());
			assertEquals("Invalid view order", "type",  list4.get(0).get(jsonName));
			assertEquals("Invalid view order", "one",   list4.get(1).get(jsonName));
			assertEquals("Invalid view order", "id",    list4.get(2).get(jsonName));
			assertEquals("Invalid view order", "two",   list4.get(3).get(jsonName));
			assertEquals("Invalid view order", "three", list4.get(4).get(jsonName));
			assertEquals("Invalid view order", "four",  list4.get(5).get(jsonName));
			assertEquals("Invalid view order", "name",  list4.get(6).get(jsonName));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	@Test
	public void testSchemaPropertyOrderInInheritedViews() {

		try (final Tx tx = app.tx()) {

			createAdminUser();
			createResourceAccess("_schema", UiAuthenticator.AUTH_USER_GET);
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		final GenericProperty jsonName = new GenericProperty("jsonName");
		NodeInterface testView         = null;
		String id                      = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface testBase = app.create(StructrTraits.SCHEMA_NODE, "TestBase");
			final NodeInterface test     = app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Test"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(SchemaNodeTraitDefinition.INHERITED_TRAITS_PROPERTY), new String[] { "TestBase" })
			);

			// create view with sort order
			testView = app.create(StructrTraits.SCHEMA_VIEW,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "test"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY), test),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SORT_ORDER_PROPERTY), "one, two, three, four, id, type, name"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY), "id, type, name")
			);

			final List<NodeInterface> list = new LinkedList<>();
			list.add(testView);

			// create properties
			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), testBase),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_VIEWS_PROPERTY), list),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "one")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), testBase),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_VIEWS_PROPERTY), list),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "two")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), testBase),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_VIEWS_PROPERTY), list),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "three")
			);

			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), testBase),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_VIEWS_PROPERTY), list),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "four")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}


		final String type            = "Test";
		final List<PropertyKey> list = new LinkedList<>(Traits.of(type).getPropertyKeysForView("test"));

		assertEquals("Invalid number of properties in sorted view", 7, list.size());
		assertEquals("one",   list.get(0).dbName());
		assertEquals("two",   list.get(1).dbName());
		assertEquals("three", list.get(2).dbName());
		assertEquals("four",  list.get(3).dbName());
		assertEquals("id",    list.get(4).dbName());
		assertEquals("type",  list.get(5).dbName());
		assertEquals("name",  list.get(6).dbName());

		try (final Tx tx = app.tx()) {

			// modify sort order
			testView.setProperty(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SORT_ORDER_PROPERTY), "type, one, id, two, three, four, name");

			// create test entity
			final NodeInterface node = app.create("Test");
			id = node.getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<PropertyKey> list2 = new LinkedList<>(Traits.of(type).getPropertyKeysForView("test"));

		assertEquals("Invalid number of properties in sorted view", 7, list2.size());
		assertEquals("type",  list2.get(0).dbName());
		assertEquals("one",   list2.get(1).dbName());
		assertEquals("id",    list2.get(2).dbName());
		assertEquals("two",   list2.get(3).dbName());
		assertEquals("three", list2.get(4).dbName());
		assertEquals("four",  list2.get(5).dbName());
		assertEquals("name",  list2.get(6).dbName());

		// test schema resource
		RestAssured.basePath = "/structr/rest";
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)

			.expect()
				.statusCode(200)

				.body("result",	                  hasSize(7))
				.body("result[0].jsonName",       equalTo("type"))
				.body("result[1].jsonName",       equalTo("one"))
				.body("result[2].jsonName",       equalTo("id"))
				.body("result[3].jsonName",       equalTo("two"))
				.body("result[4].jsonName",       equalTo("three"))
				.body("result[5].jsonName",       equalTo("four"))
				.body("result[6].jsonName",       equalTo("name"))

			.when()
				.get("/_schema/Test/test");

		// test actual REST resource (not easy to extract and verify
		// JSON property order, that's why we're using replaceAll and
		// string comparison..
		final String[] actual = RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)

			.expect()
				.statusCode(200)

			.when()
				.get("/Test/test")
			.body()
			.asString()
			.replaceAll("[\\s]+", "")
			.split("[\\W]+");

		// we can only test the actual ORDER of the JSON result object by splitting it on whitespace and validating the resulting array
		assertEquals("Invalid JSON result for sorted property view", "result",             actual[1]);
		assertEquals("Invalid JSON result for sorted property view", "type",               actual[2]);
		assertEquals("Invalid JSON result for sorted property view", "Test",               actual[3]);
		assertEquals("Invalid JSON result for sorted property view", "one",                actual[4]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[5]);
		assertEquals("Invalid JSON result for sorted property view", "id",                 actual[6]);
		assertEquals("Invalid JSON result for sorted property view", id,                   actual[7]);
		assertEquals("Invalid JSON result for sorted property view", "two",                actual[8]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[9]);
		assertEquals("Invalid JSON result for sorted property view", "three",              actual[10]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[11]);
		assertEquals("Invalid JSON result for sorted property view", "four",               actual[12]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[13]);
		assertEquals("Invalid JSON result for sorted property view", "name",               actual[14]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[15]);
		assertEquals("Invalid JSON result for sorted property view", "query_time",         actual[16]);
		assertEquals("Invalid JSON result for sorted property view", "0",                  actual[17]);
		assertEquals("Invalid JSON result for sorted property view", "result_count",       actual[19]);
		assertEquals("Invalid JSON result for sorted property view", "1",                  actual[20]);
		assertEquals("Invalid JSON result for sorted property view", "page_count",         actual[21]);
		assertEquals("Invalid JSON result for sorted property view", "1",                  actual[22]);
		assertEquals("Invalid JSON result for sorted property view", "result_count_time",  actual[23]);
		assertEquals("Invalid JSON result for sorted property view", "0",                  actual[24]);
		assertEquals("Invalid JSON result for sorted property view", "serialization_time", actual[26]);


		// try built-in function
		try {

			final List<GraphObjectMap> list3 = (List)new TypeInfoFunction().apply(new ActionContext(securityContext), null, new Object[] { "Test", "test" });

			assertEquals("Invalid number of properties in sorted view", 7, list2.size());
			assertEquals("type",  list3.get(0).get(jsonName));
			assertEquals("one",   list3.get(1).get(jsonName));
			assertEquals("id",    list3.get(2).get(jsonName));
			assertEquals("two",   list3.get(3).get(jsonName));
			assertEquals("three", list3.get(4).get(jsonName));
			assertEquals("four",  list3.get(5).get(jsonName));
			assertEquals("name",  list3.get(6).get(jsonName));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		// try scripting call
		try {

			final List<GraphObjectMap> list4 = (List)Scripting.evaluate(new ActionContext(securityContext), null, "${type_info('Test', 'test')}", "test");

			assertEquals("Invalid number of properties in sorted view", 7, list2.size());
			assertEquals("type",  list4.get(0).get(jsonName));
			assertEquals("one",   list4.get(1).get(jsonName));
			assertEquals("id",    list4.get(2).get(jsonName));
			assertEquals("two",   list4.get(3).get(jsonName));
			assertEquals("three", list4.get(4).get(jsonName));
			assertEquals("four",  list4.get(5).get(jsonName));
			assertEquals("name",  list4.get(6).get(jsonName));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	/**
	 * This test makes sure that the type of an overloaded property is kept and identical in all views.
	 */
	@Test
	public void test05PropertyTypeOfOverloadedProperty() {

		try (final Tx tx = app.tx()) {

			createAdminUser();
			createResourceAccess("_schema", UiAuthenticator.AUTH_USER_GET);
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		NodeInterface testView            = null;

		try (final Tx tx = app.tx()) {

			// create test type
			final NodeInterface test = app.create(StructrTraits.SCHEMA_NODE, "Test");

			// create view with sort order
			testView = app.create(StructrTraits.SCHEMA_VIEW,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testview"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SORT_ORDER_PROPERTY), "name"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY), test)
			);

			final List<NodeInterface> list = new LinkedList<>();
			list.add(testView);

			// create a function property to overload the String property "name" defined in {@link NodeInterface}
			app.create(StructrTraits.SCHEMA_PROPERTY,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY), test),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.SCHEMA_VIEWS_PROPERTY), list),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), "Function"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "name")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final String type            = "Test";
		final List<PropertyKey> list = new LinkedList<>(Traits.of(type).getPropertyKeysForView("testview"));

		assertEquals("Invalid number of properties in sorted view", 1, list.size());
		assertEquals("name",  list.get(0).dbName());

		// test custom view 'testview'
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)

			.expect()
				.statusCode(200)

				.body("result",	                  hasSize(1))
				.body("result[0].jsonName",       equalTo("name"))
				.body("result[0].className",      equalTo("org.structr.core.property.FunctionProperty"))

			.when()
				.get("/_schema/Test/testview");

		// test 'public' view
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)

			.expect()
				.statusCode(200)

				.body("result",	                  hasSize(3))
				.body("result[0].jsonName",       equalTo("id"))
				.body("result[1].jsonName",       equalTo("type"))
				.body("result[2].jsonName",       equalTo("name"))
				.body("result[2].className",      equalTo("org.structr.core.property.FunctionProperty"))

			.when()
				.get("/_schema/Test/public");

	}


	//@Test
	// Disabled because we don't need to allow overriding of built-in methods
	// in custom classes.
	public void testIsValidPasswordMethodOfUser() {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.getType(StructrTraits.USER).overrideMethod("isValidPassword", false, "return true;")
				.addParameter("password", "String")
				.setReturnType("boolean");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		RestAssured

			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, "wrong")

			.expect()
				.statusCode(200)

			.when()
				.get("/User");
	}

	@Test
	public void testMixedOnCreateMethods() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.getType(StructrTraits.USER).addMethod("onCreate", "log('test')");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testCustomSchemaMethod() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.getType(StructrTraits.USER).addMethod("simpleTest", "log('test')");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testViewProperties() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addStringProperty("test", PropertyView.Public);
			type.addViewProperty(PropertyView.Public, "test");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testOnDownloadMethod() {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.getType(StructrTraits.FILE);

			type.addMethod("onDownload", "{ $.log('DOWNLOAD!'); $.this.onDownloadCalled = true; }");
			type.addBooleanProperty("onDownloadCalled");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			final File newFile = app.create(StructrTraits.FILE, "test.txt").as(File.class);

			try (final PrintWriter writer = new PrintWriter(newFile.getOutputStream())) {

				writer.print("test!");
			}

			tx.success();

		} catch (IOException | FrameworkException fex) {
			fex.printStackTrace();
		}

		RestAssured.basePath = "/";
		RestAssured
			.given()
				.header(X_USER_HEADER,     ADMIN_USERNAME)
				.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
				.body(equalTo("test!"))
			.when()
				.get("/test.txt");


		// we need to wait for some time before the lifecycle method is called
		try { Thread.sleep(2000); } catch (Throwable t) {}

		String fileTypeId = null;

		// check downloaded flag
		try (final Tx tx = app.tx()) {

			final PropertyKey<Boolean> key = Traits.of(StructrTraits.FILE).key("onDownloadCalled");
			final File file                = app.nodeQuery(StructrTraits.FILE).getFirst().as(File.class);

			// store UUID of SchemaNode with name StructrTraits.FILE for later use
			final NodeInterface fileTypeNode  = app.nodeQuery(StructrTraits.SCHEMA_NODE).andName(StructrTraits.FILE).getFirst();
			fileTypeId = fileTypeNode.getUuid();

			assertTrue("Lifecycle method onDownload was not called!", file.getProperty(key));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	@Test
	public void testServiceClassBehavior() {

		try (final Tx tx = app.tx()) {

			createAdminUser();
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		final String serviceClassName  = "MyService";
		final String serviceMethodName = "doStuff";

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create(StructrTraits.SCHEMA_NODE,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), serviceClassName),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key(AbstractSchemaNodeTraitDefinition.IS_SERVICE_CLASS_PROPERTY), true)
			);

			// we do not even set "isStatic = true" because the backend should do this automatically for service classes
			final NodeInterface method = app.create(StructrTraits.SCHEMA_METHOD,
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), serviceMethodName),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "{ return 'did stuff'; }"),
					new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), node)
			);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			RestAssured

					.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
					.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)

					.expect()
					.statusCode(422)

					.when()
					.post("/" + serviceClassName);

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured

					.given()
					.contentType("application/json; charset=UTF-8")
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
					.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
					.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)

					.expect()
					.statusCode(200)

					.body("result",	equalTo("did stuff"))

					.when()
					.post("/" + serviceClassName + "/" + serviceMethodName);

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}
}
