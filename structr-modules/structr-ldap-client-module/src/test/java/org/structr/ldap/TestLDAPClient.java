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
package org.structr.ldap;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import org.junit.Assert;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.web.entity.User;

/**
 */
public class TestLDAPClient extends StructrLDAPClientModuleTest {

	@Test
	public void testLDAPClient() {

		final Class type = StructrApp.getConfiguration().getNodeEntityClass("LDAPUser");

		Assert.assertNotNull("Type LDAPUser should exist", type);

		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			app.create(LDAPGroup.class,
				new NodeAttribute<>(StructrApp.key(LDAPGroup.class, "name"),              "group1"),
				new NodeAttribute<>(StructrApp.key(LDAPGroup.class, "distinguishedName"), "ou=page1,dc=test,dc=structr,dc=org")
			);

			app.nodeQuery(ResourceAccess.class).and(ResourceAccess.signature, "User").getFirst().setProperty(ResourceAccess.flags, 1L);

			tx.success();

		} catch (Throwable t) {
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/structr/rest";
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.headers("X-User", "admin", "X-Password", "admin")

			.expect()
				.statusCode(200)

			.when()
				.get("/User/ui");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.headers("X-User", "tester", "X-Password", "password")

			.expect()
				.statusCode(200)

			.when()
				.get("/User/me");


		try {Thread.sleep(1000); } catch (Throwable t) {}
	}
}

















































