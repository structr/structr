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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Wrap multiple DOMNodes in a new DOM element
 *
 *
 */
public class WrapDOMNodesCommand extends CreateAndAppendDOMNodeCommand {

	private static final Logger logger = LoggerFactory.getLogger(WrapDOMNodesCommand.class.getName());

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

		final Map<String, Object> nodeData   = webSocketData.getNodeData();
		final String pageId                  = webSocketData.getPageId();
		final String tagName                 = (String) nodeData.get("tagName");
		final String nodeIdsRaw              = (String) nodeData.get("nodeIds");
		final Boolean inheritVisibilityFlags = (Boolean) nodeData.getOrDefault("inheritVisibilityFlags", false);
		final Boolean inheritGrantees        = (Boolean) nodeData.getOrDefault("inheritGrantees", false);

		// remove configuration elements from the nodeData so we don't set it on the node
		nodeData.remove("tagName");
		nodeData.remove("nodeIds");
		nodeData.remove("inheritVisibilityFlags");
		nodeData.remove("inheritGrantees");

		if (pageId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot wrap nodes without pageId").build(), true);

			return;
		}

		// guard before splitting, so a missing nodeIds returns 422 instead of an NPE
		if (nodeIdsRaw == null || nodeIdsRaw.isBlank()) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot wrap nodes without nodeIds").build(), true);

			return;
		}

		final List<String> nodeIds = Arrays.stream(nodeIdsRaw.split("\\s*,\\s*"))
				.filter(id -> !id.isEmpty())
				.toList();

		if (nodeIds.isEmpty()) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot wrap nodes without nodeIds").build(), true);

			return;
		}

		// collect the nodes; they must all exist and share one parent
		final List<DOMNode> domNodes = new LinkedList<>();
		DOMNode commonParent = null;

		for (final String nodeId : nodeIds) {

			final DOMNode node = getDOMNode(nodeId);
			if (node == null) {

				getWebSocket().send(MessageBuilder.status().code(404).message("Node " + nodeId + " not found").build(), true);

				return;
			}

			final DOMNode parent = node.getParent();
			if (parent == null) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Node " + nodeId + " has no parent node").build(), true);

				return;
			}

			if (commonParent == null) {

				commonParent = parent;

			} else if (!commonParent.getUuid().equals(parent.getUuid())) {

				getWebSocket().send(MessageBuilder.status().code(422).message("All nodes to wrap must share the same parent").build(), true);

				return;
			}

			domNodes.add(node);
		}

		final Page document = getPage(pageId);
		if (document == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Page not found").build(), true);

			return;
		}

		DOMNode newNode = CreateAndAppendDOMNodeCommand.createNewNode(getWebSocket(), tagName, document);
		if (newNode == null) {

			return;
		}

		// re-instantiate to get the correct type
		newNode = getDOMNode(newNode.getUuid());

		if (newNode == null) {

			return;
		}

		try {

			// the wrapper takes the place of the FIRST wrapped node, then every
			// wrapped node moves inside it, keeping their order
			final DOMNode firstNode = domNodes.get(0);
			commonParent.replaceChild(newNode, firstNode);

			for (final DOMNode oldNode : domNodes) {

				newNode.appendChild(oldNode);
			}

			if (inheritVisibilityFlags) {

				copyVisibilityFlags(commonParent, newNode);
			}

			if (inheritGrantees) {

				copyGrantees(commonParent, newNode);
			}

			TransactionCommand.registerNodeCallback(newNode, callback);

			// single success, after all nodes are wrapped
			getWebSocket().send(webSocketData, true);

		} catch (DOMException dex) {

			getWebSocket().send(MessageBuilder.status().code(422).message(dex.getMessage()).build(), true);
		}
	}

	@Override
	public String getCommand() {

		return "WRAP_DOM_NODES";
	}

	@Override
	public boolean requiresEnclosingTransaction() {

		return true;
	}
}
