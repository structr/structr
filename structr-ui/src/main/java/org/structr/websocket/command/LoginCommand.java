/**
 * Copyright (C) 2010-2015 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.rest.auth.SessionHelper;
import org.structr.schema.action.Actions;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------
/**
 *
 *
 */
public class LoginCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(LoginCommand.class.getName());

	static {

		StructrWebSocket.addCommand(LoginCommand.class);

	}

	//~--- methods --------------------------------------------------------
	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final String username = (String) webSocketData.getNodeData().get("username");
		final String password = (String) webSocketData.getNodeData().get("password");
		Principal user;

		if ((username != null) && (password != null)) {

			try {

				StructrWebSocket socket = this.getWebSocket();
				Authenticator auth = socket.getAuthenticator();

				user = auth.doLogin(socket.getRequest(), username, password);

				if (user != null) {

					final String sessionId = webSocketData.getSessionId();
					if (sessionId == null) {

						logger.log(Level.INFO, "Could not login {0}: No sessionId found", new Object[]{username, password});
						getWebSocket().send(MessageBuilder.status().code(403).build(), true);

					}

					Actions.call(Actions.NOTIFICATION_LOGIN, user);

					// Clear possible existing sessions
					SessionHelper.clearSession(sessionId);

					user.addSessionId(sessionId);

					// store token in response data
					webSocketData.getNodeData().clear();
					webSocketData.setSessionId(sessionId);
					webSocketData.getNodeData().put("username", user.getProperty(AbstractNode.name));

					// authenticate socket
					socket.setAuthenticated(sessionId, user);

					// send data..
					socket.send(webSocketData, false);

				}

			} catch (AuthenticationException e) {

				logger.log(Level.INFO, "Could not login {0} with {1}", new Object[]{username, password});
				getWebSocket().send(MessageBuilder.status().code(403).build(), true);

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Unable to execute command", fex);
			}
		}
	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {
		return "LOGIN";
	}
}
