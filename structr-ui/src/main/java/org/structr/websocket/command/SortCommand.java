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

import org.neo4j.graphdb.Direction;

import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyKey;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to sort a list of nodes.
 * @author Axel Morgner
 */
public class SortCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(SortCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		String pageId                = webSocketData.getId();
		AbstractNode node            = getNode(pageId);
		Map<String, Object> nodeData = webSocketData.getNodeData();

		if (node != null) {

			for (String id : nodeData.keySet()) {

				AbstractNode nodeToSort          = getNode(id);
				Long pos                         = Long.parseLong((String) nodeData.get(id));
				List<AbstractRelationship> rels  = nodeToSort.getRelationships(RelType.CONTAINS, Direction.INCOMING);
				PropertyKey<Long> pageIdProperty = new LongProperty(pageId);

				for (AbstractRelationship rel : rels) {

					try {

						Long oldPos = rel.getLongProperty(pageIdProperty);

						if ((oldPos != null) && !(oldPos.equals(pos))) {

							rel.setProperty(pageIdProperty, pos);
						}

					} catch (FrameworkException fex) {

						fex.printStackTrace();

					}

				}

			}

		} else {

			logger.log(Level.WARNING, "Node with uuid {0} not found.", webSocketData.getId());
			getWebSocket().send(MessageBuilder.status().code(404).build(), true);

		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "SORT";

	}

}
