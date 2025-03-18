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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 * Websocket command to create a simple HTML page.
 */
public class CreateSimplePage extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(CreateSimplePage.class.getName());

	static {

		StructrWebSocket.addCommand(CreateSimplePage.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final String pageName                 = webSocketData.getNodeDataStringValue("name");
		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		try {

			final Page page = Page.createSimplePage(securityContext, pageName);

			TransactionCommand.registerNodeCallback(page, callback);

		} catch (FrameworkException fex) {

			logger.warn("Could not create node.", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.toString()).build(), true);
		}
	}

	@Override
	public String getCommand() {

		return "CREATE_SIMPLE_PAGE";
	}
}
