/*
 *  Copyright (C) 2011 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.websocket.command;

import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to return the children of the given node
 * in the context of the given resourceId
 *
 * @author Axel Morgner
 */
public class ChildrenCommand extends AbstractCommand {

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		String resourceId               = (String) webSocketData.getNodeData().get("resourceId");
		AbstractNode node               = getNode(webSocketData.getId());
		List<AbstractRelationship> rels = node.getOutgoingRelationships(RelType.CONTAINS);
		Map<Long, GraphObject> sortMap  = new TreeMap<Long, GraphObject>();

		for (AbstractRelationship out : rels) {

			Long pos = null;

			if (out.getLongProperty(resourceId) != null) {

				pos = out.getLongProperty(resourceId);

			} else {

				// Try "*"
				pos = out.getLongProperty("*");
			}

			if (pos != null) {

				AbstractNode endNode = out.getEndNode();
				sortMap.put(pos, endNode);

			}

		}

		List<GraphObject> result = new ArrayList<GraphObject>(sortMap.values());

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
