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
import org.structr.web.entity.Image;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.property.PropertyKey;
import org.structr.rest.resource.PagingHelper;
import org.structr.web.common.RelType;
import org.structr.websocket.message.WebSocketMessage;
import org.structr.websocket.StructrWebSocket;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class ListFilesCommand extends AbstractCommand {

	static {

		StructrWebSocket.addCommand(ListFilesCommand.class);

	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {
		

		final SecurityContext securityContext  = getWebSocket().getSecurityContext();
		String rawType                         = (String) webSocketData.getNodeData().get("type");
		Class type                             = EntityContext.getEntityClassForRawType(rawType);
		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

		searchAttributes.add(Search.andExactType(type.getSimpleName()));
		
		// for image lists, suppress thumbnails
		if (type.equals(Image.class)) {
			searchAttributes.add(Search.andExactProperty(Image.isThumbnail, false));
		}

		final String sortOrder   = webSocketData.getSortOrder();
		final String sortKey     = webSocketData.getSortKey();
		final int pageSize       = webSocketData.getPageSize();
		final int page           = webSocketData.getPage();
		PropertyKey sortProperty = EntityContext.getPropertyKeyForJSONName(type, sortKey);

		try {

			// do search
			Result result = (Result) Services.command(securityContext, SearchNodeCommand.class).execute(true, false, searchAttributes, sortProperty, "desc".equals(sortOrder));
			List<AbstractNode> filteredResults     = new LinkedList<AbstractNode>();
			List<? extends GraphObject> resultList = result.getResults();

			// add only root folders to the list
			for (GraphObject obj : resultList) {

				if (obj instanceof AbstractNode) {

					AbstractNode node = (AbstractNode) obj;

					if (!node.hasRelationship(RelType.CONTAINS, Direction.INCOMING)) {

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

			fex.printStackTrace();

		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "LIST_FILES";

	}

}
