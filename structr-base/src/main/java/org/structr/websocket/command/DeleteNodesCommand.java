/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.graph.Tx;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

import java.util.List;

/**
 * Websocket command to delete multiple nodes in one transaction.
 */
public class DeleteNodesCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(DeleteNodesCommand.class.getName());

	static {

		StructrWebSocket.addCommand(DeleteNodesCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		final Boolean      recursive = webSocketData.getNodeDataBooleanValue("recursive");
		final List<String> nodeIds   = webSocketData.getNodeDataStringList("nodeIds");

		if (nodeIds != null) {

			final App app = StructrApp.getInstance(securityContext);

			try (final Tx tx = app.tx()) {

				for (final String id : nodeIds) {

					DeleteNodeCommand.deleteNode(getWebSocket(), app.getNodeById(id), recursive);
				}

				tx.success();

			} catch (FrameworkException ex) {
				logger.warn("", ex);
			}
		}
	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {
		return "DELETE_NODES";
	}
}
