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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

import java.nio.charset.Charset;

import static org.testng.AssertJUnit.*;

/**
 *
 */
public class UploadServletTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(UploadServletTest.class);

	@Test
	public void testSuccessfulFileUpload() {

		RestAssured.basePath = "/";

		// setup
		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(Traits.of("User").key("name"),     "admin"),
				new NodeAttribute<>(Traits.of("User").key("password"), "admin"),
				new NodeAttribute<>(Traits.of("User").key("isAdmin"),  true)
			);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		final String response = RestAssured
			.given()
				.header("X-User",       "admin")
				.header("X-Password",   "admin")
				.multiPart("file",      "test.txt", "This is a test!".getBytes(Charset.forName("utf-8")), "text/plain")

			.expect()
				.statusCode(200)

			.when()
				.post("structr/upload")

			.andReturn()
				.body()
				.asString();

		// find file
		try (final Tx tx = app.tx()) {

			final File file           = app.nodeQuery("File").getFirst();

			assertEquals("UUID returned from file upload does not match actual UUID", response,    file.getUuid());
			assertEquals("Name of uploaded file does not match actual name",          "test.txt",  file.getName());
			assertEquals("Location of uploaded file is not correct",                  "/test.txt", file.getFolderPath());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testSuccessfulFileUploadWithRedirectAndUUID() {

		RestAssured.basePath = "/";

		// setup
		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(Traits.of("User").key("name"),     "admin"),
				new NodeAttribute<>(Traits.of("User").key("password"), "admin"),
				new NodeAttribute<>(Traits.of("User").key("isAdmin"),  true)
			);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		final String locationHeader = RestAssured
			.given()
				.header("X-User",       "admin")
				.header("X-Password",   "admin")
				.multiPart("redirectOnSuccess",    "/nonexisting-url/")
				.multiPart("appendUuidOnRedirect", "true")
				.multiPart("uploadFolderPath",     "/uploads")
				.multiPart("file",                 "test.txt", "This is a test!".getBytes(Charset.forName("utf-8")), "text/plain")

			.expect()
				.statusCode(302)

			.when()
				.post("structr/upload")

			.andReturn()
				.header("Location");

		// find file
		try (final Tx tx = app.tx()) {

			final File file               = app.nodeQuery("File").getFirst();
			final Folder uploadFolder     = app.nodeQuery("Folder").getFirst();
			final String expectedLocation = "/nonexisting-url/" + file.getUuid();

			assertEquals("Location header of file upload response is not correct", expectedLocation,    locationHeader);
			assertEquals("Name of uploaded file does not match actual name",       "test.txt",          file.getName());
			assertEquals("Location of uploaded file is not correct",               "/uploads/test.txt", file.getFolderPath());
			assertNotNull("Requested upload folder was not created",               uploadFolder);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testSuccessfulFileUploadWithTypeChange() {

		RestAssured.basePath = "/";

		// setup
		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(Traits.of("User").key("name"),     "admin"),
				new NodeAttribute<>(Traits.of("User").key("password"), "admin"),
				new NodeAttribute<>(Traits.of("User").key("isAdmin"),  true)
			);

			JsonSchema schema   = StructrSchema.createFromDatabase(app);
			final JsonType type = schema.getType("File");
			final JsonType ext  = schema.addType("ExtendedFile");

			ext.setExtends(type);

			StructrSchema.extendDatabaseSchema(app, schema);


			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured
			.given()
				.header("X-User",     "admin")
				.header("X-Password", "admin")
				.multiPart("type",    "ExtendedFile")
				.multiPart("file",    "test.txt", "This is a test!".getBytes(Charset.forName("utf-8")), "text/plain")

			.expect()
				.statusCode(200)

			.when()
				.post("structr/upload");

		// find file
		try (final Tx tx = app.tx()) {

			final File file = (File)app.nodeQuery(StructrApp.getConfiguration().getNodeEntityClass("ExtendedFile")).getFirst();

			assertEquals("Name of uploaded file does not match actual name", "test.txt",     file.getName());
			assertEquals("Type of uploaded file does not match actual type", "ExtendedFile", file.getType());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testSuccessfulFileUploadWithRedirect() {

		RestAssured.basePath = "/";

		// setup
		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(Traits.of("User").key("name"),     "admin"),
				new NodeAttribute<>(Traits.of("User").key("password"), "admin"),
				new NodeAttribute<>(Traits.of("User").key("isAdmin"),  true)
			);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		final String locationHeader = RestAssured
			.given()
				.header("X-User",       "admin")
				.header("X-Password",   "admin")
				.multiPart("redirectOnSuccess",    "/nonexisting-url/")
				.multiPart("uploadFolderPath",     "/uploads")
				.multiPart("file",                 "test.txt", "This is a test!".getBytes(Charset.forName("utf-8")), "text/plain")

			.expect()
				.statusCode(302)

			.when()
				.post("structr/upload")

			.andReturn()
				.header("Location");

		// find file
		try (final Tx tx = app.tx()) {

			final File file               = app.nodeQuery("File").getFirst();
			final String expectedLocation = "/nonexisting-url/";
			final Folder uploadFolder     = app.nodeQuery("Folder").getFirst();

			assertEquals("Location header of file upload response is not correct", expectedLocation,    locationHeader);
			assertEquals("Name of uploaded file does not match actual name",       "test.txt",          file.getName());
			assertEquals("Location of uploaded file is not correct",               "/uploads/test.txt", file.getFolderPath());
			assertNotNull("Requested upload folder was not created",               uploadFolder);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void testErrorInFileUpload() {

		// setup
		try (final Tx tx = app.tx()) {

			createTestNode(User.class,
				new NodeAttribute<>(Traits.of("User").key("name"),     "admin"),
				new NodeAttribute<>(Traits.of("User").key("password"), "admin"),
				new NodeAttribute<>(Traits.of("User").key("isAdmin"),  true)
			);

			JsonSchema schema   = StructrSchema.createFromDatabase(app);
			final JsonType type = schema.getType("File");

			type.addMethod("onCreate", "error('something', 'forbidden', 'nope')");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/";
		RestAssured
			.given()
				.header("X-User",       "admin")
				.header("X-Password",   "admin")
				.multiPart("file", "test.txt", "This is a test!".getBytes(Charset.forName("utf-8")), "text/plain")

			.expect()
				.statusCode(422)

			.when()
				.post("structr/upload");
	}
}
