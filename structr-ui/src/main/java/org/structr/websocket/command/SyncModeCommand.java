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

import org.structr.common.SecurityContext;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.Map;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.web.entity.dom.DOMNode;

import org.structr.web.entity.relation.Sync;
import org.structr.websocket.StructrWebSocket;

/**
 *
 *
 */
public class SyncModeCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(SyncModeCommand.class);
	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		String sourceId                       = webSocketData.getId();
		Map<String, Object> properties        = webSocketData.getNodeData();
		String targetId                       = (String) properties.get("targetId");
		final String syncMode                 = (String) properties.get("syncMode");
		final DOMNode sourceNode              = (DOMNode) getNode(sourceId);
		final DOMNode targetNode              = (DOMNode) getNode(targetId);
		final App app                         = StructrApp.getInstance(securityContext);

		if ((sourceNode != null) && (targetNode != null)) {

			try {

				app.create(sourceNode, targetNode, Sync.class);

				if (syncMode.equals("bidir")) {

					app.create(targetNode, sourceNode, Sync.class);
				}

			} catch (Throwable t) {

				getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);

			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("The SYNC_MODE command needs id and targetId!").build(), true);
		}

	}

	@Override
	public String getCommand() {

		return "SYNC_MODE";

	}

}
