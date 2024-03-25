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

import org.structr.api.graph.Direction;
import org.structr.common.PropertyView;
import org.structr.core.GraphObject;
import org.structr.core.IterableAdapter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;
import org.structr.web.common.RelType;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.structr.core.graph.TransactionCommand;

/**
 * Websocket command to return the children of the given node.
 */
public class ChildrenCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(ChildrenCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		TransactionCommand.getCurrentTransaction().prefetch("DOMElement", null, Set.of());

		final RelationshipFactory factory = new RelationshipFactory(getWebSocket().getSecurityContext());
		final AbstractNode node           = getNode(webSocketData.getId());

		if (node == null) {

			return;
		}

		final List<GraphObject> result = new LinkedList();

		if (node instanceof Page) {

			DOMNode subNode = (DOMNode) ((Page) node).treeGetFirstChild();

			while (subNode != null) {

				result.add(subNode);

				subNode = (DOMNode) subNode.getNextSibling();
			}

		} else {

			final Iterable<RelationshipInterface> rels = new IterableAdapter<>(node.getNode().getRelationships(Direction.OUTGOING, RelType.CONTAINS), factory);

			for (RelationshipInterface rel : rels) {

				NodeInterface endNode = rel.getTargetNode();
				if (endNode == null) {

					continue;
				}

				result.add(endNode);
			}
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
