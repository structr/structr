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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.web.entity.dom.DOMNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.LinkedList;
import java.util.List;

/**
 * Websocket command to delete all DOM nodes which are not attached to a parent
 * element.
 */
public class DeleteUnattachedNodesCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(DeleteUnattachedNodesCommand.class.getName());

	static {

		StructrWebSocket.addCommand(DeleteUnattachedNodesCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

		final SecurityContext securityContext     = getWebSocket().getSecurityContext();
		final App app                             = StructrApp.getInstance(securityContext);
		final List<NodeInterface> filteredResults = new LinkedList<>();

		try (final Tx tx = app.tx(true, false, false)) {

			// Get all top nodes, use method from list command
			final List<NodeInterface> topNodes = ListUnattachedNodesCommand.getUnattachedNodes(app, securityContext, webSocketData);

			// Loop through all top nodes and collect all their child nodes
			for (final NodeInterface topNode : topNodes) {

				filteredResults.add(topNode);
				filteredResults.addAll(DOMNode.getAllChildNodes((DOMNode) topNode));
			}

			for (final NodeInterface node : filteredResults) {
				app.delete(node);
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("Exception occured", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

		}
	}

	@Override
	public String getCommand() {
		return "DELETE_UNATTACHED_NODES";
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}
}
