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
import org.structr.common.error.PasswordPolicyViolationException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 *
 */
public class CreateCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(CreateCommand.class.getName());

	static {

		StructrWebSocket.addCommand(CreateCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app = StructrApp.getInstance(securityContext);

		try {

			final PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, webSocketData.getNodeData());
			Class type                   = SchemaHelper.getEntityClassForRawType(properties.get(AbstractNode.type));
			final NodeInterface newNode  = app.create(type, properties);

			TransactionCommand.registerNodeCallback(newNode, callback);

			// check for File node and store in WebSocket to receive chunks
			if (newNode instanceof File) {

				getWebSocket().createFileUploadHandler((File) newNode);

			}

		} catch (PasswordPolicyViolationException ppve) {

			logger.warn("Could not create node: {}", ppve.getMessage());
			getWebSocket().send(MessageBuilder.status().code(ppve.getStatus()).message(ppve.toString()).build(), true);

		} catch (FrameworkException fex) {

			logger.warn("Could not create node.", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

		}
	}

	@Override
	public String getCommand() {
		return "CREATE";
	}
}
