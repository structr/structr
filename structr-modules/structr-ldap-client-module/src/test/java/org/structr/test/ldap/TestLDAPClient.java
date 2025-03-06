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
package org.structr.test.ldap;

import org.structr.api.config.Settings;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 */
public class TestLDAPClient extends StructrUiTest {

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional String testDatabaseConnection) {

		Settings.Services.setValue("NodeService LogService SchemaService HttpService AgentService LDAPService");

		super.setup(testDatabaseConnection);
	}

	/* this test cannot run without an Active Directory server
	@Test
	public void testLDAPClient() {

		final Class type = StructrApp.getConfiguration().getNodeEntityClass(StructrTraits.LDAP_USER);

		Assert.assertNotNull("Type LDAPUser should exist", type);

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.USER,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, PrincipalTraitDefinition.IS_ADMIN_PROPERTY),  true)
			);

			app.create(StructrTraits.LDAP_GROUP,
				new NodeAttribute<>(StructrApp.key(LDAPGroup.class, "name"),              "group1"),
				new NodeAttribute<>(StructrApp.key(LDAPGroup.class, LDAPGroupTraitDefinition.DISTINGUISHED_NAME_PROPERTY), "ou=page1,dc=test,dc=structr,dc=org")
			);

			app.nodeQuery(StructrTraits.RESOURCE_ACCESS).and(Traits.of(StructrTraits.RESOURCE_ACCESS).key("signature"), StructrTraits.USER).getFirst().setProperty(Traits.of(StructrTraits.RESOURCE_ACCESS).key(ResourceAccessTraitDefinition.FLAGS_PROPERTY), 1L);

			tx.success();

		} catch (Throwable t) {
			fail("Unexpected exception");
		}

		RestAssured.basePath = "/structr/rest";
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)

			.expect()
				.statusCode(200)

			.when()
				.get("/User/ui");

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.headers(X_USER_HEADER, "tester", X_PASSWORD_HEADER, "password")

			.expect()
				.statusCode(200)

			.when()
				.get("/User/me");


		try {Thread.sleep(1000); } catch (Throwable t) {}
	}
	*/
}
