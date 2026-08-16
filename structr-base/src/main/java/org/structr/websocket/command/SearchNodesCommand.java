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
import org.structr.core.GraphObjectMap;
import org.structr.core.Services;
import org.structr.core.traits.StructrTraits;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.*;

public class SearchNodesCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(SearchNodesCommand.class.getName());

	private static final String SEARCH_STRING_KEY    = "searchString";
	private static final String SEARCH_CONTEXTS_KEY  = "searchContexts";

	public static final String SEARCH_CONTEXT_DOM            = "dom";
	public static final String SEARCH_CONTEXT_FLOWS          = "flows";
	public static final String SEARCH_CONTEXT_SCHEMA         = "schema";
	public static final String SEARCH_CONTEXT_FILES          = "files";
	public static final String SEARCH_CONTEXT_LOCALIZATIONS  = "localizations";
	public static final String SEARCH_CONTEXT_MAIL_TEMPLATES = "mail-templates";

	@Override
	public void processMessage(final WebSocketMessage webSocketData) {

		setDoTransactionNotifications(false);

		final String searchString         = webSocketData.getNodeDataStringValue(SEARCH_STRING_KEY);
		final List<String> searchContexts = webSocketData.getNodeDataStringList(SEARCH_CONTEXTS_KEY);

		try {

			final List<GraphObject> result = executeSearch(searchString, searchContexts);
			int resultCountBeforePaging = result.size();

			webSocketData.setRawResultCount(resultCountBeforePaging);

			webSocketData.setResult(result);

			getWebSocket().send(webSocketData, true);

		} catch (UnsupportedOperationException uso) {

			logger.warn("{}", uso.getMessage());
			getWebSocket().send(MessageBuilder.status().code(400).message(uso.getMessage()).build(), true);

		} catch (Throwable t) {

			logger.warn("Exception occurred", t);
			getWebSocket().send(MessageBuilder.status().code(400).message(t.getMessage()).build(), true);
		}
	}

	@Override
	public String getCommand() {

		return "SEARCH_NODES";
	}

	public static List<GraphObject> executeSearch(final String searchString, List<String> searchContexts) {

		final DatabaseService db = Services.getInstance().getDatabaseService();
		final Set<String> types  = new LinkedHashSet<>();

		if (searchContexts.contains(SEARCH_CONTEXT_DOM))            { types.add("((n:DOMNode or n:Site or n:ActionMapping or n:ParameterMapping) AND NOT n:ShadowDocument)"); }
		if (searchContexts.contains(SEARCH_CONTEXT_FLOWS))          { types.add("(n:FlowNode)"); }
		if (searchContexts.contains(SEARCH_CONTEXT_SCHEMA))         { types.add("(n:AbstractSchemaNode OR n:SchemaReloadingNode)"); }
		if (searchContexts.contains(SEARCH_CONTEXT_FILES))          { types.add("(n:AbstractFile)"); }
		if (searchContexts.contains(SEARCH_CONTEXT_LOCALIZATIONS))  { types.add("(n:Localization)"); }
		if (searchContexts.contains(SEARCH_CONTEXT_MAIL_TEMPLATES)) { types.add("(n:MailTemplate)"); }

		final List<Map<String, Object>> rawResults = db.globalSearch(types, searchString);
		final List<GraphObject> results            = new LinkedList<>();

		for (final Map<String, Object> result : rawResults) {

			final Map<String, Object> tmp = new LinkedHashMap<>(result);
			final List<String> labels     = (List) result.get("labels");

			if (labels.contains(StructrTraits.DOM_NODE)) {

				tmp.put("isDOMElement", true);
			}

			if (labels.contains(StructrTraits.ACTION_MAPPING)) {

				tmp.put("isActionMapping", true);
			}

			if (labels.contains(StructrTraits.PARAMETER_MAPPING)) {

				tmp.put("isParameterMapping", true);
			}

			if (labels.contains(StructrTraits.SITE)) {

				tmp.put("isSiteElement", true);
			}

			if (labels.contains(StructrTraits.ABSTRACT_SCHEMA_NODE) || labels.contains(StructrTraits.SCHEMA_RELOADING_NODE)) {

				tmp.put("isSchemaElement", true);
			}

			if (labels.contains(StructrTraits.FILE) || labels.contains(StructrTraits.FOLDER)) {

				tmp.put("isFilesystemElement", true);
			}

			if (labels.contains(StructrTraits.LOCALIZATION)) {

				tmp.put("isLocalizationElement", true);
			}

			if (labels.contains(StructrTraits.MAIL_TEMPLATE)) {

				tmp.put("isMailTemplateElement", true);
			}

			results.add(GraphObjectMap.fromMap(tmp));
		}

		return results;
	}
}
