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



package org.structr.websocket.command;

import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.auth.AuthenticationException;
import org.structr.core.auth.Authenticator;
import org.structr.core.auth.AuthenticatorCommand;
import org.structr.core.entity.User;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner, Axel Morgner
 */
public class LoginCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(LoginCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public boolean processMessage(WebSocketMessage webSocketData) {

		String username = webSocketData.getData().get("username");
		String password = webSocketData.getData().get("password");
		User user       = null;

		if ((username != null) && (password != null)) {

			try {

				StructrWebSocket socket = this.getWebSocket();

				Authenticator auth = (Authenticator) Services.command(SecurityContext.getSuperUserInstance(), AuthenticatorCommand.class).execute(socket.getConfig());

				user = auth.doLogin(socket.getRequest(), username, password);

			} catch (AuthenticationException e) {
				logger.log(Level.INFO, "Could not login {0} with {1}", new Object[] { username, password });
			}

			if (user != null) {

				String token = StructrWebSocket.secureRandomString();

				// store token in user
				user.setProperty(User.Key.sessionId, token);

				// store token in response data
				webSocketData.getData().clear();
				webSocketData.setToken(token);

				// authenticate socket
				this.getWebSocket().setAuthenticated(token);

				// send data..
				this.getWebSocket().send(getConnection(), webSocketData, false);

			}


		}

		// do NOT broadcast
		return false;
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "LOGIN";
	}
}
