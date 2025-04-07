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
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.traits.Traits;
import org.structr.rest.resource.SchemaResource;
import org.structr.rest.resource.SchemaTypeResource;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 * Websocket command to retrieve type information from the schema.
 */
public class GetSchemaInfoCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(GetSchemaInfoCommand.class.getName());

	static {

		StructrWebSocket.addCommand(GetSchemaInfoCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		try {

			final String type = webSocketData.getNodeDataStringValue("type");
			if (type != null) {

				webSocketData.setResult(SchemaTypeResource.getSchemaTypeResult(getWebSocket().getSecurityContext(), type, PropertyView.All));

			} else {

				webSocketData.setResult(SchemaResource.getSchemaOverviewResult());
			}

			// send only over local connection (no broadcast)
			getWebSocket().send(webSocketData, true);

		} catch (FrameworkException ex) {

			logger.error("", ex);
			getWebSocket().send(MessageBuilder.status().code(500).build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "GET_SCHEMA_INFO";
	}
}
