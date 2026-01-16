/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.xmpp.traits.definitions.XMPPClientTraitDefinition;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;

public class XMPPTest extends StructrUiTest {

	@Test
	public void testMQTT() {

		final String clientType   = StructrTraits.XMPP_CLIENT;
		final Traits clientTraits = Traits.of(clientType);

		try (final Tx tx = app.tx()) {

			createAdminUser();

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			app.create(clientType,
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_USERNAME_PROPERTY),  "username"),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_PASSWORD_PROPERTY),  "password"),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_SERVICE_PROPERTY),   "service"),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_HOST_PROPERTY),      "host"),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.XMPP_PORT_PROPERTY),      12345),
				new NodeAttribute<>(clientTraits.key(XMPPClientTraitDefinition.PRESENCE_MODE_PROPERTY),  "available")
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		// use RestAssured to check file
		RestAssured
			.given()
			.header(X_USER_HEADER, ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
			.statusCode(200)
			.body("result[0].type",                  equalTo(clientType))
			.body("result[0].isEnabled",             equalTo(false))
			.body("result[0].isConnected",           equalTo(false))
			.body("result[0].xmppUsername",          equalTo("username"))
			.body("result[0].xmppPassword",          equalTo("password"))
			.body("result[0].xmppService",           equalTo("service"))
			.body("result[0].xmppHost",              equalTo("host"))
			.body("result[0].xmppPort",              equalTo(12345))
			.body("result[0].presenceMode",          equalTo("available"))
			.when()
			.get("/" + clientType);
	}

}
