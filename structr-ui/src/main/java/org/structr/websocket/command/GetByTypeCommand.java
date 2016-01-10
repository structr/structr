/**
 * Copyright (C) 2010-2016 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PagingHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaHelper;
import org.structr.web.entity.Image;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

//~--- classes ----------------------------------------------------------------

/**
 * Websocket command to a list of nodes by type.
 * 
 * Supports paging and ignores thumbnails.
 *
 *
 *
 */
public class GetByTypeCommand extends AbstractCommand {
	
	private static final Logger logger = Logger.getLogger(GetByTypeCommand.class.getName());
	
	static {
		
		StructrWebSocket.addCommand(GetByTypeCommand.class);
		
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		final SecurityContext securityContext  = getWebSocket().getSecurityContext();
		final String rawType                   = (String) webSocketData.getNodeData().get("type");
		final String properties                = (String) webSocketData.getNodeData().get("properties");
		final boolean includeDeletedAndHidden  = (Boolean) webSocketData.getNodeData().get("includeDeletedAndHidden");
		final Class type                       = SchemaHelper.getEntityClassForRawType(rawType);

		if (type == null) {
			getWebSocket().send(MessageBuilder.status().code(404).message("Type " + rawType + " not found").build(), true);
			return;
		}

		if (properties != null) {
			securityContext.setCustomView(StringUtils.split(properties, ","));
		}
		
		final String sortOrder   = webSocketData.getSortOrder();
		final String sortKey     = webSocketData.getSortKey();
		final int pageSize       = webSocketData.getPageSize();
		final int page           = webSocketData.getPage();
		PropertyKey sortProperty = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, sortKey);

		
		final Query query = StructrApp.getInstance(securityContext).nodeQuery(type).includeDeletedAndHidden(includeDeletedAndHidden).sort(sortProperty).order("desc".equals(sortOrder));

		// for image lists, suppress thumbnails
		if (type.equals(Image.class)) {
			query.and(Image.isThumbnail, false);
		}


		try {

			// do search
			Result result = query.getResult();

			// save raw result count
			int resultCountBeforePaging = result.size();

			// set full result list
			webSocketData.setResult(PagingHelper.subList(result.getResults(), pageSize, page, null));
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
		return "GET_BY_TYPE";
	}
}
