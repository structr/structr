/**
 * Copyright (C) 2010-2017 Structr GmbH
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

			tx.success();

		} catch (Throwable t) {
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			app.create(type,
				new NodeAttribute<>(StructrApp.key(LDAPUser.class, "name"),              "test"),
				new NodeAttribute<>(StructrApp.key(LDAPUser.class, "distinguishedName"), "distinguishedName"),
				new NodeAttribute<>(StructrApp.key(LDAPUser.class, "description"),       "description"),
				new NodeAttribute<>(StructrApp.key(LDAPUser.class, "commonName"),        "commonName"),
				new NodeAttribute<>(StructrApp.key(LDAPUser.class, "entryUuid"),         "entryUuid")
			);

			tx.success();

		} catch (Throwable t) {
			fail("Unexpected exception");
		}

		RestAssured
			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
				.headers("X-User", "admin" , "X-Password", "admin")
			.expect()
				.statusCode(200)
			.when()
				.get("/LDAPUser");

	}

}

























































