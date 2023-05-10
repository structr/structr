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
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 */
public class XmlImportTest extends StructrUiTest {

	@Test
	public void testXmlFileImport() {

		final String htmlPropertyData = "<!DOCTYPE html><html><head><title>bad idea</title></head><body><h1>ORLY?</h1></body></html>";
		String newFileId              = null;

		// test setup
		try (final Tx tx = app.tx()) {


			final String xmlData =
				"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
				"<items>\n"
				+ "	<item id=\"0\" type=\"One\" name=\"name: one\">\n"
				+ "		<test1><![CDATA[11]]></test1>\n"
				+ "		<test2>22</test2>\n"
				+ "	</item>\n"
				+ "	<item id=\"1\" type=\"Two\" name=\"name: two\" >\n"
				+ "		<test1>22</test1>\n"
				+ "		<test2>33</test2>\n"
				+ "	</item>\n"
				+ "	<item id=\"2\" type=\"Three\" name=\"name: three\" >\n"
				+ "		<test1>33</test1>\n"
				+ "		<test2>44</test2>\n"
				+ "		<test3><![CDATA[" + htmlPropertyData + "]]></test3>\n"
				+ "	</item>\n"
				+ "</items>\n";

			final byte[] fileData = xmlData.getBytes("utf-8");
			final File file       = FileHelper.createFile(securityContext, fileData, "application/xml", File.class, "test.xml", true);

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
			newType.addStringProperty("test3");

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

		final Gson gson                          = new GsonBuilder().setPrettyPrinting().create();
		final Map<String, Object> params         = new LinkedHashMap<>();
		final Map<String, Object> itemConfig     = new LinkedHashMap<>();
		final Map<String, Object> itemProperties = new LinkedHashMap<>();
		final Map<String, Object> test1Config     = new LinkedHashMap<>();
		final Map<String, Object> test2Config    = new LinkedHashMap<>();
		final Map<String, Object> test3Config    = new LinkedHashMap<>();

		params.put("/items/item", itemConfig);
		params.put("/items/item/test1", test1Config);
		params.put("/items/item/test2", test2Config);
		params.put("/items/item/test3", test3Config);

		// import parameters
		itemConfig.put("action", "createNode");
		itemConfig.put("isRoot",  true);
		itemConfig.put("type",  "Item");
		itemConfig.put("properties",   itemProperties);

		// property mapping
		itemProperties.put("id", "originId");
		itemProperties.put("type", "typeName");
		itemProperties.put("name", "name");

		test1Config.put("action", "setProperty");
		test1Config.put("propertyName",  "test1");

		test2Config.put("action", "setProperty");
		test2Config.put("propertyName",  "test2");

		test3Config.put("action", "setProperty");
		test3Config.put("propertyName",  "test3");

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
			.expect().statusCode(200).when().post("/File/" + newFileId + "/doXMLImport");

		// wait for result (import is async.)
		try { Thread.sleep(2000); } catch (Throwable t) {}

		// check imported data for correct import
		try (final Tx tx = app.tx()) {

			final ConfigurationProvider conf = StructrApp.getConfiguration();
			final Class type                 = conf.getNodeEntityClass("Item");
			final List<NodeInterface> items  = app.nodeQuery(type).sort(conf.getPropertyKeyForJSONName(type, "originId")).getAsList();

			assertEquals("Invalid XML import result, expected 3 items to be created from XML import. ", 3, items.size());

			final NodeInterface one   = items.get(0);
			final NodeInterface two   = items.get(1);
			final NodeInterface three = items.get(2);

			assertEquals("Invalid XML mapping result", 0,   one.getProperty(conf.getPropertyKeyForJSONName(type, "originId")));
			assertEquals("Invalid XML mapping result", 1,   two.getProperty(conf.getPropertyKeyForJSONName(type, "originId")));
			assertEquals("Invalid XML mapping result", 2, three.getProperty(conf.getPropertyKeyForJSONName(type, "originId")));

			assertEquals("Invalid XML mapping result", "One",   one.getProperty(conf.getPropertyKeyForJSONName(type, "typeName")));
			assertEquals("Invalid XML mapping result", "Two",   two.getProperty(conf.getPropertyKeyForJSONName(type, "typeName")));
			assertEquals("Invalid XML mapping result", "Three", three.getProperty(conf.getPropertyKeyForJSONName(type, "typeName")));

			assertEquals("Invalid XML mapping result", "name: one",   one.getProperty(conf.getPropertyKeyForJSONName(type, "name")));
			assertEquals("Invalid XML mapping result", "name: two",   two.getProperty(conf.getPropertyKeyForJSONName(type, "name")));
			assertEquals("Invalid XML mapping result", "name: three", three.getProperty(conf.getPropertyKeyForJSONName(type, "name")));

			assertEquals("Invalid XML mapping result", 11,   one.getProperty(conf.getPropertyKeyForJSONName(type, "test1")));
			assertEquals("Invalid XML mapping result", 22,   two.getProperty(conf.getPropertyKeyForJSONName(type, "test1")));
			assertEquals("Invalid XML mapping result", 33, three.getProperty(conf.getPropertyKeyForJSONName(type, "test1")));

			assertEquals("Invalid XML mapping result", 22,   one.getProperty(conf.getPropertyKeyForJSONName(type, "test2")));
			assertEquals("Invalid XML mapping result", 33,   two.getProperty(conf.getPropertyKeyForJSONName(type, "test2")));
			assertEquals("Invalid XML mapping result", 44, three.getProperty(conf.getPropertyKeyForJSONName(type, "test2")));

			assertEquals("Invalid XML mapping result", null,   one.getProperty(conf.getPropertyKeyForJSONName(type, "test3")));
			assertEquals("Invalid XML mapping result", null,   two.getProperty(conf.getPropertyKeyForJSONName(type, "test3")));
			assertEquals("Invalid XML mapping result", htmlPropertyData, three.getProperty(conf.getPropertyKeyForJSONName(type, "test3")));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

	}
}
