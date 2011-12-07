/*
 *  Copyright (C) 2011 Axel Morgner
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

import org.structr.websocket.StructrWebSocket;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketFactory;
import org.eclipse.jetty.websocket.WebSocketFactory.Acceptor;

/**
 *
 * @author Christian Morgner
 */
public class WebSocketServlet extends HttpServlet {

	private static final WebSocketFactory factory              = new WebSocketFactory(new WebSocketAcceptor());
	private static final Logger logger                         = Logger.getLogger(WebSocketServlet.class.getName());
	private static final String STRUCTR_PROTOCOL               = "structr";
	private static final String SERVLET_PARAMETER_ID_PROPERTY  = "IdProperty";

	private static String idProperty                           = null;

	@Override
	public void init() {

		// primary key
		String idPropertyName = this.getInitParameter(SERVLET_PARAMETER_ID_PROPERTY);
		if (idPropertyName != null) {
			logger.log(Level.INFO, "Setting id property to {0}", idPropertyName);
			this.idProperty = idPropertyName;
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

		// accept connection
		if(!factory.acceptWebSocket(request, response)) {

			logger.log(Level.INFO, "Request rejected.");
			response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

		} else {
			logger.log(Level.INFO, "Request accepted.");
		}
	}

	private static class WebSocketAcceptor implements Acceptor {

		@Override
		public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {

			if(STRUCTR_PROTOCOL.equals(protocol)) {
				return new StructrWebSocket(idProperty);
			} else {
				logger.log(Level.INFO, "Protocol {0} not accepted", protocol);
			}

			return null;
		}

		@Override
		public boolean checkOrigin(HttpServletRequest request, String origin) {

			// TODO: check origin
			return true;
		}
	}
}
