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

import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import org.structr.websocket.StructrWebSocket;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to return the children of the given node
 *
 * @author Axel Morgner
 */
public class ChildrenCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(ChildrenCommand.class);

	}
	
	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		AbstractNode node = getNode(webSocketData.getId());

		if (node == null) {

			return;
		}

		List<AbstractRelationship> rels   = node.getOutgoingRelationships(RelType.CONTAINS);
		List<GraphObject> result          = new LinkedList<GraphObject>();

		for (AbstractRelationship rel : rels) {

			AbstractNode endNode = rel.getEndNode();
			if (endNode == null) {

				continue;
			}

			result.add(endNode);

		}

		webSocketData.setView(PropertyView.Ui);
		webSocketData.setResult(result);

		// send only over local connection
		getWebSocket().send(webSocketData, true);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "CHILDREN";

	}

}
