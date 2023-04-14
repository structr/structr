/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 */
public class CsvImportTest extends StructrUiTest {

	@Test
	public void testCsvFileImportNoQuotes() {

		String newFileId = null;

		// test setup
		try (final Tx tx = app.tx()) {

			final String csvData =

				"id;type;name;Test header with whitespace;ümläüt header\n" +
				"0;One;name: one;11;22\n" +
				"1;Two;name: two;22;33\n" +
				"2;Three;name: three;33;44";

			final byte[] fileData = csvData.getBytes("utf-8");
			final File file   = FileHelper.createFile(securityContext, fileData, "text/csv", File.class, "test.csv", true);

			// extract UUID for later use
			newFileId = file.getUuid();

			// create new type
			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType newType  = schema.addType("Item");

			newType.addStringProperty("name");
			newType.addIntegerProperty("originId").isIndexed();
			newType.addStringProperty("typeName");
			newType.addIntegerProperty("test1");
			newType.addIntegerProperty("test2");

			StructrSchema.extendDatabaseSchema(app, schema);

			// create test user
			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final Gson gson                    = new GsonBuilder().setPrettyPrinting().create();
		final Map<String, Object> params   = new LinkedHashMap<>();
		final Map<String, Object> mappings = new LinkedHashMap<>();

		// import parameters
		params.put("targetType", "Item");
		params.put("quoteChar",  "");
		params.put("delimiter",  ";");
		params.put("mappings",   mappings);

		// property mapping
		mappings.put("originId", "id");
		mappings.put("typeName", "type");
		mappings.put("name", "name");
		mappings.put("test1",  "Test header with whitespace");
		mappings.put("test2", "ümläüt header");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.filter(RequestLoggingFilter.logRequestTo(System.out))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body(gson.toJson(params))
			.expect().statusCode(200).when().post("/File/" + newFileId + "/doCSVImport");

		// wait for result (import is async.)
		try { Thread.sleep(2000); } catch (Throwable t) {}

		// check imported data for correct import
		try (final Tx tx = app.tx()) {

			final ConfigurationProvider conf = StructrApp.getConfiguration();
			final Class type                 = conf.getNodeEntityClass("Item");
			final List<NodeInterface> items  = app.nodeQuery(type).sort(conf.getPropertyKeyForJSONName(type, "originId")).getAsList();

			assertEquals("Invalid CSV import result, expected 3 items to be created from CSV import. ", 3, items.size());

			final NodeInterface one   = items.get(0);
			final NodeInterface two   = items.get(1);
			final NodeInterface three = items.get(2);

			assertEquals("Invalid CSV mapping result", 0,   one.getProperty(conf.getPropertyKeyForJSONName(type, "originId")));
			assertEquals("Invalid CSV mapping result", 1,   two.getProperty(conf.getPropertyKeyForJSONName(type, "originId")));
			assertEquals("Invalid CSV mapping result", 2, three.getProperty(conf.getPropertyKeyForJSONName(type, "originId")));

			assertEquals("Invalid CSV mapping result", "One",   one.getProperty(conf.getPropertyKeyForJSONName(type, "typeName")));
			assertEquals("Invalid CSV mapping result", "Two",   two.getProperty(conf.getPropertyKeyForJSONName(type, "typeName")));
			assertEquals("Invalid CSV mapping result", "Three", three.getProperty(conf.getPropertyKeyForJSONName(type, "typeName")));

			assertEquals("Invalid CSV mapping result", "name: one",   one.getProperty(conf.getPropertyKeyForJSONName(type, "name")));
			assertEquals("Invalid CSV mapping result", "name: two",   two.getProperty(conf.getPropertyKeyForJSONName(type, "name")));
			assertEquals("Invalid CSV mapping result", "name: three", three.getProperty(conf.getPropertyKeyForJSONName(type, "name")));

			assertEquals("Invalid CSV mapping result", 11,   one.getProperty(conf.getPropertyKeyForJSONName(type, "test1")));
			assertEquals("Invalid CSV mapping result", 22,   two.getProperty(conf.getPropertyKeyForJSONName(type, "test1")));
			assertEquals("Invalid CSV mapping result", 33, three.getProperty(conf.getPropertyKeyForJSONName(type, "test1")));

			assertEquals("Invalid CSV mapping result", 22,   one.getProperty(conf.getPropertyKeyForJSONName(type, "test2")));
			assertEquals("Invalid CSV mapping result", 33,   two.getProperty(conf.getPropertyKeyForJSONName(type, "test2")));
			assertEquals("Invalid CSV mapping result", 44, three.getProperty(conf.getPropertyKeyForJSONName(type, "test2")));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

	}

	@Test
	public void testCsvFileImportSingleQuotes() {

		String newFileId = null;

		// test setup
		try (final Tx tx = app.tx()) {

			final String csvData =

				"'id';'type';'name';'Test header with whitespace';'ümläüt header'\n" +
				"'0';'One';'name: one';'11';'22'\n" +
				"'1';'Two';'name: two';'22';'33'\n" +
				"'2';'Three';'name: three';'33';'44'";

			final byte[] fileData = csvData.getBytes("utf-8");
			final File file       = FileHelper.createFile(securityContext, fileData, "text/csv", File.class, "test.csv", true);

			// extract UUID for later use
			newFileId = file.getUuid();

			// create new type
			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType newType  = schema.addType("Item");

			newType.addStringProperty("name");
			newType.addIntegerProperty("originId").isIndexed();
			newType.addStringProperty("typeName");
			newType.addIntegerProperty("test1");
			newType.addIntegerProperty("test2");

			StructrSchema.extendDatabaseSchema(app, schema);

			// create test user
			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final Gson gson                    = new GsonBuilder().setPrettyPrinting().create();
		final Map<String, Object> params   = new LinkedHashMap<>();
		final Map<String, Object> mappings = new LinkedHashMap<>();

		// import parameters
		params.put("targetType", "Item");
		params.put("quoteChar",  "'");
		params.put("delimiter",  ";");
		params.put("mappings",   mappings);

		// property mapping
		mappings.put("originId", "id");
		mappings.put("typeName", "type");
		mappings.put("name", "name");
		mappings.put("test1",  "Test header with whitespace");
		mappings.put("test2", "ümläüt header");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.filter(RequestLoggingFilter.logRequestTo(System.out))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body(gson.toJson(params))
			.expect().statusCode(200).when().post("/File/" + newFileId + "/doCSVImport");

		// wait for result (import is async.)
		try { Thread.sleep(2000); } catch (Throwable t) {}

		// check imported data for correct import
		try (final Tx tx = app.tx()) {

			final ConfigurationProvider conf = StructrApp.getConfiguration();
			final Class type                 = conf.getNodeEntityClass("Item");
			final List<NodeInterface> items  = app.nodeQuery(type).sort(conf.getPropertyKeyForJSONName(type, "originId")).getAsList();

			assertEquals("Invalid CSV import result, expected 3 items to be created from CSV import. ", 3, items.size());

			final NodeInterface one   = items.get(0);
			final NodeInterface two   = items.get(1);
			final NodeInterface three = items.get(2);

			assertEquals("Invalid CSV mapping result", 0,   one.getProperty(conf.getPropertyKeyForJSONName(type, "originId")));
			assertEquals("Invalid CSV mapping result", 1,   two.getProperty(conf.getPropertyKeyForJSONName(type, "originId")));
			assertEquals("Invalid CSV mapping result", 2, three.getProperty(conf.getPropertyKeyForJSONName(type, "originId")));

			assertEquals("Invalid CSV mapping result", "One",   one.getProperty(conf.getPropertyKeyForJSONName(type, "typeName")));
			assertEquals("Invalid CSV mapping result", "Two",   two.getProperty(conf.getPropertyKeyForJSONName(type, "typeName")));
			assertEquals("Invalid CSV mapping result", "Three", three.getProperty(conf.getPropertyKeyForJSONName(type, "typeName")));

			assertEquals("Invalid CSV mapping result", "name: one",   one.getProperty(conf.getPropertyKeyForJSONName(type, "name")));
			assertEquals("Invalid CSV mapping result", "name: two",   two.getProperty(conf.getPropertyKeyForJSONName(type, "name")));
			assertEquals("Invalid CSV mapping result", "name: three", three.getProperty(conf.getPropertyKeyForJSONName(type, "name")));

			assertEquals("Invalid CSV mapping result", 11,   one.getProperty(conf.getPropertyKeyForJSONName(type, "test1")));
			assertEquals("Invalid CSV mapping result", 22,   two.getProperty(conf.getPropertyKeyForJSONName(type, "test1")));
			assertEquals("Invalid CSV mapping result", 33, three.getProperty(conf.getPropertyKeyForJSONName(type, "test1")));

			assertEquals("Invalid CSV mapping result", 22,   one.getProperty(conf.getPropertyKeyForJSONName(type, "test2")));
			assertEquals("Invalid CSV mapping result", 33,   two.getProperty(conf.getPropertyKeyForJSONName(type, "test2")));
			assertEquals("Invalid CSV mapping result", 44, three.getProperty(conf.getPropertyKeyForJSONName(type, "test2")));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

	}

	@Test
	public void testCsvFileImportDoubleQuotes() {

		String newFileId = null;

		// test setup
		try (final Tx tx = app.tx()) {

			final String csvData =

				"\"id\";\"type\";\"name\";\"Test header with whitespace\";\"ümläüt header\"\n" +
				"\"0\";\"One\";\"name: one\";\"11\";\"22\"\n" +
				"\"1\";\"Two\";\"name: two\";\"22\";\"33\"\n" +
				"\"2\";\"Three\";\"name: three\";\"33\";\"44\"";

			final byte[] fileData = csvData.getBytes("utf-8");
			final File file   = FileHelper.createFile(securityContext, fileData, "text/csv", File.class, "test.csv", true);

			// extract UUID for later use
			newFileId = file.getUuid();

			// create new type
			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType newType  = schema.addType("Item");

			newType.addStringProperty("name");
			newType.addIntegerProperty("originId").isIndexed();
			newType.addStringProperty("typeName");
			newType.addIntegerProperty("test1");
			newType.addIntegerProperty("test2");

			StructrSchema.extendDatabaseSchema(app, schema);

			// create test user
			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final Gson gson                    = new GsonBuilder().setPrettyPrinting().create();
		final Map<String, Object> params   = new LinkedHashMap<>();
		final Map<String, Object> mappings = new LinkedHashMap<>();

		// import parameters
		params.put("targetType", "Item");
		params.put("quoteChar",  "\"");
		params.put("delimiter",  ";");
		params.put("mappings",   mappings);

		// property mapping
		mappings.put("originId", "id");
		mappings.put("typeName", "type");
		mappings.put("name", "name");
		mappings.put("test1",  "Test header with whitespace");
		mappings.put("test2", "ümläüt header");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.filter(RequestLoggingFilter.logRequestTo(System.out))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body(gson.toJson(params))
			.expect().statusCode(200).when().post("/File/" + newFileId + "/doCSVImport");

		// wait for result (import is async.)
		try { Thread.sleep(2000); } catch (Throwable t) {}

		// check imported data for correct import
		try (final Tx tx = app.tx()) {

			final ConfigurationProvider conf = StructrApp.getConfiguration();
			final Class type                 = conf.getNodeEntityClass("Item");
			final List<NodeInterface> items  = app.nodeQuery(type).sort(conf.getPropertyKeyForJSONName(type, "originId")).getAsList();

			assertEquals("Invalid CSV import result, expected 3 items to be created from CSV import. ", 3, items.size());

			final NodeInterface one   = items.get(0);
			final NodeInterface two   = items.get(1);
			final NodeInterface three = items.get(2);

			assertEquals("Invalid CSV mapping result", 0,   one.getProperty(conf.getPropertyKeyForJSONName(type, "originId")));
			assertEquals("Invalid CSV mapping result", 1,   two.getProperty(conf.getPropertyKeyForJSONName(type, "originId")));
			assertEquals("Invalid CSV mapping result", 2, three.getProperty(conf.getPropertyKeyForJSONName(type, "originId")));

			assertEquals("Invalid CSV mapping result", "One",   one.getProperty(conf.getPropertyKeyForJSONName(type, "typeName")));
			assertEquals("Invalid CSV mapping result", "Two",   two.getProperty(conf.getPropertyKeyForJSONName(type, "typeName")));
			assertEquals("Invalid CSV mapping result", "Three", three.getProperty(conf.getPropertyKeyForJSONName(type, "typeName")));

			assertEquals("Invalid CSV mapping result", "name: one",   one.getProperty(conf.getPropertyKeyForJSONName(type, "name")));
			assertEquals("Invalid CSV mapping result", "name: two",   two.getProperty(conf.getPropertyKeyForJSONName(type, "name")));
			assertEquals("Invalid CSV mapping result", "name: three", three.getProperty(conf.getPropertyKeyForJSONName(type, "name")));

			assertEquals("Invalid CSV mapping result", 11,   one.getProperty(conf.getPropertyKeyForJSONName(type, "test1")));
			assertEquals("Invalid CSV mapping result", 22,   two.getProperty(conf.getPropertyKeyForJSONName(type, "test1")));
			assertEquals("Invalid CSV mapping result", 33, three.getProperty(conf.getPropertyKeyForJSONName(type, "test1")));

			assertEquals("Invalid CSV mapping result", 22,   one.getProperty(conf.getPropertyKeyForJSONName(type, "test2")));
			assertEquals("Invalid CSV mapping result", 33,   two.getProperty(conf.getPropertyKeyForJSONName(type, "test2")));
			assertEquals("Invalid CSV mapping result", 44, three.getProperty(conf.getPropertyKeyForJSONName(type, "test2")));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

	}

	@Test
	public void testCsvFileImportSingleQuotesLineBreakInCell() {

		String newFileId = null;

		// test setup
		try (final Tx tx = app.tx()) {

			final String csvData =

				"'id';'type';'name';'Test header with whitespace';'ümläüt header'\n" +
				"'0';'One';'name:\none';'11';'22'\n" +
				"'1';'Two';'name: two';'22';'33'\n" +
				"'2';'Three';'name: three';'33';'44'";

			final byte[] fileData = csvData.getBytes("utf-8");
			final File file   = FileHelper.createFile(securityContext, fileData, "text/csv", File.class, "test.csv", true);

			// extract UUID for later use
			newFileId = file.getUuid();

			// create new type
			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType newType  = schema.addType("Item");

			newType.addStringProperty("name");
			newType.addIntegerProperty("originId").isIndexed();
			newType.addStringProperty("typeName");
			newType.addIntegerProperty("test1");
			newType.addIntegerProperty("test2");

			StructrSchema.extendDatabaseSchema(app, schema);

			// create test user
			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final Gson gson                    = new GsonBuilder().setPrettyPrinting().create();
		final Map<String, Object> params   = new LinkedHashMap<>();
		final Map<String, Object> mappings = new LinkedHashMap<>();

		// import parameters
		params.put("targetType", "Item");
		params.put("quoteChar",  "'");
		params.put("delimiter",  ";");
		params.put("mappings",   mappings);

		// property mapping
		mappings.put("originId", "id");
		mappings.put("typeName", "type");
		mappings.put("name", "name");
		mappings.put("test1",  "Test header with whitespace");
		mappings.put("test2", "ümläüt header");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.filter(RequestLoggingFilter.logRequestTo(System.out))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.body(gson.toJson(params))
			.expect().statusCode(200).when().post("/File/" + newFileId + "/doCSVImport");

		// wait for result (import is async.)
		try { Thread.sleep(2000); } catch (Throwable t) {}

		// check imported data for correct import
		try (final Tx tx = app.tx()) {

			final ConfigurationProvider conf = StructrApp.getConfiguration();
			final Class type                 = conf.getNodeEntityClass("Item");
			final List<NodeInterface> items  = app.nodeQuery(type).sort(conf.getPropertyKeyForJSONName(type, "originId")).getAsList();

			assertEquals("Invalid CSV import result, expected 3 items to be created from CSV import. ", 3, items.size());

			final NodeInterface one   = items.get(0);
			final NodeInterface two   = items.get(1);
			final NodeInterface three = items.get(2);

			assertEquals("Invalid CSV mapping result", 0,   one.getProperty(conf.getPropertyKeyForJSONName(type, "originId")));
			assertEquals("Invalid CSV mapping result", 1,   two.getProperty(conf.getPropertyKeyForJSONName(type, "originId")));
			assertEquals("Invalid CSV mapping result", 2, three.getProperty(conf.getPropertyKeyForJSONName(type, "originId")));

			assertEquals("Invalid CSV mapping result", "One",   one.getProperty(conf.getPropertyKeyForJSONName(type, "typeName")));
			assertEquals("Invalid CSV mapping result", "Two",   two.getProperty(conf.getPropertyKeyForJSONName(type, "typeName")));
			assertEquals("Invalid CSV mapping result", "Three", three.getProperty(conf.getPropertyKeyForJSONName(type, "typeName")));

			assertEquals("Invalid CSV mapping result", "name:\none",   one.getProperty(conf.getPropertyKeyForJSONName(type, "name")));
			assertEquals("Invalid CSV mapping result", "name: two",   two.getProperty(conf.getPropertyKeyForJSONName(type, "name")));
			assertEquals("Invalid CSV mapping result", "name: three", three.getProperty(conf.getPropertyKeyForJSONName(type, "name")));

			assertEquals("Invalid CSV mapping result", 11,   one.getProperty(conf.getPropertyKeyForJSONName(type, "test1")));
			assertEquals("Invalid CSV mapping result", 22,   two.getProperty(conf.getPropertyKeyForJSONName(type, "test1")));
			assertEquals("Invalid CSV mapping result", 33, three.getProperty(conf.getPropertyKeyForJSONName(type, "test1")));

			assertEquals("Invalid CSV mapping result", 22,   one.getProperty(conf.getPropertyKeyForJSONName(type, "test2")));
			assertEquals("Invalid CSV mapping result", 33,   two.getProperty(conf.getPropertyKeyForJSONName(type, "test2")));
			assertEquals("Invalid CSV mapping result", 44, three.getProperty(conf.getPropertyKeyForJSONName(type, "test2")));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testProperSecurityContextUsageWhenCallingExportMethods() {

		final String storeKey1 = "importFileName";
		String csvFileId = null;
		final String csvFileName = "dummyData.csv";
		final String storeKey2 = "dummyKey";
		final String valueForStoreKey2 = "dummyValue";

		try (final Tx tx = app.tx()) {

			final SchemaNode customType = createTestNode(SchemaNode.class, new NodeAttribute(SchemaMethod.name, "DummyType"));

			final List<SchemaProperty> properties = new LinkedList<>();
			properties.add(createTestNode(SchemaProperty.class, new NodeAttribute(AbstractNode.name, "testDataString"),     new NodeAttribute(SchemaProperty.propertyType, "String")));
			properties.add(createTestNode(SchemaProperty.class, new NodeAttribute(AbstractNode.name, "retrievedImportSourceFileName"), new NodeAttribute(SchemaProperty.propertyType, "String")));
			properties.add(createTestNode(SchemaProperty.class, new NodeAttribute(AbstractNode.name, "retrievedCustomString"), new NodeAttribute(SchemaProperty.propertyType, "String")));
			customType.setProperty(SchemaNode.schemaProperties, properties);

			final List<SchemaMethod> methods = new LinkedList<>();
			methods.add(createTestNode(SchemaMethod.class, new NodeAttribute(AbstractNode.name, "onCreate"),    new NodeAttribute(SchemaMethod.source, "{ var self = Structr.get('this'); self.retrievedImportSourceFileName = Structr.retrieve('" + storeKey1 + "') }")));
			methods.add(createTestNode(SchemaMethod.class, new NodeAttribute(AbstractNode.name, "afterCreate"), new NodeAttribute(SchemaMethod.source, "{ var self = Structr.get('this'); self.retrievedCustomString = Structr.retrieve('" + storeKey2 + "') }")));
			customType.setProperty(SchemaNode.schemaMethods, methods);

			final String csvData =
				"col1,col2,col3\n" +
				"row1val1,row1val2,row1val3\n" +
				"row2val1,row2val2,row2val3\n";

			final byte[] fileData = csvData.getBytes("utf-8");
			final File file = FileHelper.createFile(securityContext, fileData, "text/csv", File.class, csvFileName, true);

			// extract UUID for later use
			csvFileId = file.getUuid();

			tx.success();

		} catch(Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String callCSVImportScript =  "${{\n" +
			"\n" +
			"	var csvFile = $.find('File', '" + csvFileId +"');\n" +
			"\n" +
			"	Structr.store('" + storeKey1 + "', csvFile.name);\n" +
			"	Structr.store('" + storeKey2 + "', '" + valueForStoreKey2 + "');\n" +
			"\n" +
			"	csvFile.doCSVImport({\n" +
			"		\"targetType\":\"DummyType\",\n" +
			"		 \"delimiter\":\",\",\n" +
			"		 \"quoteChar\":\"\",\n" +
			"		 \"recordSeparator\":\"LF\",\n" +
			"		 \"commitInterval\":\"1000\",\n" +
			"		 \"rfc4180Mode\":false,\n" +
			"		 \"strictQuotes\":false,\n" +
			"		 \"ignoreInvalid\":false,\n" +
			"		 \"distinct\":false,\n" +
			"		 \"range\":\"\",\n" +
			"		 \"importType\":\"node\",\n" +
			"		 \"mixedMappingConfig\":{},\n" +
			"		 \"mappings\":{\"testDataString\":\"col2\"},\n" +
			"		 \"transforms\":{},\n" +
			"		 \"version\":2\n" +
			"	}); \n" +
			"\n" +
			"}}";

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext, null);
			Scripting.evaluate(ctx, null, callCSVImportScript, "test", null);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// wait for async import..
		try { Thread.sleep(4000); } catch (Throwable t) {}

		// check imported data for correct import
		try (final Tx tx = app.tx()) {

			final ConfigurationProvider conf = StructrApp.getConfiguration();
			final Class type                 = conf.getNodeEntityClass("DummyType");
			final PropertyKey dataKey        = conf.getPropertyKeyForJSONName(type, "testDataString");
			final PropertyKey fileNameKey    = conf.getPropertyKeyForJSONName(type, "retrievedImportSourceFileName");
			final PropertyKey customStrKey   = conf.getPropertyKeyForJSONName(type, "retrievedCustomString");
			final List<NodeInterface> items  = app.nodeQuery(type).sort(dataKey).getAsList();

			assertEquals("Wrong number of imported nodes", 2, items.size());

			final NodeInterface node1 = items.get(0);
			final NodeInterface node2 = items.get(1);

			assertEquals("wrong imported value form CSV", "row1val2", node1.getProperty(dataKey));
			assertEquals("wrong retrieved filename", csvFileName, node1.getProperty(fileNameKey));
			assertEquals("wrong retrieved key", valueForStoreKey2, node1.getProperty(customStrKey));

			assertEquals("imported value form CSV faulty", "row2val2", node2.getProperty(dataKey));
			assertEquals("wrong retrieved filename", csvFileName, node2.getProperty(fileNameKey));
			assertEquals("wrong retrieved key", valueForStoreKey2, node2.getProperty(customStrKey));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}
}
