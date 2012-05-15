/*
 *  Copyright (C) 2010-2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
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

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.core.EntityContext;
import org.structr.websocket.SynchronizationController;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class WebSocketServlet extends HttpServlet {

	private static final String SERVLET_PARAMETER_ID_PROPERTY = "IdProperty";
	private static final String STRUCTR_PROTOCOL              = "structr";
	private static ServletConfig config                       = null;
	private static WebSocketFactory factory                   = null;
	private static final Logger logger                        = Logger.getLogger(WebSocketServlet.class.getName());

	private SynchronizationController syncController          = null;

	//~--- methods --------------------------------------------------------

	@Override
	public void init() {

		// servlet config
		config = this.getServletConfig();

		// primary key
		final String idPropertyName = this.getInitParameter(SERVLET_PARAMETER_ID_PROPERTY);

		if (idPropertyName != null) {

			logger.log(Level.INFO, "Setting id property to {0}", idPropertyName);

		}

		// create GSON serializer
		final Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(WebSocketMessage.class, new WebSocketDataGSONAdapter(idPropertyName)).create();

		syncController = new SynchronizationController(gson);
		EntityContext.registerModificationListener(syncController);

		// create web socket factory
		factory = new WebSocketFactory(new Acceptor() {

			@Override
			public WebSocket doWebSocketConnect(final HttpServletRequest request, final String protocol) {

				if (STRUCTR_PROTOCOL.equals(protocol)) {

					return new StructrWebSocket(syncController, config, request, gson, idPropertyName);

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
	public void destroy() {
		EntityContext.unregisterModificationListener(syncController);
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
