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

import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.RelationClass;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.web.entity.Component;
import org.structr.web.entity.Resource;
import org.structr.web.entity.html.Html;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to clone a resource
 *
 * @author Axel Morgner
 */
public class CloneResourceCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(WrapInComponentCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		// Node to wrap
		String nodeId                  = webSocketData.getId();
		final AbstractNode nodeToClone = getNode(nodeId);

		if (nodeToClone != null) {

			StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					Resource newResource = (Resource) Services.command(securityContext,
								       CreateNodeCommand.class).execute(new NodeAttribute(AbstractNode.Key.type.name(),
									       Resource.class.getSimpleName()));

					if (newResource != null) {
						
						String resourceId = newResource.getStringProperty(AbstractNode.Key.uuid);

						List<AbstractRelationship> relsOut = nodeToClone.getOutgoingRelationships(RelType.CONTAINS);
						String originalResourceId          = nodeToClone.getStringProperty(AbstractNode.Key.uuid);
						Html htmlNode                      = null;

						for (AbstractRelationship out : relsOut) {

							// Use first HTML element of existing node (the node to be cloned)
							AbstractNode endNode = out.getEndNode();

							if (endNode.getType().equals(Html.class.getSimpleName())) {

								htmlNode = (Html) endNode;
								break;

							}
						}

						if (htmlNode != null) {

							RelationClass rel = EntityContext.getRelationClass(newResource.getClass(), htmlNode.getClass());

							if (rel != null) {

								Map<String, Object> relProps = new LinkedHashMap<String, Object>();

								relProps.put(resourceId, 0);
								relProps.put("resourceId", resourceId);

								try {
									rel.createRelationship(securityContext, newResource, htmlNode, relProps);
								} catch (Throwable t) {

									getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(),
											    true);
								}

								tagOutgoingRelsWithResourceId(newResource, newResource, originalResourceId, resourceId);
							}

						}

					} else {

						getWebSocket().send(MessageBuilder.status().code(404).build(), true);

					}

					return null;
				}
			};

			try {
				Services.command(securityContext, TransactionCommand.class).execute(transaction);
			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Could not create node.", fex);
				getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);
			}

		} else {

			logger.log(Level.WARNING, "Node with uuid {0} not found.", webSocketData.getId());
			getWebSocket().send(MessageBuilder.status().code(404).build(), true);

		}
	}

	private void tagOutgoingRelsWithResourceId(final AbstractNode startNode, final AbstractNode node, final String originalResourceId, final String resourceId) throws FrameworkException {

		for (AbstractRelationship rel : node.getRelationships(RelType.CONTAINS, Direction.OUTGOING)) {

			Long position = rel.getLongProperty(originalResourceId);
			if(position != null) {
				rel.setProperty(resourceId, position);
			}

			tagOutgoingRelsWithResourceId(startNode, rel.getEndNode(), originalResourceId, resourceId);

		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "CLONE";
	}
}
