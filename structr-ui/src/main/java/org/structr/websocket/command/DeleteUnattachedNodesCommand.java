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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.property.PropertyKey;
import org.structr.websocket.message.WebSocketMessage;
import org.structr.web.common.RelType;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.graph.DeleteNodeCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.ShadowDocument;

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
		List<SearchAttribute> searchAttributes = new LinkedList();

		// Search for all DOM elements and Contents
		searchAttributes.add(Search.orExactTypeAndSubtypes(DOMElement.class));
		searchAttributes.add(Search.orExactType(Content.class));

		final String sortOrder   = webSocketData.getSortOrder();
		final String sortKey     = webSocketData.getSortKey();
		PropertyKey sortProperty = EntityContext.getPropertyKeyForJSONName(DOMNode.class, sortKey);

		try {

			// do search
			Result result = (Result) Services.command(securityContext, SearchNodeCommand.class).execute(true, false, searchAttributes, sortProperty, "desc".equals(sortOrder));
			final List<AbstractNode> filteredResults	= new LinkedList();
			List<? extends GraphObject> resultList		= result.getResults();

			// determine which of the nodes have incoming CONTAINS relationships and are not components
			for (GraphObject obj : resultList) {

				if (obj instanceof DOMNode) {

					DOMNode node = (DOMNode) obj;
					
					Page page = (Page) node.getProperty(DOMNode.ownerDocument);

					if (!node.hasRelationship(RelType.CONTAINS, Direction.INCOMING) && !(page instanceof ShadowDocument)) {

						filteredResults.add(node);
						filteredResults.addAll(DOMNode.getAllChildNodes(node));
					}

				}

			}

			// set full result list
			final DeleteNodeCommand deleteNode = Services.command(securityContext, DeleteNodeCommand.class);
			try {

				StructrTransaction transaction = new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						for (AbstractNode node : filteredResults) {
							
							deleteNode.execute(node, true);
						}

						return null;
					}

				};

				Services.command(securityContext, TransactionCommand.class).execute(transaction);

			} catch (Throwable t) {

				getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);

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
