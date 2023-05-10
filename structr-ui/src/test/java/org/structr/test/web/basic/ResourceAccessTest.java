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
package org.structr.test.web.basic;

import io.restassured.RestAssured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.testng.annotations.Test;

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

		Folder testFolder = null;
		ResourceAccess folderGrant = null;

		try (final Tx tx = app.tx()) {

			testFolder = createTestNodes(Folder.class, 1).get(0);
			assertNotNull(testFolder);

			// no resource access node at all => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().get("/folders");
			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			folderGrant = createResourceAccess("Folder", UiAuthenticator.FORBIDDEN);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			// resource access explicetly set to FORBIDDEN => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().get("/folders");

			// allow GET for authenticated users => access without user/pass should be still forbidden
			folderGrant.setProperties(folderGrant.getSecurityContext(), new PropertyMap(GraphObject.visibleToPublicUsers, true));
			folderGrant.setFlag(UiAuthenticator.AUTH_USER_GET);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().get("/folders");

			// allow GET for non-authenticated users => access without user/pass should be allowed
			folderGrant.setFlag(UiAuthenticator.NON_AUTH_USER_GET);
			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(200).when().get("/folders");

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

		ResourceAccess folderGrant = null;
		try (final Tx tx = app.tx()) {

			// no resource access node at all => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().post("/folders");

			folderGrant = createResourceAccess("Folder", UiAuthenticator.FORBIDDEN);

			// resource access explicetly set to FORBIDDEN => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().post("/folders");

			// allow POST for authenticated users => access without user/pass should be still forbidden
			folderGrant.setFlag(UiAuthenticator.AUTH_USER_POST);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().post("/folders");

			// allow POST for non-authenticated users => access without user/pass should be allowed
			folderGrant.setProperties(folderGrant.getSecurityContext(), new PropertyMap(GraphObject.visibleToPublicUsers, true));
			folderGrant.setFlag(UiAuthenticator.NON_AUTH_USER_POST);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given().contentType("application/json; charset=UTF-8").body("{'name':'Test01'}").expect().statusCode(201).when().post("/folders");

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

		ResourceAccess folderGrant = null;
		User testUser = null;
		Folder testFolder = null;

		try (final Tx tx = app.tx()) {

			testUser = createTestNodes(User.class, 1).get(0);
			testFolder = createTestNodes(Folder.class, 1).get(0);

			assertNotNull(testFolder);

			// no resource access node at all => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().put("/folder/" + testFolder.getUuid());

			folderGrant = createResourceAccess("Folder", UiAuthenticator.FORBIDDEN);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			// resource access explicitly set to FORBIDDEN => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().put("/folder/" + testFolder.getUuid());

			// allow PUT for authenticated users => access without user/pass should be still forbidden
			folderGrant.setFlag(UiAuthenticator.AUTH_USER_PUT);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().put("/folder/" + testFolder.getUuid());

			// allow PUT for non-authenticated users =>
			folderGrant.setProperties(folderGrant.getSecurityContext(), new PropertyMap(GraphObject.visibleToPublicUsers, true));
			folderGrant.setFlag(UiAuthenticator.NON_AUTH_USER_PUT);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			// ownerless non-public node cannot be found by anonymous user
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(404).when().put("/folder/" + testFolder.getUuid());

			// Prepare for next test
			final PropertyMap testUserProperties = new PropertyMap();
			testUserProperties.put(StructrApp.key(User.class, "name"), name);
			testUserProperties.put(StructrApp.key(User.class, "password"), password);
			testUser.setProperties(testUser.getSecurityContext(), testUserProperties);

			// now we give the user ownership and expect a 200
			testFolder.setProperties(testFolder.getSecurityContext(), new PropertyMap(AbstractNode.owner, testUser));

			tx.success();
		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given()
				.headers("X-User", name, "X-Password", password)
				.contentType("application/json; charset=UTF-8").expect().statusCode(200).when().put("/folder/" + testFolder.getUuid());

			tx.success();

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test04ResourceAccessDELETE() {

		// clear resource access objects that are created by the dynamic schema
		clearResourceAccess();

		final String name = "testuser-01";
		final String password = "testpassword-01";
		Folder testFolder = null;
		User testUser = null;
		ResourceAccess folderGrant = null;

		try (final Tx tx = app.tx()) {

			testFolder = createTestNodes(Folder.class, 1).get(0);
			assertNotNull(testFolder);
			testUser = createTestNodes(User.class, 1).get(0);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			// no resource access node at all => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().delete("/folder/" + testFolder.getUuid());

			folderGrant = createResourceAccess("Folder", UiAuthenticator.FORBIDDEN);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			// resource access explicitly set to FORBIDDEN => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().delete("/folder/" + testFolder.getUuid());

			folderGrant.setFlag(UiAuthenticator.AUTH_USER_DELETE);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().delete("/folder/" + testFolder.getUuid());

			folderGrant.setProperties(folderGrant.getSecurityContext(), new PropertyMap(GraphObject.visibleToPublicUsers, true));
			folderGrant.setFlag(UiAuthenticator.NON_AUTH_USER_DELETE);

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(404).when().delete("/folder/" + testFolder.getUuid());

			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(StructrApp.key(User.class, "name"), name);
			changedProperties.put(StructrApp.key(User.class, "password"), password);
			testUser.setProperties(testUser.getSecurityContext(), changedProperties);

			// make user own folder
			testFolder.setProperties(testFolder.getSecurityContext(), new PropertyMap(AbstractNode.owner, testUser));

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			// test user owns object now => 200
			RestAssured.given()
				.headers("X-User", name, "X-Password", password)
				.contentType("application/json; charset=UTF-8").expect().statusCode(200).when().delete("/folder/" + testFolder.getUuid());

			tx.success();
		} catch (FrameworkException fex) {
			logger.warn("", fex);
			logger.error(fex.toString());
			fail("Unexpected exception");
		}
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
	public static ResourceAccess createResourceAccess(String signature, long flags) throws FrameworkException {

		final PropertyMap properties = new PropertyMap();
		final App app = StructrApp.getInstance();

		properties.put(ResourceAccess.signature, signature);
		properties.put(ResourceAccess.flags, flags);

		try {

			ResourceAccess access = app.create(ResourceAccess.class, properties);

			return access;

		} catch (Throwable t) {

			logger.warn("", t);
		}

		return null;
	}

	public static void clearResourceAccess() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final ResourceAccess access : app.nodeQuery(ResourceAccess.class).getAsList()) {
				app.delete(access);
			}

			tx.success();

		} catch (Throwable t) {

			logger.warn("", t);
		}
	}
}
