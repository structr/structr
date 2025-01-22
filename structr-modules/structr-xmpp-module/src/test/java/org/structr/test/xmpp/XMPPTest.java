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
import org.jivesoftware.smack.packet.Presence;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.User;
import org.structr.xmpp.XMPPClient;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;

/**
 */
public class XMPPTest extends StructrUiTest {

	@Test
	public void testMQTT() {

		final Class clientType = StructrApp.getConfiguration().getNodeEntityClass("XMPPClient");

		try (final Tx tx = app.tx()) {

			app.create("User",
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		try (final Tx tx = app.tx()) {

			app.create(clientType,
				new NodeAttribute<>(StructrApp.key(XMPPClient.class, "xmppUsername"),  "username"),
				new NodeAttribute<>(StructrApp.key(XMPPClient.class, "xmppPassword"),  "password"),
				new NodeAttribute<>(StructrApp.key(XMPPClient.class, "xmppService"),   "service"),
				new NodeAttribute<>(StructrApp.key(XMPPClient.class, "xmppHost"),      "host"),
				new NodeAttribute<>(StructrApp.key(XMPPClient.class, "xmppPort"),      12345),
				new NodeAttribute<>(StructrApp.key(XMPPClient.class, "presenceMode"),  Presence.Mode.available)
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		// use RestAssured to check file
		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.header("X-User", "admin")
			.header("X-Password", "admin")
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
