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
package org.structr.test.mock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.Authenticator;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SessionDataNodeTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.WebSocketDataGSONAdapter;
import org.structr.websocket.WebsocketController;
import org.structr.websocket.message.WebSocketMessage;

import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

public class StructrWebsocketBaseTest extends StructrUiTest {

	/**
	 * Creates a mocked websocket session and connected websocket and
	 * returns the two for testing purposes.
	 *
	 * @return
	 */
	protected MockedWebsocketSetup getMockedWebsocketSetup() {

		// Caution: this code is taken from WebSocketServlet.java and needs to be updated
		// if any changes to the original code are made!

		// create GSON serializer
		final GsonBuilder gsonBuilder = new GsonBuilder()
			.serializeNulls()
			.registerTypeAdapter(WebSocketMessage.class, new WebSocketDataGSONAdapter(3));

		if (Settings.WsIndentation.getValue()) {

			gsonBuilder.setPrettyPrinting();
		}

		final boolean lenient = Settings.JsonLenient.getValue();
		if (lenient) {

			// Serializes NaN, -Infinity, Infinity, see http://code.google.com/p/google-gson/issues/detail?id=378
			gsonBuilder.serializeSpecialFloatingPointValues();

		}

		final Gson gson = gsonBuilder.create();
		final WebsocketController controller = new WebsocketController(gson);
		final Authenticator authenticator    = new UiAuthenticator();
		final StructrWebSocket websocket     = new StructrWebSocket(controller, gson, authenticator);
		final MockServletRequest request     = new MockServletRequest();
		final MockWebsocketSession session   = new MockWebsocketSession(gson);

		websocket.setRequest(request);
		websocket.onWebSocketOpen(session);

		return new MockedWebsocketSetup(session, websocket);
	}

	protected String toJson(final Map<String, Object> data) {

		final Gson gson = new GsonBuilder().create();

		return gson.toJson(data);
	}

	protected Map<String, Object> fromJson(final String src) {

		final Gson gson = new GsonBuilder().create();

		return gson.fromJson(src, Map.class);
	}

	/**
	 * Logs in over the websocket the way a browser does: from a page that already holds a session.
	 *
	 * <p>Ticket 1594 made LoginCommand refuse a session id with no session behind it, because the id
	 * comes from the client and binding one it invented lets it name its own credential. The session is
	 * established here so that these tests keep standing for what they are about - use
	 * {@link #loginWithUnknownSession} to test the refusal itself.</p>
	 */
	protected void login(final StructrWebSocket websocket, final String username, final String password, final String sessionId) {

		establishSession(sessionId);

		loginWithUnknownSession(websocket, username, password, sessionId);
	}

	/** Sends the LOGIN message as it stands, with no session established for the id. */
	protected void loginWithUnknownSession(final StructrWebSocket websocket, final String username, final String password, final String sessionId) {

		websocket.onWebSocketText(toJson(Map.of("command", "LOGIN", "sessionId", sessionId, "data", Map.of("username", username, "password", password))));
	}

	/** Creates the SessionDataNode the session cache resolves a session id through, once per id. */
	protected void establishSession(final String sessionId) {

		try (final Tx tx = app.tx()) {

			final PropertyKey<String> key = Traits.of(StructrTraits.SESSION_DATA_NODE).key(SessionDataNodeTraitDefinition.SESSION_ID_PROPERTY);
			if (app.nodeQuery(StructrTraits.SESSION_DATA_NODE).key(key, sessionId).getFirst() == null) {

				createEntityAsSuperUser("/SessionDataNode", "{ vhost: '0.0.0.0', sessionId: '" + sessionId + "' }");
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception establishing a session: " + fex.getMessage());
		}
	}

	protected Map<String, Object> assertResponse(final MockedWebsocketSetup mock, final String command, final double statusCode, final boolean sessionValid) {

		final Map<String, Object> data = mock.getLastWebsocketResponse();

		assertEquals("sessionValid flag in websocket response was false", sessionValid, data.get("sessionValid"));
		assertEquals("Invalid command in websocket response",             command,      data.get("command"));
		assertEquals("Invalid status code in websocket response",         statusCode,   data.get("code"));

		return data;
	}
}
