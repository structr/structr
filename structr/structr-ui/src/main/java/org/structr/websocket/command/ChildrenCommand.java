/*
 *  Copyright (C) 2010-2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.websocket.command;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Folder;
import org.structr.web.common.RelationshipHelper;
import org.structr.web.entity.Component;
import org.structr.web.entity.Content;
import org.structr.web.entity.Group;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyKey;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to return the children of the given node
 * in the context of the given pageId
 *
 * @author Axel Morgner
 */
public class ChildrenCommand extends AbstractCommand {

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		AbstractNode node = getNode(webSocketData.getId());

		if (node == null) {

			return;
		}

		String pageId                    = (String) webSocketData.getNodeData().get("pageId");
		String componentId               = (String) webSocketData.getNodeData().get("componentId");
		List<AbstractRelationship> rels  = node.getOutgoingRelationships(RelType.CONTAINS);
		Map<Long, GraphObject> sortMap   = new TreeMap<Long, GraphObject>();
		Set<String> nodesWithChildren    = new HashSet<String>();
		List<GraphObject> result         = new LinkedList<GraphObject>();
		PropertyKey<Long> pageIdProperty = new LongProperty(pageId);

		for (AbstractRelationship rel : rels) {

			AbstractNode endNode = rel.getEndNode();

			if (endNode == null) {

				continue;
			}

			if ((node instanceof Group) || (node instanceof Folder)) {

				result.add(endNode);
				nodesWithChildren.addAll(RelationshipHelper.getChildrenInPage(endNode, null));

				continue;

			}

			if (pageId == null) {

				return;
			}

			Long pos = null;

			if (rel.getLongProperty(pageIdProperty) != null) {

				pos = rel.getLongProperty(pageIdProperty);
				
			} else {

				// Try "*"
				pos = rel.getLongProperty(new LongProperty("*"));
			}

			String relCompId             = rel.getProperty(Component.componentId);
			boolean isComponentOrContent = ((endNode instanceof Component) || (endNode instanceof Content));

			if (pos == null || (isComponentOrContent && relCompId != null && !relCompId.equals(componentId))) {

				continue;
			}

			nodesWithChildren.addAll(RelationshipHelper.getChildrenInPage(endNode, pageId));
			sortMap.put(pos, endNode);

		}

		if (!sortMap.isEmpty()) {

			result = new ArrayList<GraphObject>(sortMap.values());
		}

		webSocketData.setView(PropertyView.Ui);
		webSocketData.setResult(result);
		webSocketData.setNodesWithChildren(nodesWithChildren);

		// send only over local connection
		getWebSocket().send(webSocketData, true);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "CHILDREN";

	}

}
