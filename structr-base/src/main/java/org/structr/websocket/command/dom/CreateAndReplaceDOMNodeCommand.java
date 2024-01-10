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

import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import java.util.Map;

/**
 *
 *
 */
public class CreateAndReplaceDOMNodeCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(CreateAndReplaceDOMNodeCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final Map<String, Object> nodeData = webSocketData.getNodeData();
		final String parentId              = (String) nodeData.get("parentId");
		final String refId                 = (String) nodeData.get("refId");
		final String pageId                = webSocketData.getPageId();

		if (pageId != null) {

			// check for parent ID before creating any nodes
			if (parentId == null) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Cannot add node without parentId").build(), true);
				return;
			}

			// check if parent node with given ID exists
			final DOMNode parentNode = getDOMNode(parentId);
			if (parentNode == null) {

				getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);
				return;
			}

			// check for ref ID before creating any nodes
			if (refId == null) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Cannot add node without refId").build(), true);
				return;
			}

			// check if ref node with given ID exists
			final DOMNode refNode = getDOMNode(refId);
			if (refNode == null) {

				getWebSocket().send(MessageBuilder.status().code(404).message("Reference node not found").build(), true);
				return;
			}

			final Document document = getPage(pageId);
			if (document != null) {

				String tagName  = (String) nodeData.get("tagName");
				DOMNode newNode = null;

				try {

					if (tagName != null && !tagName.isEmpty()) {

						newNode = (DOMNode)document.createElement(tagName);

					} else {

						newNode = (DOMNode)document.createTextNode("");
					}

					// append new node to parent
					if (newNode != null) {

						parentNode.replaceChild(newNode, refNode);
					}

				} catch (DOMException dex) {

					// send DOM exception
					getWebSocket().send(MessageBuilder.status().code(422).message(dex.getMessage()).build(), true);
				}

			} else {

				getWebSocket().send(MessageBuilder.status().code(404).message("Page not found").build(), true);
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot create node without pageId").build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "CREATE_AND_REPLACE_DOM_NODE";
	}

}
