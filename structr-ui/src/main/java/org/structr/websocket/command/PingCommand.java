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
package org.structr.websocket.command;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.auth.AuthHelper;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket heartbeat command, keeps the websocket connection open.
 *
 * Checks validity of session id.
 *
 * @author Axel Morgner
 */
public class PingCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(PingCommand.class.getName());

	static {

		StructrWebSocket.addCommand(PingCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final String sessionId = webSocketData.getSessionId();
		logger.log(Level.FINE, "PING received from session {0}", sessionId);

		final Principal currentUser = AuthHelper.getPrincipalForSessionId(sessionId);

		if (currentUser != null) {

			getWebSocket().send(MessageBuilder.status().data("username", currentUser.getProperty(AbstractNode.name)).code(100).build(), true);

		} else {

			logger.log(Level.FINE, "Invalid session id");
			getWebSocket().send(MessageBuilder.status().code(403).build(), true);

		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "PING";
	}
}
