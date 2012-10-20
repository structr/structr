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
import org.structr.web.common.RelationshipHelper;
import org.structr.web.entity.Content;
import org.structr.web.entity.Page;
import org.structr.web.entity.html.Html;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.Property;
import org.structr.common.PropertyKey;

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
		String nodeToAddId                 = (String) nodeData.get("id");
		String childContent                = (String) nodeData.get("childContent");
		final Map<String, Object> relData  = webSocketData.getRelData();
		String parentId                    = webSocketData.getId();
		boolean newNodeCreated             = false;

		if (parentId != null) {

			AbstractNode nodeToAdd  = null;
			AbstractNode parentNode = getNode(parentId);

			if (nodeToAddId != null) {

				nodeToAdd = getNode(nodeToAddId);
			} else {

				StructrTransaction transaction = new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						return Services.command(securityContext, CreateNodeCommand.class).execute(nodeData);

					}

				};

				try {

					// create node in transaction
					nodeToAdd      = (AbstractNode) Services.command(securityContext, TransactionCommand.class).execute(transaction);
					newNodeCreated = true;
				} catch (FrameworkException fex) {

					logger.log(Level.WARNING, "Could not create node.", fex);
					getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

				}

			}

			if ((nodeToAdd != null) && (parentNode != null)) {

				String originalPageId                    = (String) nodeData.get("sourcePageId");
				String newPageId                         = (String) nodeData.get("targetPageId");
				RelationClass rel                        = EntityContext.getRelationClass(parentNode.getClass(), nodeToAdd.getClass());
				PropertyKey<Long> originalPageIdProperty = new Property<Long>(originalPageId);
				PropertyKey<Long> newPageIdProperty      = new Property<Long>(newPageId);

				if (rel != null) {

					try {

						AbstractRelationship existingRel = null;
						long maxPos                      = 0;

						// Search for an existing relationship between the node to add and the parent
						for (AbstractRelationship r : parentNode.getOutgoingRelationships(RelType.CONTAINS)) {

							if (r.getEndNode().equals(nodeToAdd) && r.getLongProperty(originalPageIdProperty) != null) {

								existingRel = r;

//                                                              r.setProperty(newPageId, Long.parseLong((String) relData.get(newPageId)));
//                                                              logger.log(Level.INFO, "Tagging relationship with pageId {0} and position {1}", new Object[] { newPageId, relData.get(newPageId) });
//
//                                                              addedPageIdProperty = true;

							}

							Long pos = r.getLongProperty(newPageIdProperty);

							if (pos != null) {

								maxPos = Math.max(pos, maxPos);
							}

						}

						if (existingRel != null) {

							existingRel.setProperty(newPageIdProperty, Long.parseLong((String) relData.get(newPageId)));
							logger.log(Level.INFO, "Tagging relationship with pageId {0} and position {1}", new Object[] { newPageId, maxPos + 1 });

						} else {

							// Debugging hook: Alert when parentNode is a page!
							if (parentNode instanceof Page && !(nodeToAdd instanceof Html)) {

								logger.log(Level.SEVERE, "Trying to add non Html node to Page!");
							}

							// A new node was created, no relationship exists,
							// so we create a new one.
							// overwrite with new position
							relData.put(newPageId, maxPos + 1);
							rel.createRelationship(securityContext, parentNode, nodeToAdd, relData);
							logger.log(Level.INFO, "Created new relationship between parent node {0}, added node {1} ({2})", new Object[] { parentNode.getUuid(),
								nodeToAdd.getUuid(), relData });
						}

						// set page ID on copied branch
						if ((originalPageId != null) && (newPageId != null) && !originalPageId.equals(newPageId)) {

							logger.log(Level.INFO, "Tagging branch of added node {0}: originalPageId: {1}, newPageId: {2}", new Object[] { nodeToAdd.getUuid(),
								originalPageId, newPageId });
							RelationshipHelper.tagOutgoingRelsWithPageId(nodeToAdd, nodeToAdd, originalPageId, newPageId);

						}

					} catch (Throwable t) {

						getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);

					}

				}

				// If text for a content child node is given, create and link a content node
				if (childContent != null) {

					Content contentNode             = null;
					final List<NodeAttribute> attrs = new LinkedList<NodeAttribute>();

					attrs.add(new NodeAttribute(Content.content, childContent));
					attrs.add(new NodeAttribute(Content.contentType, "text/plain"));
					attrs.add(new NodeAttribute(AbstractNode.type, Content.class.getSimpleName()));

					StructrTransaction transaction = new StructrTransaction() {

						@Override
						public Object execute() throws FrameworkException {

							return Services.command(securityContext, CreateNodeCommand.class).execute(attrs);

						}

					};

					try {

						// create content node in transaction
						contentNode = (Content) Services.command(securityContext, TransactionCommand.class).execute(transaction);
					} catch (FrameworkException fex) {

						logger.log(Level.WARNING, "Could not create content child node.", fex);
						getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

					}

					if (contentNode != null) {

						try {

							// New content node is at position 0!!
							relData.put(newPageId, 0L);
							rel.createRelationship(securityContext, nodeToAdd, contentNode, relData);

							// set page ID on copied branch
							if ((originalPageId != null) && (newPageId != null) && !originalPageId.equals(newPageId)) {

								RelationshipHelper.tagOutgoingRelsWithPageId(contentNode, contentNode, originalPageId, newPageId);
							}
						} catch (Throwable t) {

							getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);

						}

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

}
