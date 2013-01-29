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



package org.structr.websocket.command;

import java.util.LinkedList;
import java.util.List;
import org.structr.common.PropertyView;
import org.structr.websocket.message.WebSocketMessage;


import org.structr.web.entity.DataNode;
import org.structr.websocket.StructrWebSocket;

/**
 * Websocket command to return the parent of the given data node
 *
 * @author Axel Morgner
 */
public class DataNodeParentCommand extends AbstractCommand {
	
	static {
		
		StructrWebSocket.addCommand(DataNodeParentCommand.class);

	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		DataNode node = (DataNode) getNode(webSocketData.getId());
		List<DataNode> result = new LinkedList();
		String key    = (String) webSocketData.getNodeData().get("key");

		if (node == null || key == null) {

			return;
		}

		DataNode currentNode = (DataNode) node.getParent(key);
		if (currentNode != null) {
			result.add(currentNode);
		}
		
		webSocketData.setView(PropertyView.Ui);
		webSocketData.setResult(result);

		// send only over local connection
		getWebSocket().send(webSocketData, true);

	}

	@Override
	public String getCommand() {

		return "DATA_NODE_PARENT";

	}

}
