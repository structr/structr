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
package org.structr.websocket;

import com.google.gson.Gson;
import org.eclipse.jetty.ee10.servlet.ServletApiRequest;
import org.eclipse.jetty.ee10.servlet.ServletChannel;
import org.eclipse.jetty.ee10.servlet.ServletContextRequest;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.ServerUpgradeResponse;
import org.eclipse.jetty.websocket.server.WebSocketCreator;
import org.structr.core.auth.Authenticator;

import java.lang.reflect.Field;

;

/**
 * Custom creator for structr WebSockets.
 *
 *
 */
public class StructrWebSocketCreator implements WebSocketCreator {

	private static final String STRUCTR_PROTOCOL = "structr";

	private final WebsocketController syncController;
	private final Authenticator authenticator;
	private final Gson gson;

	public StructrWebSocketCreator(final WebsocketController syncController, final Gson gson, final Authenticator authenticator) {

		this.syncController = syncController;
		this.authenticator  = authenticator;
		this.gson           = gson;
	}

	@Override
	public Object createWebSocket(final ServerUpgradeRequest request, final ServerUpgradeResponse response, final Callback callback) throws Exception {

		if (request.getSubProtocols().contains(STRUCTR_PROTOCOL)) {

			response.setAcceptedSubProtocol(STRUCTR_PROTOCOL);

			StructrWebSocket webSocket = new StructrWebSocket(syncController, gson, authenticator);

			final ServletChannel servletChannel = Request.get(request, ServletContextRequest.class, ServletContextRequest::getServletChannel);
			if (servletChannel != null) {

				servletChannel.associate(request, response, callback);

				final ServletContextRequest contextRequest = servletChannel.getServletContextRequest();
				final ServletApiRequest apiRequest         = contextRequest.getServletApiRequest();

				// clear servletChannel so we get the real Request object
				final Field field = apiRequest.getClass().getDeclaredField("_servletChannel");
				field.setAccessible(true);
				field.set(apiRequest, null);

				// initialize cookies
				apiRequest.getCookies();

				webSocket.setRequest(apiRequest);
			}

			return webSocket;
		}

		return null;
	}
}
