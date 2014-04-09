/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyKey;
import org.structr.websocket.message.WebSocketMessage;
import org.structr.web.entity.Image;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;

//~--- JDK imports ------------------------------------------------------------
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Result;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.schema.SchemaHelper;
import org.structr.web.entity.AbstractFile;

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

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final Map<String, Object> properties = webSocketData.getNodeData();
		final String rawType = (String) properties.get("type");
		final boolean rootOnly = Boolean.TRUE.equals((Boolean) properties.get("rootOnly"));
		Class type = SchemaHelper.getEntityClassForRawType(rawType);

		if (type == null) {
			getWebSocket().send(MessageBuilder.status().code(404).message("Type " + rawType + " not found").build(), true);
			return;
		}

		final String sortOrder = webSocketData.getSortOrder();
		final String sortKey = webSocketData.getSortKey();
		final int pageSize = webSocketData.getPageSize();
		final int page = webSocketData.getPage();
		final PropertyKey sortProperty = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, sortKey);
		final Query query = StructrApp.getInstance(securityContext).nodeQuery(type).includeDeletedAndHidden().sort(sortProperty).order("desc".equals(sortOrder)).page(page).pageSize(pageSize);

		if (rootOnly) {

			query.and(AbstractFile.hasParent, false);
		}

		// for image lists, suppress thumbnails
		if (type.equals(Image.class)) {

			query.and(Image.isThumbnail, false);
		}

		try {

			// do search
			final Result result = query.getResult();

			// save raw result count
			int resultCountBeforePaging = result.getRawResultCount(); // filteredResults.size();

			// set full result list
			webSocketData.setResult(result.getResults());
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
