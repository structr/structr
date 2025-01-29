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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.web.entity.User;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

public class SaveLocalStorageCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(SaveLocalStorageCommand.class.getName());

	static {

		StructrWebSocket.addCommand(SaveLocalStorageCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		final String localStorageString = webSocketData.getNodeDataStringValue("localStorageString");

		if (StringUtils.isNotBlank(localStorageString)) {

			try {

				final User me = (User) securityContext.getUser(false);
				me.setLocalStorage(localStorageString);

				getWebSocket().send(MessageBuilder.forName(getCommand()).callback(webSocketData.getCallback()).build(), true);

			} catch (Throwable t) {

				logger.warn("Error saving localstorage", t);

				// send exception
				getWebSocket().send(MessageBuilder.status().code(422).message(t.toString()).build(), true);
			}

		} else {

			// send exception
			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot save localStorage").build(), true);
		}
	}

	@Override
	public String getCommand() {

		return "SAVE_LOCAL_STORAGE";
	}
}
