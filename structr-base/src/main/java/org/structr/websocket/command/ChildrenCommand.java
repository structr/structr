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
import org.structr.common.PropertyView;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Group;
import org.structr.core.graph.RelationshipFactory;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

import java.util.LinkedList;
import java.util.List;

/**
 * Websocket command to return the children of the given node.
 */
public class ChildrenCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ChildrenCommand.class);

	static {

		StructrWebSocket.addCommand(ChildrenCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final RelationshipFactory factory = new RelationshipFactory(getWebSocket().getSecurityContext());
		final AbstractNode node           = getNode(webSocketData.getId());

		if (node == null) {

			return;
		}

		final List<GraphObject> result = new LinkedList();

		if (node instanceof Page page) {

			DOMNode subNode = (DOMNode) (page).treeGetFirstChild();

			while (subNode != null) {

				result.add(subNode);

				subNode = (DOMNode) subNode.getNextSibling();
			}

		} else  if (node instanceof Group group) {

			result.addAll(Iterables.toList(group.getMembers()));

		} else  if (node instanceof Content content) {

			// Content has no children

		} else {

			logger.warn("Unsupported type {} in ChildrenCommand, requested for node {} with name {}", node.getType(), node.getUuid(), node.getName());
		}

		webSocketData.setView(PropertyView.Ui);
		webSocketData.setResult(result);

		// send only over local connection
		getWebSocket().send(webSocketData, true);

	}

	@Override
	public String getCommand() {
		return "CHILDREN";
	}
}
