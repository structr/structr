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
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.RelationClass;
import org.structr.core.entity.RelationClass.Cardinality;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class LinkCommand extends AbstractCommand {

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		// create static relationship
		String sourceId                = webSocketData.getId();
		Map<String, Object> properties = webSocketData.getData();
		String resourceId              = (String) properties.get("resourceId");
//		String rootResourceId          = (String) properties.get("rootResourceId");
//
//		if (rootResourceId == null) {
//
//			rootResourceId = "*";
//
//		}

		Integer startOffset = Integer.parseInt((String) properties.get("startOffset"));
		Integer endOffset   = Integer.parseInt((String) properties.get("endOffset"));

		properties.remove("id");

		if ((sourceId != null) && (resourceId != null)) {

			AbstractNode sourceNode   = getNode(sourceId);
			AbstractNode resourceNode = getNode(resourceId);
			AbstractNode firstNode;
			AbstractNode secondNode;
			AbstractNode thirdNode;

			if ((sourceNode != null) && (resourceNode != null)) {

				try {

					Command transactionCommand = Services.command(securityContext, TransactionCommand.class);

					// As an element can be contained in multiple resources, we have
					// to do the following for each incoming CONTAINS relationship
					List<AbstractRelationship> relsIn = sourceNode.getIncomingRelationships();

					for (AbstractRelationship relIn : relsIn) {

						if (relIn.getType().equals(RelType.CONTAINS.name())) {

							Map<String, Object> sourceRelationshipProperties = relIn.getProperties();

							for (String resourceIdFromRel : sourceRelationshipProperties.keySet()) {

								// Create first (leading) node
								final List<NodeAttribute> attrsFirstNode = new LinkedList<NodeAttribute>();

								attrsFirstNode.add(new NodeAttribute(AbstractNode.Key.type.name(), "Content"));
								attrsFirstNode.add(new NodeAttribute(AbstractNode.Key.name.name(), "First Node"));

								if (sourceNode.getType().equals("Content")) {

									String content = sourceNode.getStringProperty("content");

									attrsFirstNode.add(new NodeAttribute("content", content.substring(0, startOffset)));

								}

								StructrTransaction transaction = new StructrTransaction() {

									@Override
									public Object execute() throws FrameworkException {
										return Services.command(securityContext, CreateNodeCommand.class).execute(attrsFirstNode);
									}
								};

								firstNode = (AbstractNode) transactionCommand.execute(transaction);

								// Create second (linked) node
								final List<NodeAttribute> attrsSecondNode = new LinkedList<NodeAttribute>();

								attrsSecondNode.add(new NodeAttribute(AbstractNode.Key.type.name(), "Content"));
								attrsSecondNode.add(new NodeAttribute(AbstractNode.Key.name.name(), "Second (Link) Node"));

								if (sourceNode.getType().equals("Content")) {

									String content = sourceNode.getStringProperty("content");

									attrsSecondNode.add(new NodeAttribute("content", content.substring(startOffset, endOffset)));

								}

								transaction = new StructrTransaction() {

									@Override
									public Object execute() throws FrameworkException {
										return Services.command(securityContext, CreateNodeCommand.class).execute(attrsSecondNode);
									}
								};
								secondNode = (AbstractNode) transactionCommand.execute(transaction);

								// Create third (trailing) node
								final List<NodeAttribute> attrsThirdNode = new LinkedList<NodeAttribute>();

								attrsThirdNode.add(new NodeAttribute(AbstractNode.Key.type.name(), "Content"));
								attrsThirdNode.add(new NodeAttribute(AbstractNode.Key.name.name(), "Third Node"));

								if (sourceNode.getType().equals("Content")) {

									String content = sourceNode.getStringProperty("content");

									attrsThirdNode.add(new NodeAttribute("content", content.substring(endOffset, content.length())));

								}

								transaction = new StructrTransaction() {

									@Override
									public Object execute() throws FrameworkException {
										return Services.command(securityContext, CreateNodeCommand.class).execute(attrsThirdNode);
									}
								};
								thirdNode = (AbstractNode) transactionCommand.execute(transaction);

								// Create a CONTAINS relationship
								RelationClass rel                      = new RelationClass(null, RelType.CONTAINS, Direction.OUTGOING, Cardinality.ManyToMany, null, RelationClass.DELETE_NONE);
								Map<String, Object> newRelationshipProperties = new HashMap<String, Object>();

								newRelationshipProperties.put(resourceIdFromRel, 0);
								rel.createRelationship(securityContext, sourceNode, firstNode, newRelationshipProperties);
								newRelationshipProperties.put(resourceIdFromRel, 1);
								rel.createRelationship(securityContext, sourceNode, secondNode, newRelationshipProperties);
								newRelationshipProperties.put(resourceIdFromRel, 2);
								rel.createRelationship(securityContext, sourceNode, thirdNode, newRelationshipProperties);

								// Create a LINK relationship
								rel = new RelationClass(resourceNode.getClass(), RelType.LINK, Direction.OUTGOING, Cardinality.ManyToMany, null, RelationClass.DELETE_NONE);

								rel.createRelationship(securityContext, secondNode, resourceNode);
							}

						}

					}

					sourceNode.setType("Element");
					sourceNode.removeProperty("content");

				} catch (Throwable t) {
					getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);
				}

			} else {

				getWebSocket().send(MessageBuilder.status().code(404).build(), true);

			}

		} else {

			getWebSocket().send(MessageBuilder.status().code(400).message("The LINK command needs id and data.id!").build(), true);

		}
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {
		return "LINK";
	}
}
