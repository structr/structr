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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 */
public class SearchCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(SearchCommand.class.getName());

	private static final String SEARCH_STRING_KEY = "searchString";
	private static final String EXACT_KEY         = "exact";
	private static final String REST_QUERY_KEY    = "restQuery";
	private static final String CYPHER_QUERY_KEY  = "cypherQuery";
	private static final String CYPHER_PARAMS_KEY = "cypherParams";
	private static final String TYPE_KEY          = "type";

	static {

		StructrWebSocket.addCommand(SearchCommand.class);

	}

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final SecurityContext securityContext = getWebSocket().getSecurityContext();
		final String searchString = webSocketData.getNodeDataStringValue(SEARCH_STRING_KEY);

		String typeString = null;
		Boolean exactSearch = null;

		if (searchString != null) {

			typeString  = webSocketData.getNodeDataStringValue(TYPE_KEY);
			exactSearch = webSocketData.getNodeDataBooleanValue(EXACT_KEY);
		}

		final String restQuery    = webSocketData.getNodeDataStringValue(REST_QUERY_KEY);
		final String cypherQuery  = webSocketData.getNodeDataStringValue(CYPHER_QUERY_KEY);
		final String paramString  = webSocketData.getNodeDataStringValue(CYPHER_PARAMS_KEY);
		final int pageSize        = webSocketData.getPageSize();
		final int page            = webSocketData.getPage();

		Traits type = null;
		if (typeString != null) {

			type = Traits.of(typeString);
		}

		if (searchString == null) {

			if (cypherQuery != null) {

				try {
					Map<String, Object> obj = null;

					if (StringUtils.isNoneBlank(paramString)) {

						obj = new Gson().fromJson(paramString, Map.class);

					}

					final List<GraphObject> result = flatten(Iterables.toList(StructrApp.getInstance(securityContext).query(cypherQuery, obj)));

					int resultCountBeforePaging = result.size();
					webSocketData.setRawResultCount(resultCountBeforePaging);

					if (page != 0 && pageSize != 0) {
						webSocketData.setResult(result.subList((page-1) * pageSize, Math.min(page * pageSize, resultCountBeforePaging)));
					} else {
						webSocketData.setResult(result);
					}

					getWebSocket().send(webSocketData, true);

					return;

				} catch (JsonSyntaxException | FrameworkException ex) {

					logger.warn("Exception occured", ex);
					getWebSocket().send(MessageBuilder.status().code(400).message(ex.getMessage()).build(), true);
					return;

				}
			}

			if (restQuery != null) {
				throw new UnsupportedOperationException("Using restQuery in the SEARCH command is deprecated.");
			}
		}

		final String sortOrder         = webSocketData.getSortOrder();
		final String sortKey           = (webSocketData.getSortKey() == null ? "name" : webSocketData.getSortKey());
		final PropertyKey sortProperty = type.key(sortKey);
		final QueryGroup query         = StructrApp.getInstance(securityContext).nodeQuery().includeHidden().sort(sortProperty, "desc".equals(sortOrder)).and();

		query.key(type.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), searchString, exactSearch);

		if (type != null) {
			query.types(type);
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
		return "SEARCH";
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
