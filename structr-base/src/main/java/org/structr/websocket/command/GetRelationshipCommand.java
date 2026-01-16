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
package org.structr.websocket.command;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.core.graph.RelationshipInterface;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

import java.util.Arrays;

/**
 * Websocket command to retrieve a single relationship by id.
 *
 * An optional node ID should be used to improve performance in large databases,
 * depending on the driver implementation and underlying database.
 */
public class GetRelationshipCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(GetRelationshipCommand.class.getName());

	static {

		StructrWebSocket.addCommand(GetRelationshipCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final SecurityContext securityContext  = getWebSocket().getSecurityContext();

		final String nodeId             = webSocketData.getNodeDataStringValue("nodeId");
		final RelationshipInterface rel = getRelationship(webSocketData.getId(), nodeId);
		final String properties         = webSocketData.getRelDataStringValue("properties");

		if (properties != null) {
			securityContext.setCustomView(StringUtils.split(properties, ","));
		}

		if (rel != null) {

			webSocketData.setResult(Arrays.asList(rel));

			// send only over local connection (no broadcast)
			getWebSocket().send(webSocketData, true);

		} else {

			logger.warn("Relationship not found for id {}!", webSocketData.getId());
			// Not necessary to send a 404 here
			//getWebSocket().send(MessageBuilder.status().code(404).build(), true);

		}
	}

	@Override
	public String getCommand() {
		return "GET_RELATIONSHIP";
	}
}
