/**
 * Copyright (C) 2010-2014 Structr, c/o Morgner UG (haftungsbeschränkt) <structr@structr.org>
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyKey;
import org.structr.websocket.message.WebSocketMessage;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.web.entity.dom.relationship.DOMChildren;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to delete all DOM nodes which are not attached to a parent element
 * 
 * @author Axel Morgner
 */
public class DeleteUnattachedNodesCommand extends AbstractCommand {
	
	private static final Logger logger = Logger.getLogger(DeleteUnattachedNodesCommand.class.getName());

	static {

		StructrWebSocket.addCommand(DeleteUnattachedNodesCommand.class);

	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext  = getWebSocket().getSecurityContext();
		final App app                          = StructrApp.getInstance(securityContext);
		final String sortOrder                 = webSocketData.getSortOrder();
		final String sortKey                   = webSocketData.getSortKey();
		PropertyKey sortProperty               = StructrApp.getConfiguration().getPropertyKeyForJSONName(DOMNode.class, sortKey);
		final Query query                      = app.nodeQuery().sort(sortProperty).order("desc".equals(sortOrder)).includeDeletedAndHidden();

		query.orTypes(DOMElement.class);
		query.orType(Content.class);

		try {

			final List<AbstractNode> filteredResults = new LinkedList();
			List<? extends GraphObject> resultList   = query.getAsList();

			// determine which of the nodes have incoming CONTAINS relationships and are not components
			for (GraphObject obj : resultList) {

				if (obj instanceof DOMNode) {

					DOMNode node = (DOMNode) obj;
					
					Page page = (Page) node.getProperty(DOMNode.ownerDocument);

					if (!node.hasIncomingRelationships(DOMChildren.class) && !(page instanceof ShadowDocument)) {

						filteredResults.add(node);
						filteredResults.addAll(DOMNode.getAllChildNodes(node));
					}

				}

			}

			try {
				app.beginTx();
				for (NodeInterface node : filteredResults) {
					app.delete(node);
				}
				app.commitTx();

			} finally {

				app.finishTx();
			}

			
		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Exception occured", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);
		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "DELETE_UNATTACHED_NODES";

	}
	
}
