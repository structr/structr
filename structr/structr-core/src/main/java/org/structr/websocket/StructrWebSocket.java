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
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.jetty.websocket.WebSocket;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.command.CreateCommand;
import org.structr.websocket.command.DeleteCommand;
import org.structr.websocket.command.GetCommand;
import org.structr.websocket.command.ListCommand;
import org.structr.websocket.command.LoginCommand;
import org.structr.websocket.command.LogoutCommand;
import org.structr.websocket.command.UpdateCommand;

/**
 *
 * @author Christian Morgner
 */
public class StructrWebSocket implements WebSocket.OnTextMessage {

	private static final Map<String, StructrWebSocket> sockets = new LinkedHashMap<String, StructrWebSocket>();
	private static final Logger logger                         = Logger.getLogger(StructrWebSocket.class.getName());
	private static final SecureRandom secureRandom             = new SecureRandom();
	private static final Map<String, Class> commandSet         = new LinkedHashMap<String, Class>();
	private static final int SessionIdLength                   = 128;

	static {

		// initialize command set
		addCommand(CreateCommand.class);
		addCommand(UpdateCommand.class);
		addCommand(DeleteCommand.class);
		addCommand(LogoutCommand.class);
		addCommand(LoginCommand.class);
		addCommand(ListCommand.class);
		addCommand(GetCommand.class);
	}

	private String token          = null;
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
		this.token = secureRandomString();
		this.connection = connection;

		sockets.put(token, this);
	}

	@Override
	public void onClose(int closeCode, String message) {

		logger.log(Level.INFO, "Connection closed with closeCode {0} and message {1}", new Object[] { closeCode, message } );
		this.connection = null;

		sockets.remove(token);
	}

	@Override
	public void onMessage(String data) {

		// parse web socket data from JSON
		WebSocketMessage webSocketData = gson.fromJson(data, WebSocketMessage.class);

		try {
			String messageToken = webSocketData.getToken();
			String command      = webSocketData.getCommand();
			Class type          = commandSet.get(command);
			boolean valid       = false;

			if(type != null) {

				// request is valid if token matches or LOGIN is requested
				if(token.equals(messageToken) || type.equals(LoginCommand.class)) {
					valid = true;
				}

				if(valid) {

					AbstractCommand message = (AbstractCommand)type.newInstance();
					message.setParent(this);
					message.setConnection(connection);
					message.setIdProperty(idProperty);
					message.setToken(token);

					// process message
					if(message.processMessage(webSocketData)) {

						// successful execution, broadcast data but remove token
						webSocketData.setToken(null);
						broadcast(gson.toJson(webSocketData, WebSocketMessage.class));
					}
				}

			} else {

				logger.log(Level.WARNING, "Unknow command {0}", command);
				// ignore?
			}

		} catch(Throwable t) {
			logger.log(Level.WARNING, "Unable to parse message.", t);
		}
	}
	
	public Connection getConnection() {
		return connection;
	}

	public void send(Connection connection, WebSocketMessage message) {

		try {
			connection.sendMessage(gson.toJson(message, WebSocketMessage.class));

		} catch(Throwable t) {

			logger.log(Level.WARNING, "Error sending message to client.", t);
		}
	}

	// ----- private methods -----
	private void broadcast(String message) {

		logger.log(Level.INFO, "Broadcasting message to {0} clients..", sockets.size());
		for(StructrWebSocket socket : sockets.values()) {

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
			AbstractCommand msg = (AbstractCommand)command.newInstance();
			commandSet.put(msg.getCommand(), command);

		} catch(Throwable t) {

			logger.log(Level.SEVERE, "Unable to add command {0}", command.getName());
		}
	}

        private static final String secureRandomString() {

                byte[] binaryData = new byte[SessionIdLength];

                // create random data
                secureRandom.nextBytes(binaryData);

                // return random data encoded in Base64
                return Base64.encodeBase64URLSafeString(binaryData);
        }

}
