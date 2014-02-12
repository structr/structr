/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
import org.structr.web.entity.Image;
import org.structr.common.PagingHelper;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;

//~--- JDK imports ------------------------------------------------------------

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.schema.SchemaHelper;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to retrieve nodes of a given type which are on root level,
 * i.e. not children of another node.
 * 
 * To get all nodes of a certain type, see the {@link GetCommand}.
 * 
 * @author Christian Morgner
 * @author Axel Morgner
 */
public class ListCommand extends AbstractCommand {
	
	private static final Logger logger = Logger.getLogger(ListCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ListCommand.class);

	}

	@Override
	public void processMessage(WebSocketMessage webSocketData) {

		final SecurityContext securityContext  = getWebSocket().getSecurityContext();
		String rawType                         = (String) webSocketData.getNodeData().get("type");
		Class type                             = SchemaHelper.getEntityClassForRawType(rawType);
		
		if (type == null) {
			getWebSocket().send(MessageBuilder.status().code(404).message("Type " + rawType + " not found").build(), true);
			return;
		}

		final String sortOrder         = webSocketData.getSortOrder();
		final String sortKey           = webSocketData.getSortKey();
		final int pageSize             = webSocketData.getPageSize();
		final int page                 = webSocketData.getPage();
		final PropertyKey sortProperty = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, sortKey);
		final Query query              = StructrApp.getInstance(securityContext).nodeQuery(type).includeDeletedAndHidden().sort(sortProperty).order("desc".equals(sortOrder));
		
		// for image lists, suppress thumbnails
		if (type.equals(Image.class)) {
			
			query.and(Image.isThumbnail, false);
		}
		

		try {

			// do search
			List<AbstractNode> filteredResults     = new LinkedList<>();
			List<? extends GraphObject> resultList = query.getAsList();

			// determine which of the nodes have a parent
			for (GraphObject obj : resultList) {

				if (obj instanceof AbstractNode) {

					AbstractNode node = (AbstractNode) obj;
					
					boolean hasParent = false;
					for (AbstractRelationship rel : node.getIncomingRelationships()) {
						if ("CONTAINS".equals(rel.getType())) {
							hasParent = true;
							break;
						}
					}
					
					if (!hasParent) {
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

		return "LIST";

	}

}
