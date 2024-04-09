/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.TransactionCommand;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.auth.SessionHelper;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 * Websocket heartbeat command, keeps the websocket connection open.
 *
 * Checks validity of session id.
 */
public class PingCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(PingCommand.class.getName());

	static {

		StructrWebSocket.addCommand(PingCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final String sessionId = webSocketData.getSessionId();
		logger.debug("PING received from session {}", sessionId);

		final Principal currentUser = AuthHelper.getPrincipalForSessionId(SessionHelper.getShortSessionId(sessionId), true);

		if (currentUser != null) {

			logger.debug("User found by session id: " + currentUser.getName());

			getWebSocket().send(MessageBuilder.status()
				.callback(webSocketData.getCallback())
				.data("username", currentUser.getProperty(AbstractNode.name))
				.data("isAdmin", currentUser.isAdmin())
				.code(100).build(), true);

		} else {

			logger.debug("Invalid session id");
			getWebSocket().send(MessageBuilder.status().code(401).build(), true);

		}
	}

	@Override
	public String getCommand() {
		return "PING";
	}
}
