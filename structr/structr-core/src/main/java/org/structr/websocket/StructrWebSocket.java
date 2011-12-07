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

import com.google.gson.Gson;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.websocket.WebSocket;
import org.structr.websocket.message.AbstractMessage;
import org.structr.websocket.message.CreateCommand;
import org.structr.websocket.message.DeleteCommand;
import org.structr.websocket.message.GetCommand;
import org.structr.websocket.message.UpdateCommand;

/**
 *
 * @author Christian Morgner
 */
public class StructrWebSocket implements WebSocket.OnTextMessage {

	private static final Logger logger                 = Logger.getLogger(StructrWebSocket.class.getName());
	private static final Set<StructrWebSocket> sockets = new LinkedHashSet<StructrWebSocket>();
	private static final Map<String, Class> commandSet = new LinkedHashMap<String, Class>();

	static {

		// initialize command set
		addCommand(CreateCommand.class);
		addCommand(UpdateCommand.class);
		addCommand(DeleteCommand.class);
		addCommand(GetCommand.class);
	}


	private Connection connection = null;
	private	String idProperty     = null;
	private Gson gson             = null;

	public StructrWebSocket(Gson gson, String idProperty) {
		this.gson = gson;
		this.idProperty = idProperty;
	}

	@Override
	public void onOpen(Connection connection) {

		logger.log(Level.INFO, "New connection with protocol {0}", connection.getProtocol());
		this.connection = connection;

		sockets.add(this);
	}

	@Override
	public void onClose(int closeCode, String message) {

		logger.log(Level.INFO, "Connection closed with closeCode {0} and message {1}", new Object[] { closeCode, message } );
		this.connection = null;

		sockets.remove(this);
	}

	@Override
	public void onMessage(String data) {

		// parse web socket data from JSON
		WebSocketData webSocketData = gson.fromJson(data, WebSocketData.class);

		try {
			String command = webSocketData.getCommand();
			Class type     = commandSet.get(command);

			if(type != null) {

				AbstractMessage message = (AbstractMessage)type.newInstance();
				message.setConnection(connection);
				message.setIdProperty(idProperty);

				// process message
				if(message.processMessage(webSocketData)) {

					// successful execution
					broadcast(gson.toJson(webSocketData, WebSocketData.class));

				} else {

					// error
				}

			} else {

				logger.log(Level.WARNING, "Unknow command {0}", command);
			}

		} catch(Throwable t) {
			logger.log(Level.WARNING, "Unable to parse message.", t);
		}
	}
	
	public Connection getConnection() {
		return connection;
	}

	// ----- private methods -----
	private void broadcast(String message) {

		logger.log(Level.INFO, "Broadcasting message to {0} clients..", sockets.size());

		for(StructrWebSocket socket : sockets) {

			Connection socketConnection = socket.getConnection();
			if(socketConnection != null) {

				try {
					socketConnection.sendMessage(message);

				} catch(Throwable t) {
					
					logger.log(Level.WARNING, "Error sending message to client.", t);
				}
			}
		}
	}

	// ----- private static methods -----
	private static final void addCommand(Class command) {

		try {
			AbstractMessage msg = (AbstractMessage)command.newInstance();
			commandSet.put(msg.getCommand(), command);

		} catch(Throwable t) {

			logger.log(Level.SEVERE, "Unable to add command {0}", command.getName());
		}
	}
}
