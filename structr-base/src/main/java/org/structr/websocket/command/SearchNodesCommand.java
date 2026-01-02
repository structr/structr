/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.QueryGroup;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.websocket.StructrWebSocket;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 */
public class SearchNodesCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(SearchNodesCommand.class.getName());

	private static final String SEARCH_STRING_KEY = "searchString";
	private static final String SEARCH_DOM_BOOL_KEY = "searchDOM";
	private static final String SEARCH_FLOW_BOOL_KEY = "searchFlow";
	private static final String SEARCH_SCHEMA_BOOL_KEY = "searchSchema";

	static {
		StructrWebSocket.addCommand(SearchNodesCommand.class);
	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final String searchString = webSocketData.getNodeDataStringValue(SEARCH_STRING_KEY);

		final boolean searchDOM    = webSocketData.getNodeDataBooleanValue(SEARCH_DOM_BOOL_KEY);
		final boolean searchFlow   = webSocketData.getNodeDataBooleanValue(SEARCH_FLOW_BOOL_KEY);
		final boolean searchSchema = webSocketData.getNodeDataBooleanValue(SEARCH_SCHEMA_BOOL_KEY);

		try {

			final Map<String, Object> obj = Map.of("searchString", searchString);

			final ArrayList<String> labels = new ArrayList<>();
			if (searchDOM)    { labels.add("n:DOMNode"); }
			if (searchFlow)   { labels.add("n:FlowNode"); }
			if (searchSchema) { labels.add("n:SchemaMethod"); }

			if (labels.size() > 0) {

				final String cypherQuery = """
					MATCH (n)
						WHERE (%s)
					AND
						ANY(prop IN KEYS(n) WHERE n[prop] CONTAINS $searchString)
					WITH n, [prop IN KEYS(n) WHERE n[prop] CONTAINS $searchString | prop] AS keys
					RETURN {
						id: n.id,
						type: n.type,
						name: n.name,
						keys: keys
					}
					""".formatted(String.join(" OR ", labels));


				final List<GraphObject> result = flatten(Iterables.toList(StructrApp.getInstance(securityContext).query(cypherQuery, obj)));

				int resultCountBeforePaging = result.size();
				webSocketData.setRawResultCount(resultCountBeforePaging);

				webSocketData.setResult(result);
			}

			getWebSocket().send(webSocketData, true);

		} catch (FrameworkException ex) {

			logger.warn("Exception occurred", ex);
			getWebSocket().send(MessageBuilder.status().code(400).message(ex.getMessage()).build(), true);
		}
	}

	@Override
	public String getCommand() {
		return "SEARCH_NODES";
	}

	// ----- private methods -----
	private List<GraphObject> flatten(final List src) {

		final List<GraphObject> list = new LinkedList<>();

		flatten(list, src);

		return list;
	}

	private void flatten(final List<GraphObject> list, final Object o) {

		if (o instanceof Iterable) {

			for (final Object obj : (Iterable)o) {

				flatten(list, obj);
			}

		} else if (o instanceof GraphObject) {

			list.add((GraphObject)o);

		} else {

			logger.warn("Unable to handle object of type {}, ignoring.", o.getClass().getName());
		}
	}
}
