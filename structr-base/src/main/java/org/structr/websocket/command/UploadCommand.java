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
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;
import org.structr.web.common.FileHelper;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 * Websocket command for uploading files.
 *
 * This command expects a file name and a base64-encoded string.
 */
public class UploadCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(UploadCommand.class.getName());

	static {

		StructrWebSocket.addCommand(UploadCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(true);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		try {

			final String name      = webSocketData.getNodeDataStringValue("name");
			final String rawData   = webSocketData.getNodeDataStringValue("fileData");

			final NodeInterface newFile = FileHelper.createFileBase64(securityContext, rawData, null);

			newFile.setProperties(securityContext, new PropertyMap(newFile.getTraits().key("name"), name));

		} catch (Throwable t) {

			String msg = t.toString();

			// return error message
			getWebSocket().send(MessageBuilder.status().code(400).message("Could not upload file: ".concat((msg != null) ? msg : "")).build(), true);

		}
	}

	@Override
	public String getCommand() {
		return "UPLOAD";
	}
}
