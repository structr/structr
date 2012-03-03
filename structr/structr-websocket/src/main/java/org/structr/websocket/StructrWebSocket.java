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

import org.structr.websocket.message.WebSocketMessage;
import com.google.gson.Gson;
import java.io.IOException;

import org.apache.commons.codec.binary.Base64;

import org.eclipse.jetty.websocket.WebSocket;

import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.command.CreateCommand;
import org.structr.websocket.command.DeleteCommand;
import org.structr.websocket.command.GetCommand;
import org.structr.websocket.command.ListCommand;
import org.structr.websocket.command.LoginCommand;
import org.structr.websocket.command.LogoutCommand;
import org.structr.websocket.command.UpdateCommand;
import org.structr.websocket.message.MessageBuilder;

//~--- JDK imports ------------------------------------------------------------

import java.security.SecureRandom;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.File;
import org.structr.websocket.command.*;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class StructrWebSocket implements WebSocket.OnTextMessage {

	private static final int SessionIdLength           = 128;
	private static final SecureRandom secureRandom     = new SecureRandom();
	private static final Logger logger                 = Logger.getLogger(StructrWebSocket.class.getName());
	private static final Map<String, Class> commandSet = new LinkedHashMap<String, Class>();

	//~--- static initializers --------------------------------------------

	static {

		// initialize command set

		// login and logout
		addCommand(LoginCommand.class);
		addCommand(LogoutCommand.class);

		// list objects
		addCommand(ListCommand.class);
		
		// get (read) single object
		addCommand(GetCommand.class);

		// create a new object
		addCommand(CreateCommand.class);

		// update an object
		addCommand(UpdateCommand.class);

		// delete an object
		addCommand(DeleteCommand.class);

		// add an object to another (create relationship)
		addCommand(AddCommand.class);
		
		// remove an object from another (remove relationship)
		addCommand(RemoveCommand.class);

		// get a chunk (part) of an object
		addCommand(ChunkCommand.class);

		// render an object tree
		addCommand(TreeCommand.class);

		// create a link between objects
		addCommand(LinkCommand.class);

	}

	//~--- fields ---------------------------------------------------------

	private SynchronizationController syncController = null;
	private Map<String, FileUploadHandler> uploads   = null;
	private ServletConfig config                     = null;
	private Connection connection                    = null;
	private Gson gson                                = null;
	private String idProperty                        = null;
	private HttpServletRequest request               = null;
	private String token                             = null;
	private String callback                          = null;

	//~--- constructors ---------------------------------------------------

	public StructrWebSocket(final SynchronizationController syncController, final ServletConfig config, final HttpServletRequest request, final Gson gson, final String idProperty) {

		this.uploads        = new LinkedHashMap<String, FileUploadHandler>();
		this.syncController = syncController;
		this.config         = config;
		this.request        = request;
		this.gson           = gson;
		this.idProperty     = idProperty;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public void onOpen(final Connection connection) {

		logger.log(Level.INFO, "New connection with protocol {0}", connection.getProtocol());

		this.connection = connection;
		this.token      = null;

		syncController.registerClient(this);

		connection.setMaxTextMessageSize(1024*1024*1024);
		connection.setMaxBinaryMessageSize(1024*1024*1024);
	}

	@Override
	public void onClose(final int closeCode, final String message) {

		logger.log(Level.INFO, "Connection closed with closeCode {0} and message {1}", new Object[] { closeCode, message });

		this.token      = null;
		this.connection = null;

		syncController.unregisterClient(this);

		// flush and close open uploads
		for(FileUploadHandler upload : uploads.values()) {
			upload.finish();
		}

		uploads.clear();
	}

	@Override
	public void onMessage(final String data) {

		logger.log(Level.INFO, "############################################################ RECEIVED \n{0}", data.substring(0, Math.min(data.length(), 1000)));

		// parse web socket data from JSON
		WebSocketMessage webSocketData = gson.fromJson(data, WebSocketMessage.class);

		try {

			this.callback       = webSocketData.getCallback();
			String messageToken = webSocketData.getToken();
			String command      = webSocketData.getCommand();
			Class type          = commandSet.get(command);

			if (type != null) {

				if (!isAuthenticated() && (messageToken != null)) {

					// try to authenticated this connection by token
					authenticateToken(messageToken);
				}

				// we only permit LOGIN commands if token authentication was not successful
				if(!isAuthenticated() && !type.equals(LoginCommand.class)) {

					// send 401 Authentication Required
					send(MessageBuilder.status().code(401).message("").build(), true);
					return;
				}

				AbstractCommand abstractCommand = (AbstractCommand)type.newInstance();
				abstractCommand.setWebSocket(this);
				abstractCommand.setConnection(connection);
				abstractCommand.setIdProperty(idProperty);

				// store authenticated-Flag in webSocketData
				// so the command can access it
				webSocketData.setSessionValid(isAuthenticated());

				// clear token (no tokens in broadcast!!)
				webSocketData.setToken(null);

				// process message
				try {
					abstractCommand.processMessage(webSocketData);

				} catch(Throwable t) {

					// send 400 Bad Request
					send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);
				}

			} else {

				logger.log(Level.WARNING, "Unknow command {0}", command);

				// send 400 Bad Request
				send(MessageBuilder.status().code(400).message("Unknown command").build(), true);
			}

		} catch (Throwable t) {
			logger.log(Level.WARNING, "Unable to parse message.", t);
		}
	}

	public void send(final WebSocketMessage message, final boolean clearToken) {

		// return session status to client
		message.setSessionValid(isAuthenticated());

		// whether to clear the token (all command except LOGIN (for now) should absolutely do this!)
		if (clearToken) {
			message.setToken(null);
		}

		// set callback
		message.setCallback(callback);

		try {

			if (isAuthenticated() || "STATUS".equals(message.getCommand())) {

				String msg = gson.toJson(message, WebSocketMessage.class);

				if ("STATUS".equals(message.getCommand())) {
					logger.log(Level.INFO, "############################################################ SENDING \n{0}", msg);
				}

				connection.sendMessage(msg);

			} else {

				logger.log(Level.WARNING, "NOT sending message to unauthenticated client.");

			}

		} catch (Throwable t) {
			logger.log(Level.WARNING, "Error sending message to client.", t);
		}
	}

	// ----- file handling -----
	public void handleFileCreation(File file) {

		String uuid = file.getStringProperty(AbstractNode.Key.uuid);
		uploads.put(uuid, new FileUploadHandler(file));
	}

	public void handleFileChunk(String uuid, int sequenceNumber, int chunkSize, byte[] data) throws IOException {

		FileUploadHandler upload = uploads.get(uuid);
		if(upload != null) {

			upload.handleChunk(sequenceNumber, chunkSize, data);
		}
	}

	// ----- public static methods -----
	public static final String secureRandomString() {

		byte[] binaryData = new byte[SessionIdLength];

		// create random data
		secureRandom.nextBytes(binaryData);

		// return random data encoded in Base64
		return Base64.encodeBase64URLSafeString(binaryData);
	}

	// ----- private methods -----
	private void authenticateToken(final String messageToken) {

		User user = getUserForToken(messageToken);

		if (user != null) {

			// TODO: session timeout!
			this.setAuthenticated(messageToken);
		}
	}

	// ----- private static methods -----
	public static final void addCommand(final Class command) {

		try {

			AbstractCommand msg = (AbstractCommand) command.newInstance();

			commandSet.put(msg.getCommand(), command);

		} catch (Throwable t) {
			logger.log(Level.SEVERE, "Unable to add command {0}", command.getName());
		}
	}

	//~--- get methods ----------------------------------------------------

	public ServletConfig getConfig() {
		return config;
	}

	public Connection getConnection() {
		return connection;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public User getCurrentUser() {
		return getUserForToken(token);
	}

	public String getCallback() {
		return callback;
	}

	private User getUserForToken(final String messageToken) {

		List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

		attrs.add(Search.andExactProperty(User.Key.sessionId, messageToken));
		attrs.add(Search.andExactType("User"));

		try {
			// we need to search with a super user security context here..
			List<AbstractNode> results = (List<AbstractNode>) Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(null, false, false, attrs);

			if (!results.isEmpty()) {

				User user = (User) results.get(0);

				if (user != null && messageToken.equals(user.getProperty(User.Key.sessionId))) {

					return user;

				}

			}
		} catch(FrameworkException fex) {
			logger.log(Level.WARNING, "Error while executing SearchNodeCommand", fex);
		}

		return null;
	}

	public boolean isAuthenticated() {
		return token != null;
	}

	//~--- set methods ----------------------------------------------------

	public void setAuthenticated(final String token) {
		this.token = token;
	}
}
