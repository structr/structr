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

import org.structr.common.CaseHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;
import org.structr.web.common.RelationshipHelper;
import org.structr.websocket.message.WebSocketMessage;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class ListCommand extends AbstractCommand {

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext  = getWebSocket().getSecurityContext();
		String type                            = (String) webSocketData.getNodeData().get("type");
		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		Set<String> nodesWithChildren          = new HashSet<String>();

//              searchAttributes.addAll(Search.andExactTypeAndSubtypes(CaseHelper.toUpperCamelCase(type)));
		searchAttributes.add(Search.andExactType(CaseHelper.toUpperCamelCase(type)));

		AbstractNode topNode   = null;
		boolean includeDeleted = false;
		boolean publicOnly     = false;

		try {

			// do search
			List<GraphObject> results = (List<GraphObject>) Services.command(securityContext, SearchNodeCommand.class).execute(topNode, includeDeleted, publicOnly, searchAttributes);

			// sorting
			if (webSocketData.getSortKey() != null) {

				final String sortOrder             = webSocketData.getSortOrder();
				final String sortKey               = webSocketData.getSortKey();
				Comparator<GraphObject> comparator = null;

				try {

					if ("desc".equalsIgnoreCase(sortOrder)) {

						comparator = new Comparator<GraphObject>() {

							@Override
							public int compare(GraphObject n1, GraphObject n2) {

								Comparable c1 = (Comparable) n1.getProperty(sortKey);
								Comparable c2 = (Comparable) n2.getProperty(sortKey);

								return (c2.compareTo(c1));
							}
						};

					} else {

						comparator = new Comparator<GraphObject>() {

							@Override
							public int compare(GraphObject n1, GraphObject n2) {

								Comparable c1 = (Comparable) n1.getProperty(sortKey);
								Comparable c2 = (Comparable) n2.getProperty(sortKey);

								return (c1.compareTo(c2));
							}
						};

					}

					if (comparator != null) {

						Collections.sort(results, comparator);

					}

				} catch (Throwable t) {

					// todo: logging
				}

			}

			for (GraphObject obj : results) {

				AbstractNode node = (AbstractNode) obj;

				if ((obj instanceof AbstractNode) && RelationshipHelper.hasChildren(node, node.getUuid())) {

					nodesWithChildren.add(node.getUuid());

				}

			}

			// Determine children in this resource
			webSocketData.setNodesWithChildren(nodesWithChildren);

			// paging
			if (webSocketData.getPageSize() > 0) {

				int pageSize    = webSocketData.getPageSize();
				int page        = webSocketData.getPage();
				int resultCount = results.size();
				int fromIndex   = Math.min(resultCount, Math.max(0, (page - 1) * pageSize));
				int toIndex     = Math.min(resultCount, page * pageSize);

				// set paged results
				webSocketData.setResult(results.subList(fromIndex, toIndex));

			} else {

				// set full result list
				webSocketData.setResult(results);
			}

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
