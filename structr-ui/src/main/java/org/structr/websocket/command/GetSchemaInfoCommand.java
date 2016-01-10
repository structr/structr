/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.rest.resource.SchemaResource;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------
/**
 * Websocket command to retrieve type information from the schema.
 *
 *
 */
public class GetSchemaInfoCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(GetSchemaInfoCommand.class.getName());

	static {

		StructrWebSocket.addCommand(GetSchemaInfoCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		try {

			webSocketData.setResult(SchemaResource.getSchemaOverviewResult().getResults());

			// send only over local connection (no broadcast)
			getWebSocket().send(webSocketData, true);

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, null, ex);
			getWebSocket().send(MessageBuilder.status().code(500).build(), true);
		}
	}

	//~--- get methods ----------------------------------------------------
	@Override
	public String getCommand() {
		return "GET_SCHEMA_INFO";
	}
}
