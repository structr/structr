/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.test;


import com.jayway.restassured.RestAssured;
import org.structr.common.error.FrameworkException;
import org.structr.web.common.StructrUiTest;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.web.entity.Folder;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyMap;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.entity.User;

//~--- classes ----------------------------------------------------------------

/**
 * Test resource access security implemented in {@link UiAuthenticator}
 *
 * @author Axel Morgner
 */
public class ResourceAccessTest extends StructrUiTest {

	private static final Logger logger = Logger.getLogger(ResourceAccessTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

	public void test01ResourceAccessGET() {

		try {

			Folder	testFolder	= createTestNodes(Folder.class, 1).get(0);
			assertNotNull(testFolder);
			
			// no resource access node at all => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().get("/folders");
			
			ResourceAccess folderGrant = createResourceAccess("Folder", UiAuthenticator.FORBIDDEN);

			// resource access explicetly set to FORBIDDEN => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().get("/folders");
			
			// allow GET for authenticated users => access without user/pass should be still forbidden
			folderGrant.setFlag(UiAuthenticator.AUTH_USER_GET);
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().get("/folders");
			
			// allow GET for non-authenticated users => access without user/pass should be allowed
			folderGrant.setFlag(UiAuthenticator.NON_AUTH_USER_GET);
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(200).when().get("/folders");
			
		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test02ResourceAccessPOST() {

		try {

			// no resource access node at all => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().post("/folders");
			
			ResourceAccess folderGrant = createResourceAccess("Folder", UiAuthenticator.FORBIDDEN);

			// resource access explicetly set to FORBIDDEN => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().post("/folders");
			
			// allow POST for authenticated users => access without user/pass should be still forbidden
			folderGrant.setFlag(UiAuthenticator.AUTH_USER_POST);
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().post("/folders");
			
			// allow POST for non-authenticated users => access without user/pass should be allowed
			folderGrant.setFlag(UiAuthenticator.NON_AUTH_USER_POST);
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(201).when().post("/folders");
			
		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}
	
	public void test03ResourceAccessPUT() {

		try {

			Folder	testFolder	= createTestNodes(Folder.class, 1).get(0);
			assertNotNull(testFolder);
			
			// no resource access node at all => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().put("/folder/" + testFolder.getUuid());
			
			ResourceAccess folderGrant = createResourceAccess("Folder", UiAuthenticator.FORBIDDEN);

			// resource access explicetly set to FORBIDDEN => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().put("/folder/" + testFolder.getUuid());
			
			// allow PUT for authenticated users => access without user/pass should be still forbidden
			folderGrant.setFlag(UiAuthenticator.AUTH_USER_PUT);
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().put("/folder/" + testFolder.getUuid());
			
			// allow PUT for non-authenticated users => access is forbidden with 403 because of missing rights for the test object
			folderGrant.setFlag(UiAuthenticator.NON_AUTH_USER_PUT);
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(403).when().put("/folder/" + testFolder.getUuid());
			
			String name = "testuser-01";
			String password = "testpassword-01";
			User	testUser	= createTestNodes(User.class, 1).get(0);
			testUser.setName(name);
			testUser.setPassword(password);
			
			// test user has no specific rights on the object => still 403
			RestAssured.given()
				.headers("X-User", name, "X-Password", password)
				.contentType("application/json; charset=UTF-8").expect().statusCode(403).when().put("/folder/" + testFolder.getUuid());
			
			// now we give the user ownership and expect a 200
			testFolder.setOwner(testUser);
			
			RestAssured.given()
				.headers("X-User", name, "X-Password", password)
				.contentType("application/json; charset=UTF-8").expect().statusCode(200).when().put("/folder/" + testFolder.getUuid());
			
			
			
		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}
	
	public void test04ResourceAccessDELETE() {

		try {

			Folder	testFolder	= createTestNodes(Folder.class, 1).get(0);
			assertNotNull(testFolder);
			
			// no resource access node at all => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().delete("/folder/" + testFolder.getUuid());
			
			ResourceAccess folderGrant = createResourceAccess("Folder", UiAuthenticator.FORBIDDEN);

			// resource access explicetly set to FORBIDDEN => forbidden
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().delete("/folder/" + testFolder.getUuid());
			
			// allow DELETE for authenticated users => access without user/pass should be still forbidden
			folderGrant.setFlag(UiAuthenticator.AUTH_USER_DELETE);
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(401).when().delete("/folder/" + testFolder.getUuid());
			
			// allow DELETE for non-authenticated users => access is forbidden with 403 because of missing rights for the test object
			folderGrant.setFlag(UiAuthenticator.NON_AUTH_USER_DELETE);
			RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(403).when().delete("/folder/" + testFolder.getUuid());
			
			String name = "testuser-01";
			String password = "testpassword-01";
			User	testUser	= createTestNodes(User.class, 1).get(0);
			testUser.setName(name);
			testUser.setPassword(password);
			
			// test user has no specific rights on the object => still 403
			RestAssured.given()
				.headers("X-User", name, "X-Password", password)
				.contentType("application/json; charset=UTF-8").expect().statusCode(403).when().delete("/folder/" + testFolder.getUuid());
			
			// now we give the user ownership and expect a 200
			testFolder.setOwner(testUser);
			
			RestAssured.given()
				.headers("X-User", name, "X-Password", password)
				.contentType("application/json; charset=UTF-8").expect().statusCode(200).when().delete("/folder/" + testFolder.getUuid());
			
			
			
		} catch (FrameworkException ex) {

			ex.printStackTrace();
			
			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	/**
	 * Creates a new ResourceAccess entity with the given signature and flags in the database.
	 * 
	 * @param signature the name of the new page, defaults to "page" if not set
	 * 
	 * @return the new resource access node
	 * @throws FrameworkException 
	 */
	public static ResourceAccess createResourceAccess(String signature, long flags) throws FrameworkException {
		
		final PropertyMap properties = new PropertyMap();

		properties.put(ResourceAccess.signature, signature);
		properties.put(ResourceAccess.flags, flags);
		properties.put(AbstractNode.type, ResourceAccess.class.getSimpleName());
	
		ResourceAccess newResourceAccess = Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(new StructrTransaction<ResourceAccess>() {

			@Override
			public ResourceAccess execute() throws FrameworkException {

				return (ResourceAccess) Services.command(SecurityContext.getSuperUserInstance(), CreateNodeCommand.class).execute(properties);
			}		
		});
		
		return newResourceAccess;
	}	
}
