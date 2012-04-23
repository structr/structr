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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.RelationClass;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractRelationship;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class AddCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(AddCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		// create static relationship
		final Map<String, Object> nodeData = webSocketData.getNodeData();
		String containedNodeId             = (String) nodeData.get("id");
		final Map<String, Object> relData  = webSocketData.getRelData();
		String containingNodeId            = webSocketData.getId();

		if (containingNodeId != null) {

			AbstractNode containedNode = null;
			AbstractNode containingNode = getNode(containingNodeId);

			if (containedNodeId != null) {

				containedNode = getNode(containedNodeId);

			} else {

				StructrTransaction transaction = new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {
						return Services.command(securityContext, CreateNodeCommand.class).execute(nodeData);
					}
				};

				try {

					// create node in transaction
					containedNode = (AbstractNode) Services.command(securityContext, TransactionCommand.class).execute(transaction);
				} catch (FrameworkException fex) {

					logger.log(Level.WARNING, "Could not create node.", fex);
					getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);
				}

			}

			if ((containedNode != null) && (containingNode != null)) {
				
				String originalResourceId = (String)nodeData.get("sourceResourceId");
				String newResourceId      = (String)nodeData.get("targetResourceId");
				
				RelationClass rel = EntityContext.getRelationClass(containingNode.getClass(), containedNode.getClass());

				if (rel != null) {

					try {
						rel.createRelationship(securityContext, containingNode, containedNode, relData);
						
						// set resource ID on copied branch
						if(originalResourceId != null && newResourceId != null && !originalResourceId.equals(newResourceId)) {
							tagOutgoingRelsWithResourceId(containedNode, containedNode, originalResourceId, newResourceId);
						}
						
					} catch (Throwable t) {
						getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);
					}

				}
				

			} else {

				getWebSocket().send(MessageBuilder.status().code(404).build(), true);

			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("Add needs id and data.id!").build(), true);

		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "ADD";
	}

	public static void tagOutgoingRelsWithResourceId(final AbstractNode startNode, final AbstractNode node, final String originalResourceId, final String resourceId) throws FrameworkException {

		for (AbstractRelationship rel : node.getRelationships(RelType.CONTAINS, Direction.OUTGOING)) {

			Long position = rel.getLongProperty(originalResourceId);
			if(position != null) {
				rel.setProperty(resourceId, position);
			}

			tagOutgoingRelsWithResourceId(startNode, rel.getEndNode(), originalResourceId, resourceId);

		}
	}
}
