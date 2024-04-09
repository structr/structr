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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.entity.ContentContainer;
import org.structr.web.entity.ContentItem;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.List;

/**
 * Append a content item to a content container.
 *
 * Note that - unlike folders/files - content items can be contained in multiple containers.
 *
 */
public class AppendContentItemCommand extends AbstractCommand {

	private static final Logger logger     = LoggerFactory.getLogger(AppendContentItemCommand.class.getName());

	static {

		StructrWebSocket.addCommand(AppendContentItemCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		String id                    = webSocketData.getId();
		String parentId              = webSocketData.getNodeDataStringValue("parentId");

		// check node to append
		if (id == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot append node, no id is given").build(), true);

			return;

		}

		// check for parent ID
		if (parentId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot append node without parentId").build(), true);

			return;

		}

		// never append to self
		if (parentId.equals(id)) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot append node as its own child.").build(), true);

			return;

		}


		// check if parent node with given ID exists
		AbstractNode parentNode = getNode(parentId);

		if (parentNode == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);

			return;

		}

		if (parentNode instanceof ContentContainer) {

			final ContentContainer container = (ContentContainer) parentNode;
			final NodeInterface node         = (NodeInterface) getNode(id);

			if (node != null) {

				try {

					if (node instanceof ContentItem) {

						final ContentItem item = (ContentItem) node;

						final List<ContentItem> items = Iterables.toList(container.getItems());

						items.add(item);

						container.setProperty(StructrApp.key(ContentContainer.class, "items"), items);

					} else if (node instanceof ContentContainer) {

						final ContentContainer child = (ContentContainer) node;

						child.setProperty(StructrApp.key(ContentContainer.class, "parent"), container);

					} else {

						// send exception
						getWebSocket().send(MessageBuilder.status().code(422).message("Given object is not of type ContentItem or ContentContainer").build(), true);
						return;
					}

					TransactionCommand.registerNodeCallback(node, callback);

				} catch (FrameworkException ex) {
					logger.error("", ex);
					getWebSocket().send(MessageBuilder.status().code(422).message("Cannot append content item").build(), true);
				}
			}

		} else {

			// send exception
			getWebSocket().send(MessageBuilder.status().code(422).message("Parent node is not instance of ContentContainer").build(), true);
		}
	}

	@Override
	public String getCommand() {

		return "APPEND_CONTENT_ITEM";

	}

}
