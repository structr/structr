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

import org.apache.commons.lang.StringUtils;

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
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.web.common.RelationshipHelper;
import org.structr.web.entity.Component;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to wrap an existing node in a (new) component node
 * @author Axel Morgner
 */
public class UnwrapCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(UnwrapCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		// Node to wrap
		String nodeId                      = webSocketData.getId();
		final AbstractNode nodeToWrap      = getNode(nodeId);
		final Map<String, Object> nodeData = webSocketData.getNodeData();
		final Map<String, Object> relData  = webSocketData.getRelData();
		final String resourceId            = (String) relData.get("resourceId");
		final String parentId              = (String) nodeData.get("parentId");
		final AbstractNode parentNode      = getNode(parentId);
		final Long position                = Long.parseLong((String) relData.get(resourceId));

		if (nodeToWrap != null) {

			StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					Component newComponent = (Component) Services.command(securityContext, CreateNodeCommand.class).execute(nodeData);
					String componentId     = newComponent.getStringProperty(AbstractNode.Key.uuid);

					RelationshipHelper.moveIncomingRelationships(securityContext, nodeToWrap, newComponent, RelType.CONTAINS, resourceId,
						newComponent.getStringProperty(AbstractNode.Key.uuid), position);

					if ((parentNode != null) && (newComponent != null)) {

						RelationClass rel = EntityContext.getRelationClass(parentNode.getClass(), newComponent.getClass());

						if (rel != null) {

							// First element in new component, so set position to 0
							Map<String, Object> relProps = new LinkedHashMap<String, Object>();

							relProps.put(resourceId, 0);
							relProps.put("resourceId", resourceId);
							relProps.put("componentId", componentId);

							try {
								rel.createRelationship(securityContext, newComponent, nodeToWrap, relProps);
							} catch (Throwable t) {
								getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);
							}

							tagOutgoingRelsWithComponentId(newComponent, newComponent, componentId);
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

	private void tagOutgoingRelsWithComponentId(final AbstractNode startNode, final AbstractNode node, final String componentId) throws FrameworkException {

		for (AbstractRelationship rel : node.getRelationships(RelType.CONTAINS, Direction.OUTGOING)) {

			if (!(startNode.equals(node))) {

				rel.setProperty("componentId", componentId);

				if (node.getType().equals(Component.class.getSimpleName())) {

					return;

				}

			}

			tagOutgoingRelsWithComponentId(startNode, rel.getEndNode(), componentId);

		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "WRAP";
	}
}
