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
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.common.RelationshipHelper;
import org.structr.web.entity.Page;
import org.structr.web.entity.html.Html;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.AbstractRelationProperty;
import org.structr.core.property.PropertyKey;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to clone a page
 *
 * @author Axel Morgner
 */
public class ClonePageCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(WrapInComponentCommand.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		// Node to wrap
		String nodeId                      = webSocketData.getId();
		final AbstractNode nodeToClone     = getNode(nodeId);
		final Map<String, Object> nodeData = webSocketData.getNodeData();
		final String newName;

		if (nodeData.containsKey(AbstractNode.name.dbName())) {

			newName = (String) nodeData.get(AbstractNode.name.dbName());
		} else {

			newName = "unknown";
		}

		if (nodeToClone != null) {

			StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					Page newPage = (Page) Services.command(securityContext,
							       CreateNodeCommand.class).execute(new NodeAttribute(AbstractNode.type, Page.class.getSimpleName()),
								       new NodeAttribute(AbstractNode.name, newName),
								       new NodeAttribute(AbstractNode.visibleToAuthenticatedUsers, true));

					if (newPage != null) {

						String pageId                      = newPage.getProperty(AbstractNode.uuid);
						List<AbstractRelationship> relsOut = nodeToClone.getOutgoingRelationships(RelType.CONTAINS);
						String originalPageId              = nodeToClone.getProperty(AbstractNode.uuid);
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

							PropertyMap relProps = new PropertyMap();
							relProps.put(new LongProperty(pageId), 0);

							try {

								Page.elements.createRelationship(securityContext, newPage, htmlNode, relProps);

							} catch (Throwable t) {

								getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);

							}

							RelationshipHelper.tagOutgoingRelsWithPageId(newPage, newPage, originalPageId, pageId);

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

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "CLONE";

	}

}
