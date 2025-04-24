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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.Map;

public class CreateAndInsertRelativeToDOMNodeCommand extends CreateAndAppendDOMNodeCommand {

	static {
		StructrWebSocket.addCommand(CreateAndInsertRelativeToDOMNodeCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final Map<String, Object> nodeData   = webSocketData.getNodeData();
		final String pageId                  = webSocketData.getPageId();
		final String tagName                 = (String) nodeData.remove("tagName");
		final String childContent            = (String) nodeData.get("childContent");
		final String nodeId                  = (String) nodeData.remove("nodeId");
		final Boolean inheritVisibilityFlags = (Boolean) nodeData.getOrDefault("inheritVisibilityFlags", false);
		final Boolean inheritGrantees        = (Boolean) nodeData.getOrDefault("inheritGrantees", false);
		final String relativePosition        = (String) nodeData.remove("relativePosition");
		final RelativePosition position;

		// remove configuration elements from the nodeData so we don't set it on the node
		nodeData.remove("childContent");
		nodeData.remove("inheritVisibilityFlags");
		nodeData.remove("inheritGrantees");

		try {

			position = RelativePosition.valueOf(relativePosition);

		} catch (final IllegalArgumentException iae) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Unsupported relative position: " + relativePosition).build(), true);
			return;
		}

		if (pageId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot insert node without pageId").build(), true);
			return;
		}

		if (nodeId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot insert node without a reference nodeId").build(), true);
			return;
		}

		if (tagName == null || tagName.isEmpty()) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Cannot create node without tagname").build(), true);
			return;
		}

		// check if content node with given ID exists
		final DOMNode refNode = getDOMNode(nodeId);
		if (refNode == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Node not found").build(), true);
			return;
		}

		final Page document = getPage(pageId);
		if (document == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Page not found").build(), true);
			return;
		}

		final DOMNode parentNode = refNode.getParent();
		if (parentNode == null) {
			getWebSocket().send(MessageBuilder.status().code(404).message("Node has no parent node").build(), true);
			return;
		}

		try {

			DOMNode newNode = CreateAndAppendDOMNodeCommand.createNewNode(getWebSocket(), tagName, document);
			if (newNode == null) {
				return;
			}

			newNode = getDOMNode(newNode.getUuid());

			if (newNode != null) {

				if (RelativePosition.Before.equals(position)) {

					parentNode.insertBefore(newNode, refNode);

				} else {

					final DOMNode nextNode = refNode.getNextSibling();

					if (nextNode != null) {

						parentNode.insertBefore(newNode, nextNode);

					} else {

						parentNode.appendChild(newNode);
					}
				}

				if (inheritVisibilityFlags) {

					copyVisibilityFlags(parentNode, newNode);
				}

				if (inheritGrantees) {

					copyGrantees(parentNode, newNode);
				}

				// create a child text node if content is given
				if (StringUtils.isNotBlank(childContent)) {

					final DOMNode childNode = document.createTextNode(childContent);

					newNode.appendChild(childNode);

					if (inheritVisibilityFlags) {

						copyVisibilityFlags(parentNode, childNode);
					}

					if (inheritGrantees) {

						copyGrantees(parentNode, childNode);
					}
				}
			}

		} catch (FrameworkException fex) {

			// send DOM exception
			getWebSocket().send(MessageBuilder.status().code(422).message(fex.getMessage()).build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "CREATE_AND_INSERT_RELATIVE_TO_DOM_NODE";
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return true;
	}
}
