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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
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

			final byte[] fileData    = csvData.getBytes("utf-8");
			final NodeInterface file = FileHelper.createFile(securityContext, fileData, "text/csv", "File", "test.csv", true);

			// extract UUID for later use
			newFileId = file.getUuid();

			// create new type
			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType newType  = schema.addType("Item");
			final Traits userTraits = Traits.of("User");

			newType.addStringProperty("name");
			newType.addIntegerProperty("originId").isIndexed();
			newType.addStringProperty("typeName");
			newType.addIntegerProperty("test1");
			newType.addIntegerProperty("test2");

			StructrSchema.extendDatabaseSchema(app, schema);

			// create test user
			app.create("User",
				new NodeAttribute<>(userTraits.key("name"),     "admin"),
				new NodeAttribute<>(userTraits.key("password"), "admin"),
				new NodeAttribute<>(userTraits.key("isAdmin"),  true)
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

			final String type                   = "Item";
			final PropertyKey<Integer> originId = Traits.of(type).key("originId");
			final PropertyKey<String> typeName  = Traits.of(type).key("typeName");
			final PropertyKey<String> name      = Traits.of(type).key("name");
			final PropertyKey<String> test1     = Traits.of(type).key("test1");
			final PropertyKey<String> test2     = Traits.of(type).key("test2");
			final List<NodeInterface> items     = app.nodeQuery(type).sort(originId).getAsList();

			assertEquals("Invalid CSV import result, expected 3 items to be created from CSV import. ", 3, items.size());

			final NodeInterface one   = items.get(0);
			final NodeInterface two   = items.get(1);
			final NodeInterface three = items.get(2);

			assertEquals("Invalid CSV mapping result", (Integer)0,   one.getProperty(originId));
			assertEquals("Invalid CSV mapping result", (Integer)1,   two.getProperty(originId));
			assertEquals("Invalid CSV mapping result", (Integer)2, three.getProperty(originId));

			assertEquals("Invalid CSV mapping result", "One",   one.getProperty(typeName));
			assertEquals("Invalid CSV mapping result", "Two",   two.getProperty(typeName));
			assertEquals("Invalid CSV mapping result", "Three", three.getProperty(typeName));

			assertEquals("Invalid CSV mapping result", "name: one",   one.getProperty(name));
			assertEquals("Invalid CSV mapping result", "name: two",   two.getProperty(name));
			assertEquals("Invalid CSV mapping result", "name: three", three.getProperty(name));

			assertEquals("Invalid CSV mapping result", 11,   one.getProperty(test1));
			assertEquals("Invalid CSV mapping result", 22,   two.getProperty(test1));
			assertEquals("Invalid CSV mapping result", 33, three.getProperty(test1));

			assertEquals("Invalid CSV mapping result", 22,   one.getProperty(test2));
			assertEquals("Invalid CSV mapping result", 33,   two.getProperty(test2));
			assertEquals("Invalid CSV mapping result", 44, three.getProperty(test2));

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

			final byte[] fileData    = csvData.getBytes("utf-8");
			final NodeInterface file = FileHelper.createFile(securityContext, fileData, "text/csv", "File", "test.csv", true);

			// extract UUID for later use
			newFileId = file.getUuid();

			// create new type
			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType newType  = schema.addType("Item");
			final Traits userTraits = Traits.of("User");

			newType.addStringProperty("name");
			newType.addIntegerProperty("originId").isIndexed();
			newType.addStringProperty("typeName");
			newType.addIntegerProperty("test1");
			newType.addIntegerProperty("test2");

			StructrSchema.extendDatabaseSchema(app, schema);

			// create test user
			app.create("User",
				new NodeAttribute<>(userTraits.key("name"),     "admin"),
				new NodeAttribute<>(userTraits.key("password"), "admin"),
				new NodeAttribute<>(userTraits.key("isAdmin"),  true)
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

			final String type                   = "Item";
			final PropertyKey<Integer> originId = Traits.of(type).key("originId");
			final PropertyKey<String> typeName  = Traits.of(type).key("typeName");
			final PropertyKey<String> name      = Traits.of(type).key("name");
			final PropertyKey<String> test1     = Traits.of(type).key("test1");
			final PropertyKey<String> test2     = Traits.of(type).key("test2");
			final List<NodeInterface> items     = app.nodeQuery(type).sort(originId).getAsList();

			assertEquals("Invalid CSV import result, expected 3 items to be created from CSV import. ", 3, items.size());

			final NodeInterface one   = items.get(0);
			final NodeInterface two   = items.get(1);
			final NodeInterface three = items.get(2);

			assertEquals("Invalid CSV mapping result", (Integer)0,   one.getProperty(originId));
			assertEquals("Invalid CSV mapping result", (Integer)1,   two.getProperty(originId));
			assertEquals("Invalid CSV mapping result", (Integer)2, three.getProperty(originId));

			assertEquals("Invalid CSV mapping result", "One",   one.getProperty(typeName));
			assertEquals("Invalid CSV mapping result", "Two",   two.getProperty(typeName));
			assertEquals("Invalid CSV mapping result", "Three", three.getProperty(typeName));

			assertEquals("Invalid CSV mapping result", "name: one",   one.getProperty(name));
			assertEquals("Invalid CSV mapping result", "name: two",   two.getProperty(name));
			assertEquals("Invalid CSV mapping result", "name: three", three.getProperty(name));

			assertEquals("Invalid CSV mapping result", 11,   one.getProperty(test1));
			assertEquals("Invalid CSV mapping result", 22,   two.getProperty(test1));
			assertEquals("Invalid CSV mapping result", 33, three.getProperty(test1));

			assertEquals("Invalid CSV mapping result", 22,   one.getProperty(test2));
			assertEquals("Invalid CSV mapping result", 33,   two.getProperty(test2));
			assertEquals("Invalid CSV mapping result", 44, three.getProperty(test2));

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

			final byte[] fileData    = csvData.getBytes("utf-8");
			final NodeInterface file = FileHelper.createFile(securityContext, fileData, "text/csv", "File", "test.csv", true);

			// extract UUID for later use
			newFileId = file.getUuid();

			// create new type
			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType newType  = schema.addType("Item");
			final Traits userTraits = Traits.of("User");

			newType.addStringProperty("name");
			newType.addIntegerProperty("originId").isIndexed();
			newType.addStringProperty("typeName");
			newType.addIntegerProperty("test1");
			newType.addIntegerProperty("test2");

			StructrSchema.extendDatabaseSchema(app, schema);

			// create test user
			app.create("User",
				new NodeAttribute<>(userTraits.key("name"),     "admin"),
				new NodeAttribute<>(userTraits.key("password"), "admin"),
				new NodeAttribute<>(userTraits.key("isAdmin"),  true)
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

			final String type                   = "Item";
			final PropertyKey<Integer> originId = Traits.of(type).key("originId");
			final PropertyKey<String> typeName  = Traits.of(type).key("typeName");
			final PropertyKey<String> name      = Traits.of(type).key("name");
			final PropertyKey<String> test1     = Traits.of(type).key("test1");
			final PropertyKey<String> test2     = Traits.of(type).key("test2");
			final List<NodeInterface> items     = app.nodeQuery(type).sort(originId).getAsList();

			assertEquals("Invalid CSV import result, expected 3 items to be created from CSV import. ", 3, items.size());

			final NodeInterface one   = items.get(0);
			final NodeInterface two   = items.get(1);
			final NodeInterface three = items.get(2);

			assertEquals("Invalid CSV mapping result", (Integer)0,   one.getProperty(originId));
			assertEquals("Invalid CSV mapping result", (Integer)1,   two.getProperty(originId));
			assertEquals("Invalid CSV mapping result", (Integer)2, three.getProperty(originId));

			assertEquals("Invalid CSV mapping result", "One",   one.getProperty(typeName));
			assertEquals("Invalid CSV mapping result", "Two",   two.getProperty(typeName));
			assertEquals("Invalid CSV mapping result", "Three", three.getProperty(typeName));

			assertEquals("Invalid CSV mapping result", "name: one",   one.getProperty(name));
			assertEquals("Invalid CSV mapping result", "name: two",   two.getProperty(name));
			assertEquals("Invalid CSV mapping result", "name: three", three.getProperty(name));

			assertEquals("Invalid CSV mapping result", 11,   one.getProperty(test1));
			assertEquals("Invalid CSV mapping result", 22,   two.getProperty(test1));
			assertEquals("Invalid CSV mapping result", 33, three.getProperty(test1));

			assertEquals("Invalid CSV mapping result", 22,   one.getProperty(test2));
			assertEquals("Invalid CSV mapping result", 33,   two.getProperty(test2));
			assertEquals("Invalid CSV mapping result", 44, three.getProperty(test2));

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

			final byte[] fileData    = csvData.getBytes("utf-8");
			final NodeInterface file = FileHelper.createFile(securityContext, fileData, "text/csv", "File", "test.csv", true);

			// extract UUID for later use
			newFileId = file.getUuid();

			// create new type
			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType newType  = schema.addType("Item");
			final Traits userTraits = Traits.of("User");

			newType.addStringProperty("name");
			newType.addIntegerProperty("originId").isIndexed();
			newType.addStringProperty("typeName");
			newType.addIntegerProperty("test1");
			newType.addIntegerProperty("test2");

			StructrSchema.extendDatabaseSchema(app, schema);

			// create test user
			app.create("User",
				new NodeAttribute<>(userTraits.key("name"),     "admin"),
				new NodeAttribute<>(userTraits.key("password"), "admin"),
				new NodeAttribute<>(userTraits.key("isAdmin"),  true)
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

			final String type                   = "Item";
			final PropertyKey<Integer> originId = Traits.of(type).key("originId");
			final PropertyKey<String> typeName  = Traits.of(type).key("typeName");
			final PropertyKey<String> name      = Traits.of(type).key("name");
			final PropertyKey<String> test1     = Traits.of(type).key("test1");
			final PropertyKey<String> test2     = Traits.of(type).key("test2");
			final List<NodeInterface> items     = app.nodeQuery(type).sort(originId).getAsList();

			assertEquals("Invalid CSV import result, expected 3 items to be created from CSV import. ", 3, items.size());

			final NodeInterface one   = items.get(0);
			final NodeInterface two   = items.get(1);
			final NodeInterface three = items.get(2);

			assertEquals("Invalid CSV mapping result", (Integer)0,   one.getProperty(originId));
			assertEquals("Invalid CSV mapping result", (Integer)1,   two.getProperty(originId));
			assertEquals("Invalid CSV mapping result", (Integer)2, three.getProperty(originId));

			assertEquals("Invalid CSV mapping result", "One",   one.getProperty(typeName));
			assertEquals("Invalid CSV mapping result", "Two",   two.getProperty(typeName));
			assertEquals("Invalid CSV mapping result", "Three", three.getProperty(typeName));

			assertEquals("Invalid CSV mapping result", "name:\none",   one.getProperty(name));
			assertEquals("Invalid CSV mapping result", "name: two",   two.getProperty(name));
			assertEquals("Invalid CSV mapping result", "name: three", three.getProperty(name));

			assertEquals("Invalid CSV mapping result", 11,   one.getProperty(test1));
			assertEquals("Invalid CSV mapping result", 22,   two.getProperty(test1));
			assertEquals("Invalid CSV mapping result", 33, three.getProperty(test1));

			assertEquals("Invalid CSV mapping result", 22,   one.getProperty(test2));
			assertEquals("Invalid CSV mapping result", 33,   two.getProperty(test2));
			assertEquals("Invalid CSV mapping result", 44, three.getProperty(test2));

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

			final NodeInterface customType = createTestNode("SchemaNode", new NodeAttribute<>(Traits.of("SchemaMethod").key("name"), "DummyType"));

			final List<NodeInterface> properties = new LinkedList<>();
			properties.add(createTestNode("SchemaProperty", new NodeAttribute<>(Traits.of("NodeInterface").key("name"), "testDataString"),                new NodeAttribute<>(Traits.of("SchemaProperty").key("propertyType"), "String")));
			properties.add(createTestNode("SchemaProperty", new NodeAttribute<>(Traits.of("NodeInterface").key("name"), "retrievedImportSourceFileName"), new NodeAttribute<>(Traits.of("SchemaProperty").key("propertyType"), "String")));
			properties.add(createTestNode("SchemaProperty", new NodeAttribute<>(Traits.of("NodeInterface").key("name"), "retrievedCustomString"),         new NodeAttribute<>(Traits.of("SchemaProperty").key("propertyType"), "String")));
			customType.setProperty(Traits.of("SchemaNode").key("schemaProperties"), properties);

			final List<NodeInterface> methods = new LinkedList<>();
			methods.add(createTestNode("SchemaMethod", new NodeAttribute<>(Traits.of("NodeInterface").key("name"), "onCreate"),    new NodeAttribute<>(Traits.of("SchemaMethod").key("source"), "{ var self = Structr.get('this'); self.retrievedImportSourceFileName = Structr.retrieve('" + storeKey1 + "') }")));
			methods.add(createTestNode("SchemaMethod", new NodeAttribute<>(Traits.of("NodeInterface").key("name"), "afterCreate"), new NodeAttribute<>(Traits.of("SchemaMethod").key("source"), "{ var self = Structr.get('this'); self.retrievedCustomString = Structr.retrieve('" + storeKey2 + "') }")));
			customType.setProperty(Traits.of("SchemaNode").key("schemaMethods"), methods);

			final String csvData =
				"col1,col2,col3\n" +
				"row1val1,row1val2,row1val3\n" +
				"row2val1,row2val2,row2val3\n";

			final byte[] fileData    = csvData.getBytes("utf-8");
			final NodeInterface file = FileHelper.createFile(securityContext, fileData, "text/csv", "File", csvFileName, true);

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

			final String type                = "DummyType";
			final Traits traits              = Traits.of(type);
			final PropertyKey dataKey        = traits.key("testDataString");
			final PropertyKey fileNameKey    = traits.key("retrievedImportSourceFileName");
			final PropertyKey customStrKey   = traits.key("retrievedCustomString");
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
