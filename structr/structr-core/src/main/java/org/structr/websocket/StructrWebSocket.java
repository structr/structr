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

package org.structr.websocket;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.websocket.WebSocket;
import org.structr.websocket.message.AbstractMessage;

/**
 *
 * @author Christian Morgner
 */
public class StructrWebSocket implements WebSocket.OnTextMessage {

	private static final Logger logger = Logger.getLogger(StructrWebSocket.class.getName());
	private Connection connection = null;

	@Override
	public void onOpen(Connection connection) {

		logger.log(Level.INFO, "New connection with protocol {0}", connection.getProtocol());
		this.connection = connection;
	}

	@Override
	public void onClose(int closeCode, String message) {

		logger.log(Level.INFO, "Connection closed with closeCode {0} and message {1}", new Object[] { closeCode, message } );
		this.connection = null;
	}

	@Override
	public void onMessage(String data) {

		logger.log(Level.INFO, "Received message {0}", data);

		AbstractMessage message = AbstractMessage.createMessage(connection, data);
		if(message != null) {
			message.processMessage();
		}
	}
}
