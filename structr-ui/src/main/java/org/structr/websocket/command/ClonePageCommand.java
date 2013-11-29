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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.Html;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.CreateRelationshipCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.dom.relationship.DOMChildren;
import org.structr.websocket.StructrWebSocket;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to clone a page
 *
 * @author Axel Morgner
 */
public class ClonePageCommand extends AbstractCommand {

	private static final Logger logger = Logger.getLogger(ClonePageCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ClonePageCommand.class);

	}
	
	//~--- methods --------------------------------------------------------

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final App app = StructrApp.getInstance(securityContext);

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

			try {

				app.beginTx();
				
				Page newPage = (Page) StructrApp.getInstance(securityContext).command(
						       CreateNodeCommand.class).execute(new NodeAttribute(AbstractNode.type, Page.class.getSimpleName()),
							       new NodeAttribute(AbstractNode.name, newName),
							       new NodeAttribute(AbstractNode.visibleToAuthenticatedUsers, true));

				if (newPage != null) {

					String pageId                     = newPage.getProperty(AbstractNode.id);
					Iterable<DOMChildren> relsOut = nodeToClone.getOutgoingRelationships(DOMChildren.class);
					Html htmlNode                     = null;

					for (DOMChildren out : relsOut) {

						// Use first HTML element of existing node (the node to be cloned)
						NodeInterface endNode = out.getTargetNode();

						if (endNode.getType().equals(Html.class.getSimpleName())) {

							htmlNode = (Html) endNode;

							break;

						}
					}

					if (htmlNode != null) {

						PropertyMap relProps = new PropertyMap();
						relProps.put(new LongProperty(pageId), 0L);

						try {

							StructrApp.getInstance(securityContext).command(CreateRelationshipCommand.class).execute(newPage, htmlNode, DOMChildren.class, relProps);
							// DOMElement.children.createRelationship(securityContext, newPage, htmlNode, relProps);

						} catch (Throwable t) {

							getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);

						}

					}

				} else {

					getWebSocket().send(MessageBuilder.status().code(404).build(), true);
				}
				
				app.commitTx();

			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Could not create node.", fex);
				getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

			} finally {
				
				app.finishTx();
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
