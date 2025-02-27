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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Websocket command to retrieve nodes of a given type which are on root level,
 * i.e. not children of another node.
 *
 * To get all nodes of a certain type, see the {@link GetCommand}.
 */
public class QueryCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(QueryCommand.class.getName());

	static {

		StructrWebSocket.addCommand(QueryCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(false);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();

		final String rawType                  = webSocketData.getNodeDataStringValue("type");
		final String andProperties            = webSocketData.getNodeDataStringValue("properties");
		final String notProperties            = webSocketData.getNodeDataStringValue("notProperties");
		final Boolean exact                   = webSocketData.getNodeDataBooleanValue("exact");
		final String customView               = webSocketData.getNodeDataStringValue("customView");
		final Traits type                     = Traits.of(rawType);

		if (type == null) {
			getWebSocket().send(MessageBuilder.status().code(404).message("Type " + rawType + " not found").build(), true);
			return;
		}

		if (customView != null) {
			securityContext.setCustomView(StringUtils.split(customView, ","));
		}

		final String sortKey           = webSocketData.getSortKey();
		final int pageSize             = webSocketData.getPageSize();
		final int page                 = webSocketData.getPage();

		final Query query = StructrApp.getInstance(securityContext)
			.nodeQuery(rawType)
			.page(page)
			.pageSize(pageSize);

		if (sortKey != null) {

			final PropertyKey sortProperty = type.key(sortKey);
			final String sortOrder         = webSocketData.getSortOrder();

			query.sort(sortProperty, "desc".equals(sortOrder));
		}

		if (andProperties != null) {

			try {
				final Gson gson                          = new GsonBuilder().create();

				final Map<String, Object> andQuerySource = gson.fromJson(andProperties, new TypeToken<Map<String, Object>>() {}.getType());
				final PropertyMap andQueryMap            = PropertyMap.inputTypeToJavaType(securityContext, rawType, andQuerySource);

				final Map<String, Object> notQuerySource = gson.fromJson(notProperties, new TypeToken<Map<String, Object>>() {}.getType());
				final PropertyMap notQueryMap            = PropertyMap.inputTypeToJavaType(securityContext, rawType, notQuerySource);

				final boolean inexactQuery               = exact != null && exact == false;

				// add properties to query
				for (final Entry<PropertyKey, Object> entry : andQueryMap.entrySet()) {
					query.and(entry.getKey(), entry.getValue(), !inexactQuery);
				}

				// "not" properties
				for (final Entry<PropertyKey, Object> entry : notQueryMap.entrySet()) {
					query.not().and(entry.getKey(), entry.getValue(), !inexactQuery);
				}

			} catch (FrameworkException fex) {

				logger.warn("Exception occured", fex);
				getWebSocket().send(MessageBuilder.status().code(fex.getStatus()).message(fex.getMessage()).build(), true);

				return;
			}
		}

		try {

			// set full result list
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
		return "QUERY";
	}
}
