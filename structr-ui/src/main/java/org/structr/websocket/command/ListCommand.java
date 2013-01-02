/*
 *  Copyright (C) 2010-2013 Axel Morgner
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
import org.structr.web.common.RelationshipHelper;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import org.structr.core.entity.Image;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class ListCommand extends AbstractCommand {

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext  = getWebSocket().getSecurityContext();
		String rawType                         = (String) webSocketData.getNodeData().get("type");
		Class type                             = EntityContext.getEntityClassForRawType(rawType);
		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		Set<String> nodesWithChildren          = new HashSet<String>();

//              searchAttributes.addAll(Search.andExactTypeAndSubtypes(CaseHelper.toUpperCamelCase(type)));
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
			Result result = (Result) Services.command(securityContext, SearchNodeCommand.class).execute(true, false, searchAttributes, sortProperty, "desc".equals(sortOrder), pageSize,
						page);
			List<? extends GraphObject> resultList = result.getResults();

			// determine which of the nodes have children
			for (GraphObject obj : resultList) {

				if (obj instanceof AbstractNode) {

					AbstractNode node = (AbstractNode) obj;

					if (RelationshipHelper.hasChildren(node, node.getUuid())) {

						nodesWithChildren.add(node.getUuid());
					}

				}

			}

			// Determine children in this resource
			webSocketData.setNodesWithChildren(nodesWithChildren);

			// set full result list
			webSocketData.setResult(resultList);
			webSocketData.setRawResultCount(result.getRawResultCount());

//                      }
			// send only over local connection
			getWebSocket().send(webSocketData, true);
		} catch (FrameworkException fex) {

			fex.printStackTrace();

		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "LIST";

	}

}
