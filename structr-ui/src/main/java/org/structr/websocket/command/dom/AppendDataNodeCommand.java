/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.websocket.command.dom;

import java.util.Map;
import org.structr.common.error.FrameworkException;

import org.structr.core.entity.AbstractNode;
import org.structr.web.entity.DataNode;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;


/**
 *
 * @author Axel Morgner
 */
public class AppendDataNodeCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(AppendDataNodeCommand.class);
	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		String id                    = webSocketData.getId();
		Map<String, Object> nodeData = webSocketData.getNodeData();
		String parentId              = (String) nodeData.get("parentId");
		String key                   = (String) nodeData.get("key");

		// check node to append
		if (id == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot append node, no id is given").build(), true);

			return;

		}

		// check for parent ID
		if (parentId == null) {

			getWebSocket().send(MessageBuilder.status().code(422).message("Cannot add node without parentId").build(), true);

			return;

		}

		// check if parent node with given ID exists
		AbstractNode parentNode = getNode(parentId);

		if (parentNode == null) {

			getWebSocket().send(MessageBuilder.status().code(404).message("Parent node not found").build(), true);

			return;

		}

		if (parentNode instanceof DataNode) {

			DataNode parentDataNode = (DataNode)parentNode;
			if (parentDataNode == null) {

				getWebSocket().send(MessageBuilder.status().code(422).message("Parent node is no data node").build(), true);

				return;

			}

			DataNode node = (DataNode) getNode(id);

			try {

				// append node to parent
				if (node != null) {

					parentDataNode.appendChild(key, node);
				}
				
			} catch (FrameworkException fex) {

				// send DOM exception
				getWebSocket().send(MessageBuilder.status().code(422).message(fex.getMessage()).build(), true);
			}

		} else {
			
			// send DOM exception
			getWebSocket().send(MessageBuilder.status().code(422).message("Please use APPEND_DATA for data nodes.").build(), true);
		}
	}

	@Override
	public String getCommand() {

		return "APPEND_DATA";
	}
}
