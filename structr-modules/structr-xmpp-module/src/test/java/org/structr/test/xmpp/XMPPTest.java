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
package org.structr.test.xmpp;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;

/**
 */
public class XMPPTest extends StructrUiTest {

	@Test
	public void testMQTT() {

		final String clientType   = "XMPPClient";
		final Traits clientTraits = Traits.of("XMPPClient");

		try (final Tx tx = app.tx()) {

			createAdminUser();

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			app.create(clientType,
				new NodeAttribute<>(clientTraits.key("xmppUsername"),  "username"),
				new NodeAttribute<>(clientTraits.key("xmppPassword"),  "password"),
				new NodeAttribute<>(clientTraits.key("xmppService"),   "service"),
				new NodeAttribute<>(clientTraits.key("xmppHost"),      "host"),
				new NodeAttribute<>(clientTraits.key("xmppPort"),      12345),
				new NodeAttribute<>(clientTraits.key("presenceMode"),  "available")
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		// use RestAssured to check file
		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.header(X_USER_HEADER, ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
			.statusCode(200)
			.body("result[0].type",                  equalTo("XMPPClient"))
			.body("result[0].isEnabled",             equalTo(false))
			.body("result[0].isConnected",           equalTo(false))
			.body("result[0].xmppUsername",          equalTo("username"))
			.body("result[0].xmppPassword",          equalTo("password"))
			.body("result[0].xmppService",           equalTo("service"))
			.body("result[0].xmppHost",              equalTo("host"))
			.body("result[0].xmppPort",              equalTo(12345))
			.body("result[0].presenceMode",          equalTo("available"))
			.when()
			.get("/XMPPClient");
	}

}
