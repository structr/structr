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

import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

/**
 * Wrap a content node in a new DOM element
 *
 *
 */
public class WrapContentCommand extends AbstractCommand {

	//private static final Logger logger = LoggerFactory.getLogger(WrapContentCommand.class.getName());

	static {

		StructrWebSocket.addCommand(WrapContentCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final String parentId              = webSocketData.getNodeDataStringValue("parentId");
		final String pageId                = webSocketData.getPageId();

		webSocketData.getNodeData().remove("parentId");

		if (pageId != null) {

			// check for parent ID before creating any nodes
			if (parentId == null) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Cannot add node without parentId").build(), true);
				return;
			}

			// check if content node with given ID exists
			final DOMNode contentNode = getDOMNode(parentId);
			if (contentNode == null) {

				getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);
				return;
			}

			final Document document = getPage(pageId);
			if (document != null) {

				final String tagName  = webSocketData.getNodeDataStringValue("tagName");
				webSocketData.getNodeData().remove("tagName");

				final DOMNode parentNode = (DOMNode) contentNode.getParentNode();


				try {

					DOMNode elementNode = null;
					if (tagName != null && !tagName.isEmpty()) {

						elementNode = (DOMNode) document.createElement(tagName);

					}

					// append new node to parent parent node
					if (elementNode != null) {

						parentNode.appendChild(elementNode);

					}

					// append new node to parent parent node
					if (elementNode != null) {

						// append content node to new node
						elementNode.appendChild(contentNode);

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
		return "WRAP_CONTENT";
	}

}
