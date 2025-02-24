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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

/**
 * Websocket command to a list of nodes by type.
 *
 * Supports paging and ignores thumbnails.
 */
public class GetByTypeCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(GetByTypeCommand.class.getName());

	private static final String INCLUDE_HIDDEN_KEY       = "includeHidden";
	private static final String PROPERTIES_KEY           = "properties";
	private static final String TYPE_KEY                 = "type";

	static {

		StructrWebSocket.addCommand(GetByTypeCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		try {

			final SecurityContext securityContext  = getWebSocket().getSecurityContext();
			final String rawType                   = webSocketData.getNodeDataStringValue(TYPE_KEY);
			final String properties                = webSocketData.getNodeDataStringValue(PROPERTIES_KEY);
			final boolean includeHidden            = webSocketData.getNodeDataBooleanValue(INCLUDE_HIDDEN_KEY);
			final Traits traits                    = Traits.of(rawType);

			if (traits == null) {
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


			final Query query = StructrApp.getInstance(securityContext).nodeQuery(rawType).includeHidden(includeHidden);

			if (sortKey != null) {

				final PropertyKey sortProperty = traits.key(sortKey);
				if (sortProperty != null) {

					query.sort(sortProperty, "desc".equals(sortOrder));
				}
			}

			// for image lists, suppress thumbnails
			if (traits.contains(StructrTraits.IMAGE)) {
				query.and(traits.key("isThumbnail"), false);
			}

			webSocketData.setResult(query.getResultStream());

			// send only over local connection
			getWebSocket().send(webSocketData, true);

		} catch (FrameworkException fex) {

			logger.warn("Exception occured", fex);
			getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

		}
	}

	@Override
	public String getCommand() {
		return "GET_BY_TYPE";
	}
}
