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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import java.util.Map;

/**
 * Wrap a DOMNode in a new DOM element
 *
 *
 */
public class WrapDOMNodeCommand extends CreateAndAppendDOMNodeCommand {

	private static final Logger logger = LoggerFactory.getLogger(WrapDOMNodeCommand.class.getName());

	static {

		StructrWebSocket.addCommand(WrapDOMNodeCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

		final Map<String, Object> nodeData   = webSocketData.getNodeData();
		final String pageId                  = webSocketData.getPageId();
		final String tagName                 = (String) nodeData.get("tagName");
		final String nodeId                  = (String) nodeData.get("nodeId");
		final Boolean inheritVisibilityFlags = (Boolean) nodeData.getOrDefault("inheritVisibilityFlags", false);
		final Boolean inheritGrantees        = (Boolean) nodeData.getOrDefault("inheritGrantees", false);

		// remove configuration elements from the nodeData so we don't set it on the node
		nodeData.remove("tagName");
		nodeData.remove("nodeId");
		nodeData.remove("inheritVisibilityFlags");
		nodeData.remove("inheritGrantees");

		if (pageId != null) {

			// check for parent ID before creating any nodes
			if (nodeId == null) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Cannot wrap node without nodeId").build(), true);
				return;
			}

			// check if content node with given ID exists
			final DOMNode oldNode = getDOMNode(nodeId);
			if (oldNode == null) {

				getWebSocket().send(MessageBuilder.status().code(404).message("Node not found").build(), true);
				return;
			}

			final Page document = getPage(pageId);
			if (document != null) {

				final DOMNode parentNode = oldNode.getParent();

				if (parentNode == null) {
					getWebSocket().send(MessageBuilder.status().code(404).message("Node has no parent node").build(), true);
					return;
				}

				try {

					DOMNode newNode = CreateAndAppendDOMNodeCommand.createNewNode(getWebSocket(), tagName, document);
					if (newNode == null) {
						return;
					}

					// Instantiate node again to get correct class
					newNode = getDOMNode(newNode.getUuid());

					// append new node to parent
					if (newNode != null) {

						parentNode.replaceChild(newNode, oldNode);

						newNode.appendChild(oldNode);

						if (inheritVisibilityFlags) {

							copyVisibilityFlags(parentNode, newNode);
						}

						if (inheritGrantees) {

							copyGrantees(parentNode, newNode);
						}
					}

				} catch (DOMException dex) {

					// send DOM exception
					getWebSocket().send(MessageBuilder.status().code(422).message(dex.getMessage()).build(), true);
				}

			} else {

				getWebSocket().send(MessageBuilder.status().code(404).message("Page not found").build(), true);
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot wrap node without pageId").build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "WRAP_DOM_NODE";
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return true;
	}
}
