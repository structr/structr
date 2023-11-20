/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.Image;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.WebSocketMessage;

import java.util.LinkedList;
import java.util.List;
import org.structr.common.helper.PagingHelper;

//~--- classes ----------------------------------------------------------------

/**
 *
 *
 */
public class ListFilesCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(ListFilesCommand.class.getName());

	static {

		StructrWebSocket.addCommand(ListFilesCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final SecurityContext securityContext  = getWebSocket().getSecurityContext();
		final String rawType                   = webSocketData.getNodeDataStringValue("type");
		final Class type                       = SchemaHelper.getEntityClassForRawType(rawType);
		final String sortOrder                 = webSocketData.getSortOrder();
		final String sortKey                   = webSocketData.getSortKey();
		final int pageSize                     = webSocketData.getPageSize();
		final int page                         = webSocketData.getPage();
		final PropertyKey sortProperty         = StructrApp.key(type, sortKey);
		final Query query                      = StructrApp.getInstance(securityContext).nodeQuery(type).includeHidden().sort(sortProperty, "desc".equals(sortOrder));

		// for image lists, suppress thumbnails
		if (type.equals(Image.class)) {

			query.and(StructrApp.key(Image.class, "isThumbnail"), false);
		}

		try {

			// do search
			List<NodeInterface> filteredResults    = new LinkedList();
			List<? extends GraphObject> resultList = query.getAsList();

			// add only root folders to the list
			for (GraphObject obj : resultList) {

				if (obj instanceof AbstractFile) {

					AbstractFile node = (AbstractFile) obj;

					if (node.getParent() == null) {

						filteredResults.add(node);
					}

				}

			}

			// save raw result count
			int resultCountBeforePaging = filteredResults.size();

			// set full result list
			webSocketData.setResult(PagingHelper.subList(filteredResults, pageSize, page));
			webSocketData.setRawResultCount(resultCountBeforePaging);

			// send only over local connection
			getWebSocket().send(webSocketData, true);

		} catch (FrameworkException fex) {

			logger.warn("", fex);

		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getCommand() {

		return "LIST_FILES";

	}

}
