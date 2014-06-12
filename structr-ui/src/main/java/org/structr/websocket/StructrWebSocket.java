/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.AuthHelper;
import org.structr.core.auth.Authenticator;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.web.entity.File;
import org.structr.web.entity.User;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.command.FileUploadHandler;
import org.structr.websocket.command.LoginCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------
/**
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */

public class StructrWebSocket implements WebSocketListener {

	private static final Logger logger = Logger.getLogger(StructrWebSocket.class.getName());
	private static final Map<String, Class> commandSet = new LinkedHashMap<>();

	//~--- fields ---------------------------------------------------------
	private String callback = null;
	private Session session = null;
	private String sessionId = null;
	private Gson gson = null;
	private PropertyKey idProperty = null;
	private HttpServletRequest request = null;
	private SecurityContext securityContext = null;
	private SynchronizationController syncController = null;
	private Map<String, FileUploadHandler> uploads = null;
	private Authenticator authenticator = null;
	private String pagePath = null;

	//~--- constructors ---------------------------------------------------

	public StructrWebSocket() {}

	public StructrWebSocket(final SynchronizationController syncController, final Gson gson, final PropertyKey idProperty, final Authenticator authenticator) {

		this.uploads = new LinkedHashMap<>();
		this.syncController = syncController;
		this.gson = gson;
		this.idProperty = idProperty;
		this.authenticator = authenticator;

	}

	//~--- methods --------------------------------------------------------
	public void setRequest(final HttpServletRequest request) {
		this.request = request;
	}

	@Override
	public void onWebSocketConnect(final Session session) {

		logger.log(Level.INFO, "New connection with protocol {0}", session.getProtocolVersion());

		this.session = session;

		syncController.registerClient(this);

		pagePath = request.getQueryString();

	}

