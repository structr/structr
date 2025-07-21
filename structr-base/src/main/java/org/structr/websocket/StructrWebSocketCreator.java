/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.websocket;

import com.google.gson.Gson;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.core.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.core.server.ServerUpgradeResponse;
import org.eclipse.jetty.websocket.core.server.WebSocketCreator;
import org.structr.core.auth.Authenticator;

;

/**
 * Custom creator for structr WebSockets.
 *
 *
 */
public class StructrWebSocketCreator implements WebSocketCreator {

	private static final String STRUCTR_PROTOCOL = "structr";

	private WebsocketController syncController = null;
	private Authenticator authenticator        = null;
	private Gson gson                          = null;

	public StructrWebSocketCreator(final WebsocketController syncController, final Gson gson, final Authenticator authenticator) {

		this.syncController = syncController;
		this.authenticator  = authenticator;
		this.gson           = gson;
	}

	@Override
	public Object createWebSocket(final ServerUpgradeRequest request, final ServerUpgradeResponse response, final Callback callback) {

		for (String subprotocol : request.getSubProtocols()) {

			if (STRUCTR_PROTOCOL.equals(subprotocol)) {

				response.setAcceptedSubProtocol(subprotocol);

				StructrWebSocket webSocket = new StructrWebSocket(syncController, gson, authenticator);

				webSocket.setRequest(request);

				return webSocket;
			}
		}

		return null;
	}

}
