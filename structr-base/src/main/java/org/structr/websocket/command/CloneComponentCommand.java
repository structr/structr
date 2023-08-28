/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.dom.RelativePosition;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;

/**
 * Create a node as clone of a component.
 *
 * This command will create a SYNC relationship: (component)-[:SYNC]->(target)
 */
public class CloneComponentCommand extends AbstractCommand {

	static {
		StructrWebSocket.addCommand(CloneComponentCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final String id                  = webSocketData.getId();
		final String parentId            = webSocketData.getNodeDataStringValue("parentId");
		final String refId               = webSocketData.getNodeDataStringValue("refId");
		final String relativePosition    = webSocketData.getNodeDataStringValue("relativePosition");
		final RelativePosition position;

		if (relativePosition != null) {

			try {

				position = RelativePosition.valueOf(relativePosition);

			} catch (final IllegalArgumentException iae) {

				// default to Before
				getWebSocket().send(MessageBuilder.status().code(422).message("Unsupported relative position: " + relativePosition).build(), true);
				return;
			}

		} else {

			position = RelativePosition.Before;
		}

		// check node to append
		if (id == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot clone component, no id is given").build(), true);
			return;
		}

		// check for parent ID
		if (parentId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot clone component node without parentId").build(), true);
			return;
		}

		// check if parent node with given ID exists
		final DOMNode parentNode = (DOMNode) getNode(parentId);

		if (parentNode == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);
			return;
		}

		final DOMNode node = getDOMNode(id);

		try {

			final DOMNode clonedNode = cloneComponent(node, parentNode);
			final DOMNode refNode    = (refId != null) ? getDOMNode(refId) : null;

			if (refNode != null) {

				parentNode.insertBefore(clonedNode, refNode);

				if (RelativePosition.Before.equals(position)) {

					parentNode.insertBefore(clonedNode, refNode);

				} else {

					final DOMNode nextNode = refNode.getNextSibling();

					if (nextNode != null) {

						parentNode.insertBefore(clonedNode, nextNode);

					} else {

						parentNode.appendChild(clonedNode);
					}
				}
			}

			TransactionCommand.registerNodeCallback(node, callback);

		} catch (DOMException | FrameworkException ex) {

			// send DOM exception
			getWebSocket().send(MessageBuilder.status().code(422).message(ex.getMessage()).build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "CLONE_COMPONENT";
	}

	public static DOMNode cloneComponent(final DOMNode node, final DOMNode parentNode) throws FrameworkException {

		final DOMNode clonedNode = (DOMNode) node.cloneNode(false);

		parentNode.appendChild(clonedNode);

		final PropertyMap changedProperties = new PropertyMap();

		changedProperties.put(StructrApp.key(DOMNode.class, "sharedComponent"), node);
		changedProperties.put(StructrApp.key(DOMNode.class, "ownerDocument"), (parentNode instanceof Page ? (Page) parentNode : parentNode.getOwnerDocument()));

		clonedNode.setProperties(clonedNode.getSecurityContext(), changedProperties);

		return clonedNode;
	}
}