	@Override
	public void onWebSocketClose(final int closeCode, final String message) {

		logger.log(Level.INFO, "Connection closed with closeCode {0} and message {1}", new Object[]{closeCode, message});

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			this.session = null;

			syncController.unregisterClient(this);

			// flush and close open uploads
			for (FileUploadHandler upload : uploads.values()) {

				upload.finish();
			}

			tx.success();
			uploads.clear();

		} catch (FrameworkException fex) {

			logger.log(Level.SEVERE, "Error while closing connection", fex);

		}


	}

	@Override
	public void onWebSocketText(final String data) {

		if (data == null) {
			logger.log(Level.WARNING, "Empty text message received.");
			return;
		}

		logger.log(Level.FINE, "############################################################ RECEIVED \n{0}", data.substring(0, Math.min(data.length(), 1000)));


		// parse web socket data from JSON
		final WebSocketMessage webSocketData = gson.fromJson(data, WebSocketMessage.class);

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			this.callback = webSocketData.getCallback();

			final String command = webSocketData.getCommand();
			final Class type = commandSet.get(command);

			final String sessionIdFromMessage = webSocketData.getSessionId();

			if (type != null) {

				if (sessionIdFromMessage != null) {

					// try to authenticated this connection by sessionId
					authenticate(sessionIdFromMessage);
				}

				// we only permit LOGIN commands if authentication based on sessionId was not successful
				if (!isAuthenticated() && !type.equals(LoginCommand.class)) {

					// send 401 Authentication Required
					send(MessageBuilder.status().code(401).message("").build(), true);

					return;
				}

				AbstractCommand abstractCommand = (AbstractCommand) type.newInstance();

				abstractCommand.setWebSocket(this);
				abstractCommand.setSession(session);
				abstractCommand.setIdProperty(idProperty);

				// store authenticated-Flag in webSocketData
				// so the command can access it
				webSocketData.setSessionValid(isAuthenticated());

				// process message
				try {

					abstractCommand.processMessage(webSocketData);

					// commit transaction
					tx.success();

				} catch (FrameworkException fex) {

					fex.printStackTrace(System.out);

					// send 400 Bad Request
					send(MessageBuilder.status().code(400).message(fex.toString()).build(), true);

				}

			} else {

				logger.log(Level.WARNING, "Unknow command {0}", command);

				// send 400 Bad Request
				send(MessageBuilder.status().code(400).message("Unknown command").build(), true);

			}

		} catch (FrameworkException | IllegalAccessException | InstantiationException t) {

			logger.log(Level.WARNING, "Unable to parse message.", t);

		}

	}

	public void send(final WebSocketMessage message, final boolean clearSessionId) {

		// return session status to client
		message.setSessionValid(isAuthenticated());

		// whether to clear the token (all command except LOGIN (for now) should absolutely do this!)
		if (clearSessionId) {

			message.setSessionId(null);
		}

		// set callback
		message.setCallback(callback);
		if (isAuthenticated() || "STATUS".equals(message.getCommand())) {

			String msg = gson.toJson(message, WebSocketMessage.class);

			logger.log(Level.FINE, "############################################################ SENDING \n{0}", msg);

			try {

				session.getRemote().sendString(msg);

			} catch (Throwable t) {
				logger.log(Level.WARNING, "Unable to send websocket message to remote client");
			}

		} else {

			logger.log(Level.WARNING, "NOT sending message to unauthenticated client.");
		}
	}

	// ----- file handling -----
	public void createFileUploadHandler(File file) {

		String uuid = file.getProperty(GraphObject.id);

		uploads.put(uuid, new FileUploadHandler(file));

	}

	public void removeFileUploadHandler(final String uuid) {

		uploads.remove(uuid);

	}

	private FileUploadHandler handleExistingFile(final String uuid) {

		FileUploadHandler newHandler = null;

		try {

			File file = (File) StructrApp.getInstance(securityContext).get(uuid);

			if (file != null) {

				newHandler = new FileUploadHandler(file);

				//uploads.put(uuid, newHandler);
			}

		} catch (FrameworkException ex) {

			logger.log(Level.WARNING, "File not found with id " + uuid, ex);

		}

		return newHandler;

	}

	public void handleFileChunk(final String uuid, final int sequenceNumber, final int chunkSize, final byte[] data, final int chunks) throws IOException {

		FileUploadHandler upload = uploads.get(uuid);

		if (upload == null) {

			upload = handleExistingFile(uuid);
		}

		if (upload != null) {

			upload.handleChunk(sequenceNumber, chunkSize, data, chunks);

		}

	}

	private void authenticate(final String sessionId) {

		Principal user = AuthHelper.getPrincipalForSessionId(sessionId);

		if (user != null) {

			this.setAuthenticated(sessionId, user);
		}

	}

	public static void addCommand(final Class command) {

		try {

			AbstractCommand msg = (AbstractCommand) command.newInstance();

			commandSet.put(msg.getCommand(), command);

		} catch (Throwable t) {

			logger.log(Level.SEVERE, "Unable to add command {0}", command.getName());

		}

	}

	public Session getSession() {

		return session;

	}

	public HttpServletRequest getRequest() {

		return request;

	}

	public Principal getCurrentUser() {

		return (securityContext == null ? null : securityContext.getUser(false));

	}

	public SecurityContext getSecurityContext() {

		return securityContext;

	}

	public String getCallback() {

		return callback;

	}

	public void setCallback(final String callback) {
		this.callback = callback;
	}

	public String getPagePath() {

		return pagePath;

	}

	public boolean isAuthenticated() {

		final Principal user = getCurrentUser();
		return (user != null && (user.getProperty(Principal.isAdmin) || user.getProperty(User.backendUser)));

	}

	public Authenticator getAuthenticator() {
		return authenticator;
	}

	//~--- set methods ----------------------------------------------------
	public void setAuthenticated(final String sessionId, final Principal user) {

		this.sessionId = sessionId;

		try {

			this.securityContext = SecurityContext.getInstance(user, AccessMode.Backend);

		} catch (FrameworkException ex) {

			logger.log(Level.WARNING, "Could not get security context instance", ex);

		}

	}

	@Override
	public void onWebSocketBinary(final byte[] bytes, int i, int i1) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void onWebSocketError(final Throwable t) {
		logger.log(Level.FINE, "Error in StructrWebSocket occured", t);
	}

}
