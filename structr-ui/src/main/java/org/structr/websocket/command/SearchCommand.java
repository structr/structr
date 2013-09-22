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
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.property.PropertyKey;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class SearchCommand extends AbstractCommand {
	
	private static final Logger logger = Logger.getLogger(SearchCommand.class.getName());

	static {

		StructrWebSocket.addCommand(SearchCommand.class);

	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext  = getWebSocket().getSecurityContext();
		String searchString                    = (String) webSocketData.getNodeData().get("searchString");
		String typeString                      = (String) webSocketData.getNodeData().get("type");
		
		Class type = null;
		if (typeString != null) {
			type = EntityContext.getEntityClassForRawType(typeString);
		}

		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		
		if (searchString == null) {
			getWebSocket().send(MessageBuilder.status().code(204).message("Empty search string").build(), true);
			return;
		}

		searchAttributes.add(Search.andName(searchString));
		if (type != null) {
			
			searchAttributes.add(Search.andExactType(type));
			
		}
		

		final String sortOrder   = webSocketData.getSortOrder();
		final String sortKey     = webSocketData.getSortKey();
//		final int pageSize       = webSocketData.getPageSize();
//		final int page           = webSocketData.getPage();
		PropertyKey sortProperty = EntityContext.getPropertyKeyForJSONName(AbstractNode.class, sortKey);

		try {

			// do search
			Result result = (Result) Services.command(securityContext, SearchNodeCommand.class).execute(true, false, searchAttributes, sortProperty, "desc".equals(sortOrder));
//			List<AbstractNode> filteredResults     = new LinkedList<AbstractNode>();
//			List<? extends GraphObject> resultList = result.getResults();
//
//			// determine which of the nodes have children
//			for (GraphObject obj : resultList) {
//
//				if (obj instanceof AbstractNode) {
//
//					AbstractNode node = (AbstractNode) obj;
//
//					if (!node.hasRelationship(RelType.CONTAINS, Direction.INCOMING)) {
//
//						filteredResults.add(node);
//					}
//
//				}
//
//			}

//			// save raw result count
//			int resultCountBeforePaging = filteredResults.size();
			
			// set full result list
			//webSocketData.setResult(PagingHelper.subList(result.getResults(), pageSize, page, null));
			webSocketData.setResult(result.getResults());
//			webSocketData.setRawResultCount(resultCountBeforePaging);

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

		return "SEARCH";

	}

}
