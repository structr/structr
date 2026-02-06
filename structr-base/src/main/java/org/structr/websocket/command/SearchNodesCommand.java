/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.api.DatabaseService;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.web.function.UiFunction;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.*;

public class SearchNodesCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(SearchNodesCommand.class.getName());

	private static final String QUERY_STRING_KEY                 = "queryString";
	private static final String SEARCH_DOM_BOOL_KEY              = "searchDOM";
	private static final String SEARCH_FLOW_BOOL_KEY             = "searchFlow";
	private static final String SEARCH_SCHEMA_BOOL_KEY           = "searchSchema";

	static {
		StructrWebSocket.addCommand(SearchNodesCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final String queryString   = webSocketData.getNodeDataStringValue(QUERY_STRING_KEY);
		final boolean searchDOM    = webSocketData.getNodeDataBooleanValue(SEARCH_DOM_BOOL_KEY);
		final boolean searchFlow   = webSocketData.getNodeDataBooleanValue(SEARCH_FLOW_BOOL_KEY);
		final boolean searchSchema = webSocketData.getNodeDataBooleanValue(SEARCH_SCHEMA_BOOL_KEY);

		try {

			final List<GraphObject> result = executeSearch(queryString, searchDOM, searchSchema, searchFlow);

			int resultCountBeforePaging = result.size();
			webSocketData.setRawResultCount(resultCountBeforePaging);

			webSocketData.setResult(result);

			getWebSocket().send(webSocketData, true);

		} catch (Throwable t) {

			logger.warn("Exception occurred", t);
			getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "SEARCH_NODES";
	}

	public static List<GraphObject> executeSearch(final String query, final boolean searchDOM, final boolean searchSchema, final boolean searchFlow) {

		final DatabaseService db       = Services.getInstance().getDatabaseService();
		final List<Object> rawResults  = db.globalSearch(query, searchDOM, searchSchema, searchFlow);

		return flatten(rawResults);
	}

	// ----- private methods -----
	private static List<GraphObject> flatten(final List src) {

		final List<GraphObject> list = new LinkedList<>();

		flatten(list, src);

		return list;
	}

	private static void flatten(final List<GraphObject> list, final Object o) {

		if (o instanceof Iterable) {

			for (final Object obj : (Iterable)o) {

				flatten(list, obj);
			}

		} else {

			list.add((GraphObject) UiFunction.toGraphObject(o, 1));
		}
	}
}
