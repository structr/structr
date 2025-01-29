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

import org.structr.common.error.FrameworkException;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;
import org.w3c.dom.DOMException;

import java.util.Arrays;

public class GetOrCreateShadowPageCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(GetOrCreateShadowPageCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		try {

			final ShadowDocument hiddenDoc = CreateComponentCommand.getOrCreateHiddenDocument();

			webSocketData.setResult(Arrays.asList(hiddenDoc));

			// send only over local connection (no broadcast)
			getWebSocket().send(webSocketData, true);

		} catch (DOMException | FrameworkException ex) {

			getWebSocket().send(MessageBuilder.status().code(422).message(ex.getMessage()).build(), true);
		}
	}

	@Override
	public String getCommand() {

		return "GET_OR_CREATE_SHADOW_PAGE";
	}
}
