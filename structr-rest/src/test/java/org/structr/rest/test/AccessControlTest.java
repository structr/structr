/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.rest.test;

import static org.hamcrest.Matchers.*;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.rest.auth.RestAuthenticator;
import org.structr.rest.common.StructrRestTest;
import org.structr.rest.entity.TestOne;
import org.structr.rest.entity.TestUser;

/**
 *
 *
 */
public class AccessControlTest extends StructrRestTest {

	@Override
	protected void setUp() throws Exception {
		
		super.setUp(new HashMap<String, Object>() {
			{ put("JsonRestServlet.authenticator", RestAuthenticator.class.getName()); }
		});
		
	}
	
	
	/**
	 * Paging with deleted nodes
	 */
	public void test01PagingWithDeletedNodes() {
		
		
		List<TestOne> testOnes = new LinkedList<>();
		
		// Create two User and ten TestOne nodes
		try (final Tx tx = StructrApp.getInstance().tx()) {

			createEntityAsSuperUser("/resource_access", "{'signature': 'TestOne', 'flags': 4095}");
			
			List<TestUser> users = createTestNodes(TestUser.class, 2);
			
			users.get(0).setProperty(TestUser.name, "user1");
			users.get(0).setProperty(TestUser.password, "user1");
			
			users.get(1).setProperty(TestUser.name, "user2");
			users.get(1).setProperty(TestUser.password, "user2");
			users.get(1).setProperty(TestUser.isAdmin, true);
			
			testOnes = createTestNodes(TestOne.class, 3);
			
			int i=0;
			
			// First test user is owner
			for (TestOne t: testOnes) {
				i++;
				t.setProperty(TestOne.name, "t-one-" + i);
				t.setProperty(TestOne.owner, users.get(0));
				t.setProperty(TestOne.visibleToAuthenticatedUsers, true);
			}
			
			tx.success();
			
		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}

		// Check as user1 with pageSize=1
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.header("X-User", "user1")
				.header("X-Password", "user1")
			.expect()
				.statusCode(200)
				.body("result",                    hasSize(1))
				.body("result_count",              equalTo(3))

			.when()
				.get("/test_ones?pageSize=1&page=1");
		
		// Check as user2 with pageSize=1
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.header("X-User", "user2")
				.header("X-Password", "user2")
			.expect()
				.statusCode(200)
				.body("result",                    hasSize(1))
				.body("result_count",              equalTo(3))

			.when()
				.get("/test_ones?pageSize=1&page=1");

		try (final Tx tx = StructrApp.getInstance().tx()) {

			// "soft delete" first node
			testOnes.get(0).setProperty(TestOne.name, "deleted");
			testOnes.get(0).setProperty(TestOne.deleted, true);
			//testOnes.get(0).setProperty(TestOne.hidden, true);
			
			tx.success();
			
		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
		
		// Check as user1 with pageSize=1
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.header("X-User", "user2")
				.header("X-Password", "user2")
			.expect()
				.statusCode(200)
				.body("result",                    hasSize(1))
				.body("result_count",              equalTo(2))

			.when()
				.get("/test_ones?sort=name&pageSize=1&page=1");
		
	}


}
