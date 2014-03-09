/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.structr.websocket.WebSocketDataGSONAdapter;
import org.structr.websocket.message.WebSocketMessage;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.structr.core.graph.TransactionCommand;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.StructrWebSocketCreator;
import org.structr.websocket.SynchronizationController;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class WebSocketServlet extends org.eclipse.jetty.websocket.servlet.WebSocketServlet implements HttpServiceServlet {

	private static final Logger logger           = Logger.getLogger(WebSocketServlet.class.getName());
	
	private final StructrHttpServiceConfig config = new StructrHttpServiceConfig();
	
	//private StructrWebSocketServerFactory factory;

	@Override
	public StructrHttpServiceConfig getConfig() {
		return config;
	}
	
//	@Override
//	public void init() throws ServletException {
//
//		super.init();
//		//factory = new StructrWebSocketServerFactory(syncController, gson, config.getDefaultIdProperty(), config.getAuthenticator());
//		
//	}
	
//	
//	
//	@Override
//	protected void doGet(final HttpServletRequest request, HttpServletResponse response) throws IOException {
//
//		// accept connection
//		if (!factory.acceptWebSocket(request, response)) {
//
//			logger.log(Level.INFO, "Request rejected.");
//			response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
//
//		} else {
//
//			logger.log(Level.INFO, "Request accepted.");
//
//		}
//	}

	@Override
	public void configure(final WebSocketServletFactory factory) {
		
		// create GSON serializer
		final Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(WebSocketMessage.class, new WebSocketDataGSONAdapter(config.getDefaultIdProperty(), config.getOutputNestingDepth()))
			.create();
		
		final SynchronizationController syncController = new SynchronizationController(gson);
		
		// register (Structr) transaction listener
		TransactionCommand.registerTransactionListener(syncController);
		
		//factory.getPolicy().setIdleTimeout(10000);
		factory.setCreator(new StructrWebSocketCreator(syncController, gson, config.getDefaultIdProperty(), config.getAuthenticator()));
		factory.register(StructrWebSocket.class);
		
		// Disable compression (experimental features)
		factory.getExtensionFactory().unregister("x-webkit-deflate-frame");
		factory.getExtensionFactory().unregister("permessage-deflate");

	}
}
