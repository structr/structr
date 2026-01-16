/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.message.WebSocketMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Websocket command to return the children of the given DOM node
 *
 */
public class DOMNodeChildrenCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(DOMNodeChildrenCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(false);

		final String id    = webSocketData.getId();
		final DOMNode node = getDOMNode(id);

		if (node == null) {

			return;
		}

		prefetch(webSocketData.getId());

		final List<GraphObject> result = new LinkedList<>();

		for (final DOMNode currentNode : node.getChildren()) {

			prefetch(currentNode.getUuid());

			result.add(currentNode);
		}

		webSocketData.setView(PropertyView.All);
		webSocketData.setResult(result);

		// send only over local connection
		getWebSocket().send(webSocketData, true);
	}

	@Override
	public String getCommand() {
		return "DOM_NODE_CHILDREN";
	}

	private void prefetch(final String uuid) {

		TransactionCommand.getCurrentTransaction().prefetch("(n:NodeInterface:DOMNode { id: \"" + uuid + "\" })-[r]-(m)",

			Set.of(
				"all/OUTGOING/CONTAINS",
				"all/OUTGOING/CONTAINS_NEXT_SIBLING",
				"all/OUTGOING/SUCCESS_TARGET",
				"all/OUTGOING/FAILURE_TARGET",
				"all/OUTGOING/SUCCESS_NOTIFICATION_ELEMENT",
				"all/OUTGOING/FAILURE_NOTIFICATION_ELEMENT",
				"all/OUTGOING/RELOADS",
				"all/OUTGOING/FLOW",
				"all/OUTGOING/INPUT_ELEMENT",
				"all/OUTGOING/OWNS",
				"all/OUTGOING/PARAMETER",
				"all/OUTGOING/SECURITY",
				"all/OUTGOING/SYNC",
				"all/OUTGOING/PAGE",
				"all/OUTGOING/TRIGGERED_BY"
			),

			Set.of(

				"all/INCOMING/CONTAINS",
				"all/INCOMING/CONTAINS_NEXT_SIBLING",
				"all/INCOMING/SUCCESS_TARGET",
				"all/INCOMING/FAILURE_TARGET",
				"all/INCOMING/SUCCESS_NOTIFICATION_ELEMENT",
				"all/INCOMING/FAILURE_NOTIFICATION_ELEMENT",
				"all/INCOMING/RELOADS",
				"all/INCOMING/FLOW",
				"all/INCOMING/INPUT_ELEMENT",
				"all/INCOMING/OWNS",
				"all/INCOMING/PARAMETER",
				"all/INCOMING/SECURITY",
				"all/INCOMING/SYNC",
				"all/INCOMING/PAGE",
				"all/INCOMING/TRIGGERED_BY"
			)
		);
	}
}
