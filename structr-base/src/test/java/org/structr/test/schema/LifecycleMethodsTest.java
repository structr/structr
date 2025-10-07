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

import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

import java.nio.charset.Charset;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class LifecycleMethodsTest extends StructrUiTest {

	@Test
	public void test01LifecycleMethodsSuccess() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType customer = schema.addType("Customer");
			final JsonType logEntry = schema.addType("LogEntry");

			customer.addMethod("onNodeCreation", "create('LogEntry', 'name', concat('onNodeCreation: ', this.name))");
			customer.addMethod("onCreate", "create('LogEntry', 'name', concat('onCreate: ', this.name))");
			customer.addMethod("afterCreate", "create('LogEntry', 'name', concat('afterCreate: ', this.name))");
			customer.addMethod("onSave", "create('LogEntry', 'name', concat('onSave: ', this.name))");
			customer.addMethod("afterSave", "create('LogEntry', 'name', concat('afterSave: ', this.name))");
			customer.addMethod("onDelete", "create('LogEntry', 'name', concat('onDelete: ', this.name))");
			customer.addMethod("afterDelete", "create('LogEntry', 'name', concat('afterDelete: ', this.name))");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String customerType = "Customer";
		final String logEntryType = "LogEntry";

		// create object
		try (final Tx tx = app.tx()) {

			app.create(customerType, "Customer");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		// modify object
		try (final Tx tx = app.tx()) {

			final NodeInterface customer = app.nodeQuery(customerType).getFirst();

			customer.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Tester");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		// delete object
		try (final Tx tx = app.tx()) {

			final NodeInterface customer = app.nodeQuery(customerType).getFirst();

			app.delete(customer);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		// check results
		try (final Tx tx = app.tx()) {

			final List<AbstractNode> logEntries = (List)app.nodeQuery(logEntryType).sort(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList();

			final AbstractNode afterCreate    = logEntries.get(0);
			final AbstractNode afterDelete    = logEntries.get(1);
			final AbstractNode afterSave      = logEntries.get(2);
			final AbstractNode onCreate       = logEntries.get(3);
			final AbstractNode onDelete       = logEntries.get(4);
			final AbstractNode onNodeCreation = logEntries.get(5);
			final AbstractNode onSave         = logEntries.get(6);

			assertEquals(onNodeCreation.getName(),    "onNodeCreation: Customer");
			assertEquals(onCreate.getName(),    "onCreate: Customer");
			assertEquals(afterCreate.getName(), "afterCreate: Customer");

			assertEquals(onSave.getName(),   "onSave: Tester");
			assertEquals(afterSave.getName(),"afterSave: Tester");

			// on deletion, "this" is still available, after deletion, it is not
			assertEquals(onDelete.getName(),    "onDelete: Tester");
			assertEquals(afterDelete.getName(), "afterDelete: ");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}

	@Test
	public void test02OnCreateFailure() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType customer = schema.addType("Customer");

			customer.addMethod("onCreate", "assert(false, 422, concat('onCreate: ', this.name))");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String customerType = "Customer";

		// create object
		try (final Tx tx = app.tx()) {

			app.create(customerType, "Customer");

			tx.success();

		} catch (AssertException expected) {

			// this is expected
			assertEquals(expected.getMessage(), "onCreate: Customer");

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}
	@Test
	public void test03OnSaveFailure() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType customer = schema.addType("Customer");

			customer.addMethod("onSave", "assert(false, 422, concat('onSave: ', this.name))");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String customerType = "Customer";

		// create object
		try (final Tx tx = app.tx()) {

			app.create(customerType, "Customer");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		// modify object
		try (final Tx tx = app.tx()) {

			final NodeInterface customer = app.nodeQuery(customerType).getFirst();

			customer.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Tester");

			tx.success();

		} catch (AssertException expected) {

			// this is expected
			assertEquals(expected.getMessage(), "onSave: Tester");

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}
	@Test
	public void test04OnDeleteFailure() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType customer = schema.addType("Customer");

			customer.addMethod("onDelete", "assert(false, 422, concat('onDelete: ', this.name))");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String customerType = "Customer";

		// create object
		try (final Tx tx = app.tx()) {

			app.create(customerType, "Customer");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		// modify object
		try (final Tx tx = app.tx()) {

			final NodeInterface customer = app.nodeQuery(customerType).getFirst();

			customer.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Tester");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		// delete object
		try (final Tx tx = app.tx()) {

			final NodeInterface customer = app.nodeQuery(customerType).getFirst();

			app.delete(customer);

			tx.success();

			fail("Error in onDelete did not cause transaction rollback!");

		} catch (AssertException expected) {

			// this is expected
			assertEquals(expected.getMessage(), "onDelete: Tester");

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}

	@Test
	public void test05PropertyAccessInAfterDelete() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType customer = schema.addType("Customer");
			final JsonType logEntry = schema.addType("LogEntry");

			customer.addMethod("afterDelete", "create('LogEntry', 'name', concat('afterDelete: ', data.name))");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String customerType = "Customer";
		final String logEntryType = "LogEntry";

		// create object
		try (final Tx tx = app.tx()) {

			app.create(customerType, "Customer");

			tx.success();

		} catch (FrameworkException  fex) {

			fex.printStackTrace();
		}

		// delete object
		try (final Tx tx = app.tx()) {

			final NodeInterface customer = app.nodeQuery(customerType).getFirst();

			app.delete(customer);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		// check results
		try (final Tx tx = app.tx()) {

			final List<AbstractNode> logEntries = (List)app.nodeQuery(logEntryType).sort(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList();

			final AbstractNode afterDelete = logEntries.get(0);

			assertEquals(afterDelete.getName(), "afterDelete: Customer");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}

	@Test
	public void test06LoginLogoutCallbacks() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final User admin = app.create(StructrTraits.USER, "admin").as(User.class);
			admin.setIsAdmin(true);
			admin.setPassword("admin");

			app.create(StructrTraits.USER, "user").as(User.class).setPassword("user");

			app.create(StructrTraits.SCHEMA_NODE, "LogEntry");

			app.create(StructrTraits.SCHEMA_METHOD, "onStructrLogin").as(SchemaMethod.class).setSource("{ $.getOrCreate('LogEntry').name += 'login'; }");
			app.create(StructrTraits.SCHEMA_METHOD, "onStructrLogout").as(SchemaMethod.class).setSource("{ $.getOrCreate('LogEntry').name += 'logout'; }");

			final ResourceAccess loginAccess = app.create(StructrTraits.RESOURCE_ACCESS).as(ResourceAccess.class);

			loginAccess.setResourceSignature("_login");
			loginAccess.setFlag(UiAuthenticator.NON_AUTH_USER_POST);
			loginAccess.setVisibility(true, false);

			final ResourceAccess logoutAccess = app.create(StructrTraits.RESOURCE_ACCESS).as(ResourceAccess.class);

			logoutAccess.setResourceSignature("_logout");
			logoutAccess.setFlag(UiAuthenticator.AUTH_USER_POST);
			logoutAccess.setVisibility(false, true);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// send login request
		final String sessionId = RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.body("{ name: user, password: user }")
			.expect()
			.statusCode(200)
			.when()
			.post("/login")
			.cookie("JSESSIONID");

		// send logout request
		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.cookie("JSESSIONID", sessionId)
			.expect()
			.statusCode(200)
			.when()
			.post("/logout");

		// examine name of LogEntry entity (callback methods should modify the name of that entity)
		RestAssured
			.given()
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.body("result[0].name", equalTo("nullloginlogout"))
			.statusCode(200)
			.when()
			.get("/LogEntry");
	}

	@Test
	public void test06OnUploadDownload() {

		final App app = StructrApp.getInstance();
		String fileUuid = null;

		try (final Tx tx = app.tx()) {

			final User admin = app.create(StructrTraits.USER, "admin").as(User.class);
			admin.setIsAdmin(true);
			admin.setPassword("admin");

			app.create(StructrTraits.SCHEMA_NODE, "LogEntry");

			final JsonSchema schema = StructrSchema.createEmptySchema();
			final JsonType file = schema.addType("File");

			file.addMethod("onDownload", "{ $.getOrCreate('LogEntry').name += 'download'; }");
			file.addMethod("onUpload", "{ $.getOrCreate('LogEntry').name += 'upload'; }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		// UPLOAD file
		final String uuid = RestAssured
			.given()
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.contentType("multipart/form-data")
			.multiPart(new MultiPartSpecBuilder("This is a test!".getBytes(Charset.forName("utf-8")))
				.fileName("text.txt")
				.controlName("file")
				.mimeType("text/plain")
				.build()
			)
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.statusCode(200)
			.when()
			.post("/structr/upload")
			.body()
			.asString();

		// DOWNLOAD file
		RestAssured
			.given()
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.contentType("text/plain")
			.body(equalTo("This is a test!"))
			.statusCode(200)
			.when()
			.get("/" + uuid);

		// wait for transaction to complete (?)
		try { Thread.sleep(1000); } catch (Throwable t) {}

		RestAssured.basePath = "/structr/rest";

		// examine name of LogEntry entity (callback methods should modify the name of that entity)
		RestAssured
			.given()
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.body("result[0].name", equalTo("nulluploaddownload"))
			.statusCode(200)
			.when()
			.get("/LogEntry");
	}
}
