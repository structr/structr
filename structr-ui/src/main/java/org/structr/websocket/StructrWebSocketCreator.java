/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.websocket;

import com.google.gson.Gson;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.structr.core.auth.Authenticator;

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
	public Object createWebSocket(final ServletUpgradeRequest request, final ServletUpgradeResponse response) {

		for (String subprotocol : request.getSubProtocols()) {

			if (STRUCTR_PROTOCOL.equals(subprotocol)) {

				response.setAcceptedSubProtocol(subprotocol);

				StructrWebSocket webSocket = new StructrWebSocket(syncController, gson, authenticator);
				webSocket.setRequest(request.getHttpServletRequest());

				return webSocket;
			}
		}

		return null;
	}
}
