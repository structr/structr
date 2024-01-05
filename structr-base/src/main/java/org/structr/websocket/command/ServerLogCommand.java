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
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.function.ServerLogFunction;
import org.structr.core.property.StringProperty;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Websocket command to retrieve a snapshot of the server log.
 */
public class ServerLogCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ServerLogCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ServerLogCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		int numberOfLines      = webSocketData.getNodeDataIntegerValue("numberOfLines") != null ? webSocketData.getNodeDataIntegerValue("numberOfLines") : 20;
		int truncateLinesAfter = webSocketData.getNodeDataIntegerValue("truncateLinesAfter") != null ? webSocketData.getNodeDataIntegerValue("truncateLinesAfter") : -1;

		final String log = ServerLogFunction.getServerLog(numberOfLines, truncateLinesAfter);

		try {

			GraphObjectMap result = new GraphObjectMap();
			result.setProperty(new StringProperty("result"), log);

			List<GraphObject> resultList = new ArrayList();

			resultList.add(result);
			webSocketData.setResult(resultList);

			getWebSocket().send(webSocketData, true);

		} catch (FrameworkException fex) {

			logger.warn("Exception occured", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public String getCommand() {
		return "SERVER_LOG";
	}

}
