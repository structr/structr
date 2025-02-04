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


import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;


/**
 * Create a shared component as a clone of the source node.
 * This command will create a SYNC relationship: (source)<-[:SYNC]-(component)
 */
public class CreateComponentCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(CreateComponentCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final String id = webSocketData.getId();
		if (id != null) {

			final DOMNode node = getDOMNode(id);

			try {

				final DOMNode clonedNode = create(node);

				TransactionCommand.registerNodeCallback(clonedNode, callback);

			} catch (DOMException | FrameworkException ex) {

				// send DOM exception
				getWebSocket().send(MessageBuilder.status().code(422).message(ex.getMessage()).build(), true);
			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot append node without id").build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "CREATE_COMPONENT";
	}

	public DOMNode create(final DOMNode node) throws FrameworkException {

		if (node == null) {
			throw new FrameworkException(422, "No node to clone");
		}
		
		final DOMNode clonedNode = node.cloneNode(false);

		// Child nodes of a template must stay in page tree
		if (!(clonedNode.is("Template"))) {

			moveChildNodes(node, clonedNode);
		}

		final ShadowDocument hiddenDoc = CreateComponentCommand.getOrCreateHiddenDocument();
		clonedNode.setOwnerDocument(hiddenDoc);

		// Change page (owner document) of all children recursively
		for (final NodeInterface child : clonedNode.getAllChildNodes()) {

			child.as(DOMNode.class).setOwnerDocument(hiddenDoc);
		}

		node.setSharedComponent(clonedNode);

		return clonedNode;
	}
}
