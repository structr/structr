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
package org.structr.websocket.command.dom;

import org.structr.common.error.FrameworkException;
import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.Map;

/**
 *
 *
 */
public class ReplaceDOMNodeCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(ReplaceDOMNodeCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final Map<String, Object> nodeData = webSocketData.getNodeData();
		final String parentId              = (String) nodeData.get("parentId");
		final String newId                 = (String) nodeData.get("newId");
		final String oldId                 = (String) nodeData.get("oldId");
		final String pageId                = webSocketData.getPageId();

		if (pageId != null) {

			// check for parent ID before creating any nodes
			if (parentId == null) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Cannot replace node without parentId").build(), true);
				return;
			}

			// check if parent node with given ID exists
			final DOMNode parentNode = getDOMNode(parentId);
			if (parentNode == null) {

				getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);
				return;
			}

			// check for old ID before creating any nodes
			if (oldId == null) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Cannot replace node without oldId").build(), true);
				return;
			}

			// check if old node with given ID exists
			final DOMNode oldNode = getDOMNode(oldId);
			if (oldNode == null) {

				getWebSocket().send(MessageBuilder.status().code(404).message("Old node not found").build(), true);
				return;
			}

			// check for new ID before creating any nodes
			if (newId == null) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Cannot replace node without newId").build(), true);
				return;
			}

			// check if new node with given ID exists
			final DOMNode newNode = getDOMNode(newId);
			if (newNode == null) {

				getWebSocket().send(MessageBuilder.status().code(404).message("New node not found").build(), true);
				return;
			}


			try {
				parentNode.replaceChild(newNode, oldNode);

			} catch (FrameworkException dex) {

				// send DOM exception
				getWebSocket().send(MessageBuilder.status().code(422).message(dex.getMessage()).build(), true);
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot replace node without pageId").build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "REPLACE_DOM_NODE";
	}

}
