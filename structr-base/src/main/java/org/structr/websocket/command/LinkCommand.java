/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 * Websocket command to create a LINK relationship between a LinkSource and a Linkable.
 */
public class LinkCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(LinkCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final String sourceId          = webSocketData.getId();
		final String targetId          = webSocketData.getNodeDataStringValue("targetId");
		final NodeInterface sourceNode = getNode(sourceId);
		final NodeInterface targetNode = getNode(targetId);

		if ((sourceNode != null) && (targetNode != null)) {

			try {
				sourceNode.as(LinkSource.class).setLinkable(targetNode.as(Linkable.class));

			} catch (FrameworkException t) {

				getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("The LINK command needs id and targetId!").build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "LINK";
	}
}
