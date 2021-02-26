/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.websocket.command.dom;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Template;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

public class CreateAndInsertRelativeToDOMNodeCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(CreateAndInsertRelativeToDOMNodeCommand.class.getName());

	enum RelativePosition { Before, After }

	static {

		StructrWebSocket.addCommand(CreateAndInsertRelativeToDOMNodeCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final Map<String, Object> nodeData   = webSocketData.getNodeData();
		final String pageId                  = webSocketData.getPageId();
		final String nodeId                  = (String) nodeData.remove("nodeId");
		final Boolean inheritVisibilityFlags = (Boolean) nodeData.remove("inheritVisibilityFlags");
		final String tagName                 = (String) nodeData.remove("tagName");
		final String relativePosition        = (String) nodeData.remove("relativePosition");
		final RelativePosition position;

		try {

			position = RelativePosition.valueOf(relativePosition);

		} catch(IllegalArgumentException iae) {

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

		final Document document = getPage(pageId);

		if (document == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Page not found").build(), true);
			return;
		}

		final DOMNode parentNode = (DOMNode) refNode.getParentNode();

		if (parentNode == null) {
			getWebSocket().send(MessageBuilder.status().code(404).message("Node has no parent node").build(), true);
			return;
		}

		try {

			DOMNode newNode;

			if ("comment".equals(tagName)) {

				newNode = (DOMNode) document.createComment("#comment");

			} else if ("template".equals(tagName)) {

				newNode = (DOMNode) document.createTextNode("#template");

				try {

					newNode.unlockSystemPropertiesOnce();
					newNode.setProperties(newNode.getSecurityContext(), new PropertyMap(NodeInterface.type, Template.class.getSimpleName()));

				} catch (FrameworkException fex) {

					logger.warn("Unable to set type of node {} to Template: {}", new Object[] { newNode.getUuid(), fex.getMessage() } );
				}

			} else if ("content".equals(tagName)) {

				newNode = (DOMNode) document.createTextNode("#text");

			} else {

				newNode = (DOMNode) document.createElement(tagName);
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

					PropertyMap visibilityFlags = new PropertyMap();
					visibilityFlags.put(DOMNode.visibleToAuthenticatedUsers, parentNode.getProperty(DOMNode.visibleToAuthenticatedUsers));
					visibilityFlags.put(DOMNode.visibleToPublicUsers, parentNode.getProperty(DOMNode.visibleToPublicUsers));

					try {
						newNode.setProperties(newNode.getSecurityContext(), visibilityFlags);
					} catch (FrameworkException fex) {

						logger.warn("Unable to inherit visibility flags for node {} from parent node {}", newNode, parentNode);
					}
				}
			}

		} catch (DOMException dex) {

			// send DOM exception
			getWebSocket().send(MessageBuilder.status().code(422).message(dex.getMessage()).build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "CREATE_AND_INSERT_RELATIVE_TO_DOM_NODE";
	}
}
