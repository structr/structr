/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedException;
import org.structr.core.Services;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.rest.auth.SessionHelper;
import org.structr.rest.service.HttpService;
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

	private static final Logger logger = LoggerFactory.getLogger(LoginCommand.class.getName());

	static {

		StructrWebSocket.addCommand(LoginCommand.class);

	}

	//~--- methods --------------------------------------------------------
	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final String username = (String) webSocketData.getNodeData().get("username");
		final String password = (String) webSocketData.getNodeData().get("password");
		Principal user;

		if ((username != null) && (password != null)) {

			try {

				StructrWebSocket socket = this.getWebSocket();
				Authenticator auth = socket.getAuthenticator();

				user = auth.doLogin(socket.getRequest(), username, password);

				if (user != null) {

					String sessionId = webSocketData.getSessionId();
					if (sessionId == null) {
						
						logger.info("Unable to login {}: No sessionId found", new Object[]{ username, password });
						getWebSocket().send(MessageBuilder.status().code(403).build(), true);

						return;

					}

					sessionId = Services.getInstance().getService(HttpService.class).getSessionCache().getSessionHandler().getSessionIdManager().getId(sessionId);

					try {
						Actions.call(Actions.NOTIFICATION_LOGIN, user);

					} catch (UnlicensedException ex) {
						ex.log(logger);
					}

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

				logger.info("Unable to login {}, probably wrong password", username);
				getWebSocket().send(MessageBuilder.status().code(403).build(), true);

			} catch (FrameworkException fex) {

				logger.warn("Unable to execute command", fex);
			}
		}
	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {
		return "LOGIN";
	}
}
