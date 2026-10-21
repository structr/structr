/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.web.auth;

import com.google.gson.Gson;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 * A page that cannot read its HttpOnly session cookie sends no session id: the real websocket upgrade must take it from the cookie of the handshake.
 */
public class WebsocketHandshakeSessionTest extends StructrUiTest {

	@Test
	public void testPingWithoutSessionIdIsAuthenticatedByTheHandshakeCookie() throws Exception {

		final String sessionId = loginOverRest();

		assertEquals(100.0, ping(sessionId, "http://" + host + ":" + httpPort).get("code"));
	}

	@Test
	public void testForeignOriginAcceptedByWildcardDoesNotUseTheCookie() throws Exception {

		final String sessionId = loginOverRest();

		Settings.AccessControlAcceptedOrigins.setValue("*");

		try {

			assertEquals(401.0, ping(sessionId, "http://foreign.example").get("code"));

		} finally {

			Settings.AccessControlAcceptedOrigins.setValue(Settings.AccessControlAcceptedOrigins.getDefaultValue());
		}
	}

	// ----- private methods -----
	private String loginOverRest() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			if (app.nodeQuery(StructrTraits.USER).name("admin").getFirst() == null) {

				final Traits traits         = Traits.of(StructrTraits.USER);
				final PropertyMap properties = new PropertyMap();

				properties.put(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),  "admin");
				properties.put(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY),  "admin");
				properties.put(traits.key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true);

				app.create(StructrTraits.USER, properties);
			}

			tx.success();
		}

		grant("_login", UiAuthenticator.NON_AUTH_USER_POST, true);

		final Response login = RestAssured.given().contentType("application/json").body("{ \"name\": \"admin\", \"password\": \"admin\" }").when().post("/login");

		assertEquals(200, login.getStatusCode());

		final String sessionId = login.getCookie("JSESSIONID");

		assertNotNull("REST login must set a session cookie", sessionId);

		return sessionId;
	}

	private Map<String, Object> ping(final String sessionId, final String origin) throws Exception {

		final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
		final StringBuilder buffer           = new StringBuilder();

		final WebSocket webSocket = HttpClient.newHttpClient().newWebSocketBuilder()
			.header("Cookie", "JSESSIONID=" + sessionId)
			.header("Origin", origin)
			.subprotocols("structr")
			.buildAsync(URI.create("ws://" + host + ":" + httpPort + "/structr/ws"), new WebSocket.Listener() {

				@Override
				public CompletionStage<?> onText(final WebSocket ws, final CharSequence data, final boolean last) {

					buffer.append(data);

					if (last) {

						messages.add(buffer.toString());
						buffer.setLength(0);
					}

					ws.request(1);

					return null;
				}
			})
			.get(10, TimeUnit.SECONDS);

		try {

			webSocket.sendText("{ \"command\": \"PING\" }", true).get(10, TimeUnit.SECONDS);

			final String response = messages.poll(10, TimeUnit.SECONDS);
			if (response == null) {

				fail("no answer to PING");
			}

			return new Gson().fromJson(response, Map.class);

		} finally {

			webSocket.abort();
		}
	}
}
