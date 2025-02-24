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
package org.structr.test.websocket.setup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.structr.core.auth.Authenticator;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.WebsocketController;

import java.util.Map;

public class StructrWebsocketBaseTest extends StructrUiTest {

	private final Gson gson = new GsonBuilder().create();

	protected StructrWebSocket getWebsocket() {

		final WebsocketController controller   = new WebsocketController(gson);
		final Authenticator authenticator      = new UiAuthenticator();
		final StructrWebSocket websocket       = new StructrWebSocket(controller, gson, authenticator);
		final TestableServletRequest request   = new TestableServletRequest();
		final TestableWebsocketSession session = new TestableWebsocketSession();

		websocket.setRequest(request);
		websocket.onWebSocketConnect(session);

		return websocket;
	}

	protected String toJson(final Map<String, Object> data) {
		return gson.toJson(data);
	}

}
