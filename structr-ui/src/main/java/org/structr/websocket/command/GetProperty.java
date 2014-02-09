/**
 * Copyright (C) 2010-2014 Structr, c/o Morgner UG (haftungsbeschränkt) <structr@structr.org>
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class GetProperty extends AbstractCommand {
	
	static {
		
		StructrWebSocket.addCommand(GetProperty.class);
		
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		AbstractNode node = getNode(webSocketData.getId());
		String key        = (String) webSocketData.getNodeData().get("key");

		if (node != null) {

			webSocketData.setNodeData(key, node.getProperty(StructrApp.getConfiguration().getPropertyKeyForJSONName(node.getClass(), key)));

			// send only over local connection (no broadcast)
			getWebSocket().send(webSocketData, true);

		} else {

			getWebSocket().send(MessageBuilder.status().code(404).build(), true);

		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "GET_PROPERTY";
	}
}
