/**
 * Copyright (C) 2010-2018 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to retrieve a single graph object by id.
 *
 *
 *
 */
public class GetCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(GetCommand.class.getName());

	static {

		StructrWebSocket.addCommand(GetCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final SecurityContext securityContext  = getWebSocket().getSecurityContext();

		final String nodeId                    = (String) webSocketData.getNodeData().get("nodeId");
		final String properties                = (String) webSocketData.getNodeData().get("properties");

		if (properties != null) {
			securityContext.setCustomView(StringUtils.split(properties, ","));
		}

		final GraphObject graphObject = getGraphObject(webSocketData.getId(), nodeId);


		if (graphObject != null) {

			webSocketData.setResult(Arrays.asList(graphObject));

			// send only over local connection (no broadcast)
			getWebSocket().send(webSocketData, true);

		} else {

			//logger.warn("Node not found for id {}!", webSocketData.getId());
			// Not necessary to send a 404 here
			//getWebSocket().send(MessageBuilder.status().code(404).build(), true);

		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "GET";
	}
}
