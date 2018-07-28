/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.web.advanced;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.util.LinkedList;
import java.util.List;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
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
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.basic.FrontendTest;
import org.structr.web.basic.ResourceAccessTest;
import static org.structr.web.basic.ResourceAccessTest.createResourceAccess;
import org.structr.web.entity.User;



public class AdvancedSchemaTest extends FrontendTest {

	private static final Logger logger = LoggerFactory.getLogger(AdvancedSchemaTest.class.getName());

	private final int count1 = 34;
	private final int count2 = 44;

	@Test
	public void test01InheritanceOfFileAttributesToImage() {

		cleanDatabaseAndSchema();

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
					.get("/_schema/File/ui");

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
					.get("/_schema/Image/ui");

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}

	@Test
	public void test02InheritanceOfFileAttributesToSubclass() {

		cleanDatabaseAndSchema();

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
			subFileProperties.put(SchemaNode.extendsClass, "org.structr.dynamic.File");
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
					.get("/_schema/SubFile/ui");

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

	}

	@Test
	public void test03InheritanceOfFileAttributesToSubclassOfImage() {

		cleanDatabaseAndSchema();

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
			subFileProperties.put(SchemaNode.extendsClass, "org.structr.dynamic.Image");
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
					.get("/_schema/SubFile/ui");

			tx.success();

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}
	}

	@Test
	public void test04SchemaPropertyOrderInBuiltInViews() {

		cleanDatabaseAndSchema();

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

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}


		try (final Tx tx = app.tx()) {

			// create view with sort order
			final List<SchemaView> list = test.getProperty(SchemaNode.schemaViews);

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

		Assert.assertEquals("Invalid number of properties in sorted view", 6, list.size());
		Assert.assertEquals("id",    list.get(0).dbName());
		Assert.assertEquals("type",  list.get(1).dbName());
		Assert.assertEquals("one",   list.get(2).dbName());
		Assert.assertEquals("two",   list.get(3).dbName());
		Assert.assertEquals("three", list.get(4).dbName());
		Assert.assertEquals("four",  list.get(5).dbName());

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

		Assert.assertEquals("Invalid number of properties in sorted view", 6, list2.size());
		Assert.assertEquals("id",    list2.get(0).dbName());
		Assert.assertEquals("type",  list2.get(1).dbName());
		Assert.assertEquals("one",   list2.get(2).dbName());
		Assert.assertEquals("two",   list2.get(3).dbName());
		Assert.assertEquals("three", list2.get(4).dbName());
		Assert.assertEquals("four",  list2.get(5).dbName());

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

				.body("result",	                  hasSize(6))
				.body("result[0].jsonName",       equalTo("id"))
				.body("result[1].jsonName",       equalTo("type"))
				.body("result[2].jsonName",       equalTo("one"))
				.body("result[3].jsonName",       equalTo("two"))
				.body("result[4].jsonName",       equalTo("three"))
				.body("result[5].jsonName",       equalTo("four"))

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
		assertEquals("Invalid JSON result for sorted property view", "",                   actual[ 0]);
		assertEquals("Invalid JSON result for sorted property view", "query_time",         actual[ 1]);
		assertEquals("Invalid JSON result for sorted property view", "0",                  actual[ 2]);
		assertEquals("Invalid JSON result for sorted property view", "result_count",       actual[ 4]);
		assertEquals("Invalid JSON result for sorted property view", "1",                  actual[ 5]);
		assertEquals("Invalid JSON result for sorted property view", "result",             actual[ 6]);
		assertEquals("Invalid JSON result for sorted property view", "id",                 actual[ 7]);
		assertEquals("Invalid JSON result for sorted property view", id,                   actual[ 8]);
		assertEquals("Invalid JSON result for sorted property view", "type",               actual[ 9]);
		assertEquals("Invalid JSON result for sorted property view", "Test",               actual[10]);
		assertEquals("Invalid JSON result for sorted property view", "one",                actual[11]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[12]);
		assertEquals("Invalid JSON result for sorted property view", "two",                actual[13]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[14]);
		assertEquals("Invalid JSON result for sorted property view", "three",              actual[15]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[16]);
		assertEquals("Invalid JSON result for sorted property view", "four",               actual[17]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[18]);
		assertEquals("Invalid JSON result for sorted property view", "serialization_time", actual[19]);

		// try built-in function
		try {

			final List<GraphObjectMap> list3 = (List)new TypeInfoFunction().apply(new ActionContext(securityContext), null, new Object[] { "Test", "public" });

			Assert.assertEquals("Invalid number of properties in sorted view", 6, list3.size());
			Assert.assertEquals("id",    list3.get(0).get(jsonName));
			Assert.assertEquals("type",  list3.get(1).get(jsonName));
			Assert.assertEquals("one",   list3.get(2).get(jsonName));
			Assert.assertEquals("two",   list3.get(3).get(jsonName));
			Assert.assertEquals("three", list3.get(4).get(jsonName));
			Assert.assertEquals("four",  list3.get(5).get(jsonName));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		// try scripting call
		try {

			final List<GraphObjectMap> list4 = (List)Scripting.evaluate(new ActionContext(securityContext), null, "${type_info('Test', 'public')}", "test");

			Assert.assertEquals("Invalid number of properties in sorted view", 6, list4.size());
			Assert.assertEquals("id",    list4.get(0).get(jsonName));
			Assert.assertEquals("type",  list4.get(1).get(jsonName));
			Assert.assertEquals("one",   list4.get(2).get(jsonName));
			Assert.assertEquals("two",   list4.get(3).get(jsonName));
			Assert.assertEquals("three", list4.get(4).get(jsonName));
			Assert.assertEquals("four",  list4.get(5).get(jsonName));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	@Test
	public void test04SchemaPropertyOrderInCustomViews() {

		cleanDatabaseAndSchema();

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

			// create test type
			final SchemaNode test = app.create(SchemaNode.class, "Test");

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

		Assert.assertEquals("Invalid number of properties in sorted view", 7, list.size());
		Assert.assertEquals("one",   list.get(0).dbName());
		Assert.assertEquals("two",   list.get(1).dbName());
		Assert.assertEquals("three", list.get(2).dbName());
		Assert.assertEquals("four",  list.get(3).dbName());
		Assert.assertEquals("id",    list.get(4).dbName());
		Assert.assertEquals("type",  list.get(5).dbName());
		Assert.assertEquals("name",  list.get(6).dbName());

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

		Assert.assertEquals("Invalid number of properties in sorted view", 7, list2.size());
		Assert.assertEquals("type",  list2.get(0).dbName());
		Assert.assertEquals("one",   list2.get(1).dbName());
		Assert.assertEquals("id",    list2.get(2).dbName());
		Assert.assertEquals("two",   list2.get(3).dbName());
		Assert.assertEquals("three", list2.get(4).dbName());
		Assert.assertEquals("four",  list2.get(5).dbName());
		Assert.assertEquals("name",  list2.get(6).dbName());

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
		assertEquals("Invalid JSON result for sorted property view", "",                   actual[ 0]);
		assertEquals("Invalid JSON result for sorted property view", "query_time",         actual[ 1]);
		assertEquals("Invalid JSON result for sorted property view", "0",                  actual[ 2]);
		assertEquals("Invalid JSON result for sorted property view", "result_count",       actual[ 4]);
		assertEquals("Invalid JSON result for sorted property view", "1",                  actual[ 5]);
		assertEquals("Invalid JSON result for sorted property view", "result",             actual[ 6]);
		assertEquals("Invalid JSON result for sorted property view", "type",               actual[ 7]);
		assertEquals("Invalid JSON result for sorted property view", "Test",               actual[ 8]);
		assertEquals("Invalid JSON result for sorted property view", "one",                actual[ 9]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[10]);
		assertEquals("Invalid JSON result for sorted property view", "id",                 actual[11]);
		assertEquals("Invalid JSON result for sorted property view", id,                   actual[12]);
		assertEquals("Invalid JSON result for sorted property view", "two",                actual[13]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[14]);
		assertEquals("Invalid JSON result for sorted property view", "three",              actual[15]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[16]);
		assertEquals("Invalid JSON result for sorted property view", "four",               actual[17]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[18]);
		assertEquals("Invalid JSON result for sorted property view", "name",               actual[19]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[20]);
		assertEquals("Invalid JSON result for sorted property view", "serialization_time", actual[21]);


		// try built-in function
		try {

			final List<GraphObjectMap> list3 = (List)new TypeInfoFunction().apply(new ActionContext(securityContext), null, new Object[] { "Test", "test" });

			Assert.assertEquals("Invalid number of properties in sorted view", 7, list2.size());
			Assert.assertEquals("type",  list3.get(0).get(jsonName));
			Assert.assertEquals("one",   list3.get(1).get(jsonName));
			Assert.assertEquals("id",    list3.get(2).get(jsonName));
			Assert.assertEquals("two",   list3.get(3).get(jsonName));
			Assert.assertEquals("three", list3.get(4).get(jsonName));
			Assert.assertEquals("four",  list3.get(5).get(jsonName));
			Assert.assertEquals("name",  list3.get(6).get(jsonName));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		// try scripting call
		try {

			final List<GraphObjectMap> list4 = (List)Scripting.evaluate(new ActionContext(securityContext), null, "${type_info('Test', 'test')}", "test");

			Assert.assertEquals("Invalid number of properties in sorted view", 7, list2.size());
			Assert.assertEquals("type",  list4.get(0).get(jsonName));
			Assert.assertEquals("one",   list4.get(1).get(jsonName));
			Assert.assertEquals("id",    list4.get(2).get(jsonName));
			Assert.assertEquals("two",   list4.get(3).get(jsonName));
			Assert.assertEquals("three", list4.get(4).get(jsonName));
			Assert.assertEquals("four",  list4.get(5).get(jsonName));
			Assert.assertEquals("name",  list4.get(6).get(jsonName));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	@Test
	public void testSchemaPropertyOrderInInheritedViews() {

		cleanDatabaseAndSchema();

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
				new NodeAttribute<>(SchemaNode.extendsClass, "org.structr.dynamic.TestBase")
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

		Assert.assertEquals("Invalid number of properties in sorted view", 7, list.size());
		Assert.assertEquals("one",   list.get(0).dbName());
		Assert.assertEquals("two",   list.get(1).dbName());
		Assert.assertEquals("three", list.get(2).dbName());
		Assert.assertEquals("four",  list.get(3).dbName());
		Assert.assertEquals("id",    list.get(4).dbName());
		Assert.assertEquals("type",  list.get(5).dbName());
		Assert.assertEquals("name",  list.get(6).dbName());

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

		Assert.assertEquals("Invalid number of properties in sorted view", 7, list2.size());
		Assert.assertEquals("type",  list2.get(0).dbName());
		Assert.assertEquals("one",   list2.get(1).dbName());
		Assert.assertEquals("id",    list2.get(2).dbName());
		Assert.assertEquals("two",   list2.get(3).dbName());
		Assert.assertEquals("three", list2.get(4).dbName());
		Assert.assertEquals("four",  list2.get(5).dbName());
		Assert.assertEquals("name",  list2.get(6).dbName());

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
		assertEquals("Invalid JSON result for sorted property view", "",                   actual[ 0]);
		assertEquals("Invalid JSON result for sorted property view", "query_time",         actual[ 1]);
		assertEquals("Invalid JSON result for sorted property view", "0",                  actual[ 2]);
		assertEquals("Invalid JSON result for sorted property view", "result_count",       actual[ 4]);
		assertEquals("Invalid JSON result for sorted property view", "1",                  actual[ 5]);
		assertEquals("Invalid JSON result for sorted property view", "result",             actual[ 6]);
		assertEquals("Invalid JSON result for sorted property view", "type",               actual[ 7]);
		assertEquals("Invalid JSON result for sorted property view", "Test",               actual[ 8]);
		assertEquals("Invalid JSON result for sorted property view", "one",                actual[ 9]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[10]);
		assertEquals("Invalid JSON result for sorted property view", "id",                 actual[11]);
		assertEquals("Invalid JSON result for sorted property view", id,                   actual[12]);
		assertEquals("Invalid JSON result for sorted property view", "two",                actual[13]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[14]);
		assertEquals("Invalid JSON result for sorted property view", "three",              actual[15]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[16]);
		assertEquals("Invalid JSON result for sorted property view", "four",               actual[17]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[18]);
		assertEquals("Invalid JSON result for sorted property view", "name",               actual[19]);
		assertEquals("Invalid JSON result for sorted property view", "null",               actual[20]);
		assertEquals("Invalid JSON result for sorted property view", "serialization_time", actual[21]);


		// try built-in function
		try {

			final List<GraphObjectMap> list3 = (List)new TypeInfoFunction().apply(new ActionContext(securityContext), null, new Object[] { "Test", "test" });

			Assert.assertEquals("Invalid number of properties in sorted view", 7, list2.size());
			Assert.assertEquals("type",  list3.get(0).get(jsonName));
			Assert.assertEquals("one",   list3.get(1).get(jsonName));
			Assert.assertEquals("id",    list3.get(2).get(jsonName));
			Assert.assertEquals("two",   list3.get(3).get(jsonName));
			Assert.assertEquals("three", list3.get(4).get(jsonName));
			Assert.assertEquals("four",  list3.get(5).get(jsonName));
			Assert.assertEquals("name",  list3.get(6).get(jsonName));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		// try scripting call
		try {

			final List<GraphObjectMap> list4 = (List)Scripting.evaluate(new ActionContext(securityContext), null, "${type_info('Test', 'test')}", "test");

			Assert.assertEquals("Invalid number of properties in sorted view", 7, list2.size());
			Assert.assertEquals("type",  list4.get(0).get(jsonName));
			Assert.assertEquals("one",   list4.get(1).get(jsonName));
			Assert.assertEquals("id",    list4.get(2).get(jsonName));
			Assert.assertEquals("two",   list4.get(3).get(jsonName));
			Assert.assertEquals("three", list4.get(4).get(jsonName));
			Assert.assertEquals("four",  list4.get(5).get(jsonName));
			Assert.assertEquals("name",  list4.get(6).get(jsonName));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}
	}

	@Test
	public void testIsValidPasswordMethodOfUser() {

		cleanDatabaseAndSchema();

		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.getType("User").overrideMethod("isValidPassword", false, "return true;");

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

		cleanDatabaseAndSchema();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.getType("User").addMethod("onCreate", "log('test')", "test");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testCustomSchemaMethod() {

		cleanDatabaseAndSchema();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			schema.getType("User").addMethod("simpleTest", "log('test')", "test");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testViewProperties() {

		cleanDatabaseAndSchema();

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
}
