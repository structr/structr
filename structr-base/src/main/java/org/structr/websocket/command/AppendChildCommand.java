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

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;

public class AppendChildCommand extends AbstractCommand {

	static {
		StructrWebSocket.addCommand(AppendChildCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final String id       = webSocketData.getId();
		final String parentId = webSocketData.getNodeDataStringValue("parentId");

		// check node to append
		if (id == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot append node, no id is given").build(), true);
			return;
		}

		// check for parent ID
		if (parentId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot add node without parentId").build(), true);
			return;
		}

		// check if parent node with given ID exists
		final AbstractNode parentNode = getNode(parentId);

		if (parentNode == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);
			return;
		}

		if (parentNode instanceof DOMNode) {

			final DOMNode parentDOMNode = getDOMNode(parentId);

			if (parentDOMNode == null) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Parent node is no DOM node").build(), true);
				return;
			}

			final DOMNode node = getDOMNode(id);

			// append node to parent
			if (node != null) {

				try {

					if (!(parentDOMNode instanceof Page)) {

						final boolean isShadowPage = (parentDOMNode.getOwnerDocument() != null && parentDOMNode.getOwnerDocument().equals(CreateComponentCommand.getOrCreateHiddenDocument()));
						final boolean isTemplate   = (parentDOMNode instanceof Template);

						if (isShadowPage && isTemplate && parentDOMNode.getParent() == null) {

							getWebSocket().send(MessageBuilder.status().code(422).message("Appending children to root-level shared component Templates is not allowed").build(), true);
							return;
						}
					}

				} catch (FrameworkException ex) {

					getWebSocket().send(MessageBuilder.status().code(422).message(ex.getMessage()).build(), true);
				}

				try {

					parentDOMNode.appendChild(node);

				} catch (DOMException dex) {

					// send DOM exception
					getWebSocket().send(MessageBuilder.status().code(422).message(dex.getMessage()).build(), true);
				}
			}

			TransactionCommand.registerNodeCallback(node, callback);

		} else {

			// send exception
			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot use given node, not instance of DOMNode").build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "APPEND_CHILD";
	}
}
