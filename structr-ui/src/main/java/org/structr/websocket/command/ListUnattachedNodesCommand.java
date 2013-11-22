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
import org.structr.common.PagingHelper;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.relationship.DOMChildren;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to retrieve DOM nodes which are not attached to a parent element
 * 
 * @author Axel Morgner
 */
public class ListUnattachedNodesCommand extends AbstractCommand {
	
	private static final Logger logger = Logger.getLogger(ListUnattachedNodesCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ListUnattachedNodesCommand.class);

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
		final int pageSize       = webSocketData.getPageSize();
		final int page           = webSocketData.getPage();
		PropertyKey sortProperty = EntityContext.getPropertyKeyForJSONName(DOMNode.class, sortKey);

		try {

			// do search
			Result result = (Result) Services.command(securityContext, SearchNodeCommand.class).execute(true, false, searchAttributes, sortProperty, "desc".equals(sortOrder));
			List<AbstractNode> filteredResults     = new LinkedList();
			List<? extends GraphObject> resultList = result.getResults();

			// determine which of the nodes have no incoming CONTAINS relationships and no page id
			for (GraphObject obj : resultList) {

				if (obj instanceof AbstractNode) {

					AbstractNode node = (AbstractNode) obj;

					if (!node.hasIncomingRelationships(DOMChildren.class) && node.getProperty(DOMNode.ownerDocument) == null) {

						filteredResults.add(node);
					}

				}

			}

			// save raw result count
			int resultCountBeforePaging = filteredResults.size();
			
			// set full result list
			webSocketData.setResult(PagingHelper.subList(filteredResults, pageSize, page, null));
			webSocketData.setRawResultCount(resultCountBeforePaging);

			// send only over local connection
			getWebSocket().send(webSocketData, true);
			
		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Exception occured", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "LIST_UNATTACHED_NODES";

	}

}
