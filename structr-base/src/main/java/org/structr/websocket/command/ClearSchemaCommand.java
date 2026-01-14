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

import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.InvalidSchemaException;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

public class ClearSchemaCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ClearSchemaCommand.class.getName());

	static {
		StructrWebSocket.addCommand(ClearSchemaCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(false);

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			logger.info("Clearing schema");

			StructrSchema.replaceDatabaseSchema(app, StructrSchema.createEmptySchema());

			tx.success();

			getWebSocket().send(MessageBuilder.finished().callback(callback).data("success", true).build(), true);

		} catch (InvalidSchemaException | URISyntaxException ex) {

			getWebSocket().send(MessageBuilder.status().callback(callback).data("success", false).build(), true);

			throw new FrameworkException(422, ex.getMessage());
		}
	}

	@Override
	public String getCommand() {
		return "CLEAR_SCHEMA";
	}
}
