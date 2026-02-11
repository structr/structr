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

import groovyjarjarantlr4.v4.misc.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.Services;
import org.structr.core.property.GenericProperty;
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

		final DatabaseService db = Services.getInstance().getDatabaseService();
		final Set<String> types  = new LinkedHashSet<>();

		if (searchDOM)    { types.add("(n:DOMNode AND NOT n:ShadowDocument)"); }
		if (searchFlow)   { types.add("n:FlowNode"); }
		if (searchSchema) { types.add("(n:AbstractSchemaNode OR n:SchemaReloadingNode)"); }

		final List<Map<String, Object>> rawResults = db.globalSearch(types, query);
		final List<GraphObject> results            = new LinkedList<>();

		for (final Map<String, Object> result : rawResults) {

			final Map<String, Object> tmp = new LinkedHashMap<>(result);
			final List<String> labels     = (List) result.get("labels");

			if (labels.contains("DOMNode")) {
				tmp.put("isDOMElement", true);
			}

			if (labels.contains("AbstractSchemaNode") || labels.contains("SchemaReloadingNode")) {
				tmp.put("isSchemaElement", true);
			}

			results.add(GraphObjectMap.fromMap(tmp));
		}

		return results;
	}
}
