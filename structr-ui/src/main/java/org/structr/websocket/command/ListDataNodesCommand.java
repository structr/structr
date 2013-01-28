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

import java.util.*;

import org.structr.rest.resource.PagingHelper;
import org.structr.web.entity.DataNode;

/**
 *
 * @author Axel Morgner
 */
public class ListDataNodesCommand extends AbstractCommand {

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext  = getWebSocket().getSecurityContext();
		String rawType                         = (String) webSocketData.getNodeData().get("type");
		String key                             = (String) webSocketData.getNodeData().get("key");
		Class type                             = EntityContext.getEntityClassForRawType(rawType);
		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		Set<String> nodesWithChildren          = new HashSet();

		searchAttributes.add(Search.andExactType(type.getSimpleName()));

		final String sortOrder   = webSocketData.getSortOrder();
		final String sortKey     = webSocketData.getSortKey();
		final int pageSize       = webSocketData.getPageSize();
		final int page           = webSocketData.getPage();
		PropertyKey sortProperty = EntityContext.getPropertyKeyForJSONName(type, sortKey);

		try {

			// do search
			Result result                       = (Result) Services.command(securityContext, SearchNodeCommand.class).execute(true, false, searchAttributes, sortProperty,
								      "desc".equals(sortOrder));
			List<AbstractNode> filteredResults  = new LinkedList<AbstractNode>();
			List<? extends DataNode> resultList = result.getResults();

			// add only nodes without parent (root nodes)
			for (DataNode dataNode : resultList) {

				if (dataNode.getParent(key) == null) {

					filteredResults.add(dataNode);

					if (dataNode.hasChildren(key)) {

						nodesWithChildren.add(dataNode.getUuid());
					}

				}

			}

			// save raw result count
			int resultCountBeforePaging = filteredResults.size();

			// set full result list
			webSocketData.setResult(PagingHelper.subList(filteredResults, pageSize, page, null));
			webSocketData.setRawResultCount(resultCountBeforePaging);
			webSocketData.setNodesWithChildren(nodesWithChildren);

			// send only over local connection
			getWebSocket().send(webSocketData, true);
		} catch (FrameworkException fex) {

			fex.printStackTrace();

		}

	}

	@Override
	public String getCommand() {

		return "LIST_DATA_NODES";

	}

}
