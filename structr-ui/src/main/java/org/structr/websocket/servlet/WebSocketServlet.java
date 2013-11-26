/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.websocket.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketFactory;
import org.eclipse.jetty.websocket.WebSocketFactory.Acceptor;

import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.WebSocketDataGSONAdapter;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.TransactionCommand;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.websocket.SynchronizationController;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class WebSocketServlet extends HttpServiceServlet {

	private static final Logger logger           = Logger.getLogger(WebSocketServlet.class.getName());
	private static final String STRUCTR_PROTOCOL = "structr";
	private static WebSocketFactory factory      = null;

	@Override
	public void init() {

		// create GSON serializer
		final Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(WebSocketMessage.class, new WebSocketDataGSONAdapter(AbstractNode.uuid, outputNestingDepth))
			.create();
		
		final SynchronizationController syncController = new SynchronizationController(gson);
		
		// register (Structr) transaction listener
		TransactionCommand.registerTransactionListener(syncController);

		// create web socket factory
		factory = new WebSocketFactory(new Acceptor() {

			@Override
			public WebSocket doWebSocketConnect(final HttpServletRequest request, final String protocol) {

				if (STRUCTR_PROTOCOL.equals(protocol)) {

					return new StructrWebSocket(syncController, request, gson, AbstractNode.uuid, getAuthenticator());

				} else {

					logger.log(Level.INFO, "Protocol {0} not accepted", protocol);

				}

				return null;
			}
			
			@Override
			public boolean checkOrigin(final HttpServletRequest request, final String origin) {

				// TODO: check origin
				return true;
			}

		});
	}

	@Override
	protected void doGet(final HttpServletRequest request, HttpServletResponse response) throws IOException {

		// accept connection
		if (!factory.acceptWebSocket(request, response)) {

			logger.log(Level.INFO, "Request rejected.");
			response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

		} else {

			logger.log(Level.INFO, "Request accepted.");

		}
	}
}
