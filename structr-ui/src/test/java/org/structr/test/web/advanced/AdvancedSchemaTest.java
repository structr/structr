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
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaView;
import org.structr.core.function.TypeInfoFunction;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.basic.FrontendTest;
import org.structr.test.web.basic.ResourceAccessTest;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.entity.File;
import org.structr.web.entity.User;
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

			// Add String property "testFile" to built-in File class
			SchemaNode fileNodeDef = app.nodeQuery(SchemaNode.class).andName("File").getFirst();

			SchemaProperty testFileProperty = app.create(SchemaProperty.class);

			final PropertyMap testFileProperties = new PropertyMap();
			testFileProperties.put(SchemaProperty.name, "testFile");
			testFileProperties.put(SchemaProperty.propertyType, "String");
			testFileProperties.put(SchemaProperty.schemaNode, fileNodeDef);
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
					.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

				.expect()
					.statusCode(200)

					.body("result", Matchers.hasSize(count1))
					.body("result", Matchers.hasItem(Matchers.allOf(hasEntry("jsonName", "testFile"), hasEntry("declaringClass", "File"))))

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
					.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

				.expect()
					.statusCode(200)

					.body("result", Matchers.hasSize(count2))
					.body("result", Matchers.hasItem(Matchers.allOf(hasEntry("jsonName", "testFile"), hasEntry("declaringClass", "File"))))

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

			SchemaNode fileNodeDef = app.nodeQuery(SchemaNode.class).andName("File").getFirst();

			SchemaProperty testFileProperty = app.create(SchemaProperty.class);

			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(SchemaProperty.name, "testFile");
			changedProperties.put(SchemaProperty.propertyType, "String");
			changedProperties.put(SchemaProperty.schemaNode, fileNodeDef);
			testFileProperty.setProperties(testFileProperty.getSecurityContext(), changedProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			// Create new schema node for dynamic class SubFile which extends File
			SchemaNode subFile = app.create(SchemaNode.class);

			final PropertyMap subFileProperties = new PropertyMap();
			subFileProperties.put(SchemaNode.name, "SubFile");
			subFileProperties.put(SchemaNode.extendsClass, app.nodeQuery(SchemaNode.class).andName("File").getFirst());
			subFile.setProperties(subFile.getSecurityContext(), subFileProperties);


			// Add String property "testSubFile" to new dynamic class
			SchemaProperty testFileProperty = app.create(SchemaProperty.class);

			final PropertyMap testFileProperties = new PropertyMap();
			testFileProperties.put(SchemaProperty.name, "testSubFile");
			testFileProperties.put(SchemaProperty.propertyType, "String");
			testFileProperties.put(SchemaProperty.schemaNode, subFile);
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
					.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

				.expect()
					.statusCode(200)

					.body("result",	hasSize(count1 + 1))
					.body("result", Matchers.hasItem(Matchers.allOf(hasEntry("jsonName", "testFile"),    hasEntry("declaringClass", "File"))))
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

			SchemaNode fileNodeDef = app.nodeQuery(SchemaNode.class).andName("File").getFirst();

			SchemaProperty testFileProperty = app.create(SchemaProperty.class);

			final PropertyMap testFileProperties = new PropertyMap();
			testFileProperties.put(SchemaProperty.name, "testFile");
			testFileProperties.put(SchemaProperty.propertyType, "String");
			testFileProperties.put(SchemaProperty.schemaNode, fileNodeDef);
			testFileProperty.setProperties(testFileProperty.getSecurityContext(), testFileProperties);

			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			// Create new schema node for dynamic class SubFile which extends File
			SchemaNode subFile = app.create(SchemaNode.class);

			final PropertyMap subFileProperties = new PropertyMap();
			subFileProperties.put(SchemaNode.name, "SubFile");
			subFileProperties.put(SchemaNode.extendsClass, app.nodeQuery(SchemaNode.class).andName("Image").getFirst());
			subFile.setProperties(subFile.getSecurityContext(), subFileProperties);


			// Add String property "testSubFile" to new dynamic class
			SchemaProperty testFileProperty = app.create(SchemaProperty.class);

			final PropertyMap testFileProperties = new PropertyMap();
			testFileProperties.put(SchemaProperty.name, "testSubFile");
			testFileProperties.put(SchemaProperty.propertyType, "String");
			testFileProperties.put(SchemaProperty.schemaNode, subFile);
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
					.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

				.expect()
					.statusCode(200)

					.body("result",	hasSize(count2 + 1))
					.body("result", Matchers.hasItem(Matchers.allOf(hasEntry("jsonName", "testFile"),    hasEntry("declaringClass", "File"))))
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

		final GenericProperty jsonName = new GenericProperty("jsonName");
		SchemaNode test                = null;
		String id                      = null;

		try (final Tx tx = app.tx()) {

			// create test type
			test = app.create(SchemaNode.class, "Test");

			app.create(SchemaView.class,
				new NodeAttribute<>(SchemaView.name, "myView"),
				new NodeAttribute<>(SchemaView.schemaNode, test)
			);

			app.create(SchemaView.class,
				new NodeAttribute<>(SchemaView.name, "testView"),
				new NodeAttribute<>(SchemaView.schemaNode, test)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}


		try (final Tx tx = app.tx()) {

			// create view with sort order
			final List<SchemaView> list = Iterables.toList(test.getProperty(SchemaNode.schemaViews));

			// create properties
			app.create(SchemaProperty.class,
				new NodeAttribute<>(SchemaProperty.schemaNode, test),
				new NodeAttribute<>(SchemaProperty.schemaViews, list),
				new NodeAttribute<>(SchemaProperty.propertyType, "String"),
				new NodeAttribute<>(SchemaProperty.name, "one")
			);

			app.create(SchemaProperty.class,
				new NodeAttribute<>(SchemaProperty.schemaNode, test),
				new NodeAttribute<>(SchemaProperty.schemaViews, list),
				new NodeAttribute<>(SchemaProperty.propertyType, "String"),
				new NodeAttribute<>(SchemaProperty.name, "two")
			);

			app.create(SchemaProperty.class,
				new NodeAttribute<>(SchemaProperty.schemaNode, test),
				new NodeAttribute<>(SchemaProperty.schemaViews, list),
				new NodeAttribute<>(SchemaProperty.propertyType, "String"),
				new NodeAttribute<>(SchemaProperty.name, "three")
			);

			app.create(SchemaProperty.class,
				new NodeAttribute<>(SchemaProperty.schemaNode, test),
				new NodeAttribute<>(SchemaProperty.schemaViews, list),
				new NodeAttribute<>(SchemaProperty.propertyType, "String"),
				new NodeAttribute<>(SchemaProperty.name, "four")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final Class type            = StructrApp.getConfiguration().getNodeEntityClass("Test");
		final List<PropertyKey> list = new LinkedList<>(StructrApp.getConfiguration().getPropertySet(type, "public"));

		assertEquals("Invalid number of properties in sorted view", 7, list.size());
		assertEquals("id",    list.get(0).dbName());
		assertEquals("type",  list.get(1).dbName());
		assertEquals("name",  list.get(2).dbName());
		assertEquals("one",   list.get(3).dbName());
		assertEquals("two",   list.get(4).dbName());
		assertEquals("three", list.get(5).dbName());
		assertEquals("four",  list.get(6).dbName());

		try (final Tx tx = app.tx()) {

			for (final SchemaView testView : test.getProperty(SchemaNode.schemaViews)) {

				// modify sort order
				testView.setProperty(SchemaView.sortOrder, "type, one, id, two, three, four, name");
			}

			// create test entity
			final NodeInterface node = app.create(StructrApp.getConfiguration().getNodeEntityClass("Test"));

			// save UUID for later
			id = node.getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<PropertyKey> list2 = new LinkedList<>(StructrApp.getConfiguration().getPropertySet(type, "public"));

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
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

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
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

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
			assertEquals("id",    list3.get(0).get(jsonName));
			assertEquals("type",  list3.get(1).get(jsonName));
			assertEquals("name",  list3.get(2).get(jsonName));
			assertEquals("one",   list3.get(3).get(jsonName));
			assertEquals("two",   list3.get(4).get(jsonName));
			assertEquals("three", list3.get(5).get(jsonName));
			assertEquals("four",  list3.get(6).get(jsonName));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		// try scripting call
		try {

			final List<GraphObjectMap> list4 = (List)Scripting.evaluate(new ActionContext(securityContext), null, "${type_info('Test', 'public')}", "test");

			assertEquals("Invalid number of properties in sorted view", 7, list4.size());
			assertEquals("id",    list4.get(0).get(jsonName));
			assertEquals("type",  list4.get(1).get(jsonName));
			assertEquals("name",  list4.get(2).get(jsonName));
			assertEquals("one",   list4.get(3).get(jsonName));
			assertEquals("two",   list4.get(4).get(jsonName));
			assertEquals("three", list4.get(5).get(jsonName));
			assertEquals("four",  list4.get(6).get(jsonName));

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
		SchemaView testView1           = null;
		SchemaView testView2           = null;
		String id                      = null;

		try (final Tx tx = app.tx()) {

			// create test type
			final SchemaNode test = app.create(SchemaNode.class, "Test");

			// create view with sort order
			testView1 = app.create(SchemaView.class,
				new NodeAttribute<>(SchemaView.name, "test"),
				new NodeAttribute<>(SchemaView.schemaNode, test),
				new NodeAttribute<>(SchemaView.sortOrder, "one, two, three, four, id, type, name"),
				new NodeAttribute<>(SchemaView.nonGraphProperties, "id, type, name")
			);

			// create view with sort order
			testView2 = app.create(SchemaView.class,
				new NodeAttribute<>(SchemaView.name, "other"),
				new NodeAttribute<>(SchemaView.schemaNode, test),
				new NodeAttribute<>(SchemaView.sortOrder, "four, id, type, name, one, three, two"),
				new NodeAttribute<>(SchemaView.nonGraphProperties, "id, type, name")
			);

			final List<SchemaView> list = new LinkedList<>();
			list.add(testView1);
			list.add(testView2);

			// create properties
			app.create(SchemaProperty.class,
				new NodeAttribute<>(SchemaProperty.schemaNode, test),
				new NodeAttribute<>(SchemaProperty.schemaViews, list),
				new NodeAttribute<>(SchemaProperty.propertyType, "String"),
				new NodeAttribute<>(SchemaProperty.name, "one")
			);

			app.create(SchemaProperty.class,
				new NodeAttribute<>(SchemaProperty.schemaNode, test),
				new NodeAttribute<>(SchemaProperty.schemaViews, list),
				new NodeAttribute<>(SchemaProperty.propertyType, "String"),
				new NodeAttribute<>(SchemaProperty.name, "two")
			);

			app.create(SchemaProperty.class,
				new NodeAttribute<>(SchemaProperty.schemaNode, test),
				new NodeAttribute<>(SchemaProperty.schemaViews, list),
				new NodeAttribute<>(SchemaProperty.propertyType, "String"),
				new NodeAttribute<>(SchemaProperty.name, "three")
			);

			app.create(SchemaProperty.class,
				new NodeAttribute<>(SchemaProperty.schemaNode, test),
				new NodeAttribute<>(SchemaProperty.schemaViews, list),
				new NodeAttribute<>(SchemaProperty.propertyType, "String"),
				new NodeAttribute<>(SchemaProperty.name, "four")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final Class type            = StructrApp.getConfiguration().getNodeEntityClass("Test");
		final List<PropertyKey> list = new LinkedList<>(StructrApp.getConfiguration().getPropertySet(type, "test"));

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
			testView1.setProperty(SchemaView.sortOrder, "type, one, id, two, three, four, name");

			// create test entity
			final NodeInterface node = app.create(StructrApp.getConfiguration().getNodeEntityClass("Test"));
			id = node.getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<PropertyKey> list2 = new LinkedList<>(StructrApp.getConfiguration().getPropertySet(type, "test"));

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
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

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
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

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
		SchemaView testView            = null;
		String id                      = null;

		try (final Tx tx = app.tx()) {

			final SchemaNode testBase = app.create(SchemaNode.class, "TestBase");
			final SchemaNode test     = app.create(SchemaNode.class,
				new NodeAttribute<>(SchemaNode.name, "Test"),
				new NodeAttribute<>(SchemaNode.extendsClass, testBase)
			);

			// create view with sort order
			testView = app.create(SchemaView.class,
				new NodeAttribute<>(SchemaView.name, "test"),
				new NodeAttribute<>(SchemaView.schemaNode, test),
				new NodeAttribute<>(SchemaView.sortOrder, "one, two, three, four, id, type, name"),
				new NodeAttribute<>(SchemaView.nonGraphProperties, "id, type, name")
			);

			final List<SchemaView> list = new LinkedList<>();
			list.add(testView);

			// create properties
			app.create(SchemaProperty.class,
				new NodeAttribute<>(SchemaProperty.schemaNode, testBase),
				new NodeAttribute<>(SchemaProperty.schemaViews, list),
				new NodeAttribute<>(SchemaProperty.propertyType, "String"),
				new NodeAttribute<>(SchemaProperty.name, "one")
			);

			app.create(SchemaProperty.class,
				new NodeAttribute<>(SchemaProperty.schemaNode, testBase),
				new NodeAttribute<>(SchemaProperty.schemaViews, list),
				new NodeAttribute<>(SchemaProperty.propertyType, "String"),
				new NodeAttribute<>(SchemaProperty.name, "two")
			);

			app.create(SchemaProperty.class,
				new NodeAttribute<>(SchemaProperty.schemaNode, testBase),
				new NodeAttribute<>(SchemaProperty.schemaViews, list),
				new NodeAttribute<>(SchemaProperty.propertyType, "String"),
				new NodeAttribute<>(SchemaProperty.name, "three")
			);

			app.create(SchemaProperty.class,
				new NodeAttribute<>(SchemaProperty.schemaNode, testBase),
				new NodeAttribute<>(SchemaProperty.schemaViews, list),
				new NodeAttribute<>(SchemaProperty.propertyType, "String"),
				new NodeAttribute<>(SchemaProperty.name, "four")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}


		final Class type            = StructrApp.getConfiguration().getNodeEntityClass("Test");
		final List<PropertyKey> list = new LinkedList<>(StructrApp.getConfiguration().getPropertySet(type, "test"));

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
			testView.setProperty(SchemaView.sortOrder, "type, one, id, two, three, four, name");

			// create test entity
			final NodeInterface node = app.create(StructrApp.getConfiguration().getNodeEntityClass("Test"));
			id = node.getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final List<PropertyKey> list2 = new LinkedList<>(StructrApp.getConfiguration().getPropertySet(type, "test"));

		assertEquals("Invalid number of properties in sorted view", 7, list2.size());
		assertEquals("type",  list2.get(0).dbName());
		assertEquals("one",   list2.get(1).dbName());
		assertEquals("id",    list2.get(2).dbName());
		assertEquals("two",   list2.get(3).dbName());
		assertEquals("three", list2.get(4).dbName());
		assertEquals("four",  list2.get(5).dbName());
		assertEquals("name",  list2.get(6).dbName());

		// test schema resource
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

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
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

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

		SchemaView testView            = null;

		try (final Tx tx = app.tx()) {

			// create test type
			final SchemaNode test = app.create(SchemaNode.class, "Test");

			// create view with sort order
			testView = app.create(SchemaView.class,
				new NodeAttribute<>(SchemaView.name, "testview"),
				new NodeAttribute<>(SchemaView.sortOrder, "name"),
				new NodeAttribute<>(SchemaView.schemaNode, test)
			);

			final List<SchemaView> list = new LinkedList<>();
			list.add(testView);

			// create a function property to overload the String property "name" defined in {@link NodeInterface}
			app.create(SchemaProperty.class,
				new NodeAttribute<>(SchemaProperty.schemaNode, test),
				new NodeAttribute<>(SchemaProperty.schemaViews, list),
				new NodeAttribute<>(SchemaProperty.propertyType, "Function"),
				new NodeAttribute<>(SchemaProperty.name, "name")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		final Class type            = StructrApp.getConfiguration().getNodeEntityClass("Test");
		final List<PropertyKey> list = new LinkedList<>(StructrApp.getConfiguration().getPropertySet(type, "testview"));

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
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

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
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

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

			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.getType("User").overrideMethod("isValidPassword", false, "return true;")
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
				.headers("X-User", "admin" , "X-Password", "wrong")

			.expect()
				.statusCode(200)

			.when()
				.get("/User");
	}

	@Test
	public void testMixedOnCreateMethods() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.getType("User").addMethod("onCreate", "log('test')");

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

			schema.getType("User").addMethod("simpleTest", "log('test')");

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

			// create admin user
			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.getType("File");

			type.addMethod("onDownload", "{ $.log('DOWNLOAD!'); $.this.onDownloadCalled = true; }");
			type.addBooleanProperty("onDownloadCalled");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			final File newFile = app.create(File.class, "test.txt");

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
				.header("X-User",     "admin")
				.header("X-Password", "admin")
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

			final PropertyKey<Boolean> key = StructrApp.key(File.class, "onDownloadCalled");
			final File file                = app.nodeQuery(File.class).getFirst();

			// store UUID of SchemaNode with name "File" for later use
			final SchemaNode fileTypeNode  = app.nodeQuery(SchemaNode.class).andName("File").getFirst();
			fileTypeId = fileTypeNode.getUuid();

			assertTrue("Lifecycle method onDownload was not called!", file.getProperty(key));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		// test getGeneratedSourceCode method
		RestAssured.basePath = "/structr/rest";
		RestAssured
			.given()
				.header("X-User",     "admin")
				.header("X-Password", "admin")
			.expect()
				.statusCode(200)
				.body("result", startsWith("package org.structr.dynamic;"))
			.when()
				.post("/SchemaNode/" + fileTypeId + "/getGeneratedSourceCode");
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

			final SchemaNode node = app.create(SchemaNode.class,
					new NodeAttribute<>(SchemaNode.name, serviceClassName),
					new NodeAttribute<>(SchemaNode.isServiceClass, true)
			);

			// we do not even need to set "isStatic = true" because the backend should do this automatically for service classes
			final SchemaMethod method = app.create(SchemaMethod.class,
					new NodeAttribute<>(SchemaMethod.name, serviceMethodName),
					new NodeAttribute<>(SchemaMethod.source, "{ 'did stuff'; }"),
					new NodeAttribute<>(SchemaMethod.schemaNode, node)
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
					.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

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
					.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

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
