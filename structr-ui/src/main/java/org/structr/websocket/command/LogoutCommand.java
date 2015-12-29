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

import org.structr.common.error.FrameworkException;
import org.structr.core.auth.AuthHelper;
import org.structr.core.entity.Principal;
import org.structr.schema.action.Actions;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 *
 */
public class LogoutCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(LogoutCommand.class);

	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) throws FrameworkException {

		final Principal user = getWebSocket().getCurrentUser();

		if (user != null) {

			final String sessionId = webSocketData.getSessionId();
			if (sessionId != null) {

				AuthHelper.clearSession(sessionId);

			}

			Actions.call(Actions.NOTIFICATION_LOGOUT, user);

			getWebSocket().setAuthenticated(null, null);

			getWebSocket().send(MessageBuilder.status().code(401).build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "LOGOUT";
	}

}
