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

import org.neo4j.graphdb.Direction;

import org.structr.web.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Services;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyKey;
import org.structr.websocket.StructrWebSocket;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to sort a list of nodes.
 * @author Axel Morgner
 */
public class SortCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(SortCommand.class.getName());
	
	static {
		StructrWebSocket.addCommand(SortCommand.class);
	}

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final Map<String, Object> nodeData = webSocketData.getNodeData();
		final String pageId                = webSocketData.getId();
		final AbstractNode node            = getNode(pageId);

		if (node != null) {

			try {
				
				Services.command(getWebSocket().getSecurityContext(), TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						for (String id : nodeData.keySet()) {

							AbstractNode nodeToSort             = getNode(id);
							Long pos                            = Long.parseLong((String) nodeData.get(id));
							Iterable<AbstractRelationship> rels = nodeToSort.getRelationships(RelType.CONTAINS, Direction.INCOMING);
							PropertyKey<Long> pageIdProperty    = new LongProperty(pageId);

							for (AbstractRelationship rel : rels) {

								try {

									Long oldPos = rel.getProperty(pageIdProperty);

									if ((oldPos != null) && !(oldPos.equals(pos))) {

										rel.setProperty(pageIdProperty, pos);
									}

								} catch (FrameworkException fex) {

									fex.printStackTrace();

								}

							}

						}
						
						return null;
					}
					
				});
				
			} catch (Throwable t) {
				
				logger.log(Level.WARNING, "Unable to sort children", t);
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
