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
import org.structr.core.app.StructrApp;
import org.structr.core.graph.RelationshipInterface;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

public class DeleteRelationshipCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(DeleteRelationshipCommand.class.getName());

	static {

		StructrWebSocket.addCommand(DeleteRelationshipCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final String nodeId                   = webSocketData.getNodeDataStringValueTrimmed("nodeId");
		final RelationshipInterface obj       = getRelationship(webSocketData.getId(), nodeId);

		if (obj != null) {

			StructrApp.getInstance(securityContext).delete(obj);
		}
	}

	@Override
	public String getCommand() {
		return "DELETE_RELATIONSHIP";
	}
}
