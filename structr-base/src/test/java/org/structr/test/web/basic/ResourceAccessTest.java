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
package org.structr.test.web.basic;

import io.restassured.RestAssured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.ResourceAccessTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 * Test resource access security implemented in {@link UiAuthenticator}.
 */
public class ResourceAccessTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(ResourceAccessTest.class.getName());

	@Test
	public void test01ResourceAccessGET() {

		// clear resource access objects that are created by the dynamic schema
		clearResourceAccess();

		NodeInterface testFolder = null;
		NodeInterface folderGrant = null;

		try (final Tx tx = app.tx()) {

			testFolder = createTestNodes(StructrTraits.FOLDER, 1).get(0);
			assertNotNull(testFolder);

			// no resource access node at all => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().get("/Folder");
			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			folderGrant = createResourceAccess(StructrTraits.FOLDER, UiAuthenticator.FORBIDDEN);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			// resource access explictly set to FORBIDDEN => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().get("/Folder");

			// allow GET for authenticated users => access without user/pass should be still forbidden
			folderGrant.setVisibility(true, false);
			folderGrant.as(ResourceAccess.class).setFlag(UiAuthenticator.AUTH_USER_GET);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().get("/Folder");

			// allow GET for non-authenticated users => access without user/pass should be allowed
			folderGrant.as(ResourceAccess.class).setFlag(UiAuthenticator.NON_AUTH_USER_GET);
			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(200).when().get("/Folder");

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}
	}

	@Test
	public void test02ResourceAccessPOST() {

		// clear resource access objects that are created by the dynamic schema
		clearResourceAccess();

		NodeInterface folderGrant = null;

		try (final Tx tx = app.tx()) {

			// no resource access node at all => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().post("/Folder");

			folderGrant = createResourceAccess(StructrTraits.FOLDER, UiAuthenticator.FORBIDDEN);

			// resource access explicetly set to FORBIDDEN => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().post("/Folder");

			// allow POST for authenticated users => access without user/pass should be still forbidden
			folderGrant.as(ResourceAccess.class).setFlag(UiAuthenticator.AUTH_USER_POST);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().post("/Folder");

			// allow POST for non-authenticated users => access without user/pass should be allowed
			folderGrant.setVisibility(true, false);
			folderGrant.as(ResourceAccess.class).setFlag(UiAuthenticator.NON_AUTH_USER_POST);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given().contentType("application/json; charset=UTF-8").body("{'name':'Test01'}").expect().statusCode(201).when().post("/Folder");

			tx.success();

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test03ResourceAccessPUT() {

		// clear resource access objects that are created by the dynamic schema
		clearResourceAccess();

		final String name = "testuser-01";
		final String password = "testpassword-01";

		NodeInterface folderGrant = null;
		NodeInterface testUser = null;
		NodeInterface testFolder = null;

		try (final Tx tx = app.tx()) {

			testUser = createTestNodes(StructrTraits.USER, 1).get(0);
			testFolder = createTestNodes(StructrTraits.FOLDER, 1).get(0);

			assertNotNull(testFolder);

			// no resource access node at all => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().put("/Folder/" + testFolder.getUuid());

			folderGrant = createResourceAccess(StructrTraits.FOLDER + "/_id", UiAuthenticator.FORBIDDEN);

			tx.success();
		} catch (FrameworkException fex) {
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			// resource access explicitly set to FORBIDDEN => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().put("/Folder/" + testFolder.getUuid());

			// allow PUT for authenticated users => access without user/pass should be still forbidden
			folderGrant.as(ResourceAccess.class).setFlag(UiAuthenticator.AUTH_USER_PUT);

			tx.success();
		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().put("/Folder/" + testFolder.getUuid());

			// allow PUT for non-authenticated users =>
			folderGrant.setVisibility(true, false);
			folderGrant.as(ResourceAccess.class).setFlag(UiAuthenticator.NON_AUTH_USER_PUT);

			tx.success();
		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			// ownerless non-public node cannot be found by anonymous user
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(404).when().put("/Folder/" + testFolder.getUuid());

			// Prepare for next test
			final PropertyMap testUserProperties = new PropertyMap();
			testUserProperties.put(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);
			testUserProperties.put(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), password);
			testUser.setProperties(testUser.getSecurityContext(), testUserProperties);

			// now we give the user ownership and expect a 200
			testFolder.setProperties(testFolder.getSecurityContext(), new PropertyMap(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), testUser));

			tx.success();
		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given()
				.headers(X_USER_HEADER, name, X_PASSWORD_HEADER, password)
				.contentType("application/json; charset=UTF-8").expect().statusCode(200).when().put("/Folder/" + testFolder.getUuid());

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception");

		}

	}

	@Test
	public void test04ResourceAccessDELETE() {

		// clear resource access objects that are created by the dynamic schema
		clearResourceAccess();

		final String name         = "testuser-01";
		final String password     = "testpassword-01";
		NodeInterface testFolder  = null;
		NodeInterface testUser    = null;
		NodeInterface folderGrant = null;

		try (final Tx tx = app.tx()) {

			testFolder = createTestNodes(StructrTraits.FOLDER, 1).get(0);
			assertNotNull(testFolder);
			testUser = createTestNodes(StructrTraits.USER, 1).get(0);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			// no resource access node at all => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().delete("/Folder/" + testFolder.getUuid());

			folderGrant = createResourceAccess(StructrTraits.FOLDER + "/_id", UiAuthenticator.FORBIDDEN);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			// resource access explicitly set to FORBIDDEN => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().delete("/Folder/" + testFolder.getUuid());

			folderGrant.as(ResourceAccess.class).setFlag(UiAuthenticator.AUTH_USER_DELETE);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().delete("/Folder/" + testFolder.getUuid());

			folderGrant.setVisibility(true, false);
			folderGrant.as(ResourceAccess.class).setFlag(UiAuthenticator.NON_AUTH_USER_DELETE);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(404).when().delete("/Folder/" + testFolder.getUuid());

			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);
			changedProperties.put(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), password);
			testUser.setProperties(testUser.getSecurityContext(), changedProperties);

			// make user own folder
			testFolder.setProperties(testFolder.getSecurityContext(), new PropertyMap(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), testUser));

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			// test user owns object now => 200
			RestAssured.given()
				.headers(X_USER_HEADER, name, X_PASSWORD_HEADER, password)
				.contentType("application/json; charset=UTF-8").expect().statusCode(200).when().delete("/Folder/" + testFolder.getUuid());

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}
	}

	@Test
	public void test05ResourceAccessWithUUIDs() {

		// clear resource access objects that are created by the dynamic schema
		clearResourceAccess();

		createEntityAsSuperUser("/User", "{ name: tester, password: test }");

		String uuid = null;

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addMethod("getName", "{ return $.this.name; }");
			type.addMethod("getName2", "{ return ($.this.name + $.methodParameters.param1); }").setHttpVerb("GET").addParameter("param1", "string");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		NodeInterface test = null;

		final String testClass = "Test";

		try (final Tx tx = app.tx()) {

			test = app.create(testClass, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "test123"));
			uuid = test.getUuid();

			// set owner
			final NodeInterface tester = app.nodeQuery(StructrTraits.PRINCIPAL).name("tester").getFirst();

			test.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), tester);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		// test, expect 401
		RestAssured.given().headers(Map.of(X_USER_HEADER, "tester", X_PASSWORD_HEADER, "test")).contentType("application/json; charset=UTF-8").expect().statusCode(401).when().post("/Test/" + uuid + "/getName");
		RestAssured.given().headers(Map.of(X_USER_HEADER, "tester", X_PASSWORD_HEADER, "test")).contentType("application/json; charset=UTF-8").expect().statusCode(401).when().post("/Test/" + uuid + "/getName2/param1/param2/123");
		RestAssured.given().headers(Map.of(X_USER_HEADER, "tester", X_PASSWORD_HEADER, "test")).contentType("application/json; charset=UTF-8").expect().statusCode(401).when().get("/" + uuid);

		try (final Tx tx = app.tx()) {

			createResourceAccess("Test/_id/getName", UiAuthenticator.AUTH_USER_POST).setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			createResourceAccess("Test/_id/getName2", UiAuthenticator.AUTH_USER_GET).setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		// test, expect 200
		RestAssured.given().headers(Map.of(X_USER_HEADER, "tester", X_PASSWORD_HEADER, "test")).contentType("application/json; charset=UTF-8").expect().statusCode(200)
			.body("result", equalTo("test123"))
			.when().post("/Test/" + uuid + "/getName");

		RestAssured.given().headers(Map.of(X_USER_HEADER, "tester", X_PASSWORD_HEADER, "test")).contentType("application/json; charset=UTF-8").expect().statusCode(200)
			.body("result", equalTo("test123undefined"))
			.when().get("/Test/" + uuid + "/getName2");

		// expect success only if exactly the defined arguments are given
		RestAssured.given().headers(Map.of(X_USER_HEADER, "tester", X_PASSWORD_HEADER, "test")).contentType("application/json; charset=UTF-8").expect().statusCode(200)
			.body("result", equalTo("test123value1"))
			.when().get("/Test/" + uuid + "/getName2/value1");

		// expect error if more values are given than parameters are specified (when using values in URL path syntax)
		RestAssured.given().headers(Map.of(X_USER_HEADER, "tester", X_PASSWORD_HEADER, "test")).contentType("application/json; charset=UTF-8").expect().statusCode(400)
			.when().get("/Test/" + uuid + "/getName2/value1/value2/123");
	}

	@Test
	/**
	 * We need to make sure to not leak information, i.e. the 401 error must come before any 404 error for a non-existing object.
	 */
	public void test06OrderOfErrorCodes() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Test");

			type.addMethod("myMethod", "{ 'test!'; }");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		// this should return 401 and NOT 404
		RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().get("Test/00000000000000000000000000000000/myMethod");
	}
	/**
	 * Creates a new ResourceAccess entity with the given signature and
	 * flags in the database.
	 *
	 * @param signature the name of the new page, defaults to "page" if not
	 * set
	 * @param flags
	 *
	 * @return the new resource access node
	 * @throws FrameworkException
	 * */
	public static NodeInterface createResourceAccess(final String signature, long flags) throws FrameworkException {

		final PropertyMap properties = new PropertyMap();
		final Traits traits          = Traits.of(StructrTraits.RESOURCE_ACCESS);
		final App app                = StructrApp.getInstance();

		properties.put(traits.key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY), signature);
		properties.put(traits.key(ResourceAccessTraitDefinition.FLAGS_PROPERTY), flags);

		try {

			return app.create(StructrTraits.RESOURCE_ACCESS, properties);

		} catch (Throwable t) {

			logger.warn("", t);
		}

		return null;
	}

	public static void clearResourceAccess() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface access : app.nodeQuery(StructrTraits.RESOURCE_ACCESS).getAsList()) {
				app.delete(access);
			}

			tx.success();

		} catch (Throwable t) {

			logger.warn("", t);
		}
	}
}
